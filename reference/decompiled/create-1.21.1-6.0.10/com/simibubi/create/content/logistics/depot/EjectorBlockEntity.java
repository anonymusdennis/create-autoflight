package com.simibubi.create.content.logistics.depot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.IntAttached;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class EjectorBlockEntity extends KineticBlockEntity {
   List<IntAttached<ItemStack>> launchedItems;
   ScrollValueBehaviour maxStackSize;
   DepotBehaviour depotBehaviour;
   EntityLauncher launcher = new EntityLauncher(1, 0);
   LerpedFloat lidProgress = LerpedFloat.linear().startWithValue(1.0);
   boolean powered;
   boolean launch;
   EjectorBlockEntity.State state = EjectorBlockEntity.State.RETRACTING;
   @Nullable
   Pair<Vec3, BlockPos> earlyTarget;
   float earlyTargetTime;
   int scanCooldown;
   ItemStack trackedItem;

   public EjectorBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
      this.launchedItems = new ArrayList<>();
      this.powered = false;
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.WEIGHTED_EJECTOR.get(), (be, context) -> be.depotBehaviour.itemHandler);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      behaviours.add(this.depotBehaviour = new DepotBehaviour(this));
      this.maxStackSize = new ScrollValueBehaviour(CreateLang.translateDirect("weighted_ejector.stack_size"), this, new EjectorBlockEntity.EjectorSlot())
         .between(0, 64)
         .withFormatter(i -> i == 0 ? "*" : String.valueOf(i));
      behaviours.add(this.maxStackSize);
      this.depotBehaviour.maxStackSize = () -> this.maxStackSize.getValue();
      this.depotBehaviour.canAcceptItems = () -> this.state == EjectorBlockEntity.State.CHARGED;
      this.depotBehaviour.canFunnelsPullFrom = side -> side != this.getFacing();
      this.depotBehaviour.enableMerging();
      this.depotBehaviour.addSubBehaviours(behaviours);
   }

   @Override
   public void initialize() {
      super.initialize();
      this.updateSignal();
   }

   public void activate() {
      this.launch = true;
      this.nudgeEntities();
   }

   protected boolean cannotLaunch() {
      return this.state != EjectorBlockEntity.State.CHARGED && (!this.level.isClientSide || this.state != EjectorBlockEntity.State.LAUNCHING);
   }

   public void activateDeferred() {
      if (!this.cannotLaunch()) {
         Direction facing = this.getFacing();
         List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, new AABB(this.worldPosition).inflate(-0.0625, 0.0, -0.0625));
         boolean doLogic = !this.level.isClientSide || this.isVirtual();
         if (doLogic) {
            this.launchItems();
         }

         for (Entity entity : entities) {
            boolean isPlayerEntity = entity instanceof Player;
            if (entity.isAlive()
               && !(entity instanceof ItemEntity)
               && !(entity instanceof PackageEntity)
               && entity.getPistonPushReaction() != PushReaction.IGNORE) {
               entity.setOnGround(false);
               if (isPlayerEntity == this.level.isClientSide) {
                  entity.setPos(
                     (double)((float)this.worldPosition.getX() + 0.5F),
                     (double)(this.worldPosition.getY() + 1),
                     (double)((float)this.worldPosition.getZ() + 0.5F)
                  );
                  this.launcher.applyMotion(entity, facing);
                  if (isPlayerEntity) {
                     Player playerEntity = (Player)entity;
                     if (this.launcher.getHorizontalDistance() * this.launcher.getHorizontalDistance()
                           + this.launcher.getVerticalDistance() * this.launcher.getVerticalDistance()
                        >= 625) {
                        CatnipServices.NETWORK.sendToServer(new EjectorAwardPacket(this.worldPosition));
                     }

                     if (playerEntity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElytraItem) {
                        playerEntity.setXRot(-35.0F);
                        playerEntity.setYRot(facing.toYRot());
                        playerEntity.setDeltaMovement(playerEntity.getDeltaMovement().scale(0.75));
                        this.deployElytra(playerEntity);
                        CatnipServices.NETWORK.sendToServer(new EjectorElytraPacket(this.worldPosition));
                     }
                  }
               }
            }
         }

         if (doLogic) {
            this.lidProgress.chase(1.0, 0.8F, Chaser.EXP);
            this.state = EjectorBlockEntity.State.LAUNCHING;
            if (!this.level.isClientSide) {
               this.level.playSound(null, this.worldPosition, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.35F, 1.0F);
               this.level.playSound(null, this.worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.1F, 1.4F);
            }
         }
      }
   }

   public void deployElytra(Player playerEntity) {
      EjectorBlockEntity.EntityHack.setElytraFlying(playerEntity);
   }

   protected void launchItems() {
      ItemStack heldItemStack = this.depotBehaviour.getHeldItemStack();
      Direction funnelFacing = this.getFacing().getOpposite();
      if (AbstractFunnelBlock.getFunnelFacing(this.level.getBlockState(this.worldPosition.above())) == funnelFacing) {
         DirectBeltInputBehaviour directOutput = this.getBehaviour(DirectBeltInputBehaviour.TYPE);
         if (this.depotBehaviour.heldItem != null) {
            ItemStack remainder = directOutput.tryExportingToBeltFunnel(heldItemStack, funnelFacing, false);
            if (remainder != null) {
               if (remainder.isEmpty()) {
                  this.depotBehaviour.removeHeldItem();
               } else if (remainder.getCount() != heldItemStack.getCount()) {
                  this.depotBehaviour.heldItem.stack = remainder;
               }
            }
         }

         Iterator<TransportedItemStack> iterator = this.depotBehaviour.incoming.iterator();

         while (iterator.hasNext()) {
            TransportedItemStack transportedItemStack = iterator.next();
            ItemStack stack = transportedItemStack.stack;
            ItemStack remainder = directOutput.tryExportingToBeltFunnel(stack, funnelFacing, false);
            if (remainder != null) {
               if (remainder.isEmpty()) {
                  iterator.remove();
               } else if (!ItemStack.isSameItem(remainder, stack)) {
                  transportedItemStack.stack = remainder;
               }
            }
         }

         ItemStackHandler outputs = this.depotBehaviour.processingOutputBuffer;

         for (int i = 0; i < outputs.getSlots(); i++) {
            ItemStack remainder = directOutput.tryExportingToBeltFunnel(outputs.getStackInSlot(i), funnelFacing, false);
            if (remainder != null) {
               outputs.setStackInSlot(i, remainder);
            }
         }
      } else {
         if (!this.level.isClientSide) {
            for (Direction d : Iterate.directions) {
               BlockState blockState = this.level.getBlockState(this.worldPosition.relative(d));
               if (blockState.getBlock() instanceof ObserverBlock && blockState.getValue(ObserverBlock.FACING) == d.getOpposite()) {
                  blockState.updateShape(d.getOpposite(), blockState, this.level, this.worldPosition.relative(d), this.worldPosition);
               }
            }
         }

         if (this.depotBehaviour.heldItem != null) {
            this.addToLaunchedItems(heldItemStack);
            this.depotBehaviour.removeHeldItem();
         }

         for (TransportedItemStack transportedItemStack : this.depotBehaviour.incoming) {
            this.addToLaunchedItems(transportedItemStack.stack);
         }

         this.depotBehaviour.incoming.clear();
         ItemStackHandler outputs = this.depotBehaviour.processingOutputBuffer;

         for (int ix = 0; ix < outputs.getSlots(); ix++) {
            ItemStack extractItem = outputs.extractItem(ix, 64, false);
            if (!extractItem.isEmpty()) {
               this.addToLaunchedItems(extractItem);
            }
         }
      }
   }

   protected boolean addToLaunchedItems(ItemStack stack) {
      if ((!this.level.isClientSide || this.isVirtual()) && this.trackedItem == null && this.scanCooldown == 0) {
         this.scanCooldown = (Integer)AllConfigs.server().kinetics.ejectorScanInterval.get();
         this.trackedItem = stack;
      }

      return this.launchedItems.add(IntAttached.withZero(stack));
   }

   protected Direction getFacing() {
      BlockState blockState = this.getBlockState();
      return !AllBlocks.WEIGHTED_EJECTOR.has(blockState) ? Direction.UP : (Direction)blockState.getValue(EjectorBlock.HORIZONTAL_FACING);
   }

   @Override
   public void tick() {
      super.tick();
      boolean doLogic = !this.level.isClientSide || this.isVirtual();
      EjectorBlockEntity.State prevState = this.state;
      float totalTime = Math.max(3.0F, (float)this.launcher.getTotalFlyingTicks());
      if (this.scanCooldown > 0) {
         this.scanCooldown--;
      }

      if (this.launch) {
         this.launch = false;
         this.activateDeferred();
      }

      Iterator<IntAttached<ItemStack>> iterator = this.launchedItems.iterator();

      while (iterator.hasNext()) {
         IntAttached<ItemStack> intAttached = iterator.next();
         boolean hit = false;
         if (intAttached.getSecond() == this.trackedItem) {
            hit = this.scanTrajectoryForObstacles((Integer)intAttached.getFirst());
         }

         float maxTime = this.earlyTarget != null ? Math.min(this.earlyTargetTime, totalTime) : totalTime;
         if (hit || intAttached.exceeds((int)maxTime)) {
            this.placeItemAtTarget(doLogic, maxTime, intAttached);
            iterator.remove();
         }

         intAttached.increment();
      }

      if (this.state == EjectorBlockEntity.State.LAUNCHING) {
         this.lidProgress.chase(1.0, 0.8F, Chaser.EXP);
         this.lidProgress.tickChaser();
         if (this.lidProgress.getValue() > 0.9375F && doLogic) {
            this.state = EjectorBlockEntity.State.RETRACTING;
            this.lidProgress.setValue(1.0);
         }
      }

      if (this.state == EjectorBlockEntity.State.CHARGED) {
         this.lidProgress.setValue(0.0);
         this.lidProgress.updateChaseSpeed(0.0);
         if (doLogic) {
            this.ejectIfTriggered();
         }
      }

      if (this.state == EjectorBlockEntity.State.RETRACTING) {
         if (this.lidProgress.getChaseTarget() == 1.0F && !this.lidProgress.settled()) {
            this.lidProgress.tickChaser();
         } else {
            this.lidProgress.updateChaseTarget(0.0F);
            this.lidProgress.updateChaseSpeed(0.0);
            if (this.lidProgress.getValue() == 0.0F && doLogic) {
               this.state = EjectorBlockEntity.State.CHARGED;
               this.lidProgress.setValue(0.0);
               this.sendData();
            }

            float value = Mth.clamp(this.lidProgress.getValue() - this.getWindUpSpeed(), 0.0F, 1.0F);
            this.lidProgress.setValue((double)value);
            int soundRate = (int)(1.0F / (this.getWindUpSpeed() * 5.0F)) + 1;
            float volume = 0.125F;
            float pitch = 1.5F - this.lidProgress.getValue();
            if ((int)this.level.getGameTime() % soundRate == 0 && doLogic) {
               this.level.playSound(null, this.worldPosition, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundSource.BLOCKS, volume, pitch);
            }
         }
      }

      if (this.state != prevState) {
         this.notifyUpdate();
      }
   }

   private boolean scanTrajectoryForObstacles(int time) {
      if (time <= 2) {
         return false;
      } else {
         Vec3 source = this.getLaunchedItemLocation((float)time);
         Vec3 target = this.getLaunchedItemLocation((float)(time + 1));
         BlockHitResult rayTraceBlocks = this.level.clip(new ClipContext(source, target, Block.COLLIDER, Fluid.NONE, CollisionContext.empty()));
         boolean miss = rayTraceBlocks.getType() == Type.MISS;
         if (!miss && rayTraceBlocks.getType() == Type.BLOCK) {
            BlockState blockState = this.level.getBlockState(rayTraceBlocks.getBlockPos());
            if (FunnelBlock.isFunnel(blockState) && blockState.hasProperty(FunnelBlock.EXTRACTING) && (Boolean)blockState.getValue(FunnelBlock.EXTRACTING)) {
               miss = true;
            }
         }

         if (miss) {
            if (this.earlyTarget != null && this.earlyTargetTime < (float)(time + 1)) {
               this.earlyTarget = null;
               this.earlyTargetTime = 0.0F;
            }

            return false;
         } else {
            Vec3 vec = rayTraceBlocks.getLocation();
            this.earlyTarget = Pair.of(vec.add(Vec3.atLowerCornerOf(rayTraceBlocks.getDirection().getNormal()).scale(0.25)), rayTraceBlocks.getBlockPos());
            this.earlyTargetTime = (float)((double)time + source.distanceTo(vec) / source.distanceTo(target));
            this.sendData();
            return true;
         }
      }
   }

   protected void nudgeEntities() {
      for (Entity entity : this.level.getEntitiesOfClass(Entity.class, new AABB(this.worldPosition).inflate(-0.0625, 0.0, -0.0625))) {
         if (entity.isAlive() && entity.getPistonPushReaction() != PushReaction.IGNORE && !(entity instanceof Player)) {
            entity.setPos(entity.getX(), entity.getY() + 0.125, entity.getZ());
         }
      }
   }

   protected void ejectIfTriggered() {
      if (!this.powered) {
         int presentStackSize = this.depotBehaviour.getPresentStackSize();
         if (presentStackSize != 0) {
            if (presentStackSize >= this.maxStackSize.getValue()) {
               if (this.depotBehaviour.heldItem == null || !(this.depotBehaviour.heldItem.beltPosition < 0.49F)) {
                  Direction funnelFacing = this.getFacing().getOpposite();
                  ItemStack held = this.depotBehaviour.getHeldItemStack();
                  if (AbstractFunnelBlock.getFunnelFacing(this.level.getBlockState(this.worldPosition.above())) == funnelFacing) {
                     DirectBeltInputBehaviour directOutput = this.getBehaviour(DirectBeltInputBehaviour.TYPE);
                     if (this.depotBehaviour.heldItem != null) {
                        ItemStack tryFunnel = directOutput.tryExportingToBeltFunnel(held, funnelFacing, true);
                        if (tryFunnel == null || !tryFunnel.isEmpty()) {
                           return;
                        }
                     }
                  }

                  DirectBeltInputBehaviour targetOpenInv = this.getTargetOpenInv();
                  if (targetOpenInv == null
                     || this.depotBehaviour.heldItem == null
                     || targetOpenInv.handleInsertion(held, Direction.UP, true).getCount() != held.getCount()) {
                     this.activate();
                     this.notifyUpdate();
                  }
               }
            }
         }
      }
   }

   protected void placeItemAtTarget(boolean doLogic, float maxTime, IntAttached<ItemStack> intAttached) {
      if (doLogic) {
         if (intAttached.getSecond() == this.trackedItem) {
            this.trackedItem = null;
         }

         DirectBeltInputBehaviour targetOpenInv = this.getTargetOpenInv();
         if (targetOpenInv != null) {
            ItemStack remainder = targetOpenInv.handleInsertion((ItemStack)intAttached.getValue(), Direction.UP, false);
            intAttached.setSecond(remainder);
         }

         if (!((ItemStack)intAttached.getValue()).isEmpty()) {
            Vec3 ejectVec = this.earlyTarget != null ? (Vec3)this.earlyTarget.getFirst() : this.getLaunchedItemLocation(maxTime);
            Vec3 ejectMotionVec = this.getLaunchedItemMotion(maxTime);
            ItemEntity item = new ItemEntity(this.level, ejectVec.x, ejectVec.y, ejectVec.z, (ItemStack)intAttached.getValue());
            item.setDeltaMovement(ejectMotionVec);
            item.setDefaultPickUpDelay();
            this.level.addFreshEntity(item);
         }
      }
   }

   public DirectBeltInputBehaviour getTargetOpenInv() {
      BlockPos targetPos = this.earlyTarget != null
         ? (BlockPos)this.earlyTarget.getSecond()
         : this.worldPosition.above(this.launcher.getVerticalDistance()).relative(this.getFacing(), Math.max(1, this.launcher.getHorizontalDistance()));
      return BlockEntityBehaviour.get(this.level, targetPos, DirectBeltInputBehaviour.TYPE);
   }

   public Vec3 getLaunchedItemLocation(float time) {
      return this.launcher.getGlobalPos((double)time, this.getFacing().getOpposite(), this.worldPosition);
   }

   public Vec3 getLaunchedItemMotion(float time) {
      return this.launcher.getGlobalVelocity((double)time, this.getFacing().getOpposite(), this.worldPosition).scale(0.5);
   }

   @Override
   public void destroy() {
      super.destroy();
      this.dropFlyingItems();
   }

   public void dropFlyingItems() {
      for (IntAttached<ItemStack> intAttached : this.launchedItems) {
         Vec3 ejectVec = this.getLaunchedItemLocation((float)((Integer)intAttached.getFirst()).intValue());
         Vec3 ejectMotionVec = this.getLaunchedItemMotion((float)((Integer)intAttached.getFirst()).intValue());
         ItemEntity item = new ItemEntity(this.level, 0.0, 0.0, 0.0, (ItemStack)intAttached.getValue());
         item.setPosRaw(ejectVec.x, ejectVec.y, ejectVec.z);
         item.setDeltaMovement(ejectMotionVec);
         item.setDefaultPickUpDelay();
         this.level.addFreshEntity(item);
      }

      this.launchedItems.clear();
   }

   public float getWindUpSpeed() {
      int hd = this.launcher.getHorizontalDistance();
      int vd = this.launcher.getVerticalDistance();
      float speedFactor = Math.abs(this.getSpeed()) / 256.0F;
      float distanceFactor;
      if (hd == 0 && vd == 0) {
         distanceFactor = 1.0F;
      } else {
         distanceFactor = 1.0F * Mth.sqrt((float)(hd * hd + vd * vd));
      }

      return speedFactor / distanceFactor;
   }

   @Override
   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      compound.putInt("HorizontalDistance", this.launcher.getHorizontalDistance());
      compound.putInt("VerticalDistance", this.launcher.getVerticalDistance());
      compound.putBoolean("Powered", this.powered);
      NBTHelper.writeEnum(compound, "State", this.state);
      compound.put("Lid", this.lidProgress.writeNBT());
      compound.put("LaunchedItems", NBTHelper.writeCompoundList(this.launchedItems, ia -> ia.serializeNBT(s -> (CompoundTag)s.saveOptional(registries))));
      if (this.earlyTarget != null) {
         compound.put("EarlyTarget", VecHelper.writeNBT((Vec3)this.earlyTarget.getFirst()));
         compound.put("EarlyTargetPos", NbtUtils.writeBlockPos((BlockPos)this.earlyTarget.getSecond()));
         compound.putFloat("EarlyTargetTime", this.earlyTargetTime);
      }
   }

   @Override
   public void writeSafe(CompoundTag compound, Provider registries) {
      super.writeSafe(compound, registries);
      compound.putInt("HorizontalDistance", this.launcher.getHorizontalDistance());
      compound.putInt("VerticalDistance", this.launcher.getVerticalDistance());
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      int horizontalDistance = compound.getInt("HorizontalDistance");
      int verticalDistance = compound.getInt("VerticalDistance");
      if (this.launcher.getHorizontalDistance() != horizontalDistance || this.launcher.getVerticalDistance() != verticalDistance) {
         this.launcher.set(horizontalDistance, verticalDistance);
         this.launcher.clamp((Integer)AllConfigs.server().kinetics.maxEjectorDistance.get());
      }

      this.powered = compound.getBoolean("Powered");
      this.state = (EjectorBlockEntity.State)NBTHelper.readEnum(compound, "State", EjectorBlockEntity.State.class);
      this.lidProgress.readNBT(compound.getCompound("Lid"), false);
      this.launchedItems = NBTHelper.readCompoundList(
         compound.getList("LaunchedItems", 10), nbt -> IntAttached.read(nbt, t -> ItemStack.parseOptional(registries, t))
      );
      this.earlyTarget = null;
      this.earlyTargetTime = 0.0F;
      if (compound.contains("EarlyTarget")) {
         this.earlyTarget = Pair.of(VecHelper.readNBT(compound.getList("EarlyTarget", 6)), NBTHelper.readBlockPos(compound, "EarlyTargetPos"));
         this.earlyTargetTime = compound.getFloat("EarlyTargetTime");
      }

      if (compound.contains("ForceAngle")) {
         this.lidProgress.startWithValue((double)compound.getFloat("ForceAngle"));
      }
   }

   public void updateSignal() {
      boolean shoudPower = this.level.hasNeighborSignal(this.worldPosition);
      if (shoudPower != this.powered) {
         this.powered = shoudPower;
         this.sendData();
      }
   }

   public void setTarget(int horizontalDistance, int verticalDistance) {
      this.launcher.set(Math.max(1, horizontalDistance), verticalDistance);
      this.sendData();
   }

   public BlockPos getTargetPosition() {
      BlockState blockState = this.getBlockState();
      if (!AllBlocks.WEIGHTED_EJECTOR.has(blockState)) {
         return this.worldPosition;
      } else {
         Direction facing = (Direction)blockState.getValue(EjectorBlock.HORIZONTAL_FACING);
         return this.worldPosition.relative(facing, this.launcher.getHorizontalDistance()).above(this.launcher.getVerticalDistance());
      }
   }

   public float getLidProgress(float pt) {
      return this.lidProgress.getValue(pt);
   }

   public EjectorBlockEntity.State getState() {
      return this.state;
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public AABB getRenderBoundingBox() {
      return AABB.INFINITE;
   }

   private class EjectorSlot extends ValueBoxTransform.Sided {
      @Override
      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         return this.direction != Direction.UP
            ? super.getLocalOffset(level, pos, state)
            : new Vec3(0.5, 0.65625, 0.5).add(VecHelper.rotate(VecHelper.voxelSpace(0.0, 0.0, -5.0), (double)this.angle(state), Axis.Y));
      }

      @Override
      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         if (this.direction != Direction.UP) {
            super.rotate(level, pos, state, ms);
         } else {
            ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(this.angle(state))).rotateXDegrees(90.0F);
         }
      }

      protected float angle(BlockState state) {
         return AllBlocks.WEIGHTED_EJECTOR.has(state) ? AngleHelper.horizontalAngle((Direction)state.getValue(EjectorBlock.HORIZONTAL_FACING)) : 0.0F;
      }

      @Override
      protected boolean isSideActive(BlockState state, Direction direction) {
         return direction.getAxis() == ((Direction)state.getValue(EjectorBlock.HORIZONTAL_FACING)).getAxis()
            || direction == Direction.UP && EjectorBlockEntity.this.state != EjectorBlockEntity.State.CHARGED;
      }

      @Override
      protected Vec3 getSouthLocation() {
         return this.direction == Direction.UP ? Vec3.ZERO : VecHelper.voxelSpace(8.0, 6.0, 15.5);
      }
   }

   private abstract static class EntityHack extends Entity {
      public EntityHack(EntityType<?> p_i48580_1_, Level p_i48580_2_) {
         super(p_i48580_1_, p_i48580_2_);
      }

      public static void setElytraFlying(Entity e) {
         SynchedEntityData data = e.getEntityData();
         data.set(DATA_SHARED_FLAGS_ID, (byte)((Byte)data.get(DATA_SHARED_FLAGS_ID) | 128));
      }
   }

   public static enum State {
      CHARGED,
      LAUNCHING,
      RETRACTING;
   }
}

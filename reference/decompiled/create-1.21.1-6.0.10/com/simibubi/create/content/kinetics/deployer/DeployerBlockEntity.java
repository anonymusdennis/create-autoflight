package com.simibubi.create.content.kinetics.deployer;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.jetbrains.annotations.Nullable;

public class DeployerBlockEntity extends KineticBlockEntity implements Clearable {
   protected DeployerBlockEntity.State state;
   protected DeployerBlockEntity.Mode mode;
   protected ItemStack heldItem;
   protected DeployerFakePlayer player;
   protected int timer;
   protected float reach;
   protected boolean fistBump = false;
   protected List<ItemStack> overflowItems = new ArrayList<>();
   protected FilteringBehaviour filtering;
   protected boolean redstoneLocked;
   protected UUID owner;
   private IItemHandlerModifiable invHandler;
   private ListTag deferredInventoryList;
   private LerpedFloat animatedOffset;
   public BeltProcessingBehaviour processingBehaviour;
   ItemStackHandler recipeInv = new ItemStackHandler(2);

   public DeployerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.state = DeployerBlockEntity.State.WAITING;
      this.mode = DeployerBlockEntity.Mode.USE;
      this.heldItem = ItemStack.EMPTY;
      this.redstoneLocked = false;
      this.animatedOffset = LerpedFloat.linear().startWithValue(0.0);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.DEPLOYER.get(), (be, context) -> {
         if (be.invHandler == null) {
            be.initHandler();
         }

         return be.invHandler;
      });
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      super.addBehaviours(behaviours);
      this.filtering = new FilteringBehaviour(this, new DeployerFilterSlot());
      behaviours.add(this.filtering);
      this.processingBehaviour = new BeltProcessingBehaviour(this)
         .whenItemEnters((s, i) -> BeltDeployerCallbacks.onItemReceived(s, i, this))
         .whileItemHeld((s, i) -> BeltDeployerCallbacks.whenItemHeld(s, i, this));
      behaviours.add(this.processingBehaviour);
      this.registerAwardables(
         behaviours,
         new CreateAdvancement[]{
            AllAdvancements.TRAIN_CASING,
            AllAdvancements.ANDESITE_CASING,
            AllAdvancements.BRASS_CASING,
            AllAdvancements.COPPER_CASING,
            AllAdvancements.FIST_BUMP,
            AllAdvancements.DEPLOYER,
            AllAdvancements.SELF_DEPLOYING
         }
      );
   }

   @Override
   public void initialize() {
      super.initialize();
      this.initHandler();
   }

   private void initHandler() {
      if (this.invHandler == null) {
         if (this.level instanceof ServerLevel sLevel) {
            this.player = new DeployerFakePlayer(sLevel, this.owner);
            if (this.deferredInventoryList != null) {
               this.player.getInventory().load(this.deferredInventoryList);
               this.deferredInventoryList = null;
               this.heldItem = this.player.getMainHandItem();
               this.sendData();
            }

            Vec3 initialPos = VecHelper.getCenterOf(this.worldPosition.relative((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)));
            this.player.setPos(initialPos.x, initialPos.y, initialPos.z);
         }

         this.invHandler = this.createHandler();
      }
   }

   protected void onExtract(ItemStack stack) {
      this.player.setItemInHand(InteractionHand.MAIN_HAND, stack.copy());
      this.sendData();
      this.setChanged();
   }

   protected int getTimerSpeed() {
      return (int)(this.getSpeed() == 0.0F ? 0.0F : Mth.clamp(Math.abs(this.getSpeed() * 2.0F), 8.0F, 512.0F));
   }

   @Override
   public void tick() {
      super.tick();
      if (this.getSpeed() != 0.0F) {
         if (!this.level.isClientSide
            && this.player != null
            && this.player.blockBreakingProgress != null
            && this.level.isEmptyBlock((BlockPos)this.player.blockBreakingProgress.getKey())) {
            this.level.destroyBlockProgress(this.player.getId(), (BlockPos)this.player.blockBreakingProgress.getKey(), -1);
            this.player.blockBreakingProgress = null;
         }

         if (this.timer > 0) {
            this.timer = this.timer - this.getTimerSpeed();
         } else if (!this.level.isClientSide) {
            if (this.player != null) {
               ItemStack stack = this.player.getMainHandItem();
               if (this.state != DeployerBlockEntity.State.WAITING) {
                  if (this.state == DeployerBlockEntity.State.EXPANDING) {
                     if (this.fistBump) {
                        this.triggerFistBump();
                     }

                     this.activate();
                     this.state = DeployerBlockEntity.State.RETRACTING;
                     this.timer = 1000;
                     this.sendData();
                  } else if (this.state == DeployerBlockEntity.State.RETRACTING) {
                     this.state = DeployerBlockEntity.State.WAITING;
                     this.timer = 500;
                     this.sendData();
                  }
               } else if (!this.overflowItems.isEmpty()) {
                  this.timer = this.getTimerSpeed() * 10;
               } else {
                  boolean changed = false;
                  Inventory inventory = this.player.getInventory();

                  for (int i = 0; i < inventory.getContainerSize() && this.overflowItems.size() <= 10; i++) {
                     ItemStack item = inventory.getItem(i);
                     if (!item.isEmpty() && (item != stack || !this.filtering.test(item))) {
                        this.overflowItems.add(item);
                        inventory.setItem(i, ItemStack.EMPTY);
                        changed = true;
                     }
                  }

                  if (changed) {
                     this.sendData();
                     this.timer = this.getTimerSpeed() * 10;
                  } else {
                     Direction facing = (Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING);
                     if (this.mode == DeployerBlockEntity.Mode.USE
                        && !DeployerHandler.shouldActivate(stack, this.level, this.worldPosition.relative(facing, 2), facing)) {
                        this.timer = this.getTimerSpeed() * 10;
                     } else if (this.mode != DeployerBlockEntity.Mode.PUNCH || this.fistBump || !this.startFistBump(facing)) {
                        if (!this.redstoneLocked) {
                           this.start();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected void start() {
      this.state = DeployerBlockEntity.State.EXPANDING;
      Vec3 movementVector = this.getMovementVector();
      Vec3 rayOrigin = VecHelper.getCenterOf(this.worldPosition).add(movementVector.scale(1.5));
      Vec3 rayTarget = VecHelper.getCenterOf(this.worldPosition).add(movementVector.scale(2.5));
      ClipContext rayTraceContext = new ClipContext(rayOrigin, rayTarget, Block.OUTLINE, Fluid.NONE, this.player);
      BlockHitResult result = this.level.clip(rayTraceContext);
      this.reach = (float)(0.5 + Math.min(result.getLocation().subtract(rayOrigin).length(), 0.75));
      this.timer = 1000;
      this.sendData();
   }

   public boolean startFistBump(Direction facing) {
      int i = 0;
      DeployerBlockEntity partner = null;

      for (i = 2; i < 5; i++) {
         BlockPos otherDeployer = this.worldPosition.relative(facing, i);
         if (!this.level.isLoaded(otherDeployer)) {
            return false;
         }

         if (this.level.getBlockEntity(otherDeployer) instanceof DeployerBlockEntity dpe) {
            partner = dpe;
            break;
         }
      }

      if (partner == null) {
         return false;
      } else if (((Direction)this.level.getBlockState(partner.getBlockPos()).getValue(DirectionalKineticBlock.FACING)).getOpposite() == facing
         && partner.mode == DeployerBlockEntity.Mode.PUNCH) {
         if (partner.getSpeed() == 0.0F) {
            return false;
         } else {
            for (DeployerBlockEntity be : Arrays.asList(this, partner)) {
               be.fistBump = true;
               be.reach = (float)(i - 2) * 0.5F;
               be.timer = 1000;
               be.state = DeployerBlockEntity.State.EXPANDING;
               be.sendData();
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public void triggerFistBump() {
      int i = 0;
      DeployerBlockEntity deployerBlockEntity = null;

      for (int var6 = 2; var6 < 5; var6++) {
         BlockPos pos = this.worldPosition.relative((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING), var6);
         if (!this.level.isLoaded(pos)) {
            return;
         }

         if (this.level.getBlockEntity(pos) instanceof DeployerBlockEntity dpe) {
            deployerBlockEntity = dpe;
            break;
         }
      }

      if (deployerBlockEntity != null) {
         if (deployerBlockEntity.fistBump && deployerBlockEntity.state == DeployerBlockEntity.State.EXPANDING) {
            if (deployerBlockEntity.timer <= 0) {
               this.fistBump = false;
               deployerBlockEntity.fistBump = false;
               deployerBlockEntity.state = DeployerBlockEntity.State.RETRACTING;
               deployerBlockEntity.timer = 1000;
               deployerBlockEntity.sendData();
               this.award(AllAdvancements.FIST_BUMP);
               BlockPos soundLocation = BlockPos.containing(
                  Vec3.atCenterOf(this.worldPosition).add(Vec3.atCenterOf(deployerBlockEntity.getBlockPos())).scale(0.5)
               );
               this.level.playSound(null, soundLocation, SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.BLOCKS, 0.75F, 0.75F);
            }
         }
      }
   }

   protected void activate() {
      Vec3 movementVector = this.getMovementVector();
      Direction direction = (Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING);
      Vec3 center = VecHelper.getCenterOf(this.worldPosition);
      BlockPos clickedPos = this.worldPosition.relative(direction, 2);
      this.player.setXRot(direction == Direction.UP ? -90.0F : (direction == Direction.DOWN ? 90.0F : 0.0F));
      this.player.setYRot(direction.toYRot());
      if (direction != Direction.DOWN || BlockEntityBehaviour.get(this.level, clickedPos, TransportedItemStackHandlerBehaviour.TYPE) == null) {
         DeployerHandler.activate(this.player, center, clickedPos, movementVector, this.mode);
         this.award(AllAdvancements.DEPLOYER);
         if (this.player != null) {
            int count = this.heldItem.getCount();
            this.heldItem = this.player.getMainHandItem();
            if (count != this.heldItem.getCount()) {
               this.setChanged();
            }
         }
      }
   }

   protected Vec3 getMovementVector() {
      return !AllBlocks.DEPLOYER.has(this.getBlockState())
         ? Vec3.ZERO
         : Vec3.atLowerCornerOf(((Direction)this.getBlockState().getValue(DirectionalKineticBlock.FACING)).getNormal());
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.state = (DeployerBlockEntity.State)NBTHelper.readEnum(compound, "State", DeployerBlockEntity.State.class);
      this.mode = (DeployerBlockEntity.Mode)NBTHelper.readEnum(compound, "Mode", DeployerBlockEntity.Mode.class);
      this.timer = compound.getInt("Timer");
      this.redstoneLocked = compound.getBoolean("Powered");
      if (compound.contains("Owner")) {
         this.owner = compound.getUUID("Owner");
      }

      this.deferredInventoryList = compound.getList("Inventory", 10);
      this.overflowItems = NBTHelper.readItemList(compound.getList("Overflow", 10), registries);
      if (compound.contains("HeldItem")) {
         this.heldItem = ItemStack.parseOptional(registries, compound.getCompound("HeldItem"));
      }

      super.read(compound, registries, clientPacket);
      if (clientPacket) {
         this.fistBump = compound.getBoolean("Fistbump");
         this.reach = compound.getFloat("Reach");
         if (compound.contains("Particle")) {
            ItemStack particleStack = ItemStack.parseOptional(registries, compound.getCompound("Particle"));
            SandPaperItem.spawnParticles(
               VecHelper.getCenterOf(this.worldPosition).add(this.getMovementVector().scale((double)(this.reach + 1.0F))), particleStack, this.level
            );
         }
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      NBTHelper.writeEnum(compound, "Mode", this.mode);
      NBTHelper.writeEnum(compound, "State", this.state);
      compound.putInt("Timer", this.timer);
      compound.putBoolean("Powered", this.redstoneLocked);
      if (this.owner != null) {
         compound.putUUID("Owner", this.owner);
      }

      if (this.player != null) {
         ListTag invNBT = new ListTag();
         this.player.getInventory().save(invNBT);
         compound.put("Inventory", invNBT);
         compound.put("HeldItem", this.player.getMainHandItem().saveOptional(registries));
         compound.put("Overflow", NBTHelper.writeItemList(this.overflowItems, registries));
      } else if (this.deferredInventoryList != null) {
         compound.put("Inventory", this.deferredInventoryList);
      }

      super.write(compound, registries, clientPacket);
      if (clientPacket) {
         compound.putBoolean("Fistbump", this.fistBump);
         compound.putFloat("Reach", this.reach);
         if (this.player != null) {
            compound.put("HeldItem", this.player.getMainHandItem().saveOptional(registries));
            if (this.player.spawnedItemEffects != null) {
               compound.put("Particle", this.player.spawnedItemEffects.saveOptional(registries));
               this.player.spawnedItemEffects = null;
            }
         }
      }
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      NBTHelper.writeEnum(tag, "Mode", this.mode);
      super.writeSafe(tag, registries);
   }

   private IItemHandlerModifiable createHandler() {
      return new DeployerItemHandler(this);
   }

   public void redstoneUpdate() {
      if (!this.level.isClientSide) {
         boolean blockPowered = this.level.hasNeighborSignal(this.worldPosition);
         if (blockPowered != this.redstoneLocked) {
            this.redstoneLocked = blockPowered;
            this.sendData();
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public PartialModel getHandPose() {
      return this.mode == DeployerBlockEntity.Mode.PUNCH
         ? AllPartialModels.DEPLOYER_HAND_PUNCHING
         : (this.heldItem.isEmpty() ? AllPartialModels.DEPLOYER_HAND_POINTING : AllPartialModels.DEPLOYER_HAND_HOLDING);
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().inflate(3.0);
   }

   public void discardPlayer() {
      if (this.player != null) {
         this.player.getInventory().dropAll();
         this.overflowItems.forEach(itemstack -> this.player.drop(itemstack, true, false));
         this.player.discard();
         this.player = null;
      }
   }

   @Override
   public void invalidate() {
      super.invalidate();
      if (this.invHandler != null) {
         this.invalidateCapabilities();
      }
   }

   public void clearContent() {
      this.filtering.setFilter(ItemStack.EMPTY);
   }

   public void changeMode() {
      this.mode = this.mode == DeployerBlockEntity.Mode.PUNCH ? DeployerBlockEntity.Mode.USE : DeployerBlockEntity.Mode.PUNCH;
      this.setChanged();
      this.sendData();
   }

   @Override
   public boolean addToTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (super.addToTooltip(tooltip, isPlayerSneaking)) {
         return true;
      } else if (this.getSpeed() == 0.0F) {
         return false;
      } else if (this.overflowItems.isEmpty()) {
         return false;
      } else {
         TooltipHelper.addHint(tooltip, "hint.full_deployer");
         return true;
      }
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      CreateLang.translate("tooltip.deployer.header").forGoggles(tooltip);
      CreateLang.translate("tooltip.deployer." + (this.mode == DeployerBlockEntity.Mode.USE ? "using" : "punching"))
         .style(ChatFormatting.YELLOW)
         .forGoggles(tooltip);
      if (!this.heldItem.isEmpty()) {
         CreateLang.translate("tooltip.deployer.contains", Component.translatable(this.heldItem.getDescriptionId()).getString(), this.heldItem.getCount())
            .style(ChatFormatting.GREEN)
            .forGoggles(tooltip);
      }

      float stressAtBase = this.calculateStressApplied();
      if (IRotate.StressImpact.isEnabled() && !Mth.equal(stressAtBase, 0.0F)) {
         tooltip.add(CommonComponents.EMPTY);
         this.addStressImpactStats(tooltip, stressAtBase);
      }

      return true;
   }

   @OnlyIn(Dist.CLIENT)
   public float getHandOffset(float partialTicks) {
      if (this.isVirtual()) {
         return this.animatedOffset.getValue(partialTicks);
      } else {
         float progress = 0.0F;
         int timerSpeed = this.getTimerSpeed();
         PartialModel handPose = this.getHandPose();
         if (this.state == DeployerBlockEntity.State.EXPANDING) {
            progress = 1.0F - ((float)this.timer - partialTicks * (float)timerSpeed) / 1000.0F;
            if (this.fistBump) {
               progress *= progress;
            }
         }

         if (this.state == DeployerBlockEntity.State.RETRACTING) {
            progress = ((float)this.timer - partialTicks * (float)timerSpeed) / 1000.0F;
         }

         float handLength = handPose == AllPartialModels.DEPLOYER_HAND_POINTING ? 0.0F : (handPose == AllPartialModels.DEPLOYER_HAND_HOLDING ? 0.25F : 0.1875F);
         return Math.min(Mth.clamp(progress, 0.0F, 1.0F) * (this.reach + handLength), 1.3125F);
      }
   }

   public void setAnimatedOffset(float offset) {
      this.animatedOffset.setValue((double)offset);
   }

   @Nullable
   public RecipeHolder<? extends Recipe<? extends RecipeInput>> getRecipe(ItemStack stack) {
      if (this.player != null && this.level != null) {
         ItemStack heldItemMainhand = this.player.getMainHandItem();
         if (heldItemMainhand.getItem() instanceof SandPaperItem) {
            Optional<RecipeHolder<Recipe<RecipeInput>>> polishingRecipe = this.checkRecipe(
               AllRecipeTypes.SANDPAPER_POLISHING, new SingleRecipeInput(stack), this.level
            );
            if (polishingRecipe.isPresent()) {
               return polishingRecipe.get();
            }
         }

         this.recipeInv.setStackInSlot(0, stack);
         this.recipeInv.setStackInSlot(1, heldItemMainhand);
         DeployerRecipeSearchEvent event = new DeployerRecipeSearchEvent(this, new RecipeWrapper(this.recipeInv));
         event.addRecipe(
            () -> SequencedAssemblyRecipe.getRecipe(this.level, event.getInventory(), AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class), 100
         );
         event.addRecipe(() -> this.checkRecipe(AllRecipeTypes.DEPLOYING, event.getInventory(), this.level), 50);
         event.addRecipe(() -> this.checkRecipe(AllRecipeTypes.ITEM_APPLICATION, event.getInventory(), this.level), 50);
         NeoForge.EVENT_BUS.post(event);
         return event.getRecipe();
      } else {
         return null;
      }
   }

   private Optional<RecipeHolder<Recipe<RecipeInput>>> checkRecipe(AllRecipeTypes type, RecipeInput inv, Level level) {
      return type.<RecipeInput, Recipe<RecipeInput>>find(inv, level).filter(AllRecipeTypes.CAN_BE_AUTOMATED);
   }

   public DeployerFakePlayer getPlayer() {
      return this.player;
   }

   static enum Mode {
      PUNCH,
      USE;
   }

   static enum State {
      WAITING,
      EXPANDING,
      RETRACTING,
      DUMPING;
   }
}

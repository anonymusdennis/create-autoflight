package dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Sided;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.eriksonn.aeronautics.config.AeroConfig;
import dev.eriksonn.aeronautics.config.server.AeroBlockConfigs;
import dev.eriksonn.aeronautics.content.blocks.hot_air.BlockEntityLiftingGasProvider;
import dev.eriksonn.aeronautics.content.blocks.hot_air.GasEmitterRenderHandler;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerValueBehaviour;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroLiftingGasTypes;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.eriksonn.aeronautics.util.AeroSoundDistUtil;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SteamVentBlockEntity extends SmartBlockEntity implements BlockEntityLiftingGasProvider, IHaveGoggleInformation {
   public static final Direction CHECKING_DIR = Direction.DOWN;
   private static final MutableComponent SCROLL_OPTION_TITLE = AeroLang.translate("scroll_option.hot_air_amount").component();
   private static final String VALUE_FORMAT = "%s m³";
   public int signalStrength = 0;
   public int rawSignalStrength = 0;
   protected ScrollValueBehaviour steamAmountBehaviour;
   private GasEmitterRenderHandler renderHandler;
   private Balloon currentBalloon;
   private BlockEntityLiftingGasProvider.ClientBalloonInfo clientBalloonInfo;
   private WeakReference<FluidTankBlockEntity> source;
   private double efficiency = 0.0;
   private int ticksSinceSync;
   private int maxCapacity;
   protected LerpedFloat intensity = LerpedFloat.linear();
   @Nullable
   private BlockPos castPosition;

   public SteamVentBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.source = new WeakReference<>(null);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      AeroBlockConfigs config = AeroConfig.server().blocks;
      this.setMaxCapacity((Integer)config.steamVentMaxHotAir.get());
      this.steamAmountBehaviour = new SteamVentBlockEntity.SteamVentValueBehaviour(
            SCROLL_OPTION_TITLE, this, new SteamVentBlockEntity.SteamVentValueBoxTransform()
         )
         .between(() -> 50, config.steamVentMaxHotAir::get)
         .withFormatter(xva$0 -> "%s m³".formatted(xva$0));
      this.steamAmountBehaviour.value = this.maxCapacity;
      behaviours.add(this.steamAmountBehaviour);
   }

   public void setMaxCapacity(int maxCapacity) {
      this.maxCapacity = maxCapacity;
   }

   public void lazyTick() {
      super.lazyTick();
      this.getAndCacheTank();
      if (!this.isVirtual() && this.canOutputGas()) {
         this.tickBalloonLogic();
         this.notifyUpdate();
      }
   }

   public void tick() {
      super.tick();
      this.ticksSinceSync++;
      FluidTankBlockEntity fluidTank = this.source.get();
      if (fluidTank != null) {
         FluidTankBlockEntity controller = fluidTank.getControllerBE();
         if (controller != null) {
            this.efficiency = (double)Mth.clamp(controller.boiler.getEngineEfficiency(controller.getTotalTankSize()), 0.0F, 1.0F);
         }
      } else {
         this.efficiency = 0.0;
      }

      double intensityGoal = Math.max(0.0, (double)this.signalStrength / 15.0);
      this.intensity.chase(intensityGoal, 0.1, Chaser.EXP);
      this.intensity.tickChaser();
      if (this.level.isClientSide) {
         GasEmitterRenderHandler renderHandler = this.getRenderHandler();
         if (this.isVirtual()) {
            renderHandler.targetFromRedstoneSignal(this.signalStrength);
         } else {
            renderHandler.targetFromRedstoneSignal(this.getGasOutput() > 0.0 ? this.signalStrength : 0);
         }

         renderHandler.tick();
         if (this.canOutputGas()) {
            AeroSoundDistUtil.addPosSteamVentSound(this.getBlockPos());
         } else {
            AeroSoundDistUtil.removePosSteamVentSound(this.getBlockPos());
         }
      }
   }

   public void initialize() {
      super.initialize();
      if (!this.isVirtual() && this.canOutputGas()) {
         this.tickBalloonLogic();
         this.notifyUpdate();
      }
   }

   public void invalidate() {
      super.invalidate();
      if (this.level.isClientSide) {
         AeroSoundDistUtil.removePosSteamVentSound(this.getBlockPos());
      } else {
         this.removeFromBalloon();
      }
   }

   @Nullable
   @Override
   public BlockPos getCastPosition() {
      return this.castPosition;
   }

   @Override
   public void doRaycast() {
      BlockPos pos = this.getBlockPos();
      AeroBlockConfigs blocks = AeroConfig.server().blocks;
      int range = (Integer)blocks.steamVentMaxRange.get();
      this.castPosition = this.getRaycastedPosition(this.level, Vec3.upFromBottomCenterOf(pos, 1.0), Vec3.upFromBottomCenterOf(pos, 1.0 + (double)range));
   }

   public boolean updateRawSignal() {
      int newStrength = this.level.getBestNeighborSignal(this.getBlockPos());
      if (newStrength == this.rawSignalStrength) {
         return false;
      } else {
         if (!this.level.isClientSide) {
            BlockState existentState = this.level.getBlockState(this.getBlockPos());
            if (newStrength > 0 && this.rawSignalStrength == 0) {
               this.level.setBlockAndUpdate(this.worldPosition, (BlockState)existentState.setValue(SteamVentBlock.POWERED, true));
            } else if (newStrength == 0 && this.rawSignalStrength > 0) {
               this.level.setBlockAndUpdate(this.worldPosition, (BlockState)existentState.setValue(SteamVentBlock.POWERED, false));
            }
         }

         this.rawSignalStrength = newStrength;
         this.signalSync();
         return true;
      }
   }

   public void updateSignal(int signal) {
      if (signal != this.signalStrength) {
         if (this.signalStrength == 0 && signal != 0) {
            this.level
               .playSound(
                  null, this.worldPosition, AeroSoundEvents.STEAM_VENT_OPEN.event(), SoundSource.BLOCKS, 0.25F, 1.1F - this.level.random.nextFloat() * 0.2F
               );
         } else if (signal == 0) {
            this.level
               .playSound(
                  null, this.worldPosition, AeroSoundEvents.STEAM_VENT_CLOSE.event(), SoundSource.BLOCKS, 0.5F, 0.7F - this.level.random.nextFloat() * 0.2F
               );
         }

         this.signalStrength = signal;
         this.sendData();
      }
   }

   public static boolean inTankBounds(BlockPos pos, FluidTankBlockEntity controller) {
      int minX = controller.getBlockPos().getX();
      int minZ = controller.getBlockPos().getZ();
      int maxX = minX + controller.getWidth();
      int maxZ = minZ + controller.getWidth();
      return pos.getX() >= minX && pos.getX() < maxX && pos.getZ() >= minZ && pos.getZ() < maxZ;
   }

   public void signalSync() {
      FluidTankBlockEntity fluidTank = this.source.get();
      if (fluidTank != null) {
         FluidTankBlockEntity controller = fluidTank.getControllerBE();
         if (controller != null) {
            List<SteamVentBlockEntity> adjacent = new ArrayList<>();
            adjacent.add(this);
            int maxRaw = this.searchSignalSync(controller, new HashSet<>(), adjacent);

            for (SteamVentBlockEntity steamVentBlockEntity : adjacent) {
               steamVentBlockEntity.updateSignal(maxRaw);
            }
         }
      }
   }

   protected int searchSignalSync(FluidTankBlockEntity controller, Set<BlockPos> visited, List<SteamVentBlockEntity> vents) {
      int maxRaw = this.rawSignalStrength;
      MutableBlockPos mutablePos = new MutableBlockPos();

      for (Direction dir : Iterate.horizontalDirections) {
         mutablePos.setWithOffset(this.getBlockPos(), dir);
         if (inTankBounds(mutablePos, controller) && !visited.contains(mutablePos)) {
            visited.add(mutablePos.immutable());
            if (this.level.getBlockEntity(mutablePos) instanceof SteamVentBlockEntity vent) {
               vents.add(vent);
               maxRaw = Math.max(maxRaw, vent.searchSignalSync(controller, visited, vents));
            }
         }
      }

      return maxRaw;
   }

   public void getAndCacheTank() {
      FluidTankBlockEntity ftbe = this.source.get();
      if (ftbe == null || ftbe.isRemoved()) {
         BlockPos check = this.getBlockPos().relative(CHECKING_DIR);
         if (this.level.getBlockEntity(check) instanceof FluidTankBlockEntity fluidTank) {
            this.source = new WeakReference<>(fluidTank);
         }
      }
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putInt("SignalStrength", this.signalStrength);
      tag.putInt("RawSignalStrength", this.rawSignalStrength);
      if (clientPacket) {
         BlockEntityLiftingGasProvider.ClientBalloonInfo.writeToNBT(tag, (ServerBalloon)this.currentBalloon);
      }
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.signalStrength = tag.getInt("SignalStrength");
      this.rawSignalStrength = tag.getInt("RawSignalStrength");
      if (clientPacket) {
         this.ticksSinceSync = 0;
         this.clientBalloonInfo = BlockEntityLiftingGasProvider.ClientBalloonInfo.readFromNBT(tag);
      }
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      if (!this.canOutputGas()) {
         return false;
      } else {
         AeroLang.blockName(this.getBlockState()).text(":").forGoggles(tooltip, 1);
         if (this.clientBalloonInfo != null) {
            this.addBalloonGoggleInformation(tooltip, this.clientBalloonInfo, this.ticksSinceSync, this.getAirPressure(this.clientBalloonInfo, this.level));
         }

         return true;
      }
   }

   @Nullable
   @Override
   public Balloon getBalloon() {
      return this.currentBalloon;
   }

   @Override
   public void setBalloon(Balloon balloon) {
      this.currentBalloon = balloon;
   }

   @Override
   public double getGasOutput() {
      return (double)this.steamAmountBehaviour.getValue() * this.efficiency * (double)((float)this.signalStrength / 15.0F);
   }

   @Override
   public LiftingGasType getLiftingGasType() {
      return (LiftingGasType)AeroLiftingGasTypes.STEAM.get();
   }

   @Override
   public boolean canOutputGas() {
      return this.efficiency > 0.0 && this.signalStrength > 0 && !this.isRemoved();
   }

   @Override
   public double getClientPredictedVolume() {
      return this.clientBalloonInfo == null ? 0.0 : BlockEntityLiftingGasProvider.getPredictedVolume(this.clientBalloonInfo, this.ticksSinceSync);
   }

   public LerpedFloat getClientIntensity() {
      return this.intensity;
   }

   public GasEmitterRenderHandler getRenderHandler() {
      return this.renderHandler == null ? (this.renderHandler = new GasEmitterRenderHandler()) : this.renderHandler;
   }

   public static class SteamVentValueBehaviour extends HotAirBurnerValueBehaviour {
      public SteamVentValueBehaviour(Component label, SmartBlockEntity be, SteamVentBlockEntity.SteamVentValueBoxTransform slot) {
         super(label, be, slot);
         slot.be = be;
      }
   }

   public static class SteamVentValueBoxTransform extends Sided {
      BlockEntity be;

      public Sided fromSide(Direction direction) {
         this.direction = direction;
         Level level = this.be.getLevel();
         if (level != null && level.isClientSide && direction == Direction.UP) {
            Minecraft mc = Minecraft.getInstance();
            HitResult target = mc.hitResult;
            if (target instanceof BlockHitResult) {
               Vec3 hit = target.getLocation();
               Vec3 localHit = hit.subtract(Vec3.atCenterOf(this.be.getBlockPos()));
               if (localHit.y < 0.4) {
                  this.direction = Direction.getNearest(localHit.x, 0.0, localHit.z);
               }
            }
         }

         return this;
      }

      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 8.0, 12.0);
      }

      public float getScale() {
         return 0.45F;
      }

      protected ValueBoxTransform getMovementModeSlot() {
         return new DirectionalExtenderScrollOptionSlot((state, d) -> {
            Axis axis = d.getAxis();
            Axis shaftAxis = ((IRotate)state.getBlock()).getRotationAxis(state);
            return shaftAxis != axis;
         });
      }

      public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
         float yRot = AngleHelper.horizontalAngle(this.getSide()) + 180.0F;
         float xRot = this.getSide() == Direction.UP ? 90.0F : (this.getSide() == Direction.DOWN ? 270.0F : 0.0F);
         xRot += 22.5F;
         ((PoseTransformStack)TransformStack.of(ms).rotateYDegrees(yRot)).rotateXDegrees(xRot);
      }

      protected boolean isSideActive(BlockState state, Direction direction) {
         Level level = this.be.getLevel();
         if (level != null && level.isClientSide && direction == Direction.UP) {
            Minecraft mc = Minecraft.getInstance();
            HitResult target = mc.hitResult;
            if (target instanceof BlockHitResult) {
               Vec3 hit = target.getLocation();
               Vec3 localHit = hit.subtract(Vec3.atCenterOf(this.be.getBlockPos()));
               return localHit.y < 0.4;
            }
         }

         return true;
      }

      public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
         if (this.getSide() == Direction.DOWN) {
            return VecHelper.voxelSpace(8.0, 0.0, 8.0);
         } else {
            Vec3 location = this.getSouthLocation();
            location = location.add(VecHelper.voxelSpace(0.0, -3.0, 1.75));
            return VecHelper.rotateCentered(location, (double)AngleHelper.horizontalAngle(this.getSide()), Axis.Y);
         }
      }
   }
}

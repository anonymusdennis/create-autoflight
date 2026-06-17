package com.simibubi.create.content.decoration.steamWhistle;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.steamEngine.SteamJetParticleData;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import java.lang.ref.WeakReference;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class WhistleBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
   public WeakReference<FluidTankBlockEntity> source = new WeakReference<>(null);
   public LerpedFloat animation = LerpedFloat.linear();
   protected int pitch;
   @OnlyIn(Dist.CLIENT)
   protected WhistleSoundInstance soundInstance;

   public WhistleBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.STEAM_WHISTLE});
   }

   public void updatePitch() {
      BlockPos currentPos = this.worldPosition.above();

      int newPitch;
      for (newPitch = 0; newPitch <= 24; newPitch += 2) {
         BlockState blockState = this.level.getBlockState(currentPos);
         if (!AllBlocks.STEAM_WHISTLE_EXTENSION.has(blockState)) {
            break;
         }

         if (blockState.getValue(WhistleExtenderBlock.SHAPE) == WhistleExtenderBlock.WhistleExtenderShape.SINGLE) {
            newPitch++;
            break;
         }

         currentPos = currentPos.above();
      }

      if (this.pitch != newPitch) {
         this.pitch = newPitch;
         this.notifyUpdate();
         FluidTankBlockEntity tank = this.getTank();
         if (tank != null && tank.boiler != null) {
            tank.boiler.checkPipeOrganAdvancement(tank);
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide()) {
         if (this.isPowered()) {
            this.award(AllAdvancements.STEAM_WHISTLE);
         }
      } else {
         FluidTankBlockEntity tank = this.getTank();
         boolean powered = this.isPowered()
            && (tank != null && tank.boiler.isActive() && (tank.boiler.passiveHeat || tank.boiler.activeHeat > 0) || this.isVirtual());
         this.animation.chase(powered ? 1.0 : 0.0, powered ? 0.5 : 0.4F, powered ? Chaser.EXP : Chaser.LINEAR);
         this.animation.tickChaser();
         CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.tickAudio(this.getOctave(), powered));
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      tag.putInt("Pitch", this.pitch);
      super.write(tag, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      this.pitch = tag.getInt("Pitch");
      super.read(tag, registries, clientPacket);
   }

   @Override
   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      String[] pitches = CreateLang.translateDirect("generic.notes").getString().split(";");
      CreateLang.translate("generic.pitch", pitches[this.pitch % pitches.length]).forGoggles(tooltip);
      return true;
   }

   protected boolean isPowered() {
      return this.getBlockState().getOptionalValue(WhistleBlock.POWERED).orElse(false);
   }

   protected WhistleBlock.WhistleSize getOctave() {
      return this.getBlockState().getOptionalValue(WhistleBlock.SIZE).orElse(WhistleBlock.WhistleSize.MEDIUM);
   }

   @OnlyIn(Dist.CLIENT)
   protected void tickAudio(WhistleBlock.WhistleSize size, boolean powered) {
      if (!powered) {
         if (this.soundInstance != null) {
            this.soundInstance.fadeOut();
            this.soundInstance = null;
         }
      } else {
         float f = (float)Math.pow(2.0, (double)(-this.pitch) / 12.0);
         boolean particle = this.level.getGameTime() % 8L == 0L;
         Vec3 eyePosition = Minecraft.getInstance().cameraEntity.getEyePosition();
         float maxVolume = (float)Mth.clamp((64.0 - eyePosition.distanceTo(Vec3.atCenterOf(this.worldPosition))) / 64.0, 0.0, 1.0);
         if (this.soundInstance == null || this.soundInstance.isStopped() || this.soundInstance.getOctave() != size) {
            Minecraft.getInstance().getSoundManager().play(this.soundInstance = new WhistleSoundInstance(size, this.worldPosition));
            AllSoundEvents.WHISTLE_CHIFF
               .playAt(this.level, this.worldPosition, maxVolume * 0.175F, size == WhistleBlock.WhistleSize.SMALL ? f + 0.75F : f, false);
            particle = true;
         }

         this.soundInstance.keepAlive();
         this.soundInstance.setPitch(f);
         if (particle) {
            Direction facing = this.getBlockState().getOptionalValue(WhistleBlock.FACING).orElse(Direction.SOUTH);
            float angle = 180.0F + AngleHelper.horizontalAngle(facing);
            Vec3 sizeOffset = VecHelper.rotate(new Vec3(0.0, -0.4F, (double)(0.0625F * (float)size.ordinal())), (double)angle, Axis.Y);
            Vec3 offset = VecHelper.rotate(new Vec3(0.0, 1.0, 0.75), (double)angle, Axis.Y);
            Vec3 v = offset.scale(0.45F).add(sizeOffset).add(Vec3.atCenterOf(this.worldPosition));
            Vec3 m = offset.subtract(Vec3.atLowerCornerOf(facing.getNormal()).scale(0.75));
            this.level.addParticle(new SteamJetParticleData(1.0F), v.x, v.y, v.z, m.x, m.y, m.z);
         }
      }
   }

   public int getPitchId() {
      return this.pitch + 100 * this.getBlockState().getOptionalValue(WhistleBlock.SIZE).orElse(WhistleBlock.WhistleSize.MEDIUM).ordinal();
   }

   public FluidTankBlockEntity getTank() {
      FluidTankBlockEntity tank = this.source.get();
      if (tank == null || tank.isRemoved()) {
         if (tank != null) {
            this.source = new WeakReference<>(null);
         }

         Direction facing = WhistleBlock.getAttachedDirection(this.getBlockState());
         if (this.level.getBlockEntity(this.worldPosition.relative(facing)) instanceof FluidTankBlockEntity tankBe) {
            tank = tankBe;
            this.source = new WeakReference<>(tankBe);
         }
      }

      return tank == null ? null : tank.getControllerBE();
   }
}

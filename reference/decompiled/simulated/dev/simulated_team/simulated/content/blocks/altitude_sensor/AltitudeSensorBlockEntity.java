package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.util.Observable;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3d;

public class AltitudeSensorBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Observable, ClipboardCloneable {
   public float highSignal = 1.0F;
   public float lowSignal = 0.0F;
   public int signal = 0;
   public int tickCount = 0;
   public float visualHeight = 0.0F;
   public float previousVisualHeight = 0.0F;
   public boolean updateVisualHeight = true;

   public AltitudeSensorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public int getSignal() {
      return Math.round(this.getValue() * 15.0F);
   }

   public float getValue() {
      float y = this.getNormalHeight();
      float value = (y - this.lowSignal) / (this.highSignal - this.lowSignal);
      return Mth.clamp(value, 0.0F, 1.0F);
   }

   public float getWorldHeight() {
      Vector3d pos = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(this.getBlockPos()));
      return (float)pos.y;
   }

   public float getNormalHeight() {
      return this.toNormalHeight(this.getWorldHeight());
   }

   public float toWorldHeight(float normalHeight) {
      return Mth.map(normalHeight, 0.0F, 1.0F, (float)this.getLevel().getMinBuildHeight(), (float)this.getLevel().getMaxBuildHeight());
   }

   public float toNormalHeight(float worldHeight) {
      return Mth.map(worldHeight, (float)this.getLevel().getMinBuildHeight(), (float)this.getLevel().getMaxBuildHeight(), 0.0F, 1.0F);
   }

   public double getAirPressure() {
      return DimensionPhysicsData.getAirPressure(
         this.getLevel(), Sable.HELPER.projectOutOfSubLevel(this.getLevel(), JOMLConversion.atCenterOf(this.getBlockPos()))
      );
   }

   public float getVisualHeight(float partialTick) {
      return this.previousVisualHeight * (1.0F - partialTick) + this.visualHeight * partialTick;
   }

   public float getValue(float partialTick) {
      float y = this.getVisualHeight(partialTick);
      float value = (y - this.lowSignal) / (this.highSignal - this.lowSignal);
      return Mth.clamp(value, 0.0F, 1.0F);
   }

   public void updateSignal() {
      this.getLevel().setBlockAndUpdate(this.getBlockPos(), this.getBlockState());
      this.getLevel().updateNeighborsAt(this.getBlockPos(), this.getBlockState().getBlock());
      this.getLevel().updateNeighborsAt(this.getBlockPos().below(), this.getBlockState().getBlock());
   }

   public void tick() {
      super.tick();
      this.tickCount++;
      int lastSignal = this.signal;
      this.signal = this.getSignal();
      if (this.signal != lastSignal) {
         this.updateSignal();
      }

      if (this.getLevel().isClientSide()) {
         float worldHeight = this.getWorldHeight();
         if (this.visualHeight == 0.0F) {
            this.visualHeight = worldHeight;
         }

         float step = 0.15F;
         this.previousVisualHeight = this.visualHeight;
         if (this.updateVisualHeight) {
            this.visualHeight = this.visualHeight * 0.85F + worldHeight * 0.15F;
         }
      }
   }

   public void notifyUpdate() {
      super.notifyUpdate();
      this.updateSignal();
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.highSignal = tag.getFloat("high_signal");
      this.lowSignal = tag.getFloat("low_signal");
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putFloat("high_signal", this.highSignal);
      tag.putFloat("low_signal", this.lowSignal);
   }

   public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
      float height = this.getWorldHeight();
      float airPressure = (float)this.getAirPressure() * 100.0F;
      SimLang.blockName(this.getBlockState()).forGoggles(tooltip, 1);
      SimLang.translate("altitude_sensor.height", SimLang.text(String.format("%.2f", height)).style(ChatFormatting.AQUA))
         .style(ChatFormatting.GRAY)
         .forGoggles(tooltip, 2);
      SimLang.translate("altitude_sensor.air_pressure", SimLang.text(String.format("%.2f%%", airPressure)).style(ChatFormatting.AQUA))
         .style(ChatFormatting.GRAY)
         .forGoggles(tooltip, 2);
      this.sendObserved(this.getBlockPos());
      return true;
   }

   @Override
   public void onObserved(Player player) {
      if (this.getAirPressure() <= 0.0) {
         SimAdvancements.CAN_WE_GET_MUCH_HIGHER.awardTo(player);
      }
   }

   public String getClipboardKey() {
      return "Altitude";
   }

   public boolean writeToClipboard(@NotNull Provider registries, CompoundTag tag, Direction side) {
      tag.putFloat("high_signal", this.highSignal);
      tag.putFloat("low_signal", this.lowSignal);
      return true;
   }

   public boolean readFromClipboard(@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
      if (simulate) {
         return true;
      } else {
         this.highSignal = tag.getFloat("high_signal");
         this.lowSignal = tag.getFloat("low_signal");
         this.setChanged();
         this.sendData();
         return true;
      }
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }
}

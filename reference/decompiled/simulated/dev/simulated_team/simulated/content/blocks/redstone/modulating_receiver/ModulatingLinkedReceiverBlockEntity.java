package dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import dev.simulated_team.simulated.content.blocks.redstone.AbstractLinkedReceiverBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ModulatingLinkedReceiverBlockEntity extends AbstractLinkedReceiverBlockEntity implements ClipboardCloneable {
   public static int RANGE_LIMIT = 256;
   public int minRange;
   public int maxRange;
   private double distanceToClosest = 0.0;
   private double oldDistanceToClosest = 0.0;
   private double clientDistance = 0.0;
   private double clientOldDistance = 0.0;

   public ModulatingLinkedReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.minRange = 8;
      this.maxRange = 64;
      this.setLazyTickRate(20);
   }

   public void lazyTick() {
      super.lazyTick();
      if (this.distanceToClosest != this.oldDistanceToClosest) {
         this.sendData();
      }
   }

   @Override
   public void tick() {
      super.tick();
      if (this.level.isClientSide()) {
         if (this.clientDistance == 0.0 && this.distanceToClosest != 0.0) {
            this.clientDistance = this.distanceToClosest;
            this.clientOldDistance = this.distanceToClosest;
         } else {
            double targetDelta = (this.distanceToClosest - this.clientDistance) / (double)this.lazyTickRate;
            double delta = Math.min(Math.abs(this.distanceToClosest - this.clientDistance), Math.abs(targetDelta));
            double sign = (double)Mth.sign(targetDelta);
            this.clientOldDistance = this.clientDistance;
            this.clientDistance += delta * sign;
         }
      }
   }

   public double getClientDistance(float pt) {
      return Mth.lerp((double)pt, this.clientOldDistance, this.clientDistance);
   }

   @Override
   public void updateSignal() {
      this.oldDistanceToClosest = this.distanceToClosest;
      this.distanceToClosest = (double)RANGE_LIMIT;
      super.updateSignal();
   }

   @Override
   public Tuple<Integer, Double> getSignalFromLink(Vec3 relativePosition, int transmittedStrength) {
      double distance = relativePosition.length();
      if (this.distanceToClosest > distance) {
         this.distanceToClosest = distance;
      }

      if (distance > (double)this.maxRange) {
         return new Tuple(0, 0.0);
      } else if (this.minRange == this.maxRange) {
         return new Tuple(transmittedStrength, distance);
      } else {
         double strengthScalar = Math.clamp((distance - (double)this.maxRange) / (double)(this.minRange - this.maxRange), 0.0, 1.0);
         return new Tuple((int)Math.ceil(strengthScalar * (double)transmittedStrength), distance);
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("MinRange", this.minRange);
      compound.putInt("MaxRange", this.maxRange);
      compound.putDouble("DistanceToClosest", this.distanceToClosest);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.distanceToClosest = compound.getDouble("DistanceToClosest");
      if (!clientPacket || !(Minecraft.getInstance().screen instanceof ModulatingLinkedReceiverScreen screen) || !screen.isThisBlock(this.getBlockPos())) {
         this.minRange = compound.getInt("MinRange");
         this.maxRange = compound.getInt("MaxRange");
      }

      super.read(compound, registries, clientPacket);
   }

   public String getClipboardKey() {
      return "LinkRange";
   }

   public boolean writeToClipboard(@NotNull Provider provider, CompoundTag tag, Direction direction) {
      tag.putInt("minRange", this.minRange);
      tag.putInt("maxRange", this.maxRange);
      return true;
   }

   public boolean readFromClipboard(@NotNull Provider provider, CompoundTag tag, Player player, Direction direction, boolean simulate) {
      if (!tag.contains("minRange")) {
         return false;
      } else if (simulate) {
         return true;
      } else {
         this.minRange = tag.getInt("minRange");
         this.maxRange = tag.getInt("maxRange");
         this.sendData();
         return true;
      }
   }

   public double getDistanceToClosest() {
      return this.distanceToClosest;
   }
}

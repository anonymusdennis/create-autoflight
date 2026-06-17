package dev.simulated_team.simulated.content.blocks.redstone;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.LinkBehaviour;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlock;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform.Dual;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Vector3d;

public abstract class AbstractLinkedReceiverBlockEntity extends SmartBlockEntity {
   protected LinkBehaviour link;
   protected int lastCheckedStatus;
   public int receivedSignal;
   public double rawSignalValue;
   protected boolean receivedSignalChanged;

   public AbstractLinkedReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.createLink();
      behaviours.add(this.link);
   }

   protected void createLink() {
      Pair<ValueBoxTransform, ValueBoxTransform> slots = Dual.makeSlots(LinkedReceiverFrequencySlot::new);
      this.link = LinkBehaviour.receiver(this, slots, signal -> {
      });
   }

   public void setSignal(int power, double rawValue) {
      if (this.receivedSignal != power || rawValue != this.rawSignalValue) {
         this.receivedSignalChanged = true;
      }

      this.receivedSignal = power;
      this.rawSignalValue = rawValue;
   }

   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         int networkStatus = Create.REDSTONE_LINK_NETWORK_HANDLER.globalPowerVersion.get();
         if (networkStatus != this.lastCheckedStatus) {
            this.lastCheckedStatus = networkStatus;
         }

         this.updateSignal();
         BlockState blockState = this.getBlockState();
         if (this.getReceivedSignal() > 0 != (Boolean)blockState.getValue(RedstoneLinkBlock.POWERED)) {
            this.receivedSignalChanged = true;
            this.level.setBlockAndUpdate(this.worldPosition, (BlockState)blockState.cycle(RedstoneLinkBlock.POWERED));
         }

         if (this.receivedSignalChanged) {
            Direction attachedFace = ((Direction)blockState.getValue(RedstoneLinkBlock.FACING)).getOpposite();
            BlockPos attachedPos = this.worldPosition.relative(attachedFace);
            this.level.blockUpdated(this.worldPosition, this.level.getBlockState(this.worldPosition).getBlock());
            this.level.blockUpdated(attachedPos, this.level.getBlockState(attachedPos).getBlock());
            this.receivedSignalChanged = false;
         }
      }
   }

   public void updateSignal() {
      int newSignal = 0;
      double rawValue = 0.0;
      Map<Couple<Frequency>, Set<IRedstoneLinkable>> map = Create.REDSTONE_LINK_NETWORK_HANDLER.networksIn(this.level);
      Couple<Frequency> freq = this.link.getNetworkKey();
      Set<IRedstoneLinkable> set = map.get(freq);
      if (set != null && !set.isEmpty()) {
         Vector3d currentPos = JOMLConversion.atCenterOf(this.getBlockPos());
         SubLevel subLevel = Sable.HELPER.getContaining(this);
         if (subLevel != null) {
            subLevel.logicalPose().transformPosition(currentPos);
         }

         for (IRedstoneLinkable link : set) {
            if (link.getTransmittedStrength() > 0) {
               Vector3d targetPos = JOMLConversion.atCenterOf(link.getLocation());
               SubLevel targetWs = Sable.HELPER.getContaining(this.level, link.getLocation());
               if (targetWs != null) {
                  targetWs.logicalPose().transformPosition(targetPos);
               }

               Vector3d relativePos = targetPos.sub(currentPos);
               if (subLevel != null) {
                  subLevel.logicalPose().transformNormalInverse(relativePos);
               }

               Tuple<Integer, Double> signal = this.getSignalFromLink(JOMLConversion.toMojang(relativePos), link.getTransmittedStrength());
               if ((Integer)signal.getA() > newSignal) {
                  newSignal = (Integer)signal.getA();
                  rawValue = (Double)signal.getB();
               }
            }
         }
      }

      this.setSignal(newSignal, rawValue);
   }

   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      compound.putInt("Receive", this.getReceivedSignal());
      compound.putDouble("ReceivedValue", this.rawSignalValue);
      compound.putBoolean("ReceivedChanged", this.receivedSignalChanged);
      super.write(compound, registries, clientPacket);
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      this.receivedSignal = compound.getInt("Receive");
      this.rawSignalValue = compound.getDouble("ReceivedValue");
      this.receivedSignalChanged = compound.getBoolean("ReceivedChanged");
   }

   public int getReceivedSignal() {
      return this.receivedSignal;
   }

   public Couple<Frequency> getFrequency() {
      return this.link.getNetworkKey();
   }

   public abstract Tuple<Integer, Double> getSignalFromLink(Vec3 var1, int var2);

   public void remove() {
      super.remove();
      Direction attachedFace = ((Direction)this.getBlockState().getValue(RedstoneLinkBlock.FACING)).getOpposite();
      BlockPos attachedPos = this.worldPosition.relative(attachedFace);
      this.level.blockUpdated(this.worldPosition, this.level.getBlockState(this.worldPosition).getBlock());
      this.level.blockUpdated(attachedPos, this.level.getBlockState(attachedPos).getBlock());
   }
}

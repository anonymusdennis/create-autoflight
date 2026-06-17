package com.simibubi.create.content.logistics.tunnel;

import com.mojang.datafixers.util.Pair;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;

public class BeltTunnelBlockEntity extends SmartBlockEntity {
   public Map<Direction, LerpedFloat> flaps;
   public Set<Direction> sides;
   protected IItemHandler cap = null;
   protected List<Pair<Direction, Boolean>> flapsToSend;

   public BeltTunnelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.flaps = new EnumMap<>(Direction.class);
      this.sides = new HashSet<>();
      this.flapsToSend = new LinkedList<>();
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.ANDESITE_TUNNEL.get(), (be, context) -> {
         if (be.cap == null && AllBlocks.BELT.has(be.level.getBlockState(be.worldPosition.below()))) {
            BlockEntity beBelow = be.level.getBlockEntity(be.worldPosition.below());
            if (beBelow != null) {
               IItemHandler capBelow = (IItemHandler)be.level.getCapability(ItemHandler.BLOCK, be.worldPosition.below(), Direction.UP);
               if (capBelow != null) {
                  be.cap = capBelow;
               }
            }
         }

         return be.cap;
      });
   }

   @Override
   public void invalidate() {
      super.invalidate();
      this.invalidateCapabilities();
   }

   protected void writeFlapsAndSides(CompoundTag compound) {
      ListTag flapsNBT = new ListTag();

      for (Direction direction : this.flaps.keySet()) {
         flapsNBT.add(IntTag.valueOf(direction.get3DDataValue()));
      }

      compound.put("Flaps", flapsNBT);
      ListTag sidesNBT = new ListTag();

      for (Direction direction : this.sides) {
         sidesNBT.add(IntTag.valueOf(direction.get3DDataValue()));
      }

      compound.put("Sides", sidesNBT);
   }

   @Override
   public void writeSafe(CompoundTag tag, Provider registries) {
      this.writeFlapsAndSides(tag);
      super.writeSafe(tag, registries);
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.writeFlapsAndSides(compound);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      Set<Direction> newFlaps = new HashSet<>(6);

      for (Tag inbt : compound.getList("Flaps", 3)) {
         if (inbt instanceof IntTag) {
            newFlaps.add(Direction.from3DDataValue(((IntTag)inbt).getAsInt()));
         }
      }

      this.sides.clear();

      for (Tag inbtx : compound.getList("Sides", 3)) {
         if (inbtx instanceof IntTag) {
            this.sides.add(Direction.from3DDataValue(((IntTag)inbtx).getAsInt()));
         }
      }

      for (Direction d : Iterate.directions) {
         if (!newFlaps.contains(d)) {
            this.flaps.remove(d);
         } else if (!this.flaps.containsKey(d)) {
            this.flaps.put(d, this.createChasingFlap());
         }
      }

      if (!compound.contains("Sides") && compound.contains("Flaps")) {
         this.sides.addAll(this.flaps.keySet());
      }

      super.read(compound, registries, clientPacket);
      if (clientPacket) {
         CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> VisualizationHelper.queueUpdate(this));
      }
   }

   private LerpedFloat createChasingFlap() {
      return LerpedFloat.linear().startWithValue(0.25).chase(0.0, 0.05F, Chaser.EXP);
   }

   public void updateTunnelConnections() {
      this.flaps.clear();
      this.sides.clear();
      BlockState tunnelState = this.getBlockState();

      for (Direction direction : Iterate.horizontalDirections) {
         if (direction.getAxis() != tunnelState.getValue(BlockStateProperties.HORIZONTAL_AXIS)) {
            boolean positive = direction.getAxisDirection() == AxisDirection.POSITIVE ^ direction.getAxis() == Axis.Z;
            BeltTunnelBlock.Shape shape = (BeltTunnelBlock.Shape)tunnelState.getValue(BeltTunnelBlock.SHAPE);
            if (BeltTunnelBlock.isStraight(tunnelState)
               || positive && shape == BeltTunnelBlock.Shape.T_LEFT
               || !positive && shape == BeltTunnelBlock.Shape.T_RIGHT) {
               continue;
            }
         }

         this.sides.add(direction);
         if (this.level != null) {
            BlockState nextState = this.level.getBlockState(this.worldPosition.relative(direction));
            if (!(nextState.getBlock() instanceof BeltTunnelBlock)
               && (
                  !(nextState.getBlock() instanceof BeltFunnelBlock)
                     || nextState.getValue(BeltFunnelBlock.SHAPE) != BeltFunnelBlock.Shape.EXTENDED
                     || nextState.getValue(BeltFunnelBlock.HORIZONTAL_FACING) != direction.getOpposite()
               )) {
               this.flaps.put(direction, this.createChasingFlap());
            }
         }
      }

      this.sendData();
   }

   public void flap(Direction side, boolean inward) {
      if (this.level.isClientSide) {
         if (this.flaps.containsKey(side)) {
            this.flaps.get(side).setValue(inward ? -1.0 : 1.0);
         }
      } else {
         this.flapsToSend.add(Pair.of(side, inward));
      }
   }

   @Override
   public void initialize() {
      super.initialize();
      this.updateTunnelConnections();
   }

   @Override
   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         if (!this.flapsToSend.isEmpty()) {
            this.sendFlaps();
         }
      } else {
         this.flaps.forEach((d, value) -> value.tickChaser());
      }
   }

   private void sendFlaps() {
      if (this.level instanceof ServerLevel serverLevel) {
         CatnipServices.NETWORK.sendToClientsTrackingChunk(serverLevel, new ChunkPos(this.worldPosition), new TunnelFlapPacket(this, this.flapsToSend));
      }

      this.flapsToSend.clear();
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }
}

package com.simibubi.create.content.logistics.packagePort;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlock;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import io.netty.buffer.ByteBuf;
import java.util.Map;
import java.util.Optional;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public abstract class PackagePortTarget {
   public static final Codec<PackagePortTarget> CODEC = CreateBuiltInRegistries.PACKAGE_PORT_TARGET_TYPE
      .byNameCodec()
      .dispatch(PackagePortTarget::getType, PackagePortTargetType::codec);
   public static final StreamCodec<? super RegistryFriendlyByteBuf, PackagePortTarget> STREAM_CODEC = ByteBufCodecs.registry(
         CreateRegistries.PACKAGE_PORT_TARGET_TYPE
      )
      .dispatch(PackagePortTarget::getType, PackagePortTargetType::streamCodec);
   public BlockPos relativePos;

   public PackagePortTarget(BlockPos relativePos) {
      this.relativePos = relativePos;
   }

   public abstract boolean export(LevelAccessor var1, BlockPos var2, ItemStack var3, boolean var4);

   public void setup(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
   }

   public void register(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
   }

   public void deregister(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
   }

   public abstract Vec3 getExactTargetLocation(PackagePortBlockEntity var1, LevelAccessor var2, BlockPos var3);

   public abstract ItemStack getIcon();

   public abstract boolean canSupport(BlockEntity var1);

   public boolean depositImmediately() {
      return false;
   }

   protected abstract PackagePortTargetType getType();

   public BlockEntity be(LevelAccessor level, BlockPos portPos) {
      if (level instanceof Level l && !l.isLoaded(portPos.offset(this.relativePos))) {
         return null;
      }

      return level.getBlockEntity(portPos.offset(this.relativePos));
   }

   public static class ChainConveyorFrogportTarget extends PackagePortTarget {
      public static final MapCodec<PackagePortTarget.ChainConveyorFrogportTarget> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(
                  BlockPos.CODEC.fieldOf("relative_pos").forGetter(i -> i.relativePos),
                  Codec.FLOAT.fieldOf("chain_pos").forGetter(i -> i.chainPos),
                  BlockPos.CODEC.optionalFieldOf("connection").forGetter(i -> Optional.ofNullable(i.connection)),
                  Codec.BOOL.fieldOf("flipped").forGetter(i -> i.flipped)
               )
               .apply(instance, PackagePortTarget.ChainConveyorFrogportTarget::new)
      );
      public static final StreamCodec<ByteBuf, PackagePortTarget.ChainConveyorFrogportTarget> STREAM_CODEC = StreamCodec.composite(
         BlockPos.STREAM_CODEC,
         i -> i.relativePos,
         ByteBufCodecs.FLOAT,
         i -> i.chainPos,
         CatnipStreamCodecBuilders.nullable(BlockPos.STREAM_CODEC),
         i -> i.connection,
         ByteBufCodecs.BOOL,
         i -> i.flipped,
         PackagePortTarget.ChainConveyorFrogportTarget::new
      );
      public float chainPos;
      @Nullable
      public BlockPos connection;
      public boolean flipped;

      public ChainConveyorFrogportTarget(BlockPos relativePos, float chainPos, Optional<BlockPos> connection, boolean flipped) {
         this(relativePos, chainPos, connection.orElse(null), flipped);
      }

      public ChainConveyorFrogportTarget(BlockPos relativePos, float chainPos, @Nullable BlockPos connection, boolean flipped) {
         super(relativePos);
         this.chainPos = chainPos;
         this.connection = connection;
         this.flipped = flipped;
      }

      @Override
      public void setup(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
         if (this.be(level, portPos) instanceof ChainConveyorBlockEntity clbe) {
            this.flipped = clbe.getSpeed() < 0.0F;
         }
      }

      @Override
      public ItemStack getIcon() {
         return AllBlocks.CHAIN_CONVEYOR.asStack();
      }

      @Override
      public boolean export(LevelAccessor level, BlockPos portPos, ItemStack box, boolean simulate) {
         if (!(this.be(level, portPos) instanceof ChainConveyorBlockEntity clbe)) {
            return false;
         } else if (this.connection != null && !clbe.connections.contains(this.connection)) {
            return false;
         } else if (!simulate) {
            ChainConveyorPackage box2 = new ChainConveyorPackage(this.chainPos, box.copy());
            return this.connection == null ? clbe.addLoopingPackage(box2) : clbe.addTravellingPackage(box2, this.connection);
         } else {
            return clbe.getSpeed() != 0.0F && clbe.canAcceptPackagesFor(this.connection);
         }
      }

      @Override
      public void register(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
         if (this.be(level, portPos) instanceof ChainConveyorBlockEntity clbe) {
            ChainConveyorBlockEntity var8 = clbe;
            if (this.connection != null && clbe.getSpeed() < 0.0F != this.flipped) {
               this.deregister(ppbe, level, portPos);
               var8 = ((ChainConveyorBlock)AllBlocks.CHAIN_CONVEYOR.get()).getBlockEntity(level, clbe.getBlockPos().offset(this.connection));
               if (var8 == null) {
                  return;
               }

               clbe.prepareStats();
               ChainConveyorBlockEntity.ConnectionStats stats = clbe.connectionStats.get(this.connection);
               if (stats != null) {
                  this.chainPos = stats.chainLength() - this.chainPos;
               }

               this.connection = this.connection.multiply(-1);
               this.flipped = !this.flipped;
               this.relativePos = var8.getBlockPos().subtract(portPos);
               ppbe.notifyUpdate();
            }

            if (this.connection == null || var8.connections.contains(this.connection)) {
               String portFilter = ppbe.getFilterString();
               if (portFilter != null) {
                  var8.routingTable.receivePortInfo(portFilter, this.connection == null ? BlockPos.ZERO : this.connection);
                  Map<BlockPos, ChainConveyorBlockEntity.ConnectedPort> portMap = this.connection == null ? var8.loopPorts : var8.travelPorts;
                  portMap.put(this.relativePos.multiply(-1), new ChainConveyorBlockEntity.ConnectedPort(this.chainPos, this.connection, portFilter));
               }
            }
         }
      }

      @Override
      public void deregister(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
         if (this.be(level, portPos) instanceof ChainConveyorBlockEntity clbe) {
            clbe.loopPorts.remove(this.relativePos.multiply(-1));
            clbe.travelPorts.remove(this.relativePos.multiply(-1));
            String portFilter = ppbe.getFilterString();
            if (portFilter != null) {
               clbe.routingTable.entriesByDistance.removeIf(e -> e.endOfRoute() && e.port().equals(portFilter));
               clbe.routingTable.changed = true;
            }
         }
      }

      @Override
      public Vec3 getExactTargetLocation(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
         return this.be(level, portPos) instanceof ChainConveyorBlockEntity clbe ? clbe.getPackagePosition(this.chainPos, this.connection) : Vec3.ZERO;
      }

      @Override
      public boolean canSupport(BlockEntity be) {
         return AllBlockEntityTypes.PACKAGE_FROGPORT.is(be);
      }

      @Override
      protected PackagePortTargetType getType() {
         return (PackagePortTargetType)AllPackagePortTargetTypes.CHAIN_CONVEYOR.value();
      }

      public static class Type implements PackagePortTargetType {
         @Override
         public MapCodec<PackagePortTarget.ChainConveyorFrogportTarget> codec() {
            return PackagePortTarget.ChainConveyorFrogportTarget.CODEC;
         }

         @Override
         public StreamCodec<ByteBuf, PackagePortTarget.ChainConveyorFrogportTarget> streamCodec() {
            return PackagePortTarget.ChainConveyorFrogportTarget.STREAM_CODEC;
         }
      }
   }

   public static class TrainStationFrogportTarget extends PackagePortTarget {
      public static MapCodec<PackagePortTarget.TrainStationFrogportTarget> CODEC = RecordCodecBuilder.mapCodec(
         instance -> instance.group(BlockPos.CODEC.fieldOf("relative_pos").forGetter(i -> i.relativePos))
               .apply(instance, PackagePortTarget.TrainStationFrogportTarget::new)
      );
      public static final StreamCodec<ByteBuf, PackagePortTarget.TrainStationFrogportTarget> STREAM_CODEC = BlockPos.STREAM_CODEC
         .map(PackagePortTarget.TrainStationFrogportTarget::new, i -> i.relativePos);

      public TrainStationFrogportTarget(BlockPos relativePos) {
         super(relativePos);
      }

      @Override
      public ItemStack getIcon() {
         return AllBlocks.TRACK_STATION.asStack();
      }

      @Override
      public boolean export(LevelAccessor level, BlockPos portPos, ItemStack box, boolean simulate) {
         return false;
      }

      @Override
      public Vec3 getExactTargetLocation(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
         return Vec3.atCenterOf(portPos.offset(this.relativePos));
      }

      @Override
      public void register(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
         if (this.be(level, portPos) instanceof StationBlockEntity sbe) {
            sbe.attachPackagePort(ppbe);
         }
      }

      @Override
      public void deregister(PackagePortBlockEntity ppbe, LevelAccessor level, BlockPos portPos) {
         if (this.be(level, portPos) instanceof StationBlockEntity sbe) {
            sbe.removePackagePort(ppbe);
         }
      }

      @Override
      public boolean depositImmediately() {
         return true;
      }

      @Override
      public boolean canSupport(BlockEntity be) {
         return AllBlockEntityTypes.PACKAGE_POSTBOX.is(be);
      }

      @Override
      protected PackagePortTargetType getType() {
         return (PackagePortTargetType)AllPackagePortTargetTypes.TRAIN_STATION.value();
      }

      public static class Type implements PackagePortTargetType {
         @Override
         public MapCodec<PackagePortTarget.TrainStationFrogportTarget> codec() {
            return PackagePortTarget.TrainStationFrogportTarget.CODEC;
         }

         @Override
         public StreamCodec<ByteBuf, PackagePortTarget.TrainStationFrogportTarget> streamCodec() {
            return PackagePortTarget.TrainStationFrogportTarget.STREAM_CODEC;
         }
      }
   }
}

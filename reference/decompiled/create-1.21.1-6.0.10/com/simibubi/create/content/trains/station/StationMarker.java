package com.simibubi.create.content.trains.station;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllMapDecorationTypes;
import com.simibubi.create.content.trains.track.TrackTargetingBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.Objects;
import java.util.Optional;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public class StationMarker {
   private final BlockPos source;
   private final BlockPos target;
   private final Component name;
   private final String id;

   public StationMarker(BlockPos source, BlockPos target, Component name) {
      this.source = source;
      this.target = target;
      this.name = name;
      this.id = "create:station-" + target.getX() + "," + target.getY() + "," + target.getZ();
   }

   public static StationMarker load(CompoundTag tag, Provider registries) {
      BlockPos source = NBTHelper.readBlockPos(tag, "source");
      BlockPos target = NBTHelper.readBlockPos(tag, "target");
      Component name = Serializer.fromJson(tag.getString("name"), registries);
      if (name == null) {
         name = CommonComponents.EMPTY;
      }

      return new StationMarker(source, target, name);
   }

   public static StationMarker fromWorld(BlockGetter level, BlockPos pos) {
      Optional<StationBlockEntity> stationOption = AllBlockEntityTypes.TRACK_STATION.get(level, pos);
      if (!stationOption.isEmpty() && stationOption.get().getStation() != null) {
         String name = stationOption.get().getStation().name;
         return new StationMarker(
            pos, BlockEntityBehaviour.get(stationOption.get(), TrackTargetingBehaviour.TYPE).getPositionForMapMarker(), Component.literal(name)
         );
      } else {
         return null;
      }
   }

   public CompoundTag save(Provider registries) {
      CompoundTag tag = new CompoundTag();
      tag.put("source", NbtUtils.writeBlockPos(this.source));
      tag.put("target", NbtUtils.writeBlockPos(this.target));
      tag.putString("name", Serializer.toJson(this.name, registries));
      return tag;
   }

   public BlockPos getSource() {
      return this.source;
   }

   public BlockPos getTarget() {
      return this.target;
   }

   public Component getName() {
      return this.name;
   }

   public String getId() {
      return this.id;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         StationMarker that = (StationMarker)o;
         return !this.target.equals(that.target) ? false : this.name.equals(that.name);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.target, this.name);
   }

   public static MapDecoration createStationDecoration(byte x, byte y, Optional<Component> name) {
      return new MapDecoration(AllMapDecorationTypes.STATION_MAP_DECORATION, x, y, (byte)0, name);
   }
}

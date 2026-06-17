package com.simibubi.create.content.logistics.packagerLink;

import com.simibubi.create.Create;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class LogisticsNetwork {
   public UUID id;
   public RequestPromiseQueue panelPromises;
   public Set<GlobalPos> totalLinks;
   public Set<GlobalPos> loadedLinks;
   public UUID owner;
   public boolean locked;

   public LogisticsNetwork(UUID networkId) {
      this.id = networkId;
      this.panelPromises = new RequestPromiseQueue(Create.LOGISTICS::markDirty);
      this.totalLinks = new HashSet<>();
      this.loadedLinks = new HashSet<>();
      this.owner = null;
      this.locked = false;
   }

   public CompoundTag write(Provider registries) {
      CompoundTag tag = new CompoundTag();
      tag.putUUID("Id", this.id);
      tag.put("Promises", this.panelPromises.write(registries));
      tag.put("Links", NBTHelper.writeCompoundList(this.totalLinks, p -> {
         CompoundTag nbt = new CompoundTag();
         nbt.put("Pos", NbtUtils.writeBlockPos(p.pos()));
         if (p.dimension() != Level.OVERWORLD) {
            NBTHelper.writeResourceLocation(nbt, "Dim", p.dimension().location());
         }

         return nbt;
      }));
      if (this.owner != null) {
         tag.putUUID("Owner", this.owner);
      }

      tag.putBoolean("Locked", this.locked);
      return tag;
   }

   public static LogisticsNetwork read(CompoundTag tag, Provider registries) {
      LogisticsNetwork network = new LogisticsNetwork(tag.getUUID("Id"));
      network.panelPromises = RequestPromiseQueue.read(tag.getCompound("Promises"), registries, Create.LOGISTICS::markDirty);
      NBTHelper.iterateCompoundList(
         tag.getList("Links", 10),
         nbt -> network.totalLinks
               .add(
                  GlobalPos.of(
                     nbt.contains("Dim") ? ResourceKey.create(Registries.DIMENSION, NBTHelper.readResourceLocation(nbt, "Dim")) : Level.OVERWORLD,
                     NBTHelper.readBlockPos(nbt, "Pos")
                  )
               )
      );
      network.owner = tag.contains("Owner") ? tag.getUUID("Owner") : null;
      network.locked = tag.getBoolean("Locked");
      return network;
   }
}

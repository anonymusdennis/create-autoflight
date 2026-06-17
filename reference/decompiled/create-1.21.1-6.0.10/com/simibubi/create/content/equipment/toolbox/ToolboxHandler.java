package com.simibubi.create.content.equipment.toolbox;

import com.simibubi.create.foundation.networking.ISyncPersistentData;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class ToolboxHandler {
   public static final WorldAttached<WeakHashMap<BlockPos, ToolboxBlockEntity>> toolboxes = new WorldAttached(w -> new WeakHashMap());
   static int validationTimer = 20;

   public static void onLoad(ToolboxBlockEntity be) {
      ((WeakHashMap)toolboxes.get(be.getLevel())).put(be.getBlockPos(), be);
   }

   public static void onUnload(ToolboxBlockEntity be) {
      ((WeakHashMap)toolboxes.get(be.getLevel())).remove(be.getBlockPos());
   }

   public static void entityTick(Entity entity, Level world) {
      if (!world.isClientSide) {
         if (world instanceof ServerLevel) {
            if (entity instanceof ServerPlayer player) {
               if (entity.tickCount % validationTimer == 0) {
                  if (player.getPersistentData().contains("CreateToolboxData")) {
                     boolean sendData = false;
                     CompoundTag compound = player.getPersistentData().getCompound("CreateToolboxData");

                     for (int i = 0; i < 9; i++) {
                        String key = String.valueOf(i);
                        if (compound.contains(key)) {
                           CompoundTag data = compound.getCompound(key);
                           BlockPos pos = NBTHelper.readBlockPos(data, "Pos");
                           int slot = data.getInt("Slot");
                           if (world.isLoaded(pos)) {
                              if (!(world.getBlockState(pos).getBlock() instanceof ToolboxBlock)) {
                                 compound.remove(key);
                                 sendData = true;
                              } else {
                                 BlockEntity prevBlockEntity = world.getBlockEntity(pos);
                                 if (prevBlockEntity instanceof ToolboxBlockEntity) {
                                    ((ToolboxBlockEntity)prevBlockEntity).connectPlayer(slot, player, i);
                                 }
                              }
                           }
                        }
                     }

                     if (sendData) {
                        syncData(player);
                     }
                  }
               }
            }
         }
      }
   }

   public static void playerLogin(Player player) {
      if (player instanceof ServerPlayer) {
         if (player.getPersistentData().contains("CreateToolboxData") && !player.getPersistentData().getCompound("CreateToolboxData").isEmpty()) {
            syncData(player);
         }
      }
   }

   public static void syncData(Player player) {
      CatnipServices.NETWORK.sendToClient((ServerPlayer)player, new ISyncPersistentData.PersistentDataPacket(player));
   }

   public static List<ToolboxBlockEntity> getNearest(LevelAccessor world, Player player, int maxAmount) {
      Vec3 location = player.position();
      double maxRange = getMaxRange(player);
      return ((WeakHashMap)toolboxes.get(world))
         .keySet()
         .stream()
         .filter(p -> distance(location, p) < maxRange * maxRange)
         .sorted((p1, p2) -> Double.compare(distance(location, p1), distance(location, p2)))
         .limit((long)maxAmount)
         .map(((WeakHashMap)toolboxes.get(world))::get)
         .filter(ToolboxBlockEntity::isFullyInitialized)
         .collect(Collectors.toList());
   }

   public static void unequip(Player player, int hotbarSlot, boolean keepItems) {
      CompoundTag compound = player.getPersistentData().getCompound("CreateToolboxData");
      Level world = player.level();
      String key = String.valueOf(hotbarSlot);
      if (compound.contains(key)) {
         CompoundTag prevData = compound.getCompound(key);
         BlockPos prevPos = NBTHelper.readBlockPos(prevData, "Pos");
         int prevSlot = prevData.getInt("Slot");
         if (world.getBlockEntity(prevPos) instanceof ToolboxBlockEntity toolbox) {
            toolbox.unequip(prevSlot, player, hotbarSlot, keepItems || !withinRange(player, toolbox));
         }

         compound.remove(key);
      }
   }

   public static boolean withinRange(Player player, ToolboxBlockEntity box) {
      if (player.level() != box.getLevel()) {
         return false;
      } else {
         double maxRange = getMaxRange(player);
         return distance(player.position(), box.getBlockPos()) < maxRange * maxRange;
      }
   }

   public static double distance(Vec3 location, BlockPos p) {
      return location.distanceToSqr((double)((float)p.getX() + 0.5F), (double)p.getY(), (double)((float)p.getZ() + 0.5F));
   }

   public static double getMaxRange(Player player) {
      return ((Integer)AllConfigs.server().equipment.toolboxRange.get()).doubleValue();
   }
}

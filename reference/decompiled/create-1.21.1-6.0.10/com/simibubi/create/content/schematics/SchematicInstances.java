package com.simibubi.create.content.schematics;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.contraptions.StructureTransform;
import java.util.concurrent.TimeUnit;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jetbrains.annotations.Nullable;

public class SchematicInstances {
   private static final WorldAttached<Cache<Integer, SchematicLevel>> LOADED_SCHEMATICS = new WorldAttached(
      $ -> CacheBuilder.newBuilder().expireAfterAccess(5L, TimeUnit.MINUTES).build()
   );

   @Nullable
   public static SchematicLevel get(Level world, ItemStack schematic) {
      Cache<Integer, SchematicLevel> map = (Cache<Integer, SchematicLevel>)LOADED_SCHEMATICS.get(world);
      int hash = getHash(schematic);
      SchematicLevel ifPresent = (SchematicLevel)map.getIfPresent(hash);
      if (ifPresent != null) {
         return ifPresent;
      } else {
         SchematicLevel loadWorld = loadWorld(world, schematic);
         if (loadWorld == null) {
            return null;
         } else {
            map.put(hash, loadWorld);
            return loadWorld;
         }
      }
   }

   private static SchematicLevel loadWorld(Level wrapped, ItemStack schematic) {
      if (schematic == null || !schematic.has(AllDataComponents.SCHEMATIC_FILE)) {
         return null;
      } else if (!schematic.has(AllDataComponents.SCHEMATIC_DEPLOYED)) {
         return null;
      } else {
         StructureTemplate activeTemplate = SchematicItem.loadSchematic(wrapped, schematic);
         if (activeTemplate.getSize().equals(Vec3i.ZERO)) {
            return null;
         } else {
            BlockPos anchor = (BlockPos)schematic.get(AllDataComponents.SCHEMATIC_ANCHOR);
            SchematicLevel world = new SchematicLevel(anchor, wrapped);
            StructurePlaceSettings settings = SchematicItem.getSettings(schematic);
            activeTemplate.placeInWorld(world, anchor, anchor, settings, wrapped.getRandom(), 2);
            StructureTransform transform = new StructureTransform(settings.getRotationPivot(), Axis.Y, settings.getRotation(), settings.getMirror());

            for (BlockEntity be : world.getBlockEntities()) {
               transform.apply(be);
            }

            return world;
         }
      }
   }

   public static void clearHash(ItemStack schematic) {
      if (schematic != null && schematic.has(AllDataComponents.SCHEMATIC_FILE)) {
         schematic.remove(AllDataComponents.SCHEMATIC_HASH);
      }
   }

   public static int getHash(ItemStack schematic) {
      if (schematic != null && schematic.has(AllDataComponents.SCHEMATIC_FILE)) {
         if (!schematic.has(AllDataComponents.SCHEMATIC_HASH)) {
            schematic.set(AllDataComponents.SCHEMATIC_HASH, schematic.getComponentsPatch().hashCode());
         }

         return (Integer)schematic.getOrDefault(AllDataComponents.SCHEMATIC_HASH, -1);
      } else {
         return -1;
      }
   }
}

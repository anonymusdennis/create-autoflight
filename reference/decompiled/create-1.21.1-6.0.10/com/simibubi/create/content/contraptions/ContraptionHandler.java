package com.simibubi.create.content.contraptions;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.data.WorldAttached;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ContraptionHandler {
   public static WorldAttached<Map<Integer, WeakReference<AbstractContraptionEntity>>> loadedContraptions = new WorldAttached($ -> new HashMap());
   static WorldAttached<List<AbstractContraptionEntity>> queuedAdditions = new WorldAttached($ -> ObjectLists.synchronize(new ObjectArrayList()));

   public static void tick(Level world) {
      Map<Integer, WeakReference<AbstractContraptionEntity>> map = (Map<Integer, WeakReference<AbstractContraptionEntity>>)loadedContraptions.get(world);
      List<AbstractContraptionEntity> queued = (List<AbstractContraptionEntity>)queuedAdditions.get(world);

      for (AbstractContraptionEntity contraptionEntity : queued) {
         map.put(contraptionEntity.getId(), new WeakReference<>(contraptionEntity));
      }

      queued.clear();
      Collection<WeakReference<AbstractContraptionEntity>> values = map.values();
      Iterator<WeakReference<AbstractContraptionEntity>> iterator = values.iterator();

      while (iterator.hasNext()) {
         WeakReference<AbstractContraptionEntity> weakReference = iterator.next();
         AbstractContraptionEntity contraptionEntity = weakReference.get();
         if (contraptionEntity == null || !contraptionEntity.isAliveOrStale()) {
            iterator.remove();
         } else if (!contraptionEntity.isAlive()) {
            contraptionEntity.staleTicks--;
         } else {
            ContraptionCollider.collideEntities(contraptionEntity);
         }
      }
   }

   public static void addSpawnedContraptionsToCollisionList(Entity entity, Level world) {
      if (entity instanceof AbstractContraptionEntity) {
         ((List)queuedAdditions.get(world)).add((AbstractContraptionEntity)entity);
      }
   }

   public static void entitiesWhoJustDismountedGetSentToTheRightLocation(LivingEntity entityLiving, Level world) {
      if (world.isClientSide) {
         CompoundTag data = entityLiving.getPersistentData();
         if (data.contains("ContraptionDismountLocation")) {
            Vec3 position = VecHelper.readNBT(data.getList("ContraptionDismountLocation", 6));
            if (entityLiving.getVehicle() == null) {
               entityLiving.absMoveTo(position.x, position.y, position.z, entityLiving.getYRot(), entityLiving.getXRot());
            }

            data.remove("ContraptionDismountLocation");
            entityLiving.setOnGround(false);
         }
      }
   }
}

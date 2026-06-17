package dev.ryanhcode.sable.api;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinterface.EntityExtension;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.function.BiFunction;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;
import org.joml.Vector3dc;

@Internal
public final class SubLevelHelper {
   private static final ThreadLocal<SubLevelHelper.EntityRot> oldRot = ThreadLocal.withInitial(SubLevelHelper.EntityRot::new);
   private static final ObjectList<BiFunction<Vector3dc, Level, Vector3dc>> windProviders = new ObjectArrayList();

   public static void pushEntityLocal(SubLevel subLevel, Entity entity) {
      pushEntityLocal(subLevel, entity, Anchor.FEET);
   }

   public static void popEntityLocal(SubLevel subLevel, Entity player) {
      popEntityLocal(subLevel, player, Anchor.FEET);
   }

   public static void pushEntityLocal(SubLevel subLevel, Entity entity, Anchor anchor) {
      if (anchor == Anchor.FEET) {
         ((EntityExtension)entity).sable$setPosSuperRaw(subLevel.logicalPose().transformPositionInverse(entity.position()));
      } else {
         ((EntityExtension)entity)
            .sable$setPosSuperRaw(subLevel.logicalPose().transformPositionInverse(entity.getEyePosition()).add(0.0, (double)(-entity.getEyeHeight()), 0.0));
      }

      Vec3 playerLookAngle = entity.getLookAngle();
      playerLookAngle = subLevel.logicalPose().transformNormalInverse(playerLookAngle);
      oldRot.get().copy(entity);
      Vec3 pTarget = entity.getEyePosition().add(playerLookAngle);
      Vec3 vec3 = entity.getEyePosition();
      double d0 = pTarget.x - vec3.x;
      double d1 = pTarget.y - vec3.y;
      double d2 = pTarget.z - vec3.z;
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      entity.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * 180.0F / (float)Math.PI))));
      entity.setYRot(Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) - 90.0F));
      entity.setYHeadRot(entity.getYRot());
      entity.setDeltaMovement(subLevel.logicalPose().transformNormalInverse(entity.getDeltaMovement()));
   }

   public static void popEntityLocal(SubLevel subLevel, Entity entity, Anchor anchor) {
      if (anchor == Anchor.FEET) {
         ((EntityExtension)entity).sable$setPosSuperRaw(subLevel.logicalPose().transformPosition(entity.position()));
      } else {
         ((EntityExtension)entity)
            .sable$setPosSuperRaw(subLevel.logicalPose().transformPosition(entity.getEyePosition()).add(0.0, (double)(-entity.getEyeHeight()), 0.0));
      }

      oldRot.get().apply(entity);
      entity.setDeltaMovement(subLevel.logicalPose().transformNormal(entity.getDeltaMovement()));
   }

   public static Vector3d getVelocityRelativeToAir(Level level, Vector3dc pos, Vector3d dest) {
      Vector3d probePos = new Vector3d(pos);
      Vector3d velocity = Sable.HELPER.getVelocity(level, pos, dest);
      ObjectListIterator var5 = windProviders.iterator();

      while (var5.hasNext()) {
         BiFunction<Vector3dc, Level, Vector3dc> windProvider = (BiFunction<Vector3dc, Level, Vector3dc>)var5.next();
         Vector3dc airVelocity = windProvider.apply(probePos, level);
         if (airVelocity != null) {
            velocity.sub(airVelocity);
         }
      }

      return velocity;
   }

   public static void registerWindProvider(BiFunction<Vector3dc, Level, Vector3dc> function) {
      windProviders.add(function);
   }

   public static Collection<ServerSubLevel> getLoadingDependencyChain(ServerSubLevel subLevel) {
      ObjectOpenHashSet<ServerSubLevel> visited = new ObjectOpenHashSet();
      ObjectOpenHashSet<ServerSubLevel> frontier = new ObjectOpenHashSet();
      frontier.add(subLevel);

      while (!frontier.isEmpty()) {
         ServerSubLevel current = (ServerSubLevel)frontier.iterator().next();
         frontier.remove(current);
         visited.add(current);

         for (SubLevel neighbor : Sable.HELPER.getAllIntersecting(current.getLevel(), new BoundingBox3d(current.boundingBox()))) {
            ServerSubLevel serverNeighbor = (ServerSubLevel)neighbor;
            if (!visited.contains(serverNeighbor)) {
               frontier.add(serverNeighbor);
            }
         }

         for (BlockEntitySubLevelActor actor : current.getPlot().getBlockEntityActors()) {
            Iterable<SubLevel> loadingDependencies = actor.sable$getLoadingDependencies();
            if (loadingDependencies != null) {
               for (SubLevel dependency : loadingDependencies) {
                  ServerSubLevel serverDependency = (ServerSubLevel)dependency;
                  if (!visited.contains(serverDependency)) {
                     frontier.add(serverDependency);
                  }
               }
            }
         }
      }

      return visited;
   }

   public static Collection<SubLevel> getConnectedChain(SubLevel subLevel) {
      ObjectOpenHashSet<SubLevel> visited = new ObjectOpenHashSet();
      ObjectOpenHashSet<SubLevel> frontier = new ObjectOpenHashSet();
      frontier.add(subLevel);

      while (!frontier.isEmpty()) {
         SubLevel current = (SubLevel)frontier.iterator().next();
         frontier.remove(current);
         visited.add(current);

         for (BlockEntitySubLevelActor actor : current.getPlot().getBlockEntityActors()) {
            Iterable<SubLevel> dependencies = actor.sable$getConnectionDependencies();
            if (dependencies != null) {
               for (SubLevel dependency : dependencies) {
                  if (!visited.contains(dependency)) {
                     frontier.add(dependency);
                  }
               }
            }
         }
      }

      return visited;
   }

   private static class EntityRot {
      private float xRot;
      private float yRot;
      private float yHeadRot;

      public void apply(Entity entity) {
         entity.setXRot(this.xRot);
         entity.setYRot(this.yRot);
         entity.setYHeadRot(this.yHeadRot);
      }

      public void copy(Entity entity) {
         this.xRot = entity.getXRot();
         this.yRot = entity.getYRot();
         this.yHeadRot = entity.getYHeadRot();
      }
   }
}

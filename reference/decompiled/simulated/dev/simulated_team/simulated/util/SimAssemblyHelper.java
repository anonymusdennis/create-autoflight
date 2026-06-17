package dev.simulated_team.simulated.util;

import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper.AssemblyTransform;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.heat.SubLevelHeatMapManager;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin.accessor.ContraptionAccessor;
import dev.simulated_team.simulated.mixin.accessor.ControlledContraptionEntityAccessor;
import dev.simulated_team.simulated.mixin_interface.create_assembly.IControlContraptionExtension;
import dev.simulated_team.simulated.util.assembly.SimAssemblyContraption;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SimAssemblyHelper {
   public static void disassembleSubLevel(
      @NotNull Level level,
      @NotNull SubLevel toDisassemble,
      @NotNull BlockPos subLevelAnchor,
      @NotNull BlockPos disassemblyGoal,
      @NotNull Rotation rotation,
      @NotNull boolean playSound
   ) {
      if (playSound) {
         level.playSound(null, subLevelAnchor, SimSoundEvents.SIMULATED_CONTRAPTION_STOPS.event(), SoundSource.BLOCKS, 1.0F, 1.0F);
      }

      BoundingBox3i plotBounds = new BoundingBox3i(toDisassemble.getPlot().getBoundingBox());
      AssemblyTransform transform = new AssemblyTransform(
         subLevelAnchor, disassemblyGoal, rotation == Rotation.NONE ? 0 : 4 - rotation.ordinal(), rotation, (ServerLevel)level
      );
      ObjectArrayList<BlockPos> blocks = new ObjectArrayList();
      LevelPlot plot = toDisassemble.getPlot();

      for (PlotChunkHolder chunk : plot.getLoadedChunks()) {
         BoundingBox3ic localChunkBounds = chunk.getBoundingBox();
         if (localChunkBounds != null && localChunkBounds != BoundingBox3i.EMPTY) {
            for (int x = localChunkBounds.minX(); x <= localChunkBounds.maxX(); x++) {
               for (int y = localChunkBounds.minY(); y <= localChunkBounds.maxY(); y++) {
                  for (int z = localChunkBounds.minZ(); z <= localChunkBounds.maxZ(); z++) {
                     BlockPos pos = new BlockPos(x + chunk.getPos().getMinBlockX(), y, z + chunk.getPos().getMinBlockZ());
                     BlockState state = level.getBlockState(pos);
                     if (!state.isAir()) {
                        blocks.add(pos);
                     }
                  }
               }
            }
         }
      }

      disassembleAndAddCreateContraptions(level, plot.getBoundingBox(), blocks, false, null);
      PersistentEntitySectionManager<Entity> manager = ((ServerLevel)toDisassemble.getLevel()).entityManager;

      for (PlotChunkHolder chunkx : toDisassemble.getPlot().getLoadedChunks()) {
         Stream<EntitySection<Entity>> sections = manager.sectionStorage.getExistingSectionsInChunk(chunkx.getPos().toLong());

         for (EntitySection<Entity> section : sections.toList()) {
            for (Entity entity : section.getEntities().toList()) {
               AABB box = entity.getBoundingBox();
               box = new AABB(transform.apply(new Vec3(box.minX, box.minY, box.minZ)), transform.apply(new Vec3(box.maxX, box.maxY, box.maxZ)));
               if (entity instanceof SuperGlueEntity) {
                  entity.remove(RemovalReason.KILLED);
                  level.addFreshEntity(new SuperGlueEntity(level, box));
               } else if (entity instanceof HoneyGlueEntity) {
                  entity.remove(RemovalReason.KILLED);
                  HoneyGlueEntity newHoneyGlue = new HoneyGlueEntity(level, box);
                  level.addFreshEntity(newHoneyGlue);
                  newHoneyGlue.setBoundsAndSync(box);
               } else {
                  Vec3 newPos = transform.apply(entity.position());
                  entity.setPos(newPos);
                  entity.setYRot(entity.rotate(transform.getRotation()));
                  entity.yRotO = entity.getYRot();
                  if (entity instanceof HangingEntity hangingEntity) {
                     hangingEntity.recalculateBoundingBox();
                  }

                  entity.levelCallback.onRemove(RemovalReason.CHANGED_DIMENSION);
                  ((ServerLevel)level).addDuringTeleport(entity);
               }
            }
         }
      }

      if (!blocks.isEmpty()) {
         ((ServerLevelPlot)toDisassemble.getPlot()).kickAllEntities();
         SubLevelAssemblyHelper.moveBlocks((ServerLevel)level, transform, blocks);
      }

      SubLevelAssemblyHelper.moveTrackingPoints((ServerLevel)level, plotBounds, null, transform);
   }

   public static SimAssemblyHelper.AssemblyResult assembleFromSingleBlock(
      Level level, BlockPos selfPos, BlockPos toAssemble, boolean includeStart, boolean includeEncasingGlue
   ) throws AssemblyException {
      if (level.getBlockState(toAssemble).isAir()) {
         return null;
      } else {
         SimAssemblyContraption contraption = new SimAssemblyContraption(includeStart ? null : selfPos, !includeEncasingGlue);
         contraption.searchMovedStructure(level, toAssemble);
         Collection<BlockPos> blocks = contraption.getBlocks();
         if (!blocks.isEmpty()) {
            BoundingBox3i bounds = BoundingBox3i.from(blocks);
            Collection<SuperGlueEntity> superGlues = contraption.getGlues();
            Collection<HoneyGlueEntity> honeyGlues = contraption.getHoneyGlues();
            ObjectArrayList<AABB> collectedContraptionGlues = new ObjectArrayList();
            disassembleAndAddCreateContraptions(level, bounds, blocks, true, collectedContraptionGlues);
            BlockPos anchor = blocks.stream().findFirst().get();
            SubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks((ServerLevel)level, anchor, blocks, bounds);
            if (subLevel != null) {
               BlockPos offsetBlocks = subLevel.getPlot().getCenterBlock().subtract(anchor);
               ObjectListIterator var14 = collectedContraptionGlues.iterator();

               while (var14.hasNext()) {
                  AABB box = (AABB)var14.next();
                  level.addFreshEntity(new SuperGlueEntity(level, box.move(Vec3.atLowerCornerOf(offsetBlocks))));
               }

               for (SuperGlueEntity glue : superGlues) {
                  glue.remove(RemovalReason.KILLED);
                  level.addFreshEntity(new SuperGlueEntity(level, glue.getBoundingBox().move(Vec3.atLowerCornerOf(offsetBlocks))));
               }

               for (HoneyGlueEntity glue : honeyGlues) {
                  glue.remove(RemovalReason.KILLED);
                  AABB newBB = glue.getBoundingBox().move(Vec3.atLowerCornerOf(offsetBlocks));
                  HoneyGlueEntity entity = new HoneyGlueEntity(level, newBB);
                  level.addFreshEntity(entity);
                  entity.setBoundsAndSync(newBB);
               }

               level.playSound(null, selfPos, SimSoundEvents.SIMULATED_CONTRAPTION_MOVES.event(), SoundSource.BLOCKS, 1.0F, 1.0F);
               return new SimAssemblyHelper.AssemblyResult(subLevel, offsetBlocks);
            }
         }

         return null;
      }
   }

   private static void disassembleAndAddCreateContraptions(
      Level level, BoundingBox3ic assemblyBounds, Collection<BlockPos> blocks, boolean passGluesBack, List<AABB> collectedGlues
   ) {
      assert assemblyBounds != null;

      AABB assemblyBoundsD = new AABB(
         (double)assemblyBounds.minX(),
         (double)assemblyBounds.minY(),
         (double)assemblyBounds.minZ(),
         (double)(assemblyBounds.maxX() + 1),
         (double)(assemblyBounds.maxY() + 1),
         (double)(assemblyBounds.maxZ() + 1)
      );

      for (ControlledContraptionEntity contraptionEntity : level.getEntitiesOfClass(ControlledContraptionEntity.class, assemblyBoundsD.inflate(2.0))) {
         ControlledContraptionEntityAccessor accessor = (ControlledContraptionEntityAccessor)contraptionEntity;
         BlockPos controllerPos = accessor.getControllerPos();
         if (blocks.contains(controllerPos)) {
            Contraption contraption = contraptionEntity.getContraption();
            StructureTransform transform = accessor.invokeMakeStructureTransform();

            for (BlockPos contraptionBlock : contraption.getBlocks().keySet()) {
               BlockPos targetPos = transform.apply(contraptionBlock);
               blocks.add(targetPos);
            }

            if (passGluesBack) {
               List<AABB> superGlue = ((ContraptionAccessor)contraption).getSuperGlue();

               for (AABB aabb : superGlue) {
                  aabb = new AABB(transform.apply(new Vec3(aabb.minX, aabb.minY, aabb.minZ)), transform.apply(new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)));
                  collectedGlues.add(aabb);
               }

               superGlue.clear();
            }

            contraptionEntity.disassemble();
            if (level.getBlockEntity(controllerPos) instanceof IControlContraptionExtension controlContraption) {
               controlContraption.sable$disassemble();
            }
         }
      }
   }

   public static Rotation rotationFrom90DegRots(int rots) {
      return switch (Math.floorMod(rots, 4)) {
         case 0 -> Rotation.NONE;
         case 1 -> Rotation.COUNTERCLOCKWISE_90;
         case 2 -> Rotation.CLOCKWISE_180;
         case 3 -> Rotation.CLOCKWISE_90;
         default -> throw new AssertionError();
      };
   }

   public static void register() {
      SubLevelHeatMapManager.addSplitListener(SimAssemblyHelper::addSplitBlocks);
   }

   private static void addSplitBlocks(Level level, BoundingBox3ic boundingBox3ic, Collection<BlockPos> blocks) {
      disassembleAndAddCreateContraptions(level, boundingBox3ic, blocks, false, null);
   }

   public static record AssemblyResult(SubLevel subLevel, BlockPos offset) {
   }
}

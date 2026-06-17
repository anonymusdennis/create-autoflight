package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.simibubi.create.content.schematics.SchematicAndQuillItem;
import com.simibubi.create.content.schematics.SchematicExport;
import com.simibubi.create.content.schematics.SchematicExport.SchematicExportResult;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableNBTUtils;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SchematicExport.class})
public class SchematicExportMixin {
   @Inject(
      method = {"saveSchematic"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;fillFromWorld(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Vec3i;ZLnet/minecraft/world/level/block/Block;)V",
         shift = Shift.BEFORE
      )}
   )
   private static void sable$saveSchematic(
      Path dir,
      String fileName,
      boolean overwrite,
      Level level,
      BlockPos first,
      BlockPos second,
      CallbackInfoReturnable<SchematicExportResult> cir,
      @Share("containingSubLevel") LocalRef<SubLevel> containingSubLevelRef,
      @Share("intersectingSubLevels") LocalRef<Iterable<SubLevel>> intersectingRef
   ) {
      BoundingBox3d schematicBounds = new BoundingBox3d(
         (double)first.getX(),
         (double)first.getY(),
         (double)first.getZ(),
         (double)(second.getX() + 1),
         (double)(second.getY() + 1),
         (double)(second.getZ() + 1)
      );
      BoundingBox bb = BoundingBox.fromCorners(first, second);
      BlockPos totalOrigin = new BlockPos(bb.minX(), bb.minY(), bb.minZ());
      ActiveSableCompanion helper = Sable.HELPER;
      SubLevel containingSubLevel = helper.getContaining(level, schematicBounds.center(new Vector3d()));
      if (containingSubLevel != null) {
         Pose3d containingPose = containingSubLevel.logicalPose();
         schematicBounds.transform(containingPose, schematicBounds);
      }

      containingSubLevelRef.set(containingSubLevel);
      Iterable<SubLevel> intersecting = helper.getAllIntersecting(level, schematicBounds);
      intersectingRef.set(intersecting);
      SubLevelSchematicSerializationContext context = new SubLevelSchematicSerializationContext(
         SubLevelSchematicSerializationContext.Type.SAVE, new BoundingBox3i(first, second)
      );
      context.setSetupTransform(block -> (BlockPos)block);
      context.setPlaceTransform(block -> ((BlockPos)block).subtract(totalOrigin));

      for (SubLevel subLevel : intersecting) {
         if (subLevel != containingSubLevel) {
            BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
            BlockPos origin = new BlockPos(plotBounds.minX(), plotBounds.minY(), plotBounds.minZ());
            Vec3 pos = subLevel.logicalPose().transformPosition(Vec3.atLowerCornerOf(origin));
            Quaterniond orientation = new Quaterniond(subLevel.logicalPose().orientation());
            if (containingSubLevel != null) {
               Pose3d containingPose = containingSubLevel.logicalPose();
               pos = containingPose.transformPositionInverse(pos);
               orientation.premul(containingPose.orientation().conjugate(new Quaterniond()));
            }

            Vector3d position = JOMLConversion.toJOML(pos.subtract(Vec3.atLowerCornerOf(totalOrigin)));
            context.getMappings()
               .put(
                  subLevel.getUniqueId(),
                  new SubLevelSchematicSerializationContext.SchematicMapping(
                     position, orientation, UUID.randomUUID(), block -> ((BlockPos)block).offset(origin.multiply(-1))
                  )
               );
         }
      }

      SubLevelSchematicSerializationContext.setCurrentContext(context);
   }

   @Inject(
      method = {"saveSchematic"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/simibubi/create/content/schematics/SchematicAndQuillItem;clampGlueBoxes(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/nbt/CompoundTag;)V",
         shift = Shift.AFTER
      )}
   )
   private static void sable$saveSchematicPost(
      Path dir,
      String fileName,
      boolean overwrite,
      Level level,
      BlockPos first,
      BlockPos second,
      CallbackInfoReturnable<SchematicExportResult> cir,
      @Local CompoundTag data,
      @Share("containingSubLevel") LocalRef<SubLevel> containingSubLevelRef,
      @Share("intersectingSubLevels") LocalRef<Iterable<SubLevel>> intersectingRef
   ) {
      ListTag list = new ListTag();
      SubLevel containingSubLevel = (SubLevel)containingSubLevelRef.get();
      SubLevelSchematicSerializationContext context = SubLevelSchematicSerializationContext.getCurrentContext();

      for (SubLevel subLevel : (Iterable)intersectingRef.get()) {
         if (subLevel != containingSubLevel) {
            BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
            Vector3ic size = plotBounds.size(new Vector3i());
            BlockPos origin = new BlockPos(plotBounds.minX(), plotBounds.minY(), plotBounds.minZ());
            BlockPos bounds = new BlockPos(size.x() + 1, size.y() + 1, size.z() + 1);
            StructureTemplate structure = new StructureTemplate();
            structure.fillFromWorld(level, origin, bounds, true, Blocks.AIR);
            CompoundTag subLevelData = structure.save(new CompoundTag());
            SchematicAndQuillItem.replaceStructureVoidWithAir(subLevelData);
            SchematicAndQuillItem.clampGlueBoxes(level, new AABB(Vec3.atLowerCornerOf(origin), Vec3.atLowerCornerOf(origin.offset(bounds))), subLevelData);
            SubLevelSchematicSerializationContext.SchematicMapping mapping = context.getMapping(subLevel);
            subLevelData.putUUID("uuid", mapping.newUUID());
            subLevelData.put("position", SableNBTUtils.writeVector3d(mapping.newCorner()));
            subLevelData.put("orientation", SableNBTUtils.writeQuaternion(mapping.newOrientation()));
            list.add(subLevelData);
         }
      }

      SubLevelSchematicSerializationContext.setCurrentContext(null);
      if (!list.isEmpty()) {
         data.put("sub_levels", list);
      }
   }
}

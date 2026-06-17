package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics.StructureTemplateExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.SableNBTUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({StructureTemplate.class})
public abstract class StructureTemplateMixin implements StructureTemplateExtension {
   @Unique
   private final List<StructureTemplateExtension.SubLevelTemplate> sable$subLevelTemplates = new ObjectArrayList();

   @Inject(
      method = {"load"},
      at = {@At("TAIL")}
   )
   private void sable$load(HolderGetter<Block> holderGetter, CompoundTag tag, CallbackInfo ci) {
      for (Tag subLevelTag : tag.getList("sub_levels", 10)) {
         CompoundTag subLevelCompound = (CompoundTag)subLevelTag;
         StructureTemplate t = new StructureTemplate();
         t.load(holderGetter, subLevelCompound);
         UUID uuid = subLevelCompound.getUUID("uuid");
         Vector3d position = SableNBTUtils.readVector3d(subLevelCompound.getCompound("position"));
         Quaterniond orientation = SableNBTUtils.readQuaternion(subLevelCompound.getCompound("orientation"));
         this.sable$subLevelTemplates.add(new StructureTemplateExtension.SubLevelTemplate(uuid, position, orientation, t));
      }
   }

   @Inject(
      method = {"fillEntityList"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/List;clear()V"
      )}
   )
   private void fillEntityList(Level level, BlockPos minPos, BlockPos maxPos, CallbackInfo ci, @Local List<Entity> entities) {
      ActiveSableCompanion helper = Sable.HELPER;
      SubLevel schematicSubLevel = helper.getContaining(level, minPos);
      entities.removeIf(entity -> {
         SubLevel entitySubLevel = helper.getContaining(entity);
         return entitySubLevel != schematicSubLevel && Sable.HELPER.getTrackingSubLevel(entity) != schematicSubLevel;
      });
   }

   @Override
   public List<StructureTemplateExtension.SubLevelTemplate> sable$getSubLevels() {
      return this.sable$subLevelTemplates;
   }
}

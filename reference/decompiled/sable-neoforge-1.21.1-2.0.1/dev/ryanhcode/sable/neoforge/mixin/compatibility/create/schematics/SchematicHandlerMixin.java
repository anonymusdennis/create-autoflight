package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.schematics.client.SchematicHandler;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics.SchematicLevelExtension;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics.StructureTemplateExtension;
import java.util.List;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({SchematicHandler.class})
public class SchematicHandlerMixin {
   @WrapOperation(
      method = {"setupRenderer"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;placeInWorld(Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructurePlaceSettings;Lnet/minecraft/util/RandomSource;I)Z"
      )}
   )
   private boolean sable$setupRenderer(
      StructureTemplate template,
      ServerLevelAccessor serverLevelAccessor,
      BlockPos blockPos,
      BlockPos blockPos2,
      StructurePlaceSettings structurePlaceSettings,
      RandomSource randomSource,
      int i,
      Operation<Boolean> original,
      @Local Level level
   ) {
      if (serverLevelAccessor instanceof SchematicLevel schematicLevel) {
         StructureTemplateExtension extension = (StructureTemplateExtension)template;
         List<StructureTemplateExtension.SubLevelTemplate> subLevelTemplates = extension.sable$getSubLevels();
         SchematicLevelExtension schematicLevelExtension = (SchematicLevelExtension)schematicLevel;

         for (StructureTemplateExtension.SubLevelTemplate subLevelTemplate : subLevelTemplates) {
            SchematicLevel subSchematicLevel = new SchematicLevel(level);
            subLevelTemplate.template().placeInWorld(subSchematicLevel, BlockPos.ZERO, BlockPos.ZERO, new StructurePlaceSettings(), level.getRandom(), 2);
            schematicLevelExtension.sable$getSubLevels()
               .add(
                  new SchematicLevelExtension.SchematicSubLevel(
                     subLevelTemplate.uuid(), subLevelTemplate.position(), subLevelTemplate.orientation(), subSchematicLevel
                  )
               );
         }
      }

      return (Boolean)original.call(new Object[]{template, serverLevelAccessor, blockPos, blockPos2, structurePlaceSettings, randomSource, i});
   }
}

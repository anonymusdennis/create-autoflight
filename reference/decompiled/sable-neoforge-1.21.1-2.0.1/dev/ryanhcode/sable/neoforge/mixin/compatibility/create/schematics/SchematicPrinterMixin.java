package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicPrinter;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics.SchematicLevelExtension;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics.SchematicPrinterExtension;
import dev.ryanhcode.sable.neoforge.mixinterface.compatibility.create.schematics.StructureTemplateExtension;
import java.util.List;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SchematicPrinter.class})
public class SchematicPrinterMixin implements SchematicPrinterExtension {
   @Shadow
   private SchematicLevel blockReader;

   @Inject(
      method = {"loadSchematic"},
      at = {@At("TAIL")}
   )
   private void sable$loadSchematic(ItemStack blueprint, Level originalWorld, boolean processNBT, CallbackInfo ci, @Local StructureTransform transform) {
      for (SchematicLevelExtension.SchematicSubLevel schematicSubLevel : ((SchematicLevelExtension)this.blockReader).sable$getSubLevels()) {
         Vec3 transformedPos = transform.applyWithoutOffset(JOMLConversion.toMojang(schematicSubLevel.position()));
         JOMLConversion.toJOML(transformedPos, schematicSubLevel.position());

         double radians = switch (transform.rotation) {
            case NONE -> 0.0;
            case CLOCKWISE_90 -> -Math.PI / 2;
            case CLOCKWISE_180 -> Math.PI;
            case COUNTERCLOCKWISE_90 -> Math.PI / 2;
            default -> throw new MatchException(null, null);
         };
         schematicSubLevel.orientation().rotateLocalY(radians);
      }
   }

   @WrapOperation(
      method = {"loadSchematic"},
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
      @Local(argsOnly = true) Level level
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

   @Override
   public SchematicLevel sable$getSchematicLevel() {
      return this.blockReader;
   }
}

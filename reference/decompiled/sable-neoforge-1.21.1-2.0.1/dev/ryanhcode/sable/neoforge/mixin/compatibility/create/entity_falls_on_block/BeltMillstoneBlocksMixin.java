package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.entity_falls_on_block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlock;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({BeltBlock.class, MillstoneBlock.class})
public class BeltMillstoneBlocksMixin extends Block {
   public BeltMillstoneBlocksMixin(Properties pProperties) {
      super(pProperties);
   }

   @WrapOperation(
      method = {"updateEntityAfterFallOn"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"
      )}
   )
   public BlockPos sable$checkForSubLevels(Entity instance, Operation<BlockPos> original) {
      Level level = instance.level();
      BlockEntry<?> entry;
      if (this instanceof BeltBlock) {
         entry = AllBlocks.BELT;
      } else {
         entry = AllBlocks.MILLSTONE;
      }

      ActiveSableCompanion helper = Sable.HELPER;
      BlockPos gatheredBeltPos = helper.runIncludingSubLevels(level, instance.position(), true, null, (subLevel, internalPos) -> {
         if (entry.has(level.getBlockState(internalPos))) {
            return internalPos;
         } else {
            return entry.has(level.getBlockState(internalPos.below())) ? internalPos.below() : null;
         }
      });
      return gatheredBeltPos != null ? gatheredBeltPos : (BlockPos)original.call(new Object[]{instance});
   }
}

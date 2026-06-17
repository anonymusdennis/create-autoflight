package com.simibubi.create.api.contraption;

import com.simibubi.create.api.registry.SimpleRegistry;
import java.util.Collection;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.jetbrains.annotations.Nullable;

public enum ContraptionMovementSetting {
   MOVABLE,
   NO_PICKUP,
   UNMOVABLE;

   public static final SimpleRegistry<Block, Supplier<ContraptionMovementSetting>> REGISTRY = SimpleRegistry.create();

   @Nullable
   public static ContraptionMovementSetting get(BlockState state) {
      return get(state.getBlock());
   }

   @Nullable
   public static ContraptionMovementSetting get(Block block) {
      if (block instanceof ContraptionMovementSetting.MovementSettingProvider provider) {
         return provider.getContraptionMovementSetting();
      } else {
         Supplier<ContraptionMovementSetting> supplier = REGISTRY.get(block);
         return supplier == null ? null : supplier.get();
      }
   }

   public static boolean anyAre(Collection<StructureBlockInfo> blocks, ContraptionMovementSetting setting) {
      return blocks.stream().anyMatch(b -> get(b.state().getBlock()) == setting);
   }

   public static boolean isNoPickup(Collection<StructureBlockInfo> blocks) {
      return anyAre(blocks, NO_PICKUP);
   }

   public interface MovementSettingProvider extends IBlockExtension {
      ContraptionMovementSetting getContraptionMovementSetting();
   }
}

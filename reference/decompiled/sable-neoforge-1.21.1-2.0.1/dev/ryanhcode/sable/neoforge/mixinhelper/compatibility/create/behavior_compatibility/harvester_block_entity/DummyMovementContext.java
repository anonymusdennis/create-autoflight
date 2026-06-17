package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.behavior_compatibility.harvester_block_entity;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.jetbrains.annotations.Nullable;

public class DummyMovementContext extends MovementContext {
   public DummyMovementContext() {
      super(null, new StructureBlockInfo(BlockPos.ZERO, Blocks.AIR.defaultBlockState(), null), null);
   }

   public void update(Level level, BlockPos pos, BlockState state, @Nullable CompoundTag blockEntityData) {
      this.world = level;
      this.state = state;
      this.localPos = pos;
      this.blockEntityData = blockEntityData;
   }
}

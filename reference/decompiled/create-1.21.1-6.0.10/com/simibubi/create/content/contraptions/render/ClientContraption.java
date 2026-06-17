package com.simibubi.create.content.contraptions.render;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.createmod.catnip.render.SuperByteBufferCache;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public class ClientContraption {
   private final VirtualRenderWorld renderLevel;
   private final List<BlockEntity> renderedBlockEntities = new ArrayList<>();
   public final List<BlockEntity> renderedBlockEntityView = Collections.unmodifiableList(this.renderedBlockEntities);
   public final BitSet shouldRenderBlockEntities = new BitSet();
   public final BitSet scratchErroredBlockEntities = new BitSet();
   private final ContraptionMatrices matrices = new ContraptionMatrices();
   private final Contraption contraption;
   private int structureVersion = 0;
   private int childrenVersion = 0;

   public ClientContraption(Contraption contraption) {
      Level level = contraption.entity.level();
      this.contraption = contraption;
      BlockPos origin = contraption.anchor;
      int minY = VirtualRenderWorld.nextMultipleOf16(Mth.floor(contraption.bounds.minY - 1.0));
      int height = VirtualRenderWorld.nextMultipleOf16(Mth.ceil(contraption.bounds.maxY + 1.0)) - minY;
      this.renderLevel = new VirtualRenderWorld(level, minY, height, origin, this::invalidateStructure) {
         public boolean supportsVisualization() {
            return VisualizationManager.supportsVisualization(this.level);
         }
      };
      this.setupRenderLevelAndRenderedBlockEntities();
   }

   public int structureVersion() {
      return this.structureVersion;
   }

   public int childrenVersion() {
      return this.childrenVersion;
   }

   public void resetRenderLevel() {
      this.renderedBlockEntities.clear();
      this.renderLevel.clear();
      this.shouldRenderBlockEntities.clear();
      this.setupRenderLevelAndRenderedBlockEntities();
      this.invalidateStructure();
      this.invalidateChildren();
   }

   public void invalidateChildren() {
      this.childrenVersion++;
   }

   public void invalidateStructure() {
      for (RenderType renderType : RenderType.chunkBufferLayers()) {
         SuperByteBufferCache.getInstance().invalidate(ContraptionEntityRenderer.CONTRAPTION, Pair.of(this.contraption, renderType));
      }

      this.structureVersion++;
   }

   private void setupRenderLevelAndRenderedBlockEntities() {
      for (StructureBlockInfo info : this.contraption.getBlocks().values()) {
         this.renderLevel.setBlock(info.pos(), info.state(), 0);
         BlockEntity blockEntity = this.readBlockEntity(this.renderLevel, info, this.contraption.getIsLegacy().getBoolean(info.pos()));
         if (blockEntity != null) {
            this.renderLevel.setBlockEntity(blockEntity);
            MovementBehaviour movementBehaviour = MovementBehaviour.REGISTRY.get(info.state());
            if (movementBehaviour == null || !movementBehaviour.disableBlockEntityRendering()) {
               this.renderedBlockEntities.add(blockEntity);
            }
         }
      }

      this.shouldRenderBlockEntities.set(0, this.renderedBlockEntities.size());
      this.renderLevel.runLightEngine();
   }

   @Nullable
   public BlockEntity readBlockEntity(Level level, StructureBlockInfo info, boolean legacy) {
      BlockState state = info.state();
      BlockPos pos = info.pos();
      CompoundTag nbt = info.nbt();
      if (legacy) {
         if (nbt == null) {
            return null;
         } else {
            nbt.putInt("x", pos.getX());
            nbt.putInt("y", pos.getY());
            nbt.putInt("z", pos.getZ());
            BlockEntity be = BlockEntity.loadStatic(pos, state, nbt, level.registryAccess());
            postprocessReadBlockEntity(level, be, state);
            return be;
         }
      } else if (state.hasBlockEntity() && state.getBlock() instanceof EntityBlock entityBlock) {
         BlockEntity var10 = entityBlock.newBlockEntity(pos, state);
         postprocessReadBlockEntity(level, var10, state);
         if (var10 != null && nbt != null) {
            var10.handleUpdateTag(nbt, level.registryAccess());
         }

         return var10;
      } else {
         return null;
      }
   }

   protected static void postprocessReadBlockEntity(Level level, @Nullable BlockEntity be, BlockState blockState) {
      if (be != null) {
         be.setLevel(level);
         be.setBlockState(blockState);
         if (be instanceof KineticBlockEntity kbe) {
            kbe.setSpeed(0.0F);
         }
      }
   }

   public VirtualRenderWorld getRenderLevel() {
      return this.renderLevel;
   }

   public ContraptionMatrices getMatrices() {
      return this.matrices;
   }

   public ClientContraption.RenderedBlocks getRenderedBlocks() {
      return new ClientContraption.RenderedBlocks(pos -> {
         StructureBlockInfo info = this.contraption.getBlocks().get(pos);
         return info == null ? Blocks.AIR.defaultBlockState() : info.state();
      }, this.contraption.getBlocks().keySet());
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos localPos) {
      return this.renderLevel.getBlockEntity(localPos);
   }

   public BitSet getAndAdjustShouldRenderBlockEntities() {
      return this.shouldRenderBlockEntities;
   }

   public static record RenderedBlocks(Function<BlockPos, BlockState> lookup, Iterable<BlockPos> positions) {
   }
}

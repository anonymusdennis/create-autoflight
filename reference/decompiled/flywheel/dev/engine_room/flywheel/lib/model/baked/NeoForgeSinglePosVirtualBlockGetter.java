package dev.engine_room.flywheel.lib.model.baked;

import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

public class NeoForgeSinglePosVirtualBlockGetter extends SinglePosVirtualBlockGetter {
   @Nullable
   protected ModelData modelData;

   public NeoForgeSinglePosVirtualBlockGetter(ToIntFunction<BlockPos> blockLightFunc, ToIntFunction<BlockPos> skyLightFunc) {
      super(blockLightFunc, skyLightFunc);
   }

   public static NeoForgeSinglePosVirtualBlockGetter createFullDark() {
      return new NeoForgeSinglePosVirtualBlockGetter(p -> 0, p -> 0);
   }

   public static NeoForgeSinglePosVirtualBlockGetter createFullBright() {
      return new NeoForgeSinglePosVirtualBlockGetter(p -> 15, p -> 15);
   }

   public NeoForgeSinglePosVirtualBlockGetter pos(BlockPos pos) {
      super.pos(pos);
      return this;
   }

   public NeoForgeSinglePosVirtualBlockGetter blockState(BlockState state) {
      super.blockState(this.blockState);
      return this;
   }

   public NeoForgeSinglePosVirtualBlockGetter blockEntity(@Nullable BlockEntity blockEntity) {
      super.blockEntity(blockEntity);
      return this;
   }

   public NeoForgeSinglePosVirtualBlockGetter modelData(@Nullable ModelData modelData) {
      this.modelData = modelData;
      return this;
   }

   public ModelData getModelData(BlockPos pos) {
      if (pos.equals(this.pos)) {
         return this.modelData != null ? this.modelData : super.getModelData(pos);
      } else {
         return super.getModelData(pos);
      }
   }
}

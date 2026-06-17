package com.simibubi.create.content.decoration.palettes;

import com.simibubi.create.AllSpriteShifts;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTType;
import com.simibubi.create.foundation.block.connected.GlassPaneCTBehaviour;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WeatheredIronWindowPaneCTBehaviour extends GlassPaneCTBehaviour {
   private List<CTSpriteShiftEntry> shifts = List.of(
      AllSpriteShifts.OLD_FACTORY_WINDOW_1, AllSpriteShifts.OLD_FACTORY_WINDOW_2, AllSpriteShifts.OLD_FACTORY_WINDOW_3, AllSpriteShifts.OLD_FACTORY_WINDOW_4
   );

   public WeatheredIronWindowPaneCTBehaviour() {
      super(null);
   }

   @Nullable
   @Override
   public CTSpriteShiftEntry getShift(BlockState state, RandomSource rand, Direction direction, @NotNull TextureAtlasSprite sprite) {
      if (direction.getAxis() != Axis.Y && sprite != null) {
         CTSpriteShiftEntry entry = this.shifts.get(rand.nextInt(this.shifts.size()));
         return entry.getOriginal() == sprite ? entry : super.getShift(state, rand, direction, sprite);
      } else {
         return null;
      }
   }

   @Nullable
   @Override
   public CTSpriteShiftEntry getShift(BlockState state, Direction direction, @Nullable TextureAtlasSprite sprite) {
      return null;
   }

   @Nullable
   @Override
   public CTType getDataType(BlockAndTintGetter world, BlockPos pos, BlockState state, Direction direction) {
      return AllCTTypes.RECTANGLE;
   }
}

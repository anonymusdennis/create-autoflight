package com.simibubi.create.compat.jei.category.animations;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.gui.CustomLightingSettings;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import mezz.jei.api.gui.drawable.IDrawable;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.ILightingSettings;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.GuiGameElement.GuiRenderBuilder;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public abstract class AnimatedKinetics implements IDrawable {
   public int offset = 0;
   public static final ILightingSettings DEFAULT_LIGHTING = CustomLightingSettings.builder()
      .firstLightRotation(12.5F, -45.0F)
      .secondLightRotation(-20.0F, -50.0F)
      .build();

   public static GuiRenderBuilder defaultBlockElement(BlockState state) {
      return GuiGameElement.of(state).lighting(DEFAULT_LIGHTING);
   }

   public static GuiRenderBuilder defaultBlockElement(PartialModel partial) {
      return GuiGameElement.of(partial).lighting(DEFAULT_LIGHTING);
   }

   public static float getCurrentAngle() {
      return AnimationTickHolder.getRenderTime() * 4.0F % 360.0F;
   }

   protected BlockState shaft(Axis axis) {
      return (BlockState)AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS, axis);
   }

   protected PartialModel cogwheel() {
      return AllPartialModels.SHAFTLESS_COGWHEEL;
   }

   protected GuiRenderBuilder blockElement(BlockState state) {
      return defaultBlockElement(state);
   }

   protected GuiRenderBuilder blockElement(PartialModel partial) {
      return defaultBlockElement(partial);
   }

   public int getWidth() {
      return 50;
   }

   public int getHeight() {
      return 50;
   }
}

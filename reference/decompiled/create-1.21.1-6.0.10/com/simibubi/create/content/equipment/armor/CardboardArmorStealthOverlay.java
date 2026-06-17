package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.Create;
import com.simibubi.create.foundation.mixin.accessor.GuiAccessor;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public class CardboardArmorStealthOverlay extends Gui implements IClientItemExtensions {
   private static final ResourceLocation PACKAGE_BLUR_LOCATION = Create.asResource("textures/misc/package_blur.png");
   private static LerpedFloat opacity = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.25, Chaser.EXP);

   public CardboardArmorStealthOverlay() {
      super(Minecraft.getInstance());
   }

   public static void clientTick() {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         opacity.tickChaser();
         opacity.updateChaseTarget(CardboardArmorHandler.testForStealth(player) ? 1.0F : 0.0F);
      }
   }

   public void renderHelmetOverlay(ItemStack stack, Player player, int width, int height, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      float value = opacity.getValue(partialTick);
      if (value != 0.0F) {
         ((GuiAccessor)this).create$renderTextureOverlay(new GuiGraphics(mc, mc.renderBuffers().bufferSource()), PACKAGE_BLUR_LOCATION, value);
      }
   }
}

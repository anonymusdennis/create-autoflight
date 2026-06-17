package com.simibubi.create.infrastructure.gui;

import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.CreateBuildInfo;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.config.ui.BaseConfigScreen;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.element.BoxElement;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.foundation.ui.PonderTagIndexScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.CubeMap;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CreateMainMenuScreen extends AbstractSimiScreen {
   public static final CubeMap PANORAMA_RESOURCES = new CubeMap(Create.asResource("textures/gui/title/background/panorama"));
   public static final ResourceLocation PANORAMA_OVERLAY_TEXTURES = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
   public static final PanoramaRenderer PANORAMA = new PanoramaRenderer(PANORAMA_RESOURCES);
   private static final Component CURSEFORGE_TOOLTIP = Component.literal("CurseForge").withStyle(s -> s.withColor(16545884).withBold(true));
   private static final Component MODRINTH_TOOLTIP = Component.literal("Modrinth").withStyle(s -> s.withColor(4182827).withBold(true));
   public static final String CURSEFORGE_LINK = "https://www.curseforge.com/minecraft/mc-mods/create";
   public static final String MODRINTH_LINK = "https://modrinth.com/mod/create";
   public static final String ISSUE_TRACKER_LINK = "https://github.com/Creators-of-Create/Create/issues";
   public static final String SUPPORT_LINK = "https://github.com/Creators-of-Create/Create/wiki/Supporting-the-Project";
   protected final Screen parent;
   protected boolean returnOnClose;
   private PanoramaRenderer vanillaPanorama;
   private long firstRenderTime;
   private Button gettingStarted;

   public CreateMainMenuScreen(Screen parent) {
      this.parent = parent;
      this.returnOnClose = true;
      if (parent instanceof TitleScreen) {
         this.vanillaPanorama = Screen.PANORAMA;
      } else {
         this.vanillaPanorama = new PanoramaRenderer(TitleScreen.CUBE_MAP);
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      if (this.firstRenderTime == 0L) {
         this.firstRenderTime = Util.getMillis();
      }

      super.render(graphics, mouseX, mouseY, partialTicks);
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      float f = (float)(Util.getMillis() - this.firstRenderTime) / 1000.0F;
      float alpha = Mth.clamp(f, 0.0F, 1.0F);
      float elapsedPartials = this.minecraft.getTimer().getGameTimeDeltaPartialTick(false);
      if (this.parent instanceof TitleScreen) {
         if (alpha < 1.0F) {
            this.vanillaPanorama.render(graphics, this.width, this.height, 1.0F, elapsedPartials);
         }

         PANORAMA.render(graphics, this.width, this.height, 1.0F, elapsedPartials);
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
         graphics.blit(PANORAMA_OVERLAY_TEXTURES, 0, 0, this.width, this.height, 0.0F, 0.0F, 16, 128, 16, 128);
      }

      RenderSystem.enableDepthTest();
      PoseStack ms = graphics.pose();

      for (int side : Iterate.positiveAndNegative) {
         ms.pushPose();
         ms.translate((float)(this.width / 2), 60.0F, 200.0F);
         ms.scale((float)(24 * side), (float)(24 * side), 32.0F);
         ms.translate(-1.75 * (double)(alpha * alpha / 2.0F + 0.5F), 0.25, 0.0);
         TransformStack.of(ms).rotateXDegrees(45.0F);
         GuiGameElement.of(AllBlocks.LARGE_COGWHEEL.getDefaultState())
            .rotateBlock(0.0, (double)((float)Util.getMillis() / 32.0F * (float)side), 0.0)
            .render(graphics);
         ms.translate(-1.0F, 0.0F, -1.0F);
         GuiGameElement.of(AllBlocks.COGWHEEL.getDefaultState())
            .rotateBlock(0.0, (double)((float)Util.getMillis() / -16.0F * (float)side + 22.5F), 0.0)
            .render(graphics);
         ms.popPose();
      }

      RenderSystem.enableBlend();
      ms.pushPose();
      ms.translate((float)(this.width / 2 - 32), 32.0F, -10.0F);
      ms.pushPose();
      ms.scale(0.25F, 0.25F, 0.25F);
      AllGuiTextures.LOGO.render(graphics, 0, 0);
      ms.popPose();
      new BoxElement().withBackground(-2013265920).flatBorder(new Color(16777216)).at(-32.0F, 56.0F, 100.0F).withBounds(128, 11).render(graphics);
      ms.popPose();
      ms.pushPose();
      ms.translate(0.0F, 0.0F, 200.0F);
      graphics.drawCenteredString(
         this.font,
         Component.literal("Create")
            .withStyle(ChatFormatting.BOLD)
            .append(Component.literal(" v" + CreateBuildInfo.VERSION).withStyle(new ChatFormatting[]{ChatFormatting.BOLD, ChatFormatting.WHITE})),
         this.width / 2,
         89,
         -1787033
      );
      ms.popPose();
      RenderSystem.disableDepthTest();
   }

   protected void init() {
      super.init();
      this.returnOnClose = true;
      this.addButtons();
   }

   private void addButtons() {
      int yStart = this.height / 4 + 40;
      int center = this.width / 2;
      int bHeight = 20;
      int bShortWidth = 98;
      int bLongWidth = 200;
      this.addRenderableWidget(
         Button.builder(CreateLang.translateDirect("menu.return"), $ -> this.linkTo(this.parent))
            .bounds(center - 100, yStart + 92, bLongWidth, bHeight)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(CreateLang.translateDirect("menu.configure"), $ -> this.linkTo(new BaseConfigScreen(this, "create")))
            .bounds(center - 100, yStart + 24 + -16, bLongWidth, bHeight)
            .build()
      );
      this.gettingStarted = Button.builder(CreateLang.translateDirect("menu.ponder_index"), $ -> this.linkTo(new PonderTagIndexScreen()))
         .bounds(center + 2, yStart + 48 + -16, bShortWidth, bHeight)
         .build();
      this.gettingStarted.active = !(this.parent instanceof TitleScreen);
      this.addRenderableWidget(this.gettingStarted);
      this.addRenderableWidget(
         new CreateMainMenuScreen.PlatformIconButton(
            center - 100,
            yStart + 48 + -16,
            bShortWidth / 2,
            bHeight,
            AllGuiTextures.CURSEFORGE_LOGO,
            0.085F,
            b -> this.linkTo("https://www.curseforge.com/minecraft/mc-mods/create"),
            Tooltip.create(CURSEFORGE_TOOLTIP)
         )
      );
      this.addRenderableWidget(
         new CreateMainMenuScreen.PlatformIconButton(
            center - 50,
            yStart + 48 + -16,
            bShortWidth / 2,
            bHeight,
            AllGuiTextures.MODRINTH_LOGO,
            0.0575F,
            b -> this.linkTo("https://modrinth.com/mod/create"),
            Tooltip.create(MODRINTH_TOOLTIP)
         )
      );
      this.addRenderableWidget(
         Button.builder(CreateLang.translateDirect("menu.report_bugs"), $ -> this.linkTo("https://github.com/Creators-of-Create/Create/issues"))
            .bounds(center + 2, yStart + 68, bShortWidth, bHeight)
            .build()
      );
      this.addRenderableWidget(
         Button.builder(
               CreateLang.translateDirect("menu.support"), $ -> this.linkTo("https://github.com/Creators-of-Create/Create/wiki/Supporting-the-Project")
            )
            .bounds(center - 100, yStart + 68, bShortWidth, bHeight)
            .build()
      );
   }

   protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
      this.renderables.forEach(w -> w.render(graphics, mouseX, mouseY, partialTicks));
      if (this.parent instanceof TitleScreen) {
         if (mouseX < this.gettingStarted.getX() || mouseX > this.gettingStarted.getX() + 98) {
            return;
         }

         if (mouseY < this.gettingStarted.getY() || mouseY > this.gettingStarted.getY() + 20) {
            return;
         }

         graphics.renderComponentTooltip(
            this.font, FontHelper.cutTextComponent(CreateLang.translateDirect("menu.only_ingame"), Palette.ALL_GRAY), mouseX, mouseY
         );
      }
   }

   private void linkTo(Screen screen) {
      this.returnOnClose = false;
      ScreenOpener.open(screen);
   }

   private void linkTo(String url) {
      this.returnOnClose = false;
      ScreenOpener.open(new ConfirmLinkScreen(p_213069_2_ -> {
         if (p_213069_2_) {
            Util.getPlatform().openUri(url);
         }

         this.minecraft.setScreen(this);
      }, url, true));
   }

   public boolean isPauseScreen() {
      return true;
   }

   protected static class PlatformIconButton extends Button {
      protected final AllGuiTextures icon;
      protected final float scale;

      public PlatformIconButton(int pX, int pY, int pWidth, int pHeight, AllGuiTextures icon, float scale, OnPress pOnPress, Tooltip tooltip) {
         super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY, pOnPress, DEFAULT_NARRATION);
         this.icon = icon;
         this.scale = scale;
         this.setTooltip(tooltip);
      }

      protected void renderWidget(GuiGraphics graphics, int pMouseX, int pMouseY, float pt) {
         super.renderWidget(graphics, pMouseX, pMouseY, pt);
         PoseStack pPoseStack = graphics.pose();
         pPoseStack.pushPose();
         pPoseStack.translate(
            (float)(this.getX() + this.width / 2) - (float)this.icon.getWidth() * this.scale / 2.0F,
            (float)(this.getY() + this.height / 2) - (float)this.icon.getHeight() * this.scale / 2.0F,
            0.0F
         );
         pPoseStack.scale(this.scale, this.scale, 1.0F);
         this.icon.render(graphics, 0, 0);
         pPoseStack.popPose();
      }
   }
}

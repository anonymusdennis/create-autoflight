package com.simibubi.create.content.contraptions.wrench;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllKeys;
import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencedGearshiftBlock;
import com.simibubi.create.content.redstone.DirectedDirectionalBlock;
import com.simibubi.create.foundation.gui.AllIcons;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.element.RenderElement;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class RadialWrenchMenu extends AbstractSimiScreen {
   public static final Map<Property<?>, String> VALID_PROPERTIES = new HashMap<>();
   public static final Set<ResourceLocation> BLOCK_BLACKLIST = new HashSet<>();
   private final BlockState state;
   private final BlockPos pos;
   @Nullable
   private final BlockEntity blockEntity;
   private final Level level;
   private final NonVisualizationLevel nonVisualizationLevel;
   private final List<Entry<Property<?>, String>> propertiesForState;
   private final int innerRadius = 50;
   private final int outerRadius = 110;
   private int selectedPropertyIndex = 0;
   private List<BlockState> allStates = List.of();
   private String propertyLabel = "";
   private int ticksOpen;
   private int selectedStateIndex = 0;
   private final RenderElement iconScroll = RenderElement.of(PonderGuiTextures.ICON_SCROLL);
   private final RenderElement iconUp = RenderElement.of(AllIcons.I_PRIORITY_HIGH);
   private final RenderElement iconDown = RenderElement.of(AllIcons.I_PRIORITY_LOW);

   public static void registerRotationProperty(Property<?> property, String label) {
      if (!VALID_PROPERTIES.containsKey(property)) {
         VALID_PROPERTIES.put(property, label);
      }
   }

   public static void registerBlacklistedBlock(ResourceLocation location) {
      if (!BLOCK_BLACKLIST.contains(location)) {
         BLOCK_BLACKLIST.add(location);
      }
   }

   public static Optional<RadialWrenchMenu> tryCreateFor(BlockState state, BlockPos pos, Level level) {
      if (BLOCK_BLACKLIST.contains(RegisteredObjectsHelper.getKeyOrThrow(state.getBlock()))) {
         return Optional.empty();
      } else {
         List<Entry<Property<?>, String>> propertiesForState = VALID_PROPERTIES.entrySet().stream().filter(entry -> state.hasProperty(entry.getKey())).toList();
         return propertiesForState.isEmpty() ? Optional.empty() : Optional.of(new RadialWrenchMenu(state, pos, level, propertiesForState));
      }
   }

   private RadialWrenchMenu(BlockState state, BlockPos pos, Level level, List<Entry<Property<?>, String>> properties) {
      this.state = state;
      this.pos = pos;
      this.level = level;
      this.nonVisualizationLevel = new NonVisualizationLevel(level);
      this.blockEntity = level.getBlockEntity(pos);
      this.propertiesForState = properties;
      this.initForSelectedProperty();
   }

   private void initForSelectedProperty() {
      Entry<Property<?>, String> entry = this.propertiesForState.get(this.selectedPropertyIndex);
      this.allStates = new ArrayList<>();
      this.cycleAllPropertyValues(this.state, entry.getKey(), this.allStates);
      this.propertyLabel = entry.getValue();
   }

   private void cycleAllPropertyValues(BlockState state, Property<?> property, List<BlockState> states) {
      Optional<? extends Comparable<?>> first = property.getPossibleValues().stream().findFirst();
      if (!first.isEmpty()) {
         int offset = 0;
         int safety = 100;

         while (safety-- > 0) {
            if (state.getValue(property).equals(first.get())) {
               offset = 99 - safety;
               break;
            }

            state = (BlockState)state.cycle(property);
         }

         for (int var8 = 100; var8-- > 0 && !states.contains(state); state = (BlockState)state.cycle(property)) {
            states.add(state);
         }

         offset = Mth.clamp(offset, 0, states.size() - 1);
         this.selectedStateIndex = offset == 0 ? 0 : states.size() - offset;
      }
   }

   public void tick() {
      this.ticksOpen++;
      if (!this.level.getBlockState(this.pos).is(this.state.getBlock())) {
         Minecraft.getInstance().setScreen(null);
      }

      super.tick();
   }

   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      int x = this.width / 2;
      int y = this.height / 2;
      PoseStack ms = graphics.pose();
      ms.pushPose();
      ms.translate((float)x, (float)y, 0.0F);
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      int mouseOffsetX = mouseX - this.width / 2;
      int mouseOffsetY = mouseY - this.height / 2;
      if (Mth.length((double)mouseOffsetX, (double)mouseOffsetY) > 45.0) {
         double theta = Mth.atan2((double)mouseOffsetX, (double)mouseOffsetY);
         float sectorSize = 360.0F / (float)this.allStates.size();
         this.selectedStateIndex = (int)Math.floor(
            (double)((-AngleHelper.deg(Mth.atan2((double)mouseOffsetX, (double)mouseOffsetY)) + 180.0F + sectorSize / 2.0F) % 360.0F / sectorSize)
         );
         this.renderDirectionIndicator(graphics, theta);
      }

      this.renderRadialSectors(graphics);
      UIRenderHelper.streak(graphics, 0.0F, 0, 0, 32, 65, Color.BLACK.setAlpha(0.8F));
      UIRenderHelper.streak(graphics, 180.0F, 0, 0, 32, 65, Color.BLACK.setAlpha(0.8F));
      if (this.selectedPropertyIndex > 0) {
         this.iconScroll.at(-14.0F, -46.0F).render(graphics);
         this.iconUp.at(-1.0F, -46.0F).render(graphics);
         graphics.drawCenteredString(
            this.font, this.propertiesForState.get(this.selectedPropertyIndex - 1).getValue(), 0, -30, ((Color)UIRenderHelper.COLOR_TEXT.getFirst()).getRGB()
         );
      }

      if (this.selectedPropertyIndex < this.propertiesForState.size() - 1) {
         this.iconScroll.at(-14.0F, 30.0F).render(graphics);
         this.iconDown.at(-1.0F, 30.0F).render(graphics);
         graphics.drawCenteredString(
            this.font, this.propertiesForState.get(this.selectedPropertyIndex + 1).getValue(), 0, 22, ((Color)UIRenderHelper.COLOR_TEXT.getFirst()).getRGB()
         );
      }

      graphics.drawCenteredString(this.font, "Currently", 0, -13, ((Color)UIRenderHelper.COLOR_TEXT.getFirst()).getRGB());
      graphics.drawCenteredString(this.font, "Changing:", 0, -3, ((Color)UIRenderHelper.COLOR_TEXT.getFirst()).getRGB());
      graphics.drawCenteredString(this.font, this.propertyLabel, 0, 7, ((Color)UIRenderHelper.COLOR_TEXT.getFirst()).getRGB());
      ms.popPose();
   }

   private void renderRadialSectors(GuiGraphics graphics) {
      int sectors = this.allStates.size();
      if (sectors >= 2) {
         PoseStack poseStack = graphics.pose();
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            float sectorAngle = 360.0F / (float)sectors;
            int sectorWidth = 60;
            poseStack.pushPose();

            for (int i = 0; i < sectors; i++) {
               Color innerColor = Color.WHITE.setAlpha(0.05F);
               Color outerColor = Color.WHITE.setAlpha(0.3F);
               BlockState blockState = this.allStates.get(i);
               Property<?> property = this.propertiesForState.get(this.selectedPropertyIndex).getKey();
               poseStack.pushPose();
               if (i == this.selectedStateIndex) {
                  innerColor.mixWith(new Color(0.8F, 0.8F, 0.2F, 0.2F), 0.5F);
                  outerColor.mixWith(new Color(0.8F, 0.8F, 0.2F, 0.6F), 0.5F);
                  UIRenderHelper.drawRadialSector(graphics, 112.0F, 113.0F, -(sectorAngle / 2.0F + 90.0F), sectorAngle, outerColor, outerColor);
               }

               UIRenderHelper.drawRadialSector(graphics, 50.0F, 110.0F, -(sectorAngle / 2.0F + 90.0F), sectorAngle, innerColor, outerColor);
               Color c = innerColor.copy().setAlpha(0.5F);
               UIRenderHelper.drawRadialSector(graphics, 47.0F, 48.0F, -(sectorAngle / 2.0F + 90.0F), sectorAngle, c, c);
               ((PoseTransformStack)TransformStack.of(poseStack).translateY(-((float)sectorWidth / 2.0F + 50.0F))).rotateZDegrees((float)(-i) * sectorAngle);
               poseStack.translate(0.0F, 0.0F, 100.0F);

               try {
                  this.withLevel(
                     this.blockEntity,
                     this.nonVisualizationLevel,
                     () -> GuiGameElement.of(blockState, this.blockEntity)
                           .rotateBlock((double)player.getXRot(), (double)(player.getYRot() + 180.0F), 0.0)
                           .scale(24.0)
                           .at(-12.0F, 12.0F)
                           .render(graphics)
                  );
               } catch (Exception var14) {
                  Create.LOGGER.warn("Failed to render blockstate in RadialWrenchMenu", var14);
                  this.allStates.remove(i);
                  this.selectedStateIndex = 0;
                  return;
               }

               poseStack.translate(0.0F, 0.0F, 50.0F);
               if (i == this.selectedStateIndex) {
                  graphics.drawCenteredString(
                     this.font, blockState.getValue(property).toString(), 0, 15, ((Color)UIRenderHelper.COLOR_TEXT.getFirst()).getRGB()
                  );
               }

               poseStack.popPose();
               poseStack.pushPose();
               TransformStack.of(poseStack).rotateZDegrees(sectorAngle / 2.0F);
               poseStack.translate(0.0F, -70.0F, 10.0F);
               UIRenderHelper.angledGradient(graphics, -90.0F, 0, 0, 0.5F, (float)(sectorWidth - 10), Color.WHITE.setAlpha(0.5F), Color.WHITE.setAlpha(0.15F));
               UIRenderHelper.angledGradient(graphics, 90.0F, 0, 0, 0.5F, 25.0F, Color.WHITE.setAlpha(0.5F), Color.WHITE.setAlpha(0.15F));
               poseStack.popPose();
               TransformStack.of(poseStack).rotateZDegrees(sectorAngle);
            }

            poseStack.popPose();
         }
      }
   }

   private void renderDirectionIndicator(GuiGraphics graphics, double theta) {
      PoseStack poseStack = graphics.pose();
      float r = 0.8F;
      float g = 0.8F;
      float b = 0.8F;
      poseStack.pushPose();
      ((PoseTransformStack)((PoseTransformStack)TransformStack.of(poseStack).rotateZ((float)(-theta))).translateY(53.0F)).translateZ(15.0F);
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.begin(Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
      Matrix4f mat = poseStack.last().pose();
      bufferbuilder.addVertex(mat, 0.0F, 0.0F, 0.0F).setColor(r, g, b, 0.75F);
      bufferbuilder.addVertex(mat, 5.0F, -5.0F, 0.0F).setColor(r, g, b, 0.4F);
      bufferbuilder.addVertex(mat, 3.0F, -4.5F, 0.0F).setColor(r, g, b, 0.4F);
      bufferbuilder.addVertex(mat, 0.0F, -4.2F, 0.0F).setColor(r, g, b, 0.4F);
      bufferbuilder.addVertex(mat, -3.0F, -4.5F, 0.0F).setColor(r, g, b, 0.4F);
      bufferbuilder.addVertex(mat, -5.0F, -5.0F, 0.0F).setColor(r, g, b, 0.4F);
      BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
      poseStack.popPose();
   }

   private void submitChange() {
      BlockState selectedState = this.allStates.get(this.selectedStateIndex);
      if (selectedState != this.state) {
         CatnipServices.NETWORK.sendToServer(new RadialWrenchMenuSubmitPacket(this.pos, selectedState));
      }

      this.onClose();
   }

   private void withLevel(@Nullable BlockEntity blockEntity, Level newLevel, Runnable action) {
      boolean hasBlockEntity = blockEntity != null;
      Level originalLevel = null;
      if (hasBlockEntity) {
         originalLevel = blockEntity.getLevel();
         blockEntity.setLevel(newLevel);
      }

      try {
         action.run();
      } finally {
         if (hasBlockEntity) {
            blockEntity.setLevel(originalLevel);
         }
      }
   }

   public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      Color color = BACKGROUND_COLOR.scaleAlpha(Math.min(1.0F, ((float)this.ticksOpen + AnimationTickHolder.getPartialTicks()) / 20.0F));
      guiGraphics.fillGradient(0, 0, this.width, this.height, color.getRGB(), color.getRGB());
   }

   public boolean keyReleased(int code, int scanCode, int modifiers) {
      Key mouseKey = InputConstants.getKey(code, scanCode);
      if (AllKeys.ROTATE_MENU.getKeybind().isActiveAndMatches(mouseKey)) {
         this.submitChange();
         return true;
      } else {
         return super.keyReleased(code, scanCode, modifiers);
      }
   }

   public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
      if (pButton == 0) {
         this.submitChange();
         return true;
      } else if (pButton == 1) {
         this.onClose();
         return true;
      } else {
         return super.mouseClicked(pMouseX, pMouseY, pButton);
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      if (this.propertiesForState.size() < 2) {
         return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
      } else {
         int indexDelta = (int)Math.round(Math.signum(-scrollY));
         int newIndex = this.selectedPropertyIndex + indexDelta;
         if (newIndex < 0) {
            return false;
         } else if (newIndex >= this.propertiesForState.size()) {
            return false;
         } else {
            this.selectedPropertyIndex = newIndex;
            this.initForSelectedProperty();
            return true;
         }
      }
   }

   public void removed() {
      RadialWrenchHandler.COOLDOWN = 2;
      super.removed();
   }

   static {
      registerRotationProperty(RotatedPillarKineticBlock.AXIS, "Axis");
      registerRotationProperty(DirectionalKineticBlock.FACING, "Facing");
      registerRotationProperty(HorizontalAxisKineticBlock.HORIZONTAL_AXIS, "Axis");
      registerRotationProperty(HorizontalKineticBlock.HORIZONTAL_FACING, "Facing");
      registerRotationProperty(HopperBlock.FACING, "Facing");
      registerRotationProperty(DirectedDirectionalBlock.TARGET, "Target");
      registerRotationProperty(SequencedGearshiftBlock.VERTICAL, "Vertical");
      registerBlacklistedBlock(AllBlocks.LARGE_WATER_WHEEL.getId());
      registerBlacklistedBlock(AllBlocks.WATER_WHEEL_STRUCTURAL.getId());
   }
}

package dev.simulated_team.simulated.content.items.plunger_launcher;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetRenderHandler;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel;
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer;
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer;
import com.simibubi.create.foundation.particle.AirParticleData;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntityRenderer;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.mixin_interface.PlayerLaunchedPlungerExtension;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class PlungerLauncherItemRenderer extends CustomRenderedItemModelRenderer {
   public static final Vector3d focusPos = new Vector3d();
   public static final Matrix4f itemProjMat = new Matrix4f();

   protected void render(
      ItemStack stack,
      CustomRenderedItemModel model,
      PartialItemModelRenderer renderer,
      ItemDisplayContext transformType,
      PoseStack ms,
      MultiBufferSource buffer,
      int light,
      int overlay
   ) {
      ms.scale(0.8F, 0.8F, 0.8F);
      ms.translate(0.0F, 0.0F, 0.15F);
      renderer.render(model.getOriginalModel(), light);
      LocalPlayer player = Minecraft.getInstance().player;
      DeltaTracker timer = Minecraft.getInstance().getTimer();
      float partialTicks = timer.getGameTimeDeltaPartialTick(false);
      PlayerLaunchedPlungerExtension duck = (PlayerLaunchedPlungerExtension)player;
      LaunchedPlungerEntity plunger = duck.simulated$getLaunchedPlunger();
      if (player.getCooldowns().getCooldownPercent(stack.getItem(), partialTicks) <= 0.6F || plunger != null && plunger.getOther() == null) {
         if ((plunger == null || plunger.isRemoved() || plunger.getOther() != null)
            && player.getCooldowns().getCooldownPercent(stack.getItem(), partialTicks) <= 0.4F) {
            this.renderPlunger(ms, buffer, light, true);
         }

         this.renderPlunger(ms, buffer, light, false);
      }

      ms.translate(0.125F, -0.0625F, -0.3125F);
      ms.translate(0.0F, 0.0F, 0.0625F);
      if (transformType.firstPerson()) {
         Vector3f focusPoint = new Vector3f();
         ms.last().pose().transformPosition(focusPoint);
         itemProjMat.set(RenderSystem.getProjectionMatrix());
         focusPos.set((double)focusPoint.x, (double)focusPoint.y, (double)focusPoint.z);
      }
   }

   private void renderPlunger(PoseStack ms, MultiBufferSource buffer, int light, boolean first) {
      ms.pushPose();
      SuperByteBuffer body = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_BODY, Blocks.AIR.defaultBlockState());
      SuperByteBuffer spool = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_SPOOL, Blocks.AIR.defaultBlockState());
      SuperByteBuffer joint = CachedBuffers.partial(SimPartialModels.LAUNCHED_PLUNGER_JOINT, Blocks.AIR.defaultBlockState());
      ms.translate(0.125F * (float)(first ? -1 : 1), -0.0625F, -0.3125F);
      DeltaTracker timer = Minecraft.getInstance().getTimer();
      float partialTicks = timer.getGameTimeDeltaPartialTick(false);
      ItemCooldowns cooldowns = Minecraft.getInstance().player.getCooldowns();
      float cooldown = cooldowns.getCooldownPercent(SimItems.PLUNGER_LAUNCHER.asItem(), partialTicks);
      if (cooldown > 0.0F && PlungerLauncherItem.reloadCooldown) {
         if (!first) {
            float slideIn = Mth.clamp(Mth.map(cooldown, 0.3F, 0.6F, 0.0F, 1.0F), 0.0F, 1.0F);
            slideIn = (float)Math.pow((double)slideIn, 3.0);
            ms.translate(0.0F, 0.0F, -slideIn / 12.0F);
         } else {
            float slideIn = Mth.clamp(Mth.map(cooldown, 0.1F, 0.4F, 0.0F, 1.0F), 0.0F, 1.0F);
            slideIn = (float)Math.pow((double)slideIn, 3.0);
            ms.translate(0.0F, 0.0F, -slideIn / 12.0F);
         }
      }

      body.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      joint.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      ms.translate(0.0F, 0.0F, 0.1875F);
      spool.light(light).renderInto(ms, buffer.getBuffer(RenderType.solid()));
      ms.popPose();
   }

   public static class RenderHandler extends ShootableGadgetRenderHandler {
      public void basicShoot(InteractionHand hand) {
         LocalPlayer player = Minecraft.getInstance().player;
         if (player != null) {
            boolean rightHand = hand == InteractionHand.MAIN_HAND ^ player.getMainArm() == HumanoidArm.LEFT;
            if (rightHand) {
               this.rightHandAnimation = 0.2F;
               this.dontReequipRight = false;
            } else {
               this.leftHandAnimation = 0.2F;
               this.dontReequipLeft = false;
            }

            Vec3 focusPos1 = LaunchedPlungerEntityRenderer.getFirstPersonFocusPos(0.0F);

            for (int i = 0; (double)i < Math.random() * 4.0; i++) {
               Vec3 m2 = VecHelper.offsetRandomly(player.getViewVector(0.0F), player.level().random, 0.5F);
               player.level().addParticle(new AirParticleData(1.0F, 0.25F), focusPos1.x, focusPos1.y, focusPos1.z, m2.x, m2.y, m2.z);
            }

            this.playSound(hand, player.position());
         }
      }

      public void playSound(InteractionHand hand, Vec3 position) {
      }

      protected boolean appliesTo(ItemStack stack) {
         return SimItems.PLUNGER_LAUNCHER.is(stack.getItem());
      }

      protected void transformTool(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
         ms.translate(flip * -0.1F, 0.05F, 0.14F);
         TransformStack.of(ms).rotateXDegrees(recoil * 80.0F);
      }

      protected void transformHand(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
         ms.scale(0.0F, 0.0F, 0.0F);
      }
   }
}

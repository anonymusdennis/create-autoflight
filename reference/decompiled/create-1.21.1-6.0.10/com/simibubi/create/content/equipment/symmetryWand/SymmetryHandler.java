package com.simibubi.create.content.equipment.symmetryWand;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.symmetryWand.mirror.EmptyMirror;
import com.simibubi.create.content.equipment.symmetryWand.mirror.SymmetryMirror;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import net.neoforged.neoforge.event.level.BlockEvent.EntityPlaceEvent;
import org.joml.Vector3f;

@EventBusSubscriber
public class SymmetryHandler {
   private static int tickCounter = 0;

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onBlockPlaced(EntityPlaceEvent event) {
      if (!event.getLevel().isClientSide()) {
         if (event.getEntity() instanceof Player player) {
            Inventory var4 = player.getInventory();

            for (int i = 0; i < Inventory.getSelectionSize(); i++) {
               if (AllItems.WAND_OF_SYMMETRY.isIn(var4.getItem(i))) {
                  SymmetryWandItem.apply(player.level(), var4.getItem(i), player, event.getPos(), event.getPlacedBlock());
               }
            }
         }
      }
   }

   @SubscribeEvent(
      priority = EventPriority.LOWEST
   )
   public static void onBlockDestroyed(BreakEvent event) {
      if (!event.getLevel().isClientSide()) {
         Player player = event.getPlayer();
         Inventory inv = player.getInventory();

         for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            if (AllItems.WAND_OF_SYMMETRY.isIn(inv.getItem(i))) {
               SymmetryWandItem.remove(player.level(), inv.getItem(i), player, event.getPos());
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onRenderWorld(RenderLevelStageEvent event) {
      if (event.getStage() == Stage.AFTER_PARTICLES) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         RandomSource random = RandomSource.create();

         for (int i = 0; i < Inventory.getSelectionSize(); i++) {
            ItemStack stackInSlot = player.getInventory().getItem(i);
            if (AllItems.WAND_OF_SYMMETRY.isIn(stackInSlot) && SymmetryWandItem.isEnabled(stackInSlot)) {
               SymmetryMirror mirror = SymmetryWandItem.getMirror(stackInSlot);
               if (!(mirror instanceof EmptyMirror)) {
                  BlockPos pos = BlockPos.containing(mirror.getPosition());
                  float yShift = 0.0F;
                  double speed = 0.0625;
                  yShift = Mth.sin((float)((double)AnimationTickHolder.getRenderTime() * speed)) / 5.0F;
                  BufferSource buffer = mc.renderBuffers().bufferSource();
                  Camera info = mc.gameRenderer.getMainCamera();
                  Vec3 view = info.getPosition();
                  PoseStack ms = event.getPoseStack();
                  ms.pushPose();
                  ms.translate((double)pos.getX() - view.x(), (double)pos.getY() - view.y(), (double)pos.getZ() - view.z());
                  ms.translate(0.0F, yShift + 0.2F, 0.0F);
                  mirror.applyModelTransform(ms);
                  BakedModel model = mirror.getModel().get();
                  VertexConsumer builder = buffer.getBuffer(RenderType.solid());
                  mc.getBlockRenderer()
                     .getModelRenderer()
                     .tesselateBlock(
                        player.level(),
                        model,
                        Blocks.AIR.defaultBlockState(),
                        pos,
                        ms,
                        builder,
                        true,
                        random,
                        Mth.getSeed(pos),
                        OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY,
                        RenderType.solid()
                     );
                  ms.popPose();
                  buffer.endBatch();
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   @SubscribeEvent
   public static void onClientTick(Post event) {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (mc.level != null) {
         if (!mc.isPaused()) {
            tickCounter++;
            if (tickCounter % 10 == 0) {
               for (int i = 0; i < Inventory.getSelectionSize(); i++) {
                  ItemStack stackInSlot = player.getInventory().getItem(i);
                  if (stackInSlot != null && AllItems.WAND_OF_SYMMETRY.isIn(stackInSlot) && SymmetryWandItem.isEnabled(stackInSlot)) {
                     SymmetryMirror mirror = SymmetryWandItem.getMirror(stackInSlot);
                     if (!(mirror instanceof EmptyMirror)) {
                        RandomSource random = mc.level.random;
                        double offsetX = (random.nextDouble() - 0.5) * 0.3;
                        double offsetZ = (random.nextDouble() - 0.5) * 0.3;
                        Vec3 pos = mirror.getPosition().add(0.5 + offsetX, 0.25, 0.5 + offsetZ);
                        Vec3 speed = new Vec3(0.0, random.nextDouble() * 1.0 / 8.0, 0.0);
                        mc.level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
                     }
                  }
               }
            }
         }
      }
   }

   public static void drawEffect(BlockPos from, BlockPos to) {
      ClientLevel level = Minecraft.getInstance().level;
      RandomSource random = level.random;
      double density = 0.8F;
      Vec3 start = Vec3.atLowerCornerOf(from).add(0.5, 0.5, 0.5);
      Vec3 end = Vec3.atLowerCornerOf(to).add(0.5, 0.5, 0.5);
      Vec3 diff = end.subtract(start);
      Vec3 step = diff.normalize().scale(density);
      int steps = (int)(diff.length() / step.length());

      for (int i = 3; i < steps - 1; i++) {
         Vec3 pos = start.add(step.scale((double)i));
         Vec3 speed = new Vec3(0.0, random.nextDouble() * -40.0, 0.0);
         level.addParticle(new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F), pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
      }

      Vec3 speed = new Vec3(0.0, random.nextDouble() * 1.0 / 32.0, 0.0);
      Vec3 pos = start.add(step.scale(2.0));
      level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
      speed = new Vec3(0.0, random.nextDouble() * 1.0 / 32.0, 0.0);
      pos = start.add(step.scale((double)steps));
      level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, speed.x, speed.y, speed.z);
   }
}

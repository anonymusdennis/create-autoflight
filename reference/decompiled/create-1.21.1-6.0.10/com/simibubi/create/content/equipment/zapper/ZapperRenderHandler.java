package com.simibubi.create.content.equipment.zapper;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllSoundEvents;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ZapperRenderHandler extends ShootableGadgetRenderHandler {
   public List<ZapperRenderHandler.LaserBeam> cachedBeams;

   @Override
   protected boolean appliesTo(ItemStack stack) {
      return stack.getItem() instanceof ZapperItem;
   }

   @Override
   public void tick() {
      super.tick();
      if (this.cachedBeams == null) {
         this.cachedBeams = new LinkedList<>();
      }

      this.cachedBeams.removeIf(b -> b.itensity < 0.1F);
      if (!this.cachedBeams.isEmpty()) {
         this.cachedBeams
            .forEach(
               beam -> Outliner.getInstance()
                     .endChasingLine(beam, beam.start, beam.end, 1.0F - beam.itensity, false)
                     .disableLineNormals()
                     .colored(16777215)
                     .lineWidth(beam.itensity * 1.0F / 8.0F)
            );
         this.cachedBeams.forEach(b -> b.itensity *= 0.6F);
      }
   }

   @Override
   protected void transformTool(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
      ms.translate(flip * -0.1F, 0.1F, -0.4F);
      ms.mulPose(Axis.YP.rotationDegrees(flip * 5.0F));
   }

   @Override
   protected void transformHand(PoseStack ms, float flip, float equipProgress, float recoil, float pt) {
   }

   @Override
   protected void playSound(InteractionHand hand, Vec3 position) {
      float pitch = hand == InteractionHand.MAIN_HAND ? 0.1F : 0.9F;
      Minecraft mc = Minecraft.getInstance();
      AllSoundEvents.WORLDSHAPER_PLACE.play(mc.level, mc.player, position, 0.1F, pitch);
   }

   public void addBeam(ZapperRenderHandler.LaserBeam beam) {
      RandomSource random = Minecraft.getInstance().level.random;
      double x = beam.end.x;
      double y = beam.end.y;
      double z = beam.end.z;
      ClientLevel world = Minecraft.getInstance().level;
      Supplier<Double> randomSpeed = () -> (random.nextDouble() - 0.5) * 0.2F;
      Supplier<Double> randomOffset = () -> (random.nextDouble() - 0.5) * 0.2F;

      for (int i = 0; i < 10; i++) {
         world.addParticle(ParticleTypes.END_ROD, x, y, z, randomSpeed.get(), randomSpeed.get(), randomSpeed.get());
         world.addParticle(ParticleTypes.FIREWORK, x + randomOffset.get(), y + randomOffset.get(), z + randomOffset.get(), 0.0, 0.0, 0.0);
      }

      this.cachedBeams.add(beam);
   }

   public static class LaserBeam {
      float itensity;
      Vec3 start;
      Vec3 end;

      public LaserBeam(Vec3 start, Vec3 end) {
         this.start = start;
         this.end = end;
         this.itensity = 1.0F;
      }
   }
}

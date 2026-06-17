package dev.simulated_team.simulated.content.items.rope.RopeItem;

import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.config.server.blocks.SimBlockConfigs;
import dev.simulated_team.simulated.content.blocks.rope.RopeStrandHolderBehavior;
import dev.simulated_team.simulated.content.blocks.rope.rope_winch.RopeWinchBlockEntity;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ClientRopeItemHandler {
   public static void tick() {
      Player player = Minecraft.getInstance().player;
      Level level = Minecraft.getInstance().level;
      if (player != null && level != null) {
         if (Minecraft.getInstance().screen == null) {
            for (InteractionHand hand : InteractionHand.values()) {
               ItemStack heldItem = player.getItemInHand(hand);
               if (SimItems.ROPE_COUPLING.isIn(heldItem) && heldItem.has(SimDataComponents.ROPE_FIRST_CONNECTION)) {
                  BlockPos firstBlock = (BlockPos)heldItem.get(SimDataComponents.ROPE_FIRST_CONNECTION);
                  if (Minecraft.getInstance().hitResult instanceof BlockHitResult hitResult) {
                     BlockPos hitBlock = hitResult.getBlockPos();
                     Vec3 firstPoint = firstBlock.getCenter();
                     SimBlockConfigs blockConfig = SimConfigService.INSTANCE.server().blocks;
                     double maxRopeRange = (Double)blockConfig.maxRopeRange.get();
                     boolean inRange = Sable.HELPER.distanceSquaredWithSubLevels(level, firstPoint, hitResult.getLocation()) < maxRopeRange * maxRopeRange;
                     boolean valid = RopeItem.isValidRopeAttachment(level, hitBlock) && !hitBlock.equals(firstBlock) && inRange;
                     RopeStrandHolderBehavior holderA = RopeItem.getRopeHolder(level, hitBlock);
                     RopeStrandHolderBehavior holderB = RopeItem.getRopeHolder(level, firstBlock);
                     if (valid
                        && holderA != null
                        && holderA.blockEntity instanceof RopeWinchBlockEntity
                        && holderB != null
                        && holderB.blockEntity instanceof RopeWinchBlockEntity) {
                        valid = false;
                     }

                     Vec3 target = valid ? hitBlock.getCenter() : hitResult.getLocation();
                     Color color;
                     if (valid) {
                        color = new Color(SimColors.SUCCESS_LIME);
                     } else {
                        color = new Color(inRange ? SimColors.PERCHANCE_ORANGE : SimColors.NUH_UH_RED);
                     }

                     Outliner.getInstance()
                        .chaseAABB("FirstRopeAttachmentPoint", new AABB(firstPoint, firstPoint))
                        .colored(color)
                        .lineWidth(0.33333334F)
                        .disableLineNormals();
                     Vec3 globalFirstPoint = Sable.HELPER.projectOutOfSubLevel(level, firstPoint);
                     Vec3 globalTarget = Sable.HELPER.projectOutOfSubLevel(level, target);
                     if (valid) {
                        Outliner.getInstance()
                           .chaseAABB("SecondRopeAttachmentPoint", new AABB(target, target))
                           .colored(color)
                           .lineWidth(0.33333334F)
                           .disableLineNormals();
                        double points = Math.floor(globalFirstPoint.distanceTo(globalTarget));
                        Vec3 backwardsDiff = globalFirstPoint.subtract(globalTarget).normalize();

                        for (int i = 0; (double)i < points; i++) {
                           Vec3 point = globalTarget.add(backwardsDiff.scale((double)i));
                           Outliner.getInstance().chaseAABB("RopePoint" + i, new AABB(point, point)).colored(color).lineWidth(0.125F).disableLineNormals();
                        }
                     } else if (!inRange) {
                        globalTarget = globalTarget.subtract(globalFirstPoint).normalize().scale(maxRopeRange - 0.5).add(globalFirstPoint);
                        Outliner.getInstance()
                           .chaseAABB("SecondRopeAttachmentPoint", new AABB(globalTarget, globalTarget))
                           .colored(color)
                           .lineWidth(0.33333334F)
                           .disableLineNormals();
                     }

                     DustParticleOptions data = new DustParticleOptions(color.asVectorF(), 1.0F);
                     double totalFlyingTicks = 10.0;
                     int segments = 4;

                     for (int i = 0; i < 4; i++) {
                        Vec3 vec = globalFirstPoint.lerp(globalTarget, (double)level.random.nextFloat());
                        level.addParticle(data, vec.x, vec.y, vec.z, 0.0, 0.0, 0.0);
                     }
                  }

                  return;
               }
            }
         }
      }
   }
}

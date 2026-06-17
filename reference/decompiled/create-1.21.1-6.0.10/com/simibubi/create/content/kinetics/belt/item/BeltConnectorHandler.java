package com.simibubi.create.content.kinetics.belt.item;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BeltConnectorHandler {
   public static void tick() {
      Player player = Minecraft.getInstance().player;
      Level level = Minecraft.getInstance().level;
      if (player != null && level != null) {
         if (Minecraft.getInstance().screen == null) {
            RandomSource random = level.random;

            for (InteractionHand hand : InteractionHand.values()) {
               ItemStack heldItem = player.getItemInHand(hand);
               if (AllItems.BELT_CONNECTOR.isIn(heldItem) && heldItem.has(AllDataComponents.BELT_FIRST_SHAFT)) {
                  BlockPos first = (BlockPos)heldItem.get(AllDataComponents.BELT_FIRST_SHAFT);
                  if (level.getBlockState(first).hasProperty(BlockStateProperties.AXIS)) {
                     Axis axis = (Axis)level.getBlockState(first).getValue(BlockStateProperties.AXIS);
                     HitResult rayTrace = Minecraft.getInstance().hitResult;
                     if (rayTrace != null && rayTrace instanceof BlockHitResult) {
                        BlockPos selected = ((BlockHitResult)rayTrace).getBlockPos();
                        if (level.getBlockState(selected).canBeReplaced()) {
                           return;
                        }

                        if (!ShaftBlock.isShaft(level.getBlockState(selected))) {
                           selected = selected.relative(((BlockHitResult)rayTrace).getDirection());
                        }

                        if (!selected.closerThan(first, (double)((Integer)AllConfigs.server().kinetics.maxBeltLength.get()).intValue())) {
                           return;
                        }

                        boolean canConnect = BeltConnectorItem.validateAxis(level, selected) && BeltConnectorItem.canConnect(level, first, selected);
                        Vec3 start = Vec3.atLowerCornerOf(first);
                        Vec3 end = Vec3.atLowerCornerOf(selected);
                        Vec3 actualDiff = end.subtract(start);
                        end = end.subtract(axis.choose(actualDiff.x, 0.0, 0.0), axis.choose(0.0, actualDiff.y, 0.0), axis.choose(0.0, 0.0, actualDiff.z));
                        Vec3 diff = end.subtract(start);
                        double x = Math.abs(diff.x);
                        double y = Math.abs(diff.y);
                        double z = Math.abs(diff.z);
                        float length = (float)Math.max(x, Math.max(y, z));
                        Vec3 step = diff.normalize();
                        int sames = (x == y ? 1 : 0) + (y == z ? 1 : 0) + (z == x ? 1 : 0);
                        if (sames == 0) {
                           List<Vec3> validDiffs = new LinkedList<>();

                           for (int i = -1; i <= 1; i++) {
                              for (int j = -1; j <= 1; j++) {
                                 for (int k = -1; k <= 1; k++) {
                                    if (axis.choose(i, j, k) == 0 && (axis != Axis.Y || i == 0 || k == 0) && (i != 0 || j != 0 || k != 0)) {
                                       validDiffs.add(new Vec3((double)i, (double)j, (double)k));
                                    }
                                 }
                              }
                           }

                           int closestIndex = 0;
                           float closest = Float.MAX_VALUE;

                           for (Vec3 validDiff : validDiffs) {
                              double distanceTo = step.distanceTo(validDiff);
                              if (distanceTo < (double)closest) {
                                 closest = (float)distanceTo;
                                 closestIndex = validDiffs.indexOf(validDiff);
                              }
                           }

                           step = validDiffs.get(closestIndex);
                        }

                        if (axis == Axis.Y && step.x != 0.0 && step.z != 0.0) {
                           return;
                        }

                        step = new Vec3(Math.signum(step.x), Math.signum(step.y), Math.signum(step.z));

                        for (float f = 0.0F; f < length; f += 0.0625F) {
                           Vec3 position = start.add(step.scale((double)f));
                           if (random.nextInt(10) == 0) {
                              level.addParticle(
                                 new DustParticleOptions(new Vector3f(canConnect ? 0.3F : 0.9F, canConnect ? 0.9F : 0.3F, 0.5F), 1.0F),
                                 position.x + 0.5,
                                 position.y + 0.5,
                                 position.z + 0.5,
                                 0.0,
                                 0.0,
                                 0.0
                              );
                           }
                        }

                        return;
                     }

                     if (random.nextInt(50) == 0) {
                        level.addParticle(
                           new DustParticleOptions(new Vector3f(0.3F, 0.9F, 0.5F), 1.0F),
                           (double)((float)first.getX() + 0.5F + randomOffset(random, 0.25F)),
                           (double)((float)first.getY() + 0.5F + randomOffset(random, 0.25F)),
                           (double)((float)first.getZ() + 0.5F + randomOffset(random, 0.25F)),
                           0.0,
                           0.0,
                           0.0
                        );
                     }

                     return;
                  }
               }
            }
         }
      }
   }

   private static float randomOffset(RandomSource random, float range) {
      return (random.nextFloat() - 0.5F) * 2.0F * range;
   }
}

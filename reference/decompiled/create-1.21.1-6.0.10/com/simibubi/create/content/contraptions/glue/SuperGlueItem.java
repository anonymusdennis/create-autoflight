package com.simibubi.create.content.contraptions.glue;

import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber
public class SuperGlueItem extends Item {
   @SubscribeEvent
   public static void glueItemAlwaysPlacesWhenUsed(RightClickBlock event) {
      if (event.getHitVec() != null) {
         BlockState blockState = event.getLevel().getBlockState(event.getHitVec().getBlockPos());
         if (blockState.getBlock() instanceof AbstractChassisBlock cb && cb.getGlueableSide(blockState, event.getFace()) != null) {
            return;
         }
      }

      if (event.getItemStack().getItem() instanceof SuperGlueItem) {
         event.setUseBlock(TriState.FALSE);
      }
   }

   public SuperGlueItem(Properties properties) {
      super(properties);
   }

   public boolean canAttackBlock(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
      return false;
   }

   @OnlyIn(Dist.CLIENT)
   public static void spawnParticles(Level world, BlockPos pos, Direction direction, boolean fullBlock) {
      Vec3 vec = Vec3.atLowerCornerOf(direction.getNormal());
      Vec3 plane = VecHelper.axisAlingedPlaneOf(vec);
      Vec3 facePos = VecHelper.getCenterOf(pos).add(vec.scale(0.5));
      float distance = fullBlock ? 1.0F : 0.25F + 0.25F * (world.random.nextFloat() - 0.5F);
      plane = plane.scale((double)distance);
      ItemStack stack = new ItemStack(Items.SLIME_BALL);

      for (int i = fullBlock ? 40 : 15; i > 0; i--) {
         Vec3 offset = VecHelper.rotate(plane, (double)(360.0F * world.random.nextFloat()), direction.getAxis());
         Vec3 motion = offset.normalize().scale(0.0625);
         if (fullBlock) {
            offset = new Vec3(Mth.clamp(offset.x, -0.5, 0.5), Mth.clamp(offset.y, -0.5, 0.5), Mth.clamp(offset.z, -0.5, 0.5));
         }

         Vec3 particlePos = facePos.add(offset);
         world.addParticle(new ItemParticleOption(ParticleTypes.ITEM, stack), particlePos.x, particlePos.y, particlePos.z, motion.x, motion.y, motion.z);
      }
   }
}

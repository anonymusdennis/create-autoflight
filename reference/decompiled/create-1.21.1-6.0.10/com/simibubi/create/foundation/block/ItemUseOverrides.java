package com.simibubi.create.foundation.block;

import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.HashSet;
import java.util.Set;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;

@EventBusSubscriber
public class ItemUseOverrides {
   private static final Set<ResourceLocation> OVERRIDES = new HashSet<>();

   public static void addBlock(Block block) {
      OVERRIDES.add(RegisteredObjectsHelper.getKeyOrThrow(block));
   }

   @SubscribeEvent
   public static void onBlockActivated(RightClickBlock event) {
      if (!AllItems.WRENCH.isIn(event.getItemStack())) {
         Level level = event.getLevel();
         BlockPos pos = event.getPos();
         Direction face = event.getFace();
         Player player = event.getEntity();
         InteractionHand hand = event.getHand();
         BlockState state = level.getBlockState(pos);
         ResourceLocation id = RegisteredObjectsHelper.getKeyOrThrow(state.getBlock());
         if (OVERRIDES.contains(id)) {
            BlockHitResult blockTrace = new BlockHitResult(VecHelper.getCenterOf(pos), face, pos, true);
            InteractionResult result = BlockHelper.invokeUse(state, level, player, hand, blockTrace);
            if (result.consumesAction()) {
               event.setCanceled(true);
               event.setCancellationResult(result);
            }
         }
      }
   }
}

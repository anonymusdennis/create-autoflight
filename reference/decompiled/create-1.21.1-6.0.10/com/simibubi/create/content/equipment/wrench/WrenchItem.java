package com.simibubi.create.content.equipment.wrench;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import org.jetbrains.annotations.NotNull;

public class WrenchItem extends Item {
   public WrenchItem(Properties properties) {
      super(properties);
   }

   @OnlyIn(Dist.CLIENT)
   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(SimpleCustomRenderer.create(this, new WrenchItemRenderer()));
   }

   @NotNull
   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      if (player != null && player.mayBuild()) {
         BlockState state = context.getLevel().getBlockState(context.getClickedPos());
         if (state.getBlock() instanceof IWrenchable actor) {
            return player.isShiftKeyDown() ? actor.onSneakWrenched(state, context) : actor.onWrenched(state, context);
         } else {
            return player.isShiftKeyDown() && this.canWrenchPickup(state) ? this.onItemUseOnOther(context) : super.useOn(context);
         }
      } else {
         return super.useOn(context);
      }
   }

   private boolean canWrenchPickup(BlockState state) {
      return AllTags.AllBlockTags.WRENCH_PICKUP.matches(state);
   }

   private InteractionResult onItemUseOnOther(UseOnContext context) {
      Player player = context.getPlayer();
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState state = world.getBlockState(pos);
      if (!(world instanceof ServerLevel)) {
         return InteractionResult.SUCCESS;
      } else {
         if (player != null && !player.isCreative()) {
            Block.getDrops(state, (ServerLevel)world, pos, world.getBlockEntity(pos), player, context.getItemInHand())
               .forEach(itemStack -> player.getInventory().placeItemBackInInventory(itemStack));
         }

         state.spawnAfterBreak((ServerLevel)world, pos, ItemStack.EMPTY, true);
         world.destroyBlock(pos, false);
         AllSoundEvents.WRENCH_REMOVE.playOnServer(world, pos, 1.0F, Create.RANDOM.nextFloat() * 0.5F + 0.5F);
         return InteractionResult.SUCCESS;
      }
   }

   public static void wrenchInstaKillsMinecarts(AttackEntityEvent event) {
      if (event.getTarget() instanceof AbstractMinecart minecart) {
         Player player = event.getEntity();
         ItemStack heldItem = player.getMainHandItem();
         if (AllItems.WRENCH.isIn(heldItem)) {
            if (!player.isCreative()) {
               minecart.hurt(minecart.damageSources().playerAttack(player), 100.0F);
            }
         }
      }
   }
}

package net.createmod.catnip.placement;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

public class PlacementOffset {
   private final boolean success;
   private Vec3i pos;
   private Function<BlockState, BlockState> stateTransform;
   @Nullable
   private BlockState ghostState;

   private PlacementOffset(boolean success) {
      this.success = success;
      this.pos = BlockPos.ZERO;
      this.stateTransform = Function.identity();
      this.ghostState = null;
   }

   public static PlacementOffset fail() {
      return new PlacementOffset(false);
   }

   public static PlacementOffset success() {
      return new PlacementOffset(true);
   }

   public static PlacementOffset success(Vec3i pos) {
      return success().at(pos);
   }

   public static PlacementOffset success(Vec3i pos, Function<BlockState, BlockState> transform) {
      return success().at(pos).withTransform(transform);
   }

   public PlacementOffset at(Vec3i pos) {
      this.pos = pos;
      return this;
   }

   public PlacementOffset withTransform(Function<BlockState, BlockState> stateTransform) {
      this.stateTransform = stateTransform;
      return this;
   }

   public PlacementOffset withGhostState(BlockState ghostState) {
      this.ghostState = ghostState;
      return this;
   }

   public boolean isSuccessful() {
      return this.success;
   }

   public Vec3i getPos() {
      return this.pos;
   }

   public BlockPos getBlockPos() {
      return this.pos instanceof BlockPos ? (BlockPos)this.pos : new BlockPos(this.pos);
   }

   public Function<BlockState, BlockState> getTransform() {
      return this.stateTransform;
   }

   public boolean hasGhostState() {
      return this.ghostState != null;
   }

   @Nullable
   public BlockState getGhostState() {
      return this.ghostState;
   }

   public boolean isReplaceable(Level world) {
      return !this.success ? false : world.getBlockState(new BlockPos(this.pos)).canBeReplaced();
   }

   public ItemInteractionResult placeInWorld(Level world, BlockItem blockItem, Player player, InteractionHand hand, BlockHitResult ray) {
      if (!this.isReplaceable(world)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (world.isClientSide) {
         return ItemInteractionResult.SUCCESS;
      } else {
         UseOnContext context = new UseOnContext(player, hand, ray);
         BlockPos newPos = new BlockPos(this.pos);
         ItemStack stackBefore = player.getItemInHand(hand).copy();
         if (!world.mayInteract(player, newPos)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else {
            BlockState state = this.stateTransform.apply(blockItem.getBlock().defaultBlockState());
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
               FluidState fluidState = world.getFluidState(newPos);
               state = (BlockState)state.setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
            }

            if (CatnipServices.HOOKS.playerPlaceSingleBlock(player, world, newPos, state)) {
               return ItemInteractionResult.FAIL;
            } else {
               BlockState newState = world.getBlockState(newPos);
               SoundType soundtype = newState.getSoundType();
               world.playSound(null, newPos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
               world.gameEvent(GameEvent.BLOCK_PLACE, newPos, Context.of(player, newState));
               player.awardStat(Stats.ITEM_USED.get(blockItem));
               newState.getBlock().setPlacedBy(world, newPos, newState, player, stackBefore);
               if (player instanceof ServerPlayer) {
                  CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)player, newPos, context.getItemInHand());
               }

               if (!player.isCreative()) {
                  context.getItemInHand().shrink(1);
               }

               return ItemInteractionResult.SUCCESS;
            }
         }
      }
   }
}

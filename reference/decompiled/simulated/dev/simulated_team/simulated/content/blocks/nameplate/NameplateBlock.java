package dev.simulated_team.simulated.content.blocks.nameplate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NameplateBlock extends HorizontalDirectionalBlock implements IBE<NameplateBlockEntity>, IWrenchable, BlockSubLevelAssemblyListener {
   public static final EnumProperty<NameplateBlock.Position> POSITION = EnumProperty.create("position", NameplateBlock.Position.class);
   public static final MapCodec<NameplateBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(propertiesCodec(), DyeColor.CODEC.fieldOf("DyeColor").forGetter(NameplateBlock::getColor))
            .apply(instance, NameplateBlock::new)
   );
   private static final int placementHelperId = PlacementHelpers.register(new NameplateBlock.PlacementHelper());
   protected final DyeColor color;

   public NameplateBlock(Properties properties, DyeColor color) {
      super(properties);
      this.color = color;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder);
      pBuilder.add(new Property[]{POSITION});
      pBuilder.add(new Property[]{FACING});
   }

   public static boolean hasBackSupport(Direction facingDir, LevelReader level, BlockPos pos) {
      return !level.getBlockState(pos.relative(facingDir, -1)).isAir();
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      Direction facing = (Direction)pState.getValue(FACING);
      if (hasBackSupport(facing, pLevel, pPos)) {
         return true;
      } else {
         BlockState leftState = pLevel.getBlockState(pPos.relative(facing.getClockWise()));
         if (leftState.getBlock().equals(pState.getBlock()) && leftState.getValue(FACING) == facing) {
            return true;
         } else {
            BlockState rightState = pLevel.getBlockState(pPos.relative(facing.getCounterClockWise()));
            return rightState.getBlock().equals(pState.getBlock()) && rightState.getValue(FACING) == facing;
         }
      }
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState state = super.getStateForPlacement(pContext);
      Direction direction = pContext.getClickedFace();
      if (pContext.getClickedFace().getAxis().equals(Axis.Y)) {
         direction = pContext.getHorizontalDirection().getOpposite();
      }

      BlockPos pos = pContext.getClickedPos();
      Level level = pContext.getLevel();
      NameplateBlock.Position position = this.getPositionState(level, pos, direction);
      return (BlockState)((BlockState)state.setValue(POSITION, position)).setValue(FACING, direction);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SimBlockShapes.NAMEPLATE.get((Direction)pState.getValue(FACING));
   }

   public NameplateBlock.Position getPositionState(LevelAccessor level, BlockPos pos, Direction facing) {
      NameplateBlock.Position outPos;
      BlockState leftState;
      BlockState rightState;
      boolean var10000;
      label43: {
         outPos = NameplateBlock.Position.SINGLE;
         leftState = level.getBlockState(pos.offset(facing.getClockWise(Axis.Y).getNormal()));
         rightState = level.getBlockState(pos.offset(facing.getCounterClockWise(Axis.Y).getNormal()));
         if (leftState.getBlock() instanceof NameplateBlock npb && npb.getColor() == this.getColor()) {
            var10000 = true;
            break label43;
         }

         var10000 = false;
      }

      boolean left;
      label38: {
         left = var10000;
         if (rightState.getBlock() instanceof NameplateBlock npb && npb.getColor() == this.getColor()) {
            var10000 = true;
            break label38;
         }

         var10000 = false;
      }

      boolean right = var10000;
      if (left) {
         boolean leftBlock = ((Direction)leftState.getValue(FACING)).equals(facing);
         if (leftBlock) {
            outPos = NameplateBlock.Position.RIGHT;
         }
      }

      if (right) {
         boolean rightBlock = ((Direction)rightState.getValue(FACING)).equals(facing);
         if (rightBlock) {
            outPos = NameplateBlock.Position.LEFT;
         }
      }

      if (left && right) {
         boolean rightBlock = ((Direction)rightState.getValue(FACING)).equals(facing);
         boolean leftBlock = ((Direction)leftState.getValue(FACING)).equals(facing);
         if (leftBlock && rightBlock) {
            outPos = NameplateBlock.Position.MIDDLE;
         }
      }

      return outPos;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         IPlacementHelper placementHelper = PlacementHelpers.get(placementHelperId);
         if (itemStack.getItem() instanceof BlockItem bi && blockState.is(bi.getBlock()) && placementHelper.matchesItem(itemStack)) {
            ItemInteractionResult result = placementHelper.getOffset(player, level, blockState, blockPos, blockHitResult)
               .placeInWorld(level, (BlockItem)itemStack.getItem(), player, interactionHand, blockHitResult);
            if (result == ItemInteractionResult.SUCCESS) {
               return ItemInteractionResult.SUCCESS;
            }
         }
      }

      if (player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         ItemStack heldItem = player.getItemInHand(interactionHand);
         if (heldItem.getItem() instanceof SignApplicator signApplicator) {
            MutableBoolean success = new MutableBoolean(false);
            this.withBlockEntityDo(
               level,
               blockPos,
               nbe -> {
                  NameplateBlockEntity controller = nbe.findController();
                  if (controller.allowsEditing()) {
                     SignBlockEntity dummySign = new SignBlockEntity(blockPos, Blocks.OAK_SIGN.defaultBlockState());
                     dummySign.setLevel(controller.getLevel());
                     SignText text = dummySign.getFrontText()
                        .setMessage(0, Component.literal(controller.getName()))
                        .setColor(controller.getTextColor())
                        .setHasGlowingText(controller.glowing);
                     dummySign.setText(text, true);
                     dummySign.setWaxed(controller.waxed);
                     if (signApplicator.canApplyToSign(text, player) && signApplicator.tryApplyToSign(controller.getLevel(), dummySign, true, player)) {
                        text = dummySign.getFrontText();
                        controller.setTextColor(text.getColor(), true);
                        controller.glowing = text.hasGlowingText();
                        controller.waxed = dummySign.isWaxed();
                        controller.updateNameplates(this.getColor(), (Direction)blockState.getValue(FACING));
                        success.setTrue();
                     }

                     nbe.sendData();
                  }
               }
            );
            return success.booleanValue() ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else {
            if (level.isClientSide) {
               this.withBlockEntityDo(level, blockPos, nbe -> {
                  NameplateBlockEntity controller = nbe.findController();
                  if (!controller.waxed) {
                     NameplateScreen.setScreen(nbe);
                  } else {
                     level.playSound(player, blockPos, SoundEvents.WAXED_SIGN_INTERACT_FAIL, SoundSource.BLOCKS);
                  }
               });
            }

            return ItemInteractionResult.SUCCESS;
         }
      }
   }

   public void neighborChanged(BlockState state, Level level, BlockPos selfPos, Block neighborBlock, BlockPos neighborPos, boolean pMovedByPiston) {
      super.neighborChanged(state, level, selfPos, neighborBlock, neighborPos, pMovedByPiston);
      if (level.getBlockEntity(selfPos) instanceof NameplateBlockEntity nbe) {
         if (neighborPos.equals(selfPos.relative(((Direction)state.getValue(FACING)).getClockWise(Axis.Y)))) {
            nbe.checkAndUpdateController(this.color, (Direction)state.getValue(FACING));
         } else {
            nbe.findController().checkAndUpdateController(this.color, (Direction)state.getValue(FACING));
         }

         if (!NameplateBlockEntity.hasSupport(nbe)) {
            level.destroyBlock(selfPos, true);
         }
      }
   }

   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pPos, BlockPos pNeighborPos) {
      NameplateBlock.Position posState = this.getPositionState(pLevel, pPos, (Direction)pState.getValue(FACING));
      return (BlockState)super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos).setValue(POSITION, posState);
   }

   public Class<NameplateBlockEntity> getBlockEntityClass() {
      return NameplateBlockEntity.class;
   }

   public BlockEntityType<? extends NameplateBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends NameplateBlockEntity>)SimBlockEntityTypes.NAMEPLATE.get();
   }

   public DyeColor getColor() {
      return this.color;
   }

   protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   public void afterMove(ServerLevel serverLevel, ServerLevel resultingLevel, BlockState blockState, BlockPos oldPos, BlockPos newPos) {
      SubLevel subLevel = Sable.HELPER.getContaining(resultingLevel, newPos);
      NameplateBlockEntity nameplate = (NameplateBlockEntity)this.getBlockEntity(resultingLevel, newPos);
      if (nameplate != null && nameplate.getName() != null && subLevel != null && subLevel.getName() == null) {
         subLevel.setName(nameplate.getName());
      }
   }

   private static class PlacementHelper implements IPlacementHelper {
      public Predicate<ItemStack> getItemPredicate() {
         return stack -> {
            for (BlockEntry<NameplateBlock> nameplate : SimBlocks.NAMEPLATES) {
               if (nameplate.is(stack.getItem())) {
                  return true;
               }
            }

            return false;
         };
      }

      public Predicate<BlockState> getStatePredicate() {
         return state -> {
            for (BlockEntry<NameplateBlock> nameplate : SimBlocks.NAMEPLATES) {
               if (nameplate.has(state)) {
                  return true;
               }
            }

            return false;
         };
      }

      public PlacementOffset getOffset(Player player, Level world, BlockState state, BlockPos pos, BlockHitResult ray, ItemStack heldItem) {
         if (heldItem.getItem() instanceof BlockItem bi && state.is(bi.getBlock())) {
            return super.getOffset(player, world, state, pos, ray, heldItem);
         }

         return PlacementOffset.fail();
      }

      public PlacementOffset getOffset(Player player, Level level, BlockState blockState, BlockPos blockPos, BlockHitResult blockHitResult) {
         List<Direction> directions = IPlacementHelper.orderedByDistance(blockPos, blockHitResult.getLocation(), dir -> {
            if (dir.getAxis() != ((Direction)blockState.getValue(HorizontalDirectionalBlock.FACING)).getClockWise().getAxis()) {
               return false;
            } else {
               BlockPos relPos = blockPos.relative(dir);
               return level.getBlockState(relPos).canBeReplaced() && blockState.canSurvive(level, relPos);
            }
         });
         return directions.isEmpty()
            ? PlacementOffset.fail()
            : PlacementOffset.success(
               blockPos.relative(directions.getFirst()),
               s -> (BlockState)s.setValue(HorizontalDirectionalBlock.FACING, (Direction)blockState.getValue(HorizontalDirectionalBlock.FACING))
            );
      }
   }

   public static enum Position implements StringRepresentable {
      SINGLE,
      LEFT,
      RIGHT,
      MIDDLE;

      @NotNull
      public String getSerializedName() {
         return this.name().toLowerCase(Locale.ROOT);
      }
   }
}

package dev.simulated_team.simulated.content.blocks.swivel_bearing;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import com.simibubi.create.foundation.block.IBE;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraKinetics;
import dev.simulated_team.simulated.util.placement_helpers.CogwheelPlacementExtension;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SwivelBearingBlock
   extends DirectionalKineticBlock
   implements IBE<SwivelBearingBlockEntity>,
   IRotate,
   ExtraKinetics.ExtraKineticsBlock,
   BlockSubLevelAssemblyListener {
   public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   private static final int placementHelperId = PlacementHelpers.register(
      new CogwheelPlacementExtension(i -> i.getItem() instanceof CogwheelBlockItem, SimBlocks.SWIVEL_BEARING::has)
   );

   public SwivelBearingBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(ASSEMBLED, false)).setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{ASSEMBLED}).add(new Property[]{POWERED}));
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.FAIL;
      } else if (player.isShiftKeyDown()) {
         return ItemInteractionResult.FAIL;
      } else if (player.getItemInHand(interactionHand).isEmpty()) {
         if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
         } else {
            this.withBlockEntityDo(level, blockPos, be -> be.assembleNextTick = true);
            return ItemInteractionResult.SUCCESS;
         }
      } else {
         ItemStack heldItem = player.getItemInHand(interactionHand);
         IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
         return helper.matchesItem(heldItem)
            ? helper.getOffset(player, level, blockState, blockPos, blockHitResult)
               .placeInWorld(level, (BlockItem)heldItem.getItem(), player, interactionHand, blockHitResult)
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public Axis getRotationAxis(BlockState blockState) {
      return ((Direction)blockState.getValue(FACING)).getAxis();
   }

   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      Direction facing = (Direction)state.getValue(FACING);
      return state.getValue(ASSEMBLED) ? face == facing.getOpposite() : face.getAxis() == facing.getAxis();
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      BlockState rotated = this.getRotatedBlockState(state, context.getClickedFace());
      if (!rotated.canSurvive(level, context.getClickedPos())) {
         return InteractionResult.PASS;
      } else {
         if (!level.isClientSide) {
            this.withBlockEntityDo(level, pos, SwivelBearingBlockEntity::disassemble);
         }

         rotated = this.getRotatedBlockState(level.getBlockState(pos), context.getClickedFace());
         KineticBlockEntity.switchToBlockState(level, pos, this.updateAfterWrenched(rotated, context));
         if (level.getBlockState(pos) != state) {
            IWrenchable.playRotateSound(level, pos);
         }

         return InteractionResult.SUCCESS;
      }
   }

   public Class<SwivelBearingBlockEntity> getBlockEntityClass() {
      return SwivelBearingBlockEntity.class;
   }

   public BlockEntityType<? extends SwivelBearingBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SwivelBearingBlockEntity>)SimBlockEntityTypes.SWIVEL_BEARING.get();
   }

   @Override
   public IRotate getExtraKineticsRotationConfiguration() {
      return SwivelBearingBlockEntity.SwivelBearingCogwheelBlockEntity.EXTRA_COGWHEEL_CONFIG;
   }

   protected VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
      return blockState.getValue(ASSEMBLED) ? SimBlockShapes.SWIVEL_BEARING_ASSEMBLED.get((Direction)blockState.getValue(FACING)) : Shapes.block();
   }

   public void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      this.withBlockEntityDo(originLevel, oldPos, SwivelBearingBlockEntity::beforeAssembly);
   }

   public void afterMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      this.withBlockEntityDo(resultingLevel, newPos, SwivelBearingBlockEntity::associatePlateWithParent);
   }
}

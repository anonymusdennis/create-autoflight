package com.simibubi.create.content.contraptions.mounted;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.redstone.rail.ControllerRailBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CartAssemblerBlock extends BaseRailBlock implements IBE<CartAssemblerBlockEntity>, IWrenchable, SpecialBlockItemRequirement {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final BooleanProperty BACKWARDS = BooleanProperty.create("backwards");
   public static final Property<RailShape> RAIL_SHAPE = EnumProperty.create(
      "shape", RailShape.class, new RailShape[]{RailShape.EAST_WEST, RailShape.NORTH_SOUTH}
   );
   public static final Property<CartAssembleRailType> RAIL_TYPE = EnumProperty.create("rail_type", CartAssembleRailType.class);
   public static final MapCodec<CartAssemblerBlock> CODEC = simpleCodec(CartAssemblerBlock::new);

   public CartAssemblerBlock(Properties properties) {
      super(true, properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(POWERED, false)).setValue(BACKWARDS, false))
               .setValue(RAIL_TYPE, CartAssembleRailType.POWERED_RAIL))
            .setValue(WATERLOGGED, false)
      );
   }

   public static BlockState createAnchor(BlockState state) {
      Axis axis = state.getValue(RAIL_SHAPE) == RailShape.NORTH_SOUTH ? Axis.Z : Axis.X;
      return (BlockState)AllBlocks.MINECART_ANCHOR.getDefaultState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis);
   }

   private static Item getRailItem(BlockState state) {
      return ((CartAssembleRailType)state.getValue(RAIL_TYPE)).getItem();
   }

   public static BlockState getRailBlock(BlockState state) {
      BaseRailBlock railBlock = (BaseRailBlock)((CartAssembleRailType)state.getValue(RAIL_TYPE)).getBlock();
      BlockState railState = (BlockState)railBlock.defaultBlockState().setValue(railBlock.getShapeProperty(), (RailShape)state.getValue(RAIL_SHAPE));
      if (railState.hasProperty(ControllerRailBlock.BACKWARDS)) {
         railState = (BlockState)railState.setValue(ControllerRailBlock.BACKWARDS, (Boolean)state.getValue(BACKWARDS));
      }

      return railState;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{RAIL_SHAPE, POWERED, RAIL_TYPE, BACKWARDS, WATERLOGGED});
      super.createBlockStateDefinition(builder);
   }

   public boolean canMakeSlopes(@NotNull BlockState state, @NotNull BlockGetter world, @NotNull BlockPos pos) {
      return false;
   }

   public void onMinecartPass(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, AbstractMinecart cart) {
      if (canAssembleTo(cart)) {
         if (!world.isClientSide) {
            this.withBlockEntityDo(world, pos, be -> be.assembleNextTick(cart));
         }
      }
   }

   public static CartAssemblerBlock.CartAssemblerAction getActionForCart(BlockState state, AbstractMinecart cart) {
      CartAssembleRailType type = (CartAssembleRailType)state.getValue(RAIL_TYPE);
      boolean powered = (Boolean)state.getValue(POWERED);
      switch (type) {
         case ACTIVATOR_RAIL:
            return powered ? CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE : CartAssemblerBlock.CartAssemblerAction.PASS;
         case CONTROLLER_RAIL:
            return powered ? CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE_DIRECTIONAL : CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE_BRAKE;
         case DETECTOR_RAIL:
            return cart.getPassengers().isEmpty()
               ? CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE
               : CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE;
         case POWERED_RAIL:
            return powered ? CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE : CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE_BRAKE;
         case REGULAR:
            return powered ? CartAssemblerBlock.CartAssemblerAction.ASSEMBLE : CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE;
         default:
            return CartAssemblerBlock.CartAssemblerAction.PASS;
      }
   }

   public static boolean canAssembleTo(AbstractMinecart cart) {
      return cart.canBeRidden() || cart instanceof MinecartFurnace || cart instanceof MinecartChest;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      Item previousItem = getRailItem(state);
      Item heldItem = stack.getItem();
      if (heldItem != previousItem) {
         CartAssembleRailType newType = null;

         for (CartAssembleRailType type : CartAssembleRailType.values()) {
            if (heldItem == type.getItem()) {
               newType = type;
            }
         }

         if (newType == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(RAIL_TYPE, newType));
            if (!player.isCreative()) {
               stack.shrink(1);
               player.getInventory().placeItemBackInInventory(new ItemStack(previousItem));
            }

            return ItemInteractionResult.SUCCESS;
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public void neighborChanged(
      @NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving
   ) {
      if (!worldIn.isClientSide) {
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         if (previouslyPowered != worldIn.hasNeighborSignal(pos)) {
            worldIn.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
         }

         super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
      }
   }

   @NotNull
   public Property<RailShape> getShapeProperty() {
      return RAIL_SHAPE;
   }

   @NotNull
   public VoxelShape getShape(BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
      return AllShapes.CART_ASSEMBLER.get(this.getRailAxis(state));
   }

   protected Axis getRailAxis(BlockState state) {
      return state.getValue(RAIL_SHAPE) == RailShape.NORTH_SOUTH ? Axis.Z : Axis.X;
   }

   @NotNull
   public VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, CollisionContext context) {
      if (context instanceof EntityCollisionContext) {
         Entity entity = ((EntityCollisionContext)context).getEntity();
         if (entity instanceof AbstractMinecart) {
            return Shapes.empty();
         }

         if (entity instanceof Player) {
            return AllShapes.CART_ASSEMBLER_PLAYER_COLLISION.get(this.getRailAxis(state));
         }
      }

      return Shapes.block();
   }

   @NotNull
   public PushReaction getPistonPushReaction(@NotNull BlockState state) {
      return PushReaction.BLOCK;
   }

   @Override
   public Class<CartAssemblerBlockEntity> getBlockEntityClass() {
      return CartAssemblerBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends CartAssemblerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends CartAssemblerBlockEntity>)AllBlockEntityTypes.CART_ASSEMBLER.get();
   }

   public boolean canSurvive(@NotNull BlockState state, @NotNull LevelReader world, @NotNull BlockPos pos) {
      return false;
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
      ArrayList<ItemStack> requiredItems = new ArrayList<>();
      requiredItems.add(new ItemStack(getRailItem(state)));
      requiredItems.add(new ItemStack(this.asItem()));
      return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, requiredItems);
   }

   @NotNull
   public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      List<ItemStack> drops = super.getDrops(state, builder);
      drops.addAll(getRailBlock(state).getDrops(builder));
      return drops;
   }

   public List<ItemStack> getDropsNoRail(
      BlockState state, ServerLevel world, BlockPos pos, @Nullable BlockEntity p_220077_3_, @Nullable Entity p_220077_4_, ItemStack p_220077_5_
   ) {
      return super.getDrops(
         state,
         new net.minecraft.world.level.storage.loot.LootParams.Builder(world)
            .withParameter(LootContextParams.ORIGIN, Vec3.atLowerCornerOf(pos))
            .withParameter(LootContextParams.TOOL, p_220077_5_)
            .withOptionalParameter(LootContextParams.THIS_ENTITY, p_220077_4_)
            .withOptionalParameter(LootContextParams.BLOCK_ENTITY, p_220077_3_)
      );
   }

   @Override
   public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      Player player = context.getPlayer();
      if (world.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         if (player != null && !player.isCreative()) {
            this.getDropsNoRail(state, (ServerLevel)world, pos, world.getBlockEntity(pos), player, context.getItemInHand())
               .forEach(itemStack -> player.getInventory().placeItemBackInInventory(itemStack));
         }

         if (world instanceof ServerLevel) {
            state.spawnAfterBreak((ServerLevel)world, pos, ItemStack.EMPTY, true);
         }

         world.setBlockAndUpdate(pos, getRailBlock(state));
         return InteractionResult.SUCCESS;
      }
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level world = context.getLevel();
      if (world.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         BlockPos pos = context.getClickedPos();
         world.setBlock(pos, this.rotate(state, Rotation.CLOCKWISE_90), 3);
         world.updateNeighborsAt(pos.below(), this);
         return InteractionResult.SUCCESS;
      }
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      if (rotation == Rotation.NONE) {
         return state;
      } else {
         BlockState base = ((BlockState)((BlockState)AllBlocks.CONTROLLER_RAIL
                  .getDefaultState()
                  .setValue(ControllerRailBlock.SHAPE, (RailShape)state.getValue(RAIL_SHAPE)))
               .setValue(ControllerRailBlock.BACKWARDS, (Boolean)state.getValue(BACKWARDS)))
            .rotate(rotation);
         return (BlockState)((BlockState)state.setValue(RAIL_SHAPE, (RailShape)base.getValue(ControllerRailBlock.SHAPE)))
            .setValue(BACKWARDS, (Boolean)base.getValue(ControllerRailBlock.BACKWARDS));
      }
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      if (mirror == Mirror.NONE) {
         return state;
      } else {
         BlockState base = ((BlockState)((BlockState)AllBlocks.CONTROLLER_RAIL
                  .getDefaultState()
                  .setValue(ControllerRailBlock.SHAPE, (RailShape)state.getValue(RAIL_SHAPE)))
               .setValue(ControllerRailBlock.BACKWARDS, (Boolean)state.getValue(BACKWARDS)))
            .mirror(mirror);
         return (BlockState)state.setValue(BACKWARDS, (Boolean)base.getValue(ControllerRailBlock.BACKWARDS));
      }
   }

   public static Direction getHorizontalDirection(BlockState blockState) {
      if (!(blockState.getBlock() instanceof CartAssemblerBlock)) {
         return Direction.SOUTH;
      } else {
         Direction pointingTo = getPointingTowards(blockState);
         return blockState.getValue(BACKWARDS) ? pointingTo.getOpposite() : pointingTo;
      }
   }

   private static Direction getPointingTowards(BlockState state) {
      switch ((RailShape)state.getValue(RAIL_SHAPE)) {
         case EAST_WEST:
            return Direction.WEST;
         default:
            return Direction.NORTH;
      }
   }

   @NotNull
   protected MapCodec<? extends BaseRailBlock> codec() {
      return CODEC;
   }

   public static enum CartAssemblerAction {
      ASSEMBLE,
      DISASSEMBLE,
      ASSEMBLE_ACCELERATE,
      DISASSEMBLE_BRAKE,
      ASSEMBLE_ACCELERATE_DIRECTIONAL,
      PASS;

      public boolean shouldAssemble() {
         return this == ASSEMBLE || this == ASSEMBLE_ACCELERATE || this == ASSEMBLE_ACCELERATE_DIRECTIONAL;
      }

      public boolean shouldDisassemble() {
         return this == DISASSEMBLE || this == DISASSEMBLE_BRAKE;
      }
   }

   public static class MinecartAnchorBlock extends Block {
      public MinecartAnchorBlock(Properties p_i48440_1_) {
         super(p_i48440_1_);
      }

      protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
         builder.add(new Property[]{BlockStateProperties.HORIZONTAL_AXIS});
         super.createBlockStateDefinition(builder);
      }

      @NotNull
      public VoxelShape getShape(
         @NotNull BlockState p_220053_1_, @NotNull BlockGetter p_220053_2_, @NotNull BlockPos p_220053_3_, @NotNull CollisionContext p_220053_4_
      ) {
         return Shapes.empty();
      }
   }
}

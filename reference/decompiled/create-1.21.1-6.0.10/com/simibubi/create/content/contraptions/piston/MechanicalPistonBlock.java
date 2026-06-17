package com.simibubi.create.content.contraptions.piston;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags.Items;

public class MechanicalPistonBlock extends DirectionalAxisKineticBlock implements IBE<MechanicalPistonBlockEntity> {
   public static final EnumProperty<MechanicalPistonBlock.PistonState> STATE = EnumProperty.create("state", MechanicalPistonBlock.PistonState.class);
   protected boolean isSticky;

   public static MechanicalPistonBlock normal(Properties properties) {
      return new MechanicalPistonBlock(properties, false);
   }

   public static MechanicalPistonBlock sticky(Properties properties) {
      return new MechanicalPistonBlock(properties, true);
   }

   protected MechanicalPistonBlock(Properties properties, boolean sticky) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)this.defaultBlockState().setValue(FACING, Direction.NORTH)).setValue(STATE, MechanicalPistonBlock.PistonState.RETRACTED)
      );
      this.isSticky = sticky;
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{STATE});
      super.createBlockStateDefinition(builder);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (!stack.is(Items.SLIMEBALLS)) {
         if (stack.isEmpty()) {
            this.withBlockEntityDo(level, pos, be -> be.assembleNextTick = true);
            return ItemInteractionResult.SUCCESS;
         } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         }
      } else if (state.getValue(STATE) != MechanicalPistonBlock.PistonState.RETRACTED) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         Direction direction = (Direction)state.getValue(FACING);
         if (hitResult.getDirection() != direction) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (((MechanicalPistonBlock)state.getBlock()).isSticky) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
         } else if (level.isClientSide) {
            Vec3 vec = hitResult.getLocation();
            level.addParticle(ParticleTypes.ITEM_SLIME, vec.x, vec.y, vec.z, 0.0, 0.0, 0.0);
            return ItemInteractionResult.SUCCESS;
         } else {
            AllSoundEvents.SLIME_ADDED.playOnServer(level, pos, 0.5F, 1.0F);
            if (!player.isCreative()) {
               stack.shrink(1);
            }

            level.setBlockAndUpdate(
               pos,
               (BlockState)((BlockState)AllBlocks.STICKY_MECHANICAL_PISTON.getDefaultState().setValue(FACING, direction))
                  .setValue(AXIS_ALONG_FIRST_COORDINATE, (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE))
            );
            return ItemInteractionResult.SUCCESS;
         }
      }
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
      Direction direction = (Direction)state.getValue(FACING);
      if (fromPos.equals(pos.relative(direction.getOpposite()))) {
         if (!level.isClientSide && !level.getBlockTicks().willTickThisTick(pos, this)) {
            level.scheduleTick(pos, this, 1);
         }
      }
   }

   public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource r) {
      Direction direction = (Direction)state.getValue(FACING);
      BlockState pole = worldIn.getBlockState(pos.relative(direction.getOpposite()));
      if (AllBlocks.PISTON_EXTENSION_POLE.has(pole)) {
         if (((Direction)pole.getValue(PistonExtensionPoleBlock.FACING)).getAxis() == direction.getAxis()) {
            this.withBlockEntityDo(worldIn, pos, be -> {
               if (be.lastException != null) {
                  be.lastException = null;
                  be.sendData();
               }
            });
         }
      }
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      return state.getValue(STATE) != MechanicalPistonBlock.PistonState.RETRACTED ? InteractionResult.PASS : super.onWrenched(state, context);
   }

   public BlockState playerWillDestroy(Level worldIn, BlockPos pos, BlockState state, Player player) {
      Direction direction = (Direction)state.getValue(FACING);
      BlockPos pistonHead = null;
      boolean dropBlocks = player == null || !player.isCreative();
      Integer maxPoles = maxAllowedPistonPoles();

      for (int offset = 1; offset < maxPoles; offset++) {
         BlockPos currentPos = pos.relative(direction, offset);
         BlockState block = worldIn.getBlockState(currentPos);
         if (!isExtensionPole(block) || direction.getAxis() != ((Direction)block.getValue(BlockStateProperties.FACING)).getAxis()) {
            if (isPistonHead(block) && block.getValue(BlockStateProperties.FACING) == direction) {
               pistonHead = currentPos;
            }
            break;
         }
      }

      if (pistonHead != null && pos != null) {
         BlockPos.betweenClosedStream(pos, pistonHead).filter(p -> !p.equals(pos)).forEach(p -> worldIn.destroyBlock(p, dropBlocks));
      }

      for (int offsetx = 1; offsetx < maxPoles; offsetx++) {
         BlockPos currentPos = pos.relative(direction.getOpposite(), offsetx);
         BlockState block = worldIn.getBlockState(currentPos);
         if (!isExtensionPole(block) || direction.getAxis() != ((Direction)block.getValue(BlockStateProperties.FACING)).getAxis()) {
            break;
         }

         worldIn.destroyBlock(currentPos, dropBlocks);
      }

      return super.playerWillDestroy(worldIn, pos, state, player);
   }

   public static int maxAllowedPistonPoles() {
      return (Integer)AllConfigs.server().kinetics.maxPistonPoles.get();
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      if (state.getValue(STATE) == MechanicalPistonBlock.PistonState.EXTENDED) {
         return AllShapes.MECHANICAL_PISTON_EXTENDED.get((Direction)state.getValue(FACING));
      } else {
         return state.getValue(STATE) == MechanicalPistonBlock.PistonState.MOVING
            ? AllShapes.MECHANICAL_PISTON.get((Direction)state.getValue(FACING))
            : Shapes.block();
      }
   }

   @Override
   public Class<MechanicalPistonBlockEntity> getBlockEntityClass() {
      return MechanicalPistonBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends MechanicalPistonBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends MechanicalPistonBlockEntity>)AllBlockEntityTypes.MECHANICAL_PISTON.get();
   }

   public static boolean isPiston(BlockState state) {
      return AllBlocks.MECHANICAL_PISTON.has(state) || isStickyPiston(state);
   }

   public static boolean isStickyPiston(BlockState state) {
      return AllBlocks.STICKY_MECHANICAL_PISTON.has(state);
   }

   public static boolean isExtensionPole(BlockState state) {
      return AllBlocks.PISTON_EXTENSION_POLE.has(state);
   }

   public static boolean isPistonHead(BlockState state) {
      return AllBlocks.MECHANICAL_PISTON_HEAD.has(state);
   }

   public static enum PistonState implements StringRepresentable {
      RETRACTED,
      MOVING,
      EXTENDED;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}

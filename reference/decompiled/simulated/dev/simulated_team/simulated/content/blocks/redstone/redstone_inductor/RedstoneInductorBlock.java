package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.redstone.diodes.AbstractDiodeBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.multiloader.CommonRedstoneBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

public class RedstoneInductorBlock extends AbstractDiodeBlock implements IBE<RedstoneInductorBlockEntity>, CommonRedstoneBlock {
   public static final MapCodec<RedstoneInductorBlock> CODEC = simpleCodec(RedstoneInductorBlock::new);
   public static final BooleanProperty INVERTED = BooleanProperty.create("inverted");

   public RedstoneInductorBlock(Properties builder) {
      super(builder);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(INVERTED, false)).setValue(POWERED, false));
   }

   protected MapCodec<? extends DiodeBlock> codec() {
      return CODEC;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      return this.toggle(level, blockPos, blockState, player, interactionHand);
   }

   public ItemInteractionResult toggle(Level pLevel, BlockPos pPos, BlockState pState, Player player, InteractionHand pHand) {
      if (!player.mayBuild()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (player.isShiftKeyDown()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (AllItems.WRENCH.isIn(player.getItemInHand(pHand))) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (pLevel.isClientSide) {
         addParticles(pState, pLevel, pPos, 1.0F);
         return ItemInteractionResult.SUCCESS;
      } else {
         pLevel.setBlock(pPos, (BlockState)pState.cycle(INVERTED), 3);
         return this.onBlockEntityUseItemOn(pLevel, pPos, be -> {
            int backSignal = this.getBackSignal(pLevel, pPos, pState);
            be.updateSignal();
            float f = !pState.getValue(INVERTED) ? 0.6F : 0.5F;
            pLevel.playSound(null, pPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, f);
            return ItemInteractionResult.SUCCESS;
         });
      }
   }

   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
      this.withBlockEntityDo(pLevel, pPos, be -> {
         if (((Boolean)pState.getValue(POWERED) || be.outputSignal > 0) && pRandom.nextFloat() < 0.25F) {
            addParticles(pState, pLevel, pPos, 1.0F);
         }
      });
   }

   private static void addParticles(BlockState state, LevelAccessor level, BlockPos pos, float alpha) {
      level.addParticle(
         new DustParticleOptions(new Vector3f(1.0F, 0.0F, 0.0F), alpha),
         (double)((float)pos.getX() + 0.5F),
         (double)((float)pos.getY() + 0.5F),
         (double)((float)pos.getZ() + 0.5F),
         0.0,
         0.0,
         0.0
      );
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED, INVERTED, FACING});
   }

   @Override
   public boolean commonConnectRedstone(BlockState state, BlockGetter world, BlockPos pos, Direction side) {
      return side == null ? false : side.getAxis() == ((Direction)state.getValue(FACING)).getAxis();
   }

   protected int getDelay(BlockState pState) {
      return 0;
   }

   public int getBackSignal(Level level, BlockPos pos, BlockState state) {
      Direction direction = (Direction)state.getValue(FACING);
      BlockPos blockpos = pos.relative(direction);
      return Math.max(level.getSignal(blockpos, direction), state.is(Blocks.REDSTONE_WIRE) ? (Integer)state.getValue(RedStoneWireBlock.POWER) : 0);
   }

   protected int getOutputSignal(BlockGetter level, BlockPos pos, BlockState state) {
      RedstoneInductorBlockEntity be = (RedstoneInductorBlockEntity)level.getBlockEntity(pos);

      assert be != null;

      boolean inverted = (Boolean)state.getValue(INVERTED);
      return inverted ? 15 - be.outputSignal : be.outputSignal;
   }

   public int getSignal(BlockState state, BlockGetter blockGetter, BlockPos pos, Direction side) {
      return state.getValue(FACING) == side ? this.getOutputSignal(blockGetter, pos, state) : 0;
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SimBlockShapes.REDSTONE_INDUCTOR.get((Direction)pState.getValue(FACING));
   }

   public Class<RedstoneInductorBlockEntity> getBlockEntityClass() {
      return RedstoneInductorBlockEntity.class;
   }

   public BlockEntityType<? extends RedstoneInductorBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends RedstoneInductorBlockEntity>)SimBlockEntityTypes.REDSTONE_INDUCTOR.get();
   }
}

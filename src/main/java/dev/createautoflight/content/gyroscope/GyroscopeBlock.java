package dev.createautoflight.content.gyroscope;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.createautoflight.client.ClientGyroscopeHandler;
import dev.createautoflight.registry.ModBlockEntities;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class GyroscopeBlock extends RotatedPillarBlock implements IBE<GyroscopeBlockEntity>, IWrenchable {
    public static final MapCodec<GyroscopeBlock> CODEC = simpleCodec(GyroscopeBlock::new);
    private static final VoxelShape SHAPE = Shapes.block();
    private static final Map<BlockPos, Direction> PLACEMENT_DOWN = new HashMap<>();

    public GyroscopeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends RotatedPillarBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && !context.getLevel().isClientSide) {
            PLACEMENT_DOWN.put(
                    context.getClickedPos().relative(context.getClickedFace()),
                    context.getClickedFace().getOpposite()
            );
        }
        return state;
    }

    static Direction consumePlacementDown(BlockPos pos) {
        return PLACEMENT_DOWN.remove(pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        PLACEMENT_DOWN.remove(pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (player != null && player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return onBlockEntityUseItemOn(level, pos, be -> {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> ClientGyroscopeHandler.open(be));
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    protected void neighborChanged(
            BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable BlockPos neighborPos, boolean movedByPiston
    ) {
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos, GyroscopeBlockEntity::onNeighborChanged);
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    @Override
    public Class<GyroscopeBlockEntity> getBlockEntityClass() {
        return GyroscopeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends GyroscopeBlockEntity> getBlockEntityType() {
        return ModBlockEntities.GYROSCOPE.get();
    }
}

package dev.createautoflight.content.navigation;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import dev.createautoflight.client.ClientNavigationHandler;
import dev.createautoflight.registry.ModBlockEntities;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class NavigationBlock extends Block implements IBE<NavigationBlockEntity> {
    public static final MapCodec<NavigationBlock> CODEC = simpleCodec(NavigationBlock::new);
    private static final VoxelShape SHAPE = Shapes.block();

    public NavigationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        withBlockEntityDo(level, pos, be -> be.onRemovedFromLevel(level.isClientSide));
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
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> ClientNavigationHandler.open(be));
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    public Class<NavigationBlockEntity> getBlockEntityClass() {
        return NavigationBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends NavigationBlockEntity> getBlockEntityType() {
        return ModBlockEntities.NAVIGATION.get();
    }
}

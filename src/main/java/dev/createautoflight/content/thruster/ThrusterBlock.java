package dev.createautoflight.content.thruster;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import dev.createautoflight.client.ClientThrusterHandler;
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

public class ThrusterBlock extends Block implements IBE<ThrusterBlockEntity> {
    public static final MapCodec<ThrusterBlock> CODEC = simpleCodec(ThrusterBlock::new);
    private static final VoxelShape SHAPE = Shapes.block();

    public ThrusterBlock(Properties properties) {
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
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> ClientThrusterHandler.open(be));
            return ItemInteractionResult.SUCCESS;
        });
    }

    @Override
    public Class<ThrusterBlockEntity> getBlockEntityClass() {
        return ThrusterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ThrusterBlockEntity> getBlockEntityType() {
        return ModBlockEntities.THRUSTER.get();
    }
}

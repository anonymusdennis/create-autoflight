package dev.createautoflight.content.thrust;

import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.createautoflight.client.ClientThrustGearboxHandler;
import dev.createautoflight.registry.ModBlockEntities;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ThrustVectoringGearboxBlock extends AbstractEncasedShaftBlock
        implements IBE<ThrustVectoringGearboxBlockEntity> {
    public ThrustVectoringGearboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<ThrustVectoringGearboxBlockEntity> getBlockEntityClass() {
        return ThrustVectoringGearboxBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ThrustVectoringGearboxBlockEntity> getBlockEntityType() {
        return ModBlockEntities.THRUST_VECTORING_GEARBOX.get();
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
            CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> ClientThrustGearboxHandler.open(be));
            return ItemInteractionResult.SUCCESS;
        });
    }
}

package com.simibubi.create.content.schematics.cannon;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class SchematicannonBlock extends Block implements IBE<SchematicannonBlockEntity> {
   public SchematicannonBlock(Properties properties) {
      super(properties);
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return AllShapes.SCHEMATICANNON_SHAPE;
   }

   public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
      if (entity != null) {
         this.withBlockEntityDo(
            level,
            pos,
            be -> be.defaultYaw = (float)(-Mth.floor((entity.getYRot() + (entity.isShiftKeyDown() ? 180.0F : 0.0F)) * 16.0F / 360.0F + 0.5F) & 15)
                  * 360.0F
                  / 16.0F
         );
      }
   }

   protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
      if (level.isClientSide) {
         return InteractionResult.SUCCESS;
      } else {
         this.withBlockEntityDo(level, pos, be -> player.openMenu(be, be::sendToMenu));
         return InteractionResult.SUCCESS;
      }
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      this.withBlockEntityDo(worldIn, pos, be -> be.neighbourCheckCooldown = 0);
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
         this.withBlockEntityDo(worldIn, pos, be -> ItemHelper.dropContents(worldIn, pos, be.inventory));
         worldIn.removeBlockEntity(pos);
      }
   }

   @Override
   public Class<SchematicannonBlockEntity> getBlockEntityClass() {
      return SchematicannonBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends SchematicannonBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SchematicannonBlockEntity>)AllBlockEntityTypes.SCHEMATICANNON.get();
   }
}

package com.simibubi.create.content.fluids.tank;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.createmod.catnip.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class FluidTankBlock extends Block implements IWrenchable, IBE<FluidTankBlockEntity> {
   public static final BooleanProperty TOP = BooleanProperty.create("top");
   public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
   public static final EnumProperty<FluidTankBlock.Shape> SHAPE = EnumProperty.create("shape", FluidTankBlock.Shape.class);
   private boolean creative;
   static final VoxelShape CAMPFIRE_SMOKE_CLIP = Block.box(0.0, 4.0, 0.0, 16.0, 16.0, 16.0);
   public static final SoundType SILENCED_METAL = new DeferredSoundType(
      0.1F,
      1.5F,
      () -> SoundEvents.METAL_BREAK,
      () -> SoundEvents.METAL_STEP,
      () -> SoundEvents.METAL_PLACE,
      () -> SoundEvents.METAL_HIT,
      () -> SoundEvents.METAL_FALL
   );

   public static FluidTankBlock regular(Properties p_i48440_1_) {
      return new FluidTankBlock(p_i48440_1_, false);
   }

   public static FluidTankBlock creative(Properties p_i48440_1_) {
      return new FluidTankBlock(p_i48440_1_, true);
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
   }

   protected FluidTankBlock(Properties p_i48440_1_, boolean creative) {
      super(p_i48440_1_);
      this.creative = creative;
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(TOP, true)).setValue(BOTTOM, true))
            .setValue(SHAPE, FluidTankBlock.Shape.WINDOW)
      );
   }

   public static boolean isTank(BlockState state) {
      return state.getBlock() instanceof FluidTankBlock;
   }

   public void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean moved) {
      if (oldState.getBlock() != state.getBlock()) {
         if (!moved) {
            this.withBlockEntityDo(world, pos, FluidTankBlockEntity::updateConnectivity);
            BlockState newState = world.getBlockState(pos);
            if (state != newState && newState.getBlock() == this) {
               world.markAndNotifyBlock(pos, world.getChunkAt(pos), oldState, newState, 11, 512);
            }
         }
      }
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> p_206840_1_) {
      p_206840_1_.add(new Property[]{TOP, BOTTOM, SHAPE});
   }

   public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
      FluidTankBlockEntity tankAt = ConnectivityHandler.partAt(this.getBlockEntityType(), world, pos);
      if (tankAt != null && tankAt.hasLevel()) {
         FluidTankBlockEntity controllerBE = tankAt.getControllerBE();
         return controllerBE != null && controllerBE.window ? tankAt.luminosity : 0;
      } else {
         return 0;
      }
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      this.withBlockEntityDo(context.getLevel(), context.getClickedPos(), FluidTankBlockEntity::toggleWindows);
      return InteractionResult.SUCCESS;
   }

   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return pContext == CollisionContext.empty() ? CAMPFIRE_SMOKE_CLIP : pState.getShape(pLevel, pPos);
   }

   public VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      return Shapes.block();
   }

   public BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      if (pDirection == Direction.DOWN && pNeighborState.getBlock() != this) {
         this.withBlockEntityDo(pLevel, pCurrentPos, FluidTankBlockEntity::updateBoilerTemperature);
      }

      return pState;
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      boolean onClient = level.isClientSide;
      if (stack.isEmpty()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (!player.isCreative() && !this.creative) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else {
         FluidHelper.FluidExchange exchange = null;
         FluidTankBlockEntity be = ConnectivityHandler.partAt(this.getBlockEntityType(), level, pos);
         if (be == null) {
            return ItemInteractionResult.FAIL;
         } else {
            IFluidHandler tankCapability = (IFluidHandler)level.getCapability(FluidHandler.BLOCK, be.getBlockPos(), null);
            if (tankCapability == null) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               FluidStack prevFluidInTank = tankCapability.getFluidInTank(0).copy();
               if (FluidHelper.tryEmptyItemIntoBE(level, player, hand, stack, be)) {
                  exchange = FluidHelper.FluidExchange.ITEM_TO_TANK;
               } else if (FluidHelper.tryFillItemFromBE(level, player, hand, stack, be)) {
                  exchange = FluidHelper.FluidExchange.TANK_TO_ITEM;
               }

               if (exchange == null) {
                  return !GenericItemEmptying.canItemBeEmptied(level, stack) && !GenericItemFilling.canItemBeFilled(level, stack)
                     ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                     : ItemInteractionResult.SUCCESS;
               } else {
                  SoundEvent soundevent = null;
                  BlockState fluidState = null;
                  FluidStack fluidInTank = tankCapability.getFluidInTank(0);
                  if (exchange == FluidHelper.FluidExchange.ITEM_TO_TANK) {
                     if (this.creative && !onClient) {
                        FluidStack fluidInItem = (FluidStack)GenericItemEmptying.emptyItem(level, stack, true).getFirst();
                        if (!fluidInItem.isEmpty() && tankCapability instanceof CreativeFluidTankBlockEntity.CreativeSmartFluidTank) {
                           ((CreativeFluidTankBlockEntity.CreativeSmartFluidTank)tankCapability).setContainedFluid(fluidInItem);
                        }
                     }

                     Fluid fluid = fluidInTank.getFluid();
                     fluidState = fluid.defaultFluidState().createLegacyBlock();
                     soundevent = FluidHelper.getEmptySound(fluidInTank);
                  }

                  if (exchange == FluidHelper.FluidExchange.TANK_TO_ITEM) {
                     if (this.creative && !onClient && tankCapability instanceof CreativeFluidTankBlockEntity.CreativeSmartFluidTank) {
                        ((CreativeFluidTankBlockEntity.CreativeSmartFluidTank)tankCapability).setContainedFluid(FluidStack.EMPTY);
                     }

                     Fluid fluid = prevFluidInTank.getFluid();
                     fluidState = fluid.defaultFluidState().createLegacyBlock();
                     soundevent = FluidHelper.getFillSound(prevFluidInTank);
                  }

                  if (soundevent != null && !onClient) {
                     float pitch = Mth.clamp(
                        1.0F - 1.0F * (float)fluidInTank.getAmount() / (float)(FluidTankBlockEntity.getCapacityMultiplier() * 16), 0.0F, 1.0F
                     );
                     pitch /= 1.5F;
                     pitch += 0.5F;
                     pitch += (level.random.nextFloat() - 0.5F) / 4.0F;
                     level.playSound(null, pos, soundevent, SoundSource.BLOCKS, 0.5F, pitch);
                  }

                  if (!FluidStack.isSameFluidSameComponents(fluidInTank, prevFluidInTank) && be instanceof FluidTankBlockEntity) {
                     FluidTankBlockEntity controllerBE = be.getControllerBE();
                     if (controllerBE != null) {
                        if (fluidState != null && onClient) {
                           BlockParticleOption blockParticleData = new BlockParticleOption(ParticleTypes.BLOCK, fluidState);
                           float fluidLevel = (float)fluidInTank.getAmount() / (float)tankCapability.getTankCapacity(0);
                           boolean reversed = fluidInTank.getFluid().getFluidType().isLighterThanAir();
                           if (reversed) {
                              fluidLevel = 1.0F - fluidLevel;
                           }

                           Vec3 vec = hitResult.getLocation();
                           vec = new Vec3(
                              vec.x, (double)((float)controllerBE.getBlockPos().getY() + fluidLevel * ((float)controllerBE.height - 0.5F) + 0.25F), vec.z
                           );
                           Vec3 motion = player.position().subtract(vec).scale(0.05F);
                           vec = vec.add(motion);
                           level.addParticle(blockParticleData, vec.x, vec.y, vec.z, motion.x, motion.y, motion.z);
                           return ItemInteractionResult.SUCCESS;
                        }

                        controllerBE.sendDataImmediately();
                        controllerBE.setChanged();
                     }
                  }

                  return ItemInteractionResult.SUCCESS;
               }
            }
         }
      }
   }

   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.hasBlockEntity() && (state.getBlock() != newState.getBlock() || !newState.hasBlockEntity())) {
         if (!(world.getBlockEntity(pos) instanceof FluidTankBlockEntity tankBE)) {
            return;
         }

         world.removeBlockEntity(pos);
         ConnectivityHandler.splitMulti(tankBE);
      }
   }

   @Override
   public Class<FluidTankBlockEntity> getBlockEntityClass() {
      return FluidTankBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
      return this.creative ? (BlockEntityType)AllBlockEntityTypes.CREATIVE_FLUID_TANK.get() : (BlockEntityType)AllBlockEntityTypes.FLUID_TANK.get();
   }

   public BlockState mirror(BlockState state, Mirror mirror) {
      if (mirror == Mirror.NONE) {
         return state;
      } else {
         boolean x = mirror == Mirror.FRONT_BACK;
         switch ((FluidTankBlock.Shape)state.getValue(SHAPE)) {
            case WINDOW_NW:
               return (BlockState)state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_NE : FluidTankBlock.Shape.WINDOW_SW);
            case WINDOW_SW:
               return (BlockState)state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_SE : FluidTankBlock.Shape.WINDOW_NW);
            case WINDOW_NE:
               return (BlockState)state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_NW : FluidTankBlock.Shape.WINDOW_SE);
            case WINDOW_SE:
               return (BlockState)state.setValue(SHAPE, x ? FluidTankBlock.Shape.WINDOW_SW : FluidTankBlock.Shape.WINDOW_NE);
            default:
               return state;
         }
      }
   }

   public BlockState rotate(BlockState state, Rotation rotation) {
      for (int i = 0; i < rotation.ordinal(); i++) {
         state = this.rotateOnce(state);
      }

      return state;
   }

   private BlockState rotateOnce(BlockState state) {
      switch ((FluidTankBlock.Shape)state.getValue(SHAPE)) {
         case WINDOW_NW:
            return (BlockState)state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_NE);
         case WINDOW_SW:
            return (BlockState)state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_NW);
         case WINDOW_NE:
            return (BlockState)state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_SE);
         case WINDOW_SE:
            return (BlockState)state.setValue(SHAPE, FluidTankBlock.Shape.WINDOW_SW);
         default:
            return state;
      }
   }

   public SoundType getSoundType(BlockState state, LevelReader world, BlockPos pos, Entity entity) {
      SoundType soundType = super.getSoundType(state, world, pos, entity);
      return entity != null && entity.getPersistentData().contains("SilenceTankSound") ? SILENCED_METAL : soundType;
   }

   public boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   public int getAnalogOutputSignal(BlockState blockState, Level worldIn, BlockPos pos) {
      return this.getBlockEntityOptional(worldIn, pos)
         .map(FluidTankBlockEntity::getControllerBE)
         .map(be -> ComparatorUtil.fractionToRedstoneLevel((double)be.getFillState()))
         .orElse(0);
   }

   public static void updateBoilerState(BlockState pState, Level pLevel, BlockPos tankPos) {
      BlockState tankState = pLevel.getBlockState(tankPos);
      if (tankState.getBlock() instanceof FluidTankBlock tank) {
         FluidTankBlockEntity tankBE = tank.getBlockEntity(pLevel, tankPos);
         if (tankBE != null) {
            FluidTankBlockEntity controllerBE = tankBE.getControllerBE();
            if (controllerBE != null) {
               controllerBE.updateBoilerState();
            }
         }
      }
   }

   public static enum Shape implements StringRepresentable {
      PLAIN,
      WINDOW,
      WINDOW_NW,
      WINDOW_SW,
      WINDOW_NE,
      WINDOW_SE;

      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}

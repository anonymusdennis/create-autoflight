package com.simibubi.create.content.kinetics.belt;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.contraption.transformable.TransformableBlock;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.equipment.armor.DivingBootsItem;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler;
import com.simibubi.create.content.kinetics.belt.transport.BeltTunnelInteractionHandler;
import com.simibubi.create.content.logistics.box.PackageEntity;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.block.render.MultiPosDestructionHandler;
import com.simibubi.create.foundation.block.render.ReducedDestroyEffects;
import com.simibubi.create.foundation.item.ItemHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.apache.commons.lang3.mutable.MutableBoolean;

public class BeltBlock extends HorizontalKineticBlock implements IBE<BeltBlockEntity>, SpecialBlockItemRequirement, TransformableBlock, ProperWaterloggedBlock {
   public static final Property<BeltSlope> SLOPE = EnumProperty.create("slope", BeltSlope.class);
   public static final Property<BeltPart> PART = EnumProperty.create("part", BeltPart.class);
   public static final BooleanProperty CASING = BooleanProperty.create("casing");

   public BeltBlock(Properties properties) {
      super(properties);
      this.registerDefaultState(
         (BlockState)((BlockState)((BlockState)((BlockState)this.defaultBlockState().setValue(SLOPE, BeltSlope.HORIZONTAL)).setValue(PART, BeltPart.START))
               .setValue(CASING, false))
            .setValue(WATERLOGGED, false)
      );
   }

   @Override
   protected boolean areStatesKineticallyEquivalent(BlockState oldState, BlockState newState) {
      return super.areStatesKineticallyEquivalent(oldState, newState) && oldState.getValue(PART) == newState.getValue(PART);
   }

   @Override
   public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
      return face.getAxis() != this.getRotationAxis(state) ? false : this.getBlockEntityOptional(world, pos).map(BeltBlockEntity::hasPulley).orElse(false);
   }

   @Override
   public Axis getRotationAxis(BlockState state) {
      return state.getValue(SLOPE) == BeltSlope.SIDEWAYS ? Axis.Y : ((Direction)state.getValue(HORIZONTAL_FACING)).getClockWise().getAxis();
   }

   public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos, Player player) {
      return AllItems.BELT_CONNECTOR.asStack();
   }

   public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
      List<ItemStack> drops = super.getDrops(state, builder);
      BlockEntity blockEntity = (BlockEntity)builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
      if (blockEntity instanceof BeltBlockEntity && ((BeltBlockEntity)blockEntity).hasPulley()) {
         drops.addAll(AllBlocks.SHAFT.getDefaultState().getDrops(builder));
      }

      return drops;
   }

   public void spawnAfterBreak(BlockState state, ServerLevel worldIn, BlockPos pos, ItemStack p_220062_4_, boolean b) {
      BeltBlockEntity controllerBE = BeltHelper.getControllerBE(worldIn, pos);
      if (controllerBE != null) {
         controllerBE.getInventory().ejectAll();
      }
   }

   public boolean isFlammable(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
      return false;
   }

   public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
      super.updateEntityAfterFallOn(worldIn, entityIn);
      BlockPos entityPosition = entityIn.blockPosition();
      BlockPos beltPos = null;
      if (AllBlocks.BELT.has(worldIn.getBlockState(entityPosition))) {
         beltPos = entityPosition;
      } else if (AllBlocks.BELT.has(worldIn.getBlockState(entityPosition.below()))) {
         beltPos = entityPosition.below();
      }

      if (beltPos != null) {
         if (worldIn instanceof Level) {
            this.entityInside(worldIn.getBlockState(beltPos), (Level)worldIn, beltPos, entityIn);
         }
      }
   }

   public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn) {
      if (canTransportObjects(state)) {
         if (entityIn instanceof Player player) {
            if (player.isShiftKeyDown() && !AllItems.CARDBOARD_BOOTS.isIn(player.getItemBySlot(EquipmentSlot.FEET))) {
               return;
            }

            if (player.getAbilities().flying) {
               return;
            }
         }

         if (!DivingBootsItem.isWornBy(entityIn)) {
            BeltBlockEntity belt = BeltHelper.getSegmentBE(worldIn, pos);
            if (belt != null) {
               ItemStack asItem = ItemHelper.fromItemEntity(entityIn);
               if (!asItem.isEmpty()) {
                  if (!worldIn.isClientSide) {
                     if (!(entityIn.getDeltaMovement().y > 0.0)) {
                        Vec3 targetLocation = VecHelper.getCenterOf(pos).add(0.0, 0.3125, 0.0);
                        if (PackageEntity.centerPackage(entityIn, targetLocation)) {
                           if (BeltTunnelInteractionHandler.getTunnelOnPosition(worldIn, pos) == null) {
                              this.withBlockEntityDo(worldIn, pos, be -> {
                                 IItemHandler handler = (IItemHandler)worldIn.getCapability(ItemHandler.BLOCK, pos, state, be, null);
                                 if (handler != null) {
                                    ItemStack remainder = handler.insertItem(0, asItem, false);
                                    if (remainder.isEmpty()) {
                                       entityIn.discard();
                                    } else if (entityIn instanceof ItemEntity itemEntity && remainder.getCount() != itemEntity.getItem().getCount()) {
                                       itemEntity.setItem(remainder);
                                    }
                                 }
                              });
                           }
                        }
                     }
                  }
               } else {
                  BeltBlockEntity controller = BeltHelper.getControllerBE(worldIn, pos);
                  if (controller != null && controller.passengers != null) {
                     if (controller.passengers.containsKey(entityIn)) {
                        BeltMovementHandler.TransportedEntityInfo info = controller.passengers.get(entityIn);
                        if (info.getTicksSinceLastCollision() != 0 || pos.equals(entityIn.blockPosition())) {
                           info.refresh(pos, state);
                        }
                     } else {
                        controller.passengers.put(entityIn, new BeltMovementHandler.TransportedEntityInfo(pos, state));
                        entityIn.setOnGround(true);
                     }
                  }
               }
            }
         }
      }
   }

   public static boolean canTransportObjects(BlockState state) {
      if (!AllBlocks.BELT.has(state)) {
         return false;
      } else {
         BeltSlope slope = (BeltSlope)state.getValue(SLOPE);
         return slope != BeltSlope.VERTICAL && slope != BeltSlope.SIDEWAYS;
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (!player.isShiftKeyDown() && player.mayBuild()) {
         boolean isWrench = AllItems.WRENCH.isIn(stack);
         boolean isConnector = AllItems.BELT_CONNECTOR.isIn(stack);
         boolean isShaft = AllBlocks.SHAFT.isIn(stack);
         boolean isDye = stack.is(Items.DYES);
         boolean hasWater = ((FluidStack)GenericItemEmptying.emptyItem(level, stack, true).getFirst()).getFluid().isSame(Fluids.WATER);
         boolean isHand = stack.isEmpty() && hand == InteractionHand.MAIN_HAND;
         if (isDye || hasWater) {
            return this.onBlockEntityUseItemOn(
               level,
               pos,
               be -> be.applyColor(DyeColor.getColor(stack)) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
            );
         } else if (isConnector) {
            return BeltSlicer.useConnector(state, level, pos, player, hand, hitResult, new BeltSlicer.Feedback());
         } else if (isWrench) {
            return BeltSlicer.useWrench(state, level, pos, player, hand, hitResult, new BeltSlicer.Feedback());
         } else {
            BeltBlockEntity belt = BeltHelper.getSegmentBE(level, pos);
            if (belt == null) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               if (PackageItem.isPackage(stack)) {
                  ItemStack toInsert = stack.copy();
                  IItemHandler handler = (IItemHandler)level.getCapability(ItemHandler.BLOCK, belt.getBlockPos(), null);
                  if (handler == null) {
                     return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                  }

                  ItemStack remainder = handler.insertItem(0, toInsert, false);
                  if (remainder.isEmpty()) {
                     stack.shrink(1);
                     return ItemInteractionResult.SUCCESS;
                  }
               }

               if (isHand) {
                  BeltBlockEntity controllerBelt = belt.getControllerBE();
                  if (controllerBelt == null) {
                     return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                  }

                  if (level.isClientSide) {
                     return ItemInteractionResult.SUCCESS;
                  }

                  MutableBoolean success = new MutableBoolean(false);
                  controllerBelt.getInventory().applyToEachWithin((float)belt.index + 0.5F, 0.55F, transportedItemStack -> {
                     player.getInventory().placeItemBackInInventory(transportedItemStack.stack);
                     success.setTrue();
                     return TransportedItemStackHandlerBehaviour.TransportedResult.removeItem();
                  });
                  if (success.isTrue()) {
                     level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, 1.0F + level.random.nextFloat());
                  }
               }

               if (isShaft) {
                  if (state.getValue(PART) != BeltPart.MIDDLE) {
                     return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                  } else if (level.isClientSide) {
                     return ItemInteractionResult.SUCCESS;
                  } else {
                     if (!player.isCreative()) {
                        stack.shrink(1);
                     }

                     KineticBlockEntity.switchToBlockState(level, pos, (BlockState)state.setValue(PART, BeltPart.PULLEY));
                     return ItemInteractionResult.SUCCESS;
                  }
               } else if (AllBlocks.BRASS_CASING.isIn(stack)) {
                  this.withBlockEntityDo(level, pos, be -> be.setCasingType(BeltBlockEntity.CasingType.BRASS));
                  this.updateCoverProperty(level, pos, level.getBlockState(pos));
                  SoundType soundType = AllBlocks.BRASS_CASING.getDefaultState().getSoundType(level, pos, player);
                  level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
                  return ItemInteractionResult.SUCCESS;
               } else if (AllBlocks.ANDESITE_CASING.isIn(stack)) {
                  this.withBlockEntityDo(level, pos, be -> be.setCasingType(BeltBlockEntity.CasingType.ANDESITE));
                  this.updateCoverProperty(level, pos, level.getBlockState(pos));
                  SoundType soundType = AllBlocks.ANDESITE_CASING.getDefaultState().getSoundType(level, pos, player);
                  level.playSound(null, pos, soundType.getPlaceSound(), SoundSource.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
                  return ItemInteractionResult.SUCCESS;
               } else {
                  return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
               }
            }
         }
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   @Override
   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level world = context.getLevel();
      Player player = context.getPlayer();
      BlockPos pos = context.getClickedPos();
      if ((Boolean)state.getValue(CASING)) {
         if (world.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            this.withBlockEntityDo(world, pos, be -> be.setCasingType(BeltBlockEntity.CasingType.NONE));
            return InteractionResult.SUCCESS;
         }
      } else if (state.getValue(PART) == BeltPart.PULLEY) {
         if (world.isClientSide) {
            return InteractionResult.SUCCESS;
         } else {
            KineticBlockEntity.switchToBlockState(world, pos, (BlockState)state.setValue(PART, BeltPart.MIDDLE));
            if (player != null && !player.isCreative()) {
               player.getInventory().placeItemBackInInventory(AllBlocks.SHAFT.asStack());
            }

            return InteractionResult.SUCCESS;
         }
      } else {
         return InteractionResult.PASS;
      }
   }

   @Override
   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{SLOPE, PART, CASING, WATERLOGGED});
      super.createBlockStateDefinition(builder);
   }

   public PathType getBlockPathType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
      return PathType.RAIL;
   }

   public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      return BeltShapes.getShape(state);
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      if (state.getBlock() != this) {
         return Shapes.empty();
      } else {
         VoxelShape shape = this.getShape(state, worldIn, pos, context);
         return !(context instanceof EntityCollisionContext) ? shape : this.getBlockEntityOptional(worldIn, pos).map(be -> {
            Entity entity = ((EntityCollisionContext)context).getEntity();
            if (entity == null) {
               return shape;
            } else {
               BeltBlockEntity controller = be.getControllerBE();
               if (controller == null) {
                  return shape;
               } else {
                  return controller.passengers != null && controller.passengers.containsKey(entity) ? shape : BeltShapes.getCollisionShape(state);
               }
            }
         }).orElse(shape);
      }
   }

   public RenderShape getRenderShape(BlockState state) {
      return state.getValue(CASING) ? RenderShape.MODEL : RenderShape.ENTITYBLOCK_ANIMATED;
   }

   public static void initBelt(Level world, BlockPos pos) {
      if (!world.isClientSide) {
         if (!(world instanceof ServerLevel) || !(((ServerLevel)world).getChunkSource().getGenerator() instanceof DebugLevelSource)) {
            BlockState state = world.getBlockState(pos);
            if (AllBlocks.BELT.has(state)) {
               int limit = 1000;
               BlockPos currentPos = pos;

               while (limit-- > 0) {
                  BlockState currentState = world.getBlockState(currentPos);
                  if (!AllBlocks.BELT.has(currentState)) {
                     world.destroyBlock(pos, true);
                     return;
                  }

                  BlockPos nextSegmentPosition = nextSegmentPosition(currentState, currentPos, false);
                  if (nextSegmentPosition == null) {
                     break;
                  }

                  if (!world.isLoaded(nextSegmentPosition)) {
                     return;
                  }

                  currentPos = nextSegmentPosition;
               }

               int index = 0;
               List<BlockPos> beltChain = getBeltChain(world, currentPos);
               if (beltChain.size() < 2) {
                  world.destroyBlock(currentPos, true);
               } else {
                  for (BlockPos beltPos : beltChain) {
                     BlockEntity blockEntity = world.getBlockEntity(beltPos);
                     BlockState currentStatex = world.getBlockState(beltPos);
                     if (!(blockEntity instanceof BeltBlockEntity be) || !AllBlocks.BELT.has(currentStatex)) {
                        world.destroyBlock(currentPos, true);
                        return;
                     }

                     be.setController(currentPos);
                     be.beltLength = beltChain.size();
                     be.index = index;
                     be.attachKinetics();
                     be.setChanged();
                     be.sendData();
                     if (be.isController() && !canTransportObjects(currentStatex)) {
                        be.getInventory().ejectAll();
                     }

                     index++;
                  }
               }
            }
         }
      }
   }

   @Override
   public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
      super.onRemove(state, world, pos, newState, isMoving);
      if (!world.isClientSide) {
         if (state.getBlock() != newState.getBlock()) {
            if (!isMoving) {
               for (boolean forward : Iterate.trueAndFalse) {
                  BlockPos currentPos = nextSegmentPosition(state, pos, forward);
                  if (currentPos != null) {
                     BlockState currentState = world.getBlockState(currentPos);
                     if (AllBlocks.BELT.has(currentState)) {
                        boolean hasPulley = false;
                        BlockEntity blockEntity = world.getBlockEntity(currentPos);
                        if (blockEntity instanceof BeltBlockEntity) {
                           BeltBlockEntity belt = (BeltBlockEntity)blockEntity;
                           if (belt.isController()) {
                              belt.getInventory().ejectAll();
                           }

                           hasPulley = belt.hasPulley();
                        }

                        world.removeBlockEntity(currentPos);
                        BlockState shaftState = (BlockState)AllBlocks.SHAFT
                           .getDefaultState()
                           .setValue(BlockStateProperties.AXIS, this.getRotationAxis(currentState));
                        world.setBlock(
                           currentPos, ProperWaterloggedBlock.withWater(world, hasPulley ? shaftState : Blocks.AIR.defaultBlockState(), currentPos), 3
                        );
                        world.levelEvent(2001, currentPos, Block.getId(currentState));
                     }
                  }
               }
            }
         }
      }
   }

   public BlockState updateShape(BlockState state, Direction side, BlockState p_196271_3_, LevelAccessor world, BlockPos pos, BlockPos p_196271_6_) {
      this.updateWater(world, state, pos);
      if (side.getAxis().isHorizontal()) {
         this.updateTunnelConnections(world, pos.above());
      }

      if (side == Direction.UP) {
         this.updateCoverProperty(world, pos, state);
      }

      return state;
   }

   public void updateCoverProperty(LevelAccessor world, BlockPos pos, BlockState state) {
      if (!world.isClientSide()) {
         if ((Boolean)state.getValue(CASING) && state.getValue(SLOPE) == BeltSlope.HORIZONTAL) {
            this.withBlockEntityDo(world, pos, bbe -> bbe.setCovered(isBlockCoveringBelt(world, pos.above())));
         }
      }
   }

   public static boolean isBlockCoveringBelt(LevelAccessor world, BlockPos pos) {
      BlockState blockState = world.getBlockState(pos);
      VoxelShape collisionShape = blockState.getCollisionShape(world, pos);
      if (collisionShape.isEmpty()) {
         return false;
      } else {
         AABB bounds = collisionShape.bounds();
         if (bounds.getXsize() < 0.5 || bounds.getZsize() < 0.5) {
            return false;
         } else if (bounds.minY > 0.0) {
            return false;
         } else if (AllBlocks.CRUSHING_WHEEL_CONTROLLER.has(blockState)) {
            return false;
         } else {
            return FunnelBlock.isFunnel(blockState) && FunnelBlock.getFunnelFacing(blockState) != Direction.UP
               ? false
               : !(blockState.getBlock() instanceof BeltTunnelBlock);
         }
      }
   }

   private void updateTunnelConnections(LevelAccessor world, BlockPos pos) {
      Block tunnelBlock = world.getBlockState(pos).getBlock();
      if (tunnelBlock instanceof BeltTunnelBlock) {
         ((BeltTunnelBlock)tunnelBlock).updateTunnel(world, pos);
      }
   }

   public static List<BlockPos> getBeltChain(LevelAccessor world, BlockPos controllerPos) {
      List<BlockPos> positions = new LinkedList<>();
      BlockState blockState = world.getBlockState(controllerPos);
      if (!AllBlocks.BELT.has(blockState)) {
         return positions;
      } else {
         int limit = 1000;
         BlockPos current = controllerPos;

         while (limit-- > 0 && current != null) {
            BlockState state = world.getBlockState(current);
            if (!AllBlocks.BELT.has(state)) {
               break;
            }

            positions.add(current);
            current = nextSegmentPosition(state, current, true);
         }

         return positions;
      }
   }

   public static BlockPos nextSegmentPosition(BlockState state, BlockPos pos, boolean forward) {
      Direction direction = (Direction)state.getValue(HORIZONTAL_FACING);
      BeltSlope slope = (BeltSlope)state.getValue(SLOPE);
      BeltPart part = (BeltPart)state.getValue(PART);
      int offset = forward ? 1 : -1;
      if ((part != BeltPart.END || !forward) && (part != BeltPart.START || forward)) {
         if (slope == BeltSlope.VERTICAL) {
            return pos.above(direction.getAxisDirection() == AxisDirection.POSITIVE ? offset : -offset);
         } else {
            pos = pos.relative(direction, offset);
            return slope != BeltSlope.HORIZONTAL && slope != BeltSlope.SIDEWAYS ? pos.above(slope == BeltSlope.UPWARD ? offset : -offset) : pos;
         }
      } else {
         return null;
      }
   }

   @Override
   public Class<BeltBlockEntity> getBlockEntityClass() {
      return BeltBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends BeltBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends BeltBlockEntity>)AllBlockEntityTypes.BELT.get();
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
      List<ItemStack> required = new ArrayList<>();
      if (state.getValue(PART) != BeltPart.MIDDLE) {
         required.add(AllBlocks.SHAFT.asStack());
      }

      if (state.getValue(PART) == BeltPart.START) {
         required.add(AllItems.BELT_CONNECTOR.asStack());
      }

      return required.isEmpty() ? ItemRequirement.NONE : new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, required);
   }

   @Override
   public BlockState rotate(BlockState state, Rotation rot) {
      BlockState rotate = super.rotate(state, rot);
      if (state.getValue(SLOPE) != BeltSlope.VERTICAL) {
         return rotate;
      } else {
         if (((Direction)state.getValue(HORIZONTAL_FACING)).getAxisDirection() != ((Direction)rotate.getValue(HORIZONTAL_FACING)).getAxisDirection()) {
            if (state.getValue(PART) == BeltPart.START) {
               return (BlockState)rotate.setValue(PART, BeltPart.END);
            }

            if (state.getValue(PART) == BeltPart.END) {
               return (BlockState)rotate.setValue(PART, BeltPart.START);
            }
         }

         return rotate;
      }
   }

   @Override
   public BlockState transform(BlockState state, StructureTransform transform) {
      if (transform.mirror != null) {
         state = this.mirror(state, transform.mirror);
      }

      return transform.rotationAxis == Axis.Y ? this.rotate(state, transform.rotation) : this.transformInner(state, transform);
   }

   protected BlockState transformInner(BlockState state, StructureTransform transform) {
      boolean halfTurn = transform.rotation == Rotation.CLOCKWISE_180;
      Direction initialDirection = (Direction)state.getValue(HORIZONTAL_FACING);
      boolean diagonal = state.getValue(SLOPE) == BeltSlope.DOWNWARD || state.getValue(SLOPE) == BeltSlope.UPWARD;
      if (!diagonal) {
         for (int i = 0; i < transform.rotation.ordinal(); i++) {
            Direction direction = (Direction)state.getValue(HORIZONTAL_FACING);
            BeltSlope slope = (BeltSlope)state.getValue(SLOPE);
            boolean vertical = slope == BeltSlope.VERTICAL;
            boolean horizontal = slope == BeltSlope.HORIZONTAL;
            boolean sideways = slope == BeltSlope.SIDEWAYS;
            Direction newDirection = direction.getOpposite();
            BeltSlope newSlope = BeltSlope.VERTICAL;
            if (vertical) {
               if (direction.getAxis() == transform.rotationAxis) {
                  newDirection = direction.getCounterClockWise();
                  newSlope = BeltSlope.SIDEWAYS;
               } else {
                  newSlope = BeltSlope.HORIZONTAL;
                  newDirection = direction;
                  if (direction.getAxis() == Axis.Z) {
                     newDirection = direction.getOpposite();
                  }
               }
            }

            if (sideways) {
               newDirection = direction;
               if (direction.getAxis() == transform.rotationAxis) {
                  newSlope = BeltSlope.HORIZONTAL;
               } else {
                  newDirection = direction.getCounterClockWise();
               }
            }

            if (horizontal) {
               newDirection = direction;
               if (direction.getAxis() == transform.rotationAxis) {
                  newSlope = BeltSlope.SIDEWAYS;
               } else if (direction.getAxis() != Axis.Z) {
                  newDirection = direction.getOpposite();
               }
            }

            state = (BlockState)state.setValue(HORIZONTAL_FACING, newDirection);
            state = (BlockState)state.setValue(SLOPE, newSlope);
         }
      } else if (initialDirection.getAxis() != transform.rotationAxis) {
         for (int i = 0; i < transform.rotation.ordinal(); i++) {
            Direction directionx = (Direction)state.getValue(HORIZONTAL_FACING);
            Direction newDirectionx = directionx.getOpposite();
            BeltSlope slopex = (BeltSlope)state.getValue(SLOPE);
            boolean upward = slopex == BeltSlope.UPWARD;
            boolean downward = slopex == BeltSlope.DOWNWARD;
            if (directionx.getAxisDirection() == AxisDirection.POSITIVE ^ downward ^ directionx.getAxis() == Axis.Z) {
               state = (BlockState)state.setValue(SLOPE, upward ? BeltSlope.DOWNWARD : BeltSlope.UPWARD);
            } else {
               state = (BlockState)state.setValue(HORIZONTAL_FACING, newDirectionx);
            }
         }
      } else if (halfTurn) {
         Direction directionx = (Direction)state.getValue(HORIZONTAL_FACING);
         Direction newDirectionx = directionx.getOpposite();
         BeltSlope slopex = (BeltSlope)state.getValue(SLOPE);
         boolean verticalx = slopex == BeltSlope.VERTICAL;
         if (diagonal) {
            state = (BlockState)state.setValue(
               SLOPE, slopex == BeltSlope.UPWARD ? BeltSlope.DOWNWARD : (slopex == BeltSlope.DOWNWARD ? BeltSlope.UPWARD : slopex)
            );
         } else if (verticalx) {
            state = (BlockState)state.setValue(HORIZONTAL_FACING, newDirectionx);
         }
      }

      return state;
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public static class RenderProperties extends ReducedDestroyEffects implements MultiPosDestructionHandler {
      @Override
      public Set<BlockPos> getExtraPositions(ClientLevel level, BlockPos pos, BlockState blockState, int progress) {
         return level.getBlockEntity(pos) instanceof BeltBlockEntity belt ? new HashSet<>(BeltBlock.getBeltChain(level, belt.getController())) : null;
      }
   }
}

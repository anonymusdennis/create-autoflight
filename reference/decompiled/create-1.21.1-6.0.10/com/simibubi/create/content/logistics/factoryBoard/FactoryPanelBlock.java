package com.simibubi.create.content.logistics.factoryBoard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent.BreakEvent;
import org.jetbrains.annotations.NotNull;

public class FactoryPanelBlock
   extends FaceAttachedHorizontalDirectionalBlock
   implements ProperWaterloggedBlock,
   IBE<FactoryPanelBlockEntity>,
   IWrenchable,
   SpecialBlockItemRequirement {
   public static final MapCodec<FactoryPanelBlock> CODEC = simpleCodec(FactoryPanelBlock::new);
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

   public FactoryPanelBlock(Properties p_53182_) {
      super(p_53182_);
      this.registerDefaultState((BlockState)((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false)).setValue(POWERED, false));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> pBuilder) {
      super.createBlockStateDefinition(pBuilder.add(new Property[]{FACE, FACING, WATERLOGGED, POWERED}));
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return canAttachLenient(pLevel, pPos, getConnectedDirection(pState).getOpposite());
   }

   public static boolean canAttachLenient(LevelReader pReader, BlockPos pPos, Direction pDirection) {
      BlockPos blockpos = pPos.relative(pDirection);
      return !pReader.getBlockState(blockpos).getCollisionShape(pReader, blockpos).isEmpty();
   }

   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      BlockState stateForPlacement = super.getStateForPlacement(pContext);
      if (stateForPlacement == null) {
         return null;
      } else {
         if (stateForPlacement.getValue(FACE) == AttachFace.FLOOR) {
            stateForPlacement = (BlockState)stateForPlacement.setValue(FACING, ((Direction)stateForPlacement.getValue(FACING)).getOpposite());
         }

         Level level = pContext.getLevel();
         BlockPos pos = pContext.getClickedPos();
         BlockState blockState = level.getBlockState(pos);
         FactoryPanelBlockEntity fpbe = this.getBlockEntity(level, pos);
         Vec3 location = pContext.getClickLocation();
         if (blockState.is(this) && location != null && fpbe != null) {
            if (!level.isClientSide()) {
               FactoryPanelBlock.PanelSlot targetedSlot = getTargetedSlot(pos, blockState, location);
               ItemStack panelItem = FactoryPanelBlockItem.fixCtrlCopiedStack(pContext.getItemInHand());
               UUID networkFromStack = LogisticallyLinkedBlockItem.networkFromStack(panelItem);
               Player pPlayer = pContext.getPlayer();
               if (fpbe.addPanel(targetedSlot, networkFromStack) && pPlayer != null) {
                  pPlayer.displayClientMessage(CreateLang.translateDirect("logistically_linked.connected"), true);
                  if (!pPlayer.isCreative()) {
                     panelItem.shrink(1);
                     if (panelItem.isEmpty()) {
                        pPlayer.setItemInHand(pContext.getHand(), ItemStack.EMPTY);
                     }
                  }
               }
            }

            stateForPlacement = blockState;
         }

         return this.withWater(stateForPlacement, pContext);
      }
   }

   @Override
   public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
      Level world = context.getLevel();
      BlockPos pos = context.getClickedPos();
      Player player = context.getPlayer();
      FactoryPanelBlock.PanelSlot slot = getTargetedSlot(pos, state, context.getClickLocation());
      return !(world instanceof ServerLevel) ? InteractionResult.SUCCESS : this.onBlockEntityUse(world, pos, be -> {
         FactoryPanelBehaviour behaviour = be.panels.get(slot);
         if (behaviour != null && behaviour.isActive()) {
            BreakEvent event = new BreakEvent(world, pos, world.getBlockState(pos), player);
            NeoForge.EVENT_BUS.post(event);
            if (event.isCanceled()) {
               return InteractionResult.SUCCESS;
            } else if (!be.removePanel(slot)) {
               return InteractionResult.SUCCESS;
            } else {
               if (!player.isCreative()) {
                  player.getInventory().placeItemBackInInventory(AllBlocks.FACTORY_GAUGE.asStack());
               }

               IWrenchable.playRemoveSound(world, pos);
               if (be.activePanels() == 0) {
                  world.destroyBlock(pos, false);
               }

               return InteractionResult.SUCCESS;
            }
         } else {
            return InteractionResult.SUCCESS;
         }
      });
   }

   public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
      super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
      if (pPlacer != null) {
         AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
         double range = pPlacer.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
         HitResult hitResult = pPlacer.pick(range, 1.0F, false);
         Vec3 location = hitResult.getLocation();
         if (location != null) {
            FactoryPanelBlock.PanelSlot initialSlot = getTargetedSlot(pPos, pState, location);
            this.withBlockEntityDo(pLevel, pPos, fpbe -> fpbe.addPanel(initialSlot, LogisticallyLinkedBlockItem.networkFromStack(pStack)));
         }
      }
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (player == null) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (level.isClientSide) {
         return ItemInteractionResult.SUCCESS;
      } else if (!AllBlocks.FACTORY_GAUGE.isIn(stack)) {
         return ItemInteractionResult.SUCCESS;
      } else {
         Vec3 location = hitResult.getLocation();
         if (location == null) {
            return ItemInteractionResult.SUCCESS;
         } else if (!FactoryPanelBlockItem.isTuned(stack)) {
            AllSoundEvents.DENY.playOnServer(level, pos);
            player.displayClientMessage(CreateLang.translate("factory_panel.tune_before_placing").component(), true);
            return ItemInteractionResult.FAIL;
         } else {
            FactoryPanelBlock.PanelSlot newSlot = getTargetedSlot(pos, state, location);
            this.withBlockEntityDo(level, pos, fpbe -> {
               if (fpbe.addPanel(newSlot, LogisticallyLinkedBlockItem.networkFromStack(FactoryPanelBlockItem.fixCtrlCopiedStack(stack)))) {
                  player.displayClientMessage(CreateLang.translateDirect("logistically_linked.connected"), true);
                  level.playSound(null, pos, this.soundType.getPlaceSound(), SoundSource.BLOCKS);
                  if (!player.isCreative()) {
                     stack.shrink(1);
                     if (stack.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                     }
                  }
               }
            });
            return ItemInteractionResult.SUCCESS;
         }
      }
   }

   public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
      return this.tryDestroySubPanelFirst(state, level, pos, player) ? false : super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
   }

   private boolean tryDestroySubPanelFirst(BlockState state, Level level, BlockPos pos, Player player) {
      double range = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) + 1.0;
      HitResult hitResult = player.pick(range, 1.0F, false);
      Vec3 location = hitResult.getLocation();
      FactoryPanelBlock.PanelSlot destroyedSlot = getTargetedSlot(pos, state, location);
      return InteractionResult.SUCCESS == this.onBlockEntityUse(level, pos, fpbe -> {
         if (fpbe.activePanels() < 2) {
            return InteractionResult.FAIL;
         } else if (!fpbe.removePanel(destroyedSlot)) {
            return InteractionResult.FAIL;
         } else {
            if (!player.isCreative()) {
               popResource(level, pos, AllBlocks.FACTORY_GAUGE.asStack());
            }

            return InteractionResult.SUCCESS;
         }
      });
   }

   public boolean isSignalSource(BlockState pState) {
      return true;
   }

   public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pBlockState.getValue(POWERED) ? 15 : 0;
   }

   public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
      return pBlockState.getValue(POWERED) && getConnectedDirection(pBlockState) == pSide ? 15 : 0;
   }

   public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
      if (!AllBlocks.FACTORY_GAUGE.isIn(pUseContext.getItemInHand())) {
         return false;
      } else {
         Vec3 location = pUseContext.getClickLocation();
         BlockPos pos = pUseContext.getClickedPos();
         FactoryPanelBlock.PanelSlot slot = getTargetedSlot(pos, pState, location);
         FactoryPanelBlockEntity blockEntity = this.getBlockEntity(pUseContext.getLevel(), pos);
         return blockEntity == null ? false : !blockEntity.panels.get(slot).isActive();
      }
   }

   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (pContext instanceof EntityCollisionContext ecc && ecc.getEntity() == null) {
         return this.getShape(pState, pLevel, pPos, pContext);
      }

      return Shapes.empty();
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      FactoryPanelBlockEntity blockEntity = this.getBlockEntity(pLevel, pPos);
      return blockEntity != null ? blockEntity.getShape() : AllShapes.FACTORY_PANEL_FALLBACK.get(getConnectedDirection(pState));
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public static Direction connectedDirection(BlockState state) {
      return getConnectedDirection(state);
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
   }

   public static FactoryPanelBlock.PanelSlot getTargetedSlot(BlockPos pos, BlockState blockState, Vec3 clickLocation) {
      double bestDistance = Double.MAX_VALUE;
      FactoryPanelBlock.PanelSlot bestSlot = FactoryPanelBlock.PanelSlot.BOTTOM_LEFT;
      Vec3 localClick = clickLocation.subtract(Vec3.atLowerCornerOf(pos));
      float xRot = (180.0F / (float)Math.PI) * getXRot(blockState);
      float yRot = (180.0F / (float)Math.PI) * getYRot(blockState);

      for (FactoryPanelBlock.PanelSlot slot : FactoryPanelBlock.PanelSlot.values()) {
         Vec3 vec = new Vec3(0.25 + (double)slot.xOffset * 0.5, 0.0, 0.25 + (double)slot.yOffset * 0.5);
         vec = VecHelper.rotateCentered(vec, 180.0, Axis.Y);
         vec = VecHelper.rotateCentered(vec, (double)(xRot + 90.0F), Axis.X);
         vec = VecHelper.rotateCentered(vec, (double)yRot, Axis.Y);
         double diff = vec.distanceToSqr(localClick);
         if (!(diff > bestDistance)) {
            bestDistance = diff;
            bestSlot = slot;
         }
      }

      return bestSlot;
   }

   @Override
   public Class<FactoryPanelBlockEntity> getBlockEntityClass() {
      return FactoryPanelBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends FactoryPanelBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends FactoryPanelBlockEntity>)AllBlockEntityTypes.FACTORY_PANEL.get();
   }

   public static float getXRot(BlockState state) {
      AttachFace face = state.getOptionalValue(FACE).orElse(AttachFace.FLOOR);
      return face == AttachFace.CEILING ? (float) (Math.PI / 2) : (face == AttachFace.FLOOR ? (float) (-Math.PI / 2) : 0.0F);
   }

   public static float getYRot(BlockState state) {
      Direction facing = state.getOptionalValue(FACING).orElse(Direction.SOUTH);
      AttachFace face = state.getOptionalValue(FACE).orElse(AttachFace.FLOOR);
      return (face == AttachFace.CEILING ? (float) Math.PI : 0.0F) + AngleHelper.rad((double)AngleHelper.horizontalAngle(facing));
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
      return ItemRequirement.NONE;
   }

   @NotNull
   protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
      return CODEC;
   }

   public static enum PanelSlot implements StringRepresentable {
      TOP_LEFT(1, 1),
      TOP_RIGHT(0, 1),
      BOTTOM_LEFT(1, 0),
      BOTTOM_RIGHT(0, 0);

      public static final Codec<FactoryPanelBlock.PanelSlot> CODEC = StringRepresentable.fromValues(FactoryPanelBlock.PanelSlot::values);
      public static final StreamCodec<ByteBuf, FactoryPanelBlock.PanelSlot> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(FactoryPanelBlock.PanelSlot.class);
      public final int xOffset;
      public final int yOffset;

      private PanelSlot(int xOffset, int yOffset) {
         this.xOffset = xOffset;
         this.yOffset = yOffset;
      }

      @NotNull
      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }

   public static enum PanelState {
      PASSIVE,
      ACTIVE;
   }

   public static enum PanelType {
      NETWORK,
      PACKAGER;
   }
}

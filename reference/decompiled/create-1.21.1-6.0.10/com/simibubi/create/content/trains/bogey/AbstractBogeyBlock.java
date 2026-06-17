package com.simibubi.create.content.trains.bogey;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.AllItems;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.ProperWaterloggedBlock;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class AbstractBogeyBlock<T extends AbstractBogeyBlockEntity>
   extends Block
   implements IBE<T>,
   ProperWaterloggedBlock,
   SpecialBlockItemRequirement,
   IWrenchable {
   public static final StreamCodec<RegistryFriendlyByteBuf, AbstractBogeyBlock<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.BLOCK)
      .map(block -> (AbstractBogeyBlock)block, Function.identity());
   public static final EnumProperty<Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
   static final List<ResourceLocation> BOGEYS = new ArrayList<>();
   public BogeySizes.BogeySize size;
   static final EnumSet<Direction> STICKY_X = EnumSet.of(Direction.EAST, Direction.WEST);
   static final EnumSet<Direction> STICKY_Z = EnumSet.of(Direction.SOUTH, Direction.NORTH);

   public AbstractBogeyBlock(Properties pProperties, BogeySizes.BogeySize size) {
      super(pProperties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(WATERLOGGED, false));
      this.size = size;
   }

   public boolean isOnIncompatibleTrack(Carriage carriage, boolean leading) {
      TravellingPoint point = leading ? carriage.getLeadingPoint() : carriage.getTrailingPoint();
      CarriageBogey bogey = leading ? carriage.leadingBogey() : carriage.trailingBogey();
      TrackEdge currentEdge = point.edge;
      return currentEdge == null ? false : currentEdge.getTrackMaterial().trackType != this.getTrackType(bogey.getStyle());
   }

   public Set<TrackMaterial.TrackType> getValidPathfindingTypes(BogeyStyle style) {
      return ImmutableSet.of(this.getTrackType(style));
   }

   public abstract TrackMaterial.TrackType getTrackType(BogeyStyle var1);

   @Internal
   public static void registerStandardBogey(ResourceLocation block) {
      BOGEYS.add(block);
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{AXIS, WATERLOGGED});
      super.createBlockStateDefinition(builder);
   }

   public BlockState updateShape(
      BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos
   ) {
      this.updateWater(pLevel, pState, pCurrentPos);
      return pState;
   }

   public FluidState getFluidState(BlockState pState) {
      return this.fluidState(pState);
   }

   public EnumSet<Direction> getStickySurfaces(BlockGetter world, BlockPos pos, BlockState state) {
      return state.getValue(BlockStateProperties.HORIZONTAL_AXIS) == Axis.X ? STICKY_X : STICKY_Z;
   }

   public abstract double getWheelPointSpacing();

   public abstract double getWheelRadius();

   public Vec3 getConnectorAnchorOffset(boolean upsideDown) {
      return this.getConnectorAnchorOffset();
   }

   protected abstract Vec3 getConnectorAnchorOffset();

   public boolean allowsSingleBogeyCarriage() {
      return true;
   }

   public abstract BogeyStyle getDefaultStyle();

   public boolean captureBlockEntityForTrain() {
      return false;
   }

   public BogeySizes.BogeySize getSize() {
      return this.size;
   }

   public Direction getBogeyUpDirection() {
      return Direction.UP;
   }

   public boolean isTrackAxisAlongFirstCoordinate(BlockState state) {
      return state.getValue(AXIS) == Axis.X;
   }

   @Nullable
   public BlockState getMatchingBogey(Direction upDirection, boolean axisAlongFirst) {
      return upDirection != Direction.UP ? null : (BlockState)this.defaultBlockState().setValue(AXIS, axisAlongFirst ? Axis.X : Axis.Z);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      if (level.isClientSide) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (!player.isShiftKeyDown()
         && stack.is((Item)AllItems.WRENCH.get())
         && !player.getCooldowns().isOnCooldown(stack.getItem())
         && AllBogeyStyles.BOGEY_STYLES.size() > 1) {
         if (!(level.getBlockEntity(pos) instanceof AbstractBogeyBlockEntity sbbe)) {
            return ItemInteractionResult.FAIL;
         } else {
            player.getCooldowns().addCooldown(stack.getItem(), 20);
            BogeyStyle currentStyle = sbbe.getStyle();
            BogeySizes.BogeySize size = this.getSize();
            BogeyStyle style = this.getNextStyle(currentStyle);
            if (style == currentStyle) {
               return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            } else {
               Set<BogeySizes.BogeySize> validSizes = style.validSizes();

               for (int i = 0; i < BogeySizes.all().size() && !validSizes.contains(size); i++) {
                  size = size.nextBySize();
               }

               sbbe.setBogeyStyle(style);
               CompoundTag defaultData = style.defaultData;
               sbbe.setBogeyData(sbbe.getBogeyData().merge(defaultData));
               if (size == this.getSize()) {
                  if (state.getBlock() != style.getBlockForSize(size)) {
                     CompoundTag oldData = sbbe.getBogeyData();
                     level.setBlock(pos, this.copyProperties(state, this.getStateOfSize(sbbe, size)), 3);
                     if (!(level.getBlockEntity(pos) instanceof AbstractBogeyBlockEntity bogeyBlockEntity)) {
                        return ItemInteractionResult.FAIL;
                     }

                     bogeyBlockEntity.setBogeyData(oldData);
                  }

                  player.displayClientMessage(CreateLang.translateDirect("bogey.style.updated_style").append(": ").append(style.displayName), true);
               } else {
                  CompoundTag oldData = sbbe.getBogeyData();
                  level.setBlock(pos, this.getStateOfSize(sbbe, size), 3);
                  if (!(level.getBlockEntity(pos) instanceof AbstractBogeyBlockEntity bogeyBlockEntity)) {
                     return ItemInteractionResult.FAIL;
                  }

                  bogeyBlockEntity.setBogeyData(oldData);
                  player.displayClientMessage(CreateLang.translateDirect("bogey.style.updated_style_and_size").append(": ").append(style.displayName), true);
               }

               return ItemInteractionResult.CONSUME;
            }
         }
      } else {
         return this.onInteractWithBogey(state, level, pos, player, hand, hitResult);
      }
   }

   protected ItemInteractionResult onInteractWithBogey(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   protected List<ResourceLocation> getBogeyBlockCycle() {
      return BOGEYS;
   }

   @Override
   public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
      Block block = state.getBlock();
      List<ResourceLocation> bogeyCycle = this.getBogeyBlockCycle();
      int indexOf = bogeyCycle.indexOf(RegisteredObjectsHelper.getKeyOrThrow(block));
      if (indexOf == -1) {
         return state;
      } else {
         int index = (indexOf + 1) % bogeyCycle.size();
         Direction bogeyUpDirection = this.getBogeyUpDirection();

         for (boolean trackAxisAlongFirstCoordinate = this.isTrackAxisAlongFirstCoordinate(state); index != indexOf; index = (index + 1) % bogeyCycle.size()) {
            ResourceLocation id = bogeyCycle.get(index);
            Block newBlock = (Block)BuiltInRegistries.BLOCK.get(id);
            if (newBlock instanceof AbstractBogeyBlock<?> bogey) {
               BlockState matchingBogey = bogey.getMatchingBogey(bogeyUpDirection, trackAxisAlongFirstCoordinate);
               if (matchingBogey != null) {
                  return this.copyProperties(state, matchingBogey);
               }
            }
         }

         return state;
      }
   }

   public BlockState getNextSize(Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof AbstractBogeyBlockEntity sbbe ? this.getNextSize(sbbe) : level.getBlockState(pos);
   }

   public List<Property<?>> propertiesToCopy() {
      return ImmutableList.of(WATERLOGGED, AXIS);
   }

   private <V extends Comparable<V>> BlockState copyProperty(BlockState source, BlockState target, Property<V> property) {
      return source.hasProperty(property) && target.hasProperty(property) ? (BlockState)target.setValue(property, source.getValue(property)) : target;
   }

   private BlockState copyProperties(BlockState source, BlockState target) {
      for (Property<?> property : this.propertiesToCopy()) {
         target = this.copyProperty(source, target, property);
      }

      return target;
   }

   public BlockState getNextSize(AbstractBogeyBlockEntity sbbe) {
      BogeySizes.BogeySize size = this.getSize();
      BogeyStyle style = sbbe.getStyle();
      BlockState nextBlock = style.getNextBlock(size).defaultBlockState();
      return this.copyProperties(sbbe.getBlockState(), nextBlock);
   }

   public BlockState getStateOfSize(AbstractBogeyBlockEntity sbbe, BogeySizes.BogeySize size) {
      BogeyStyle style = sbbe.getStyle();
      BlockState state = style.getBlockForSize(size).defaultBlockState();
      return this.copyProperties(sbbe.getBlockState(), state);
   }

   public BogeyStyle getNextStyle(Level level, BlockPos pos) {
      return level.getBlockEntity(pos) instanceof AbstractBogeyBlockEntity sbbe ? this.getNextStyle(sbbe.getStyle()) : this.getDefaultStyle();
   }

   public BogeyStyle getNextStyle(BogeyStyle style) {
      Collection<BogeyStyle> allStyles = style.getCycleGroup().values();
      if (allStyles.size() <= 1) {
         return style;
      } else {
         List<BogeyStyle> list = new ArrayList<>(allStyles);
         return (BogeyStyle)Iterate.cycleValue(list, style);
      }
   }

   @NotNull
   public BlockState rotate(@NotNull BlockState pState, Rotation pRotation) {
      return switch (pRotation) {
         case COUNTERCLOCKWISE_90, CLOCKWISE_90 -> (BlockState)pState.cycle(AXIS);
         default -> pState;
      };
   }

   @Override
   public ItemRequirement getRequiredItems(BlockState state, BlockEntity te) {
      return new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, AllBlocks.RAILWAY_CASING.asStack());
   }

   public boolean canBeUpsideDown() {
      return false;
   }

   public boolean isUpsideDown(BlockState state) {
      return false;
   }

   public BlockState getVersion(BlockState base, boolean upsideDown) {
      return base;
   }
}

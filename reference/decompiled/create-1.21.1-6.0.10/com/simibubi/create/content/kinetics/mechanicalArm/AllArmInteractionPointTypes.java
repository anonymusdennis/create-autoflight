package com.simibubi.create.content.kinetics.mechanicalArm;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.Create;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlock;
import com.simibubi.create.content.kinetics.crafter.MechanicalCrafterBlockEntity;
import com.simibubi.create.content.kinetics.deployer.DeployerBlock;
import com.simibubi.create.content.kinetics.saw.SawBlock;
import com.simibubi.create.content.logistics.chute.AbstractChuteBlock;
import com.simibubi.create.content.logistics.funnel.AbstractFunnelBlock;
import com.simibubi.create.content.logistics.funnel.BeltFunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.processing.basin.BasinBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.SmartInventory;
import java.util.Optional;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AllArmInteractionPointTypes {
   private static <T extends ArmInteractionPointType> void register(String name, T type) {
      Registry.register(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, Create.asResource(name), type);
   }

   @Internal
   public static void init() {
   }

   static {
      register("basin", new AllArmInteractionPointTypes.BasinType());
      register("belt", new AllArmInteractionPointTypes.BeltType());
      register("blaze_burner", new AllArmInteractionPointTypes.BlazeBurnerType());
      register("chute", new AllArmInteractionPointTypes.ChuteType());
      register("crafter", new AllArmInteractionPointTypes.CrafterType());
      register("crushing_wheels", new AllArmInteractionPointTypes.CrushingWheelsType());
      register("deployer", new AllArmInteractionPointTypes.DeployerType());
      register("depot", new AllArmInteractionPointTypes.DepotType());
      register("funnel", new AllArmInteractionPointTypes.FunnelType());
      register("millstone", new AllArmInteractionPointTypes.MillstoneType());
      register("packager", new AllArmInteractionPointTypes.PackagerType());
      register("saw", new AllArmInteractionPointTypes.SawType());
      register("campfire", new AllArmInteractionPointTypes.CampfireType());
      register("composter", new AllArmInteractionPointTypes.ComposterType());
      register("jukebox", new AllArmInteractionPointTypes.JukeboxType());
      register("respawn_anchor", new AllArmInteractionPointTypes.RespawnAnchorType());
   }

   public static class BasinType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return BasinBlock.isBasin(level, pos);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new ArmInteractionPoint(this, level, pos, state);
      }
   }

   public static class BeltPoint extends AllArmInteractionPointTypes.DepotPoint {
      public BeltPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      public void keepAlive() {
         super.keepAlive();
         BeltBlockEntity beltBE = BeltHelper.getSegmentBE(this.level, this.pos);
         if (beltBE != null) {
            TransportedItemStackHandlerBehaviour transport = beltBE.getBehaviour(TransportedItemStackHandlerBehaviour.TYPE);
            if (transport != null) {
               MutableBoolean found = new MutableBoolean(false);
               transport.handleProcessingOnAllItems(tis -> {
                  if (found.isTrue()) {
                     return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
                  } else {
                     tis.lockedExternally = true;
                     found.setTrue();
                     return TransportedItemStackHandlerBehaviour.TransportedResult.doNothing();
                  }
               });
            }
         }
      }
   }

   public static class BeltType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.BELT.has(state) && !(level.getBlockState(pos.above()).getBlock() instanceof BeltTunnelBlock) && BeltBlock.canTransportObjects(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.BeltPoint(this, level, pos, state);
      }
   }

   public static class BlazeBurnerPoint extends AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint {
      public BlazeBurnerPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
         ItemStack input = stack.copy();
         InteractionResultHolder<ItemStack> res = BlazeBurnerBlock.tryInsert(this.cachedState, this.level, this.pos, input, false, false, simulate);
         ItemStack remainder = (ItemStack)res.getObject();
         if (input.isEmpty()) {
            return remainder;
         } else {
            if (!simulate) {
               Containers.dropItemStack(this.level, (double)this.pos.getX(), (double)this.pos.getY(), (double)this.pos.getZ(), remainder);
            }

            return input;
         }
      }
   }

   public static class BlazeBurnerType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.BLAZE_BURNER.has(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.BlazeBurnerPoint(this, level, pos, state);
      }
   }

   public static class CampfirePoint extends AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint {
      public CampfirePoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
         if (!(this.level.getBlockEntity(this.pos) instanceof CampfireBlockEntity campfireBE)) {
            return stack;
         } else {
            Optional<RecipeHolder<CampfireCookingRecipe>> recipe = campfireBE.getCookableRecipe(stack);
            if (recipe.isEmpty()) {
               return stack;
            } else if (!simulate) {
               ItemStack remainder = stack.copy();
               campfireBE.placeFood(null, remainder, ((CampfireCookingRecipe)recipe.get().value()).getCookingTime());
               return remainder;
            } else {
               boolean hasSpace = false;

               for (ItemStack campfireStack : campfireBE.getItems()) {
                  if (campfireStack.isEmpty()) {
                     hasSpace = true;
                     break;
                  }
               }

               if (!hasSpace) {
                  return stack;
               } else {
                  ItemStack remainder = stack.copy();
                  remainder.shrink(1);
                  return remainder;
               }
            }
         }
      }
   }

   public static class CampfireType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return state.getBlock() instanceof CampfireBlock;
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.CampfirePoint(this, level, pos, state);
      }
   }

   public static class ChuteType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AbstractChuteBlock.isChute(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.TopFaceArmInteractionPoint(this, level, pos, state);
      }
   }

   public static class ComposterPoint extends ArmInteractionPoint {
      public ComposterPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      protected Vec3 getInteractionPositionVector() {
         return Vec3.atLowerCornerOf(this.pos).add(0.5, 0.8125, 0.5);
      }

      @Override
      public void updateCachedState() {
         BlockState oldState = this.cachedState;
         super.updateCachedState();
         if (this.cachedHandler != null && oldState != this.cachedState) {
            this.level.invalidateCapabilities(this.cachedHandler.pos());
         }
      }

      @Nullable
      @Override
      protected IItemHandler getHandler(ArmBlockEntity armBlockEntity) {
         return null;
      }

      protected WorldlyContainer getContainer() {
         ComposterBlock composterBlock = (ComposterBlock)Blocks.COMPOSTER;
         return composterBlock.getContainer(this.cachedState, this.level, this.pos);
      }

      @Override
      public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
         IItemHandler handler = new SidedInvWrapper(this.getContainer(), Direction.UP);
         return ItemHandlerHelper.insertItem(handler, stack, simulate);
      }

      @Override
      public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
         IItemHandler handler = new SidedInvWrapper(this.getContainer(), Direction.DOWN);
         return handler.extractItem(slot, amount, simulate);
      }

      @Override
      public int getSlotCount(ArmBlockEntity armBlockEntity) {
         return 2;
      }
   }

   public static class ComposterType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return state.is(Blocks.COMPOSTER);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.ComposterPoint(this, level, pos, state);
      }
   }

   public static class CrafterPoint extends ArmInteractionPoint {
      public CrafterPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      protected Direction getInteractionDirection() {
         return this.cachedState.getOptionalValue(MechanicalCrafterBlock.HORIZONTAL_FACING).orElse(Direction.SOUTH).getOpposite();
      }

      @Override
      protected Vec3 getInteractionPositionVector() {
         return super.getInteractionPositionVector().add(Vec3.atLowerCornerOf(this.getInteractionDirection().getNormal()).scale(0.5));
      }

      @Override
      public void updateCachedState() {
         BlockState oldState = this.cachedState;
         super.updateCachedState();
         if (oldState != this.cachedState) {
            this.cachedAngles = null;
         }
      }

      @Override
      public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
         if (this.level.getBlockEntity(this.pos) instanceof MechanicalCrafterBlockEntity crafter) {
            SmartInventory inventory = crafter.getInventory();
            inventory.allowExtraction();
            ItemStack extract = super.extract(armBlockEntity, slot, amount, simulate);
            inventory.forbidExtraction();
            return extract;
         } else {
            return ItemStack.EMPTY;
         }
      }
   }

   public static class CrafterType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.MECHANICAL_CRAFTER.has(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.CrafterPoint(this, level, pos, state);
      }
   }

   public static class CrushingWheelPoint extends AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint {
      public CrushingWheelPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      protected Vec3 getInteractionPositionVector() {
         return Vec3.atLowerCornerOf(this.pos).add(0.5, 1.0, 0.5);
      }
   }

   public static class CrushingWheelsType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.CRUSHING_WHEEL_CONTROLLER.has(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.CrushingWheelPoint(this, level, pos, state);
      }
   }

   public static class DeployerPoint extends ArmInteractionPoint {
      public DeployerPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      protected Direction getInteractionDirection() {
         return this.cachedState.getOptionalValue(DeployerBlock.FACING).orElse(Direction.UP).getOpposite();
      }

      @Override
      protected Vec3 getInteractionPositionVector() {
         return super.getInteractionPositionVector().add(Vec3.atLowerCornerOf(this.getInteractionDirection().getNormal()).scale(0.65F));
      }

      @Override
      public void updateCachedState() {
         BlockState oldState = this.cachedState;
         super.updateCachedState();
         if (oldState != this.cachedState) {
            this.cachedAngles = null;
         }
      }
   }

   public static class DeployerType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.DEPLOYER.has(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.DeployerPoint(this, level, pos, state);
      }
   }

   public static class DepositOnlyArmInteractionPoint extends ArmInteractionPoint {
      public DepositOnlyArmInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      public void cycleMode() {
      }

      @Override
      public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
         return ItemStack.EMPTY;
      }

      @Override
      public int getSlotCount(ArmBlockEntity armBlockEntity) {
         return 0;
      }
   }

   public static class DepotPoint extends ArmInteractionPoint {
      public DepotPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      protected Vec3 getInteractionPositionVector() {
         return Vec3.atLowerCornerOf(this.pos).add(0.5, 0.875, 0.5);
      }
   }

   public static class DepotType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.DEPOT.has(state) || AllBlocks.WEIGHTED_EJECTOR.has(state) || AllBlocks.TRACK_STATION.has(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.DepotPoint(this, level, pos, state);
      }
   }

   public static class FunnelPoint extends AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint {
      public FunnelPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      protected Vec3 getInteractionPositionVector() {
         Direction funnelFacing = FunnelBlock.getFunnelFacing(this.cachedState);
         Vec3i normal = funnelFacing != null ? funnelFacing.getNormal() : Vec3i.ZERO;
         return VecHelper.getCenterOf(this.pos).add(Vec3.atLowerCornerOf(normal).scale(-0.15F));
      }

      @Override
      protected Direction getInteractionDirection() {
         Direction funnelFacing = FunnelBlock.getFunnelFacing(this.cachedState);
         return funnelFacing != null ? funnelFacing.getOpposite() : Direction.UP;
      }

      @Override
      public void updateCachedState() {
         BlockState oldState = this.cachedState;
         super.updateCachedState();
         if (oldState != this.cachedState) {
            this.cachedAngles = null;
         }
      }

      @Override
      public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
         FilteringBehaviour filtering = BlockEntityBehaviour.get(this.level, this.pos, FilteringBehaviour.TYPE);
         InvManipulationBehaviour inserter = BlockEntityBehaviour.get(this.level, this.pos, InvManipulationBehaviour.TYPE);
         if (this.cachedState.getOptionalValue(BlockStateProperties.POWERED).orElse(false)) {
            return stack;
         } else if (inserter == null) {
            return stack;
         } else if (filtering != null && !filtering.test(stack)) {
            return stack;
         } else {
            if (simulate) {
               inserter.simulate();
            }

            ItemStack insert = inserter.insert(stack);
            if (!simulate && insert.getCount() != stack.getCount() && this.level.getBlockEntity(this.pos) instanceof FunnelBlockEntity funnelBlockEntity) {
               funnelBlockEntity.onTransfer(stack);
               if (funnelBlockEntity.hasFlap()) {
                  funnelBlockEntity.flap(true);
               }
            }

            return insert;
         }
      }
   }

   public static class FunnelType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return state.getBlock() instanceof AbstractFunnelBlock
            && (!state.hasProperty(FunnelBlock.EXTRACTING) || !(Boolean)state.getValue(FunnelBlock.EXTRACTING))
            && (!state.hasProperty(BeltFunnelBlock.SHAPE) || state.getValue(BeltFunnelBlock.SHAPE) != BeltFunnelBlock.Shape.PUSHING);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.FunnelPoint(this, level, pos, state);
      }
   }

   public static class JukeboxPoint extends AllArmInteractionPointTypes.TopFaceArmInteractionPoint {
      public JukeboxPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      public int getSlotCount(ArmBlockEntity armBlockEntity) {
         return 1;
      }

      @Override
      public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
         if (stack.get(DataComponents.JUKEBOX_PLAYABLE) == null) {
            return stack;
         } else if (this.cachedState.getOptionalValue(JukeboxBlock.HAS_RECORD).orElse(true)) {
            return stack;
         } else if (this.level.getBlockEntity(this.pos) instanceof JukeboxBlockEntity jukeboxBE) {
            if (!jukeboxBE.getTheItem().isEmpty()) {
               return stack;
            } else {
               ItemStack remainder = stack.copy();
               ItemStack toInsert = remainder.split(1);
               if (!simulate) {
                  jukeboxBE.setTheItem(toInsert);
               }

               return remainder;
            }
         } else {
            return stack;
         }
      }

      @Override
      public ItemStack extract(ArmBlockEntity armBlockEntity, int slot, int amount, boolean simulate) {
         if (!this.cachedState.getOptionalValue(JukeboxBlock.HAS_RECORD).orElse(false)) {
            return ItemStack.EMPTY;
         } else if (this.level.getBlockEntity(this.pos) instanceof JukeboxBlockEntity jukeboxBE) {
            return !simulate ? jukeboxBE.removeItem(slot, amount) : jukeboxBE.getTheItem();
         } else {
            return ItemStack.EMPTY;
         }
      }
   }

   public static class JukeboxType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return state.is(Blocks.JUKEBOX);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.JukeboxPoint(this, level, pos, state);
      }
   }

   public static class MillstoneType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.MILLSTONE.has(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new ArmInteractionPoint(this, level, pos, state);
      }
   }

   public static class PackagerType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.PACKAGER.has(state) || AllBlocks.REPACKAGER.has(state);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new ArmInteractionPoint(this, level, pos, state);
      }
   }

   public static class RespawnAnchorPoint extends AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint {
      public RespawnAnchorPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      protected Vec3 getInteractionPositionVector() {
         return Vec3.atLowerCornerOf(this.pos).add(0.5, 1.0, 0.5);
      }

      @Override
      public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
         if (!stack.is(Items.GLOWSTONE)) {
            return stack;
         } else if (this.cachedState.getOptionalValue(RespawnAnchorBlock.CHARGE).orElse(4) == 4) {
            return stack;
         } else {
            if (!simulate) {
               RespawnAnchorBlock.charge(null, this.level, this.pos, this.cachedState);
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(1);
            return remainder;
         }
      }
   }

   public static class RespawnAnchorType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return state.is(Blocks.RESPAWN_ANCHOR);
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.RespawnAnchorPoint(this, level, pos, state);
      }
   }

   public static class SawType extends ArmInteractionPointType {
      @Override
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return AllBlocks.MECHANICAL_SAW.has(state)
            && state.getValue(SawBlock.FACING) == Direction.UP
            && ((KineticBlockEntity)level.getBlockEntity(pos)).getSpeed() != 0.0F;
      }

      @Override
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new AllArmInteractionPointTypes.DepotPoint(this, level, pos, state);
      }
   }

   public static class TopFaceArmInteractionPoint extends ArmInteractionPoint {
      public TopFaceArmInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      @Override
      protected Vec3 getInteractionPositionVector() {
         return Vec3.atLowerCornerOf(this.pos).add(0.5, 1.0, 0.5);
      }
   }
}

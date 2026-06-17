package com.simibubi.create.content.contraptions.mounted;

import com.simibubi.create.AllAttachmentTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.minecart.CouplingHandler;
import com.simibubi.create.content.contraptions.minecart.capability.MinecartController;
import com.simibubi.create.content.redstone.rail.ControllerRailBlock;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.advancement.CreateAdvancement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import java.util.UUID;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.MinecartFurnace;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;

public class CartAssemblerBlockEntity extends SmartBlockEntity implements IDisplayAssemblyExceptions {
   private static final int assemblyCooldown = 8;
   protected ScrollOptionBehaviour<CartAssemblerBlockEntity.CartMovementMode> movementMode;
   private int ticksSinceMinecartUpdate = 8;
   protected AssemblyException lastException;
   protected AbstractMinecart cartToAssemble;

   public CartAssemblerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.ticksSinceMinecartUpdate < 8) {
         this.ticksSinceMinecartUpdate++;
      }

      this.tryAssemble(this.cartToAssemble);
      this.cartToAssemble = null;
   }

   public void tryAssemble(AbstractMinecart cart) {
      if (cart != null) {
         if (this.isMinecartUpdateValid()) {
            this.resetTicksSinceMinecartUpdate();
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (AllBlocks.CART_ASSEMBLER.has(state)) {
               CartAssemblerBlock block = (CartAssemblerBlock)state.getBlock();
               CartAssemblerBlock.CartAssemblerAction action = CartAssemblerBlock.getActionForCart(state, cart);
               if (action.shouldAssemble()) {
                  this.assemble(this.level, this.worldPosition, cart);
               }

               if (action.shouldDisassemble()) {
                  this.disassemble(this.level, this.worldPosition, cart);
               }

               if (action == CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE && cart.getDeltaMovement().length() > 0.0078125) {
                  Direction facing = cart.getMotionDirection();
                  RailShape railShape = (RailShape)state.getValue(CartAssemblerBlock.RAIL_SHAPE);

                  for (Direction d : Iterate.directionsInAxis(railShape == RailShape.EAST_WEST ? Axis.X : Axis.Z)) {
                     if (this.level.getBlockState(this.worldPosition.relative(d)).isRedstoneConductor(this.level, this.worldPosition.relative(d))) {
                        facing = d.getOpposite();
                     }
                  }

                  float speed = block.getRailMaxSpeed(state, this.level, this.worldPosition, cart);
                  cart.setDeltaMovement(
                     (double)((float)facing.getStepX() * speed), (double)((float)facing.getStepY() * speed), (double)((float)facing.getStepZ() * speed)
                  );
               }

               if (action == CartAssemblerBlock.CartAssemblerAction.ASSEMBLE_ACCELERATE_DIRECTIONAL) {
                  Vec3i accelerationVector = ControllerRailBlock.getAccelerationVector(
                     (BlockState)((BlockState)AllBlocks.CONTROLLER_RAIL
                           .getDefaultState()
                           .setValue(ControllerRailBlock.SHAPE, (RailShape)state.getValue(CartAssemblerBlock.RAIL_SHAPE)))
                        .setValue(ControllerRailBlock.BACKWARDS, (Boolean)state.getValue(CartAssemblerBlock.BACKWARDS))
                  );
                  float speed = block.getRailMaxSpeed(state, this.level, this.worldPosition, cart);
                  cart.setDeltaMovement(Vec3.atLowerCornerOf(accelerationVector).scale((double)speed));
               }

               if (action == CartAssemblerBlock.CartAssemblerAction.DISASSEMBLE_BRAKE) {
                  Vec3 diff = VecHelper.getCenterOf(this.worldPosition).subtract(cart.position());
                  cart.setDeltaMovement(diff.x / 16.0, 0.0, diff.z / 16.0);
               }
            }
         }
      }
   }

   protected void assemble(Level world, BlockPos pos, AbstractMinecart cart) {
      if (cart.getPassengers().isEmpty()) {
         MinecartController controller = (MinecartController)cart.getData(AllAttachmentTypes.MINECART_CONTROLLER);
         if (controller == MinecartController.EMPTY || !controller.isCoupledThroughContraption()) {
            CartAssemblerBlockEntity.CartMovementMode mode = CartAssemblerBlockEntity.CartMovementMode.values()[this.movementMode.value];
            MountedContraption contraption = new MountedContraption(mode);

            try {
               if (!contraption.assemble(world, pos)) {
                  return;
               }

               this.lastException = null;
               this.sendData();
            } catch (AssemblyException var11) {
               this.lastException = var11;
               this.sendData();
               return;
            }

            boolean couplingFound = contraption.connectedCart != null;
            Direction initialOrientation = CartAssemblerBlock.getHorizontalDirection(this.getBlockState());
            if (couplingFound) {
               cart.setPos((double)((float)pos.getX() + 0.5F), (double)pos.getY(), (double)((float)pos.getZ() + 0.5F));
               if (!CouplingHandler.tryToCoupleCarts(null, world, cart.getId(), contraption.connectedCart.getId())) {
                  return;
               }
            }

            contraption.removeBlocksFromWorld(world, BlockPos.ZERO);
            contraption.startMoving(world);
            contraption.expandBoundsAroundAxis(Axis.Y);
            if (couplingFound) {
               Vec3 diff = contraption.connectedCart.position().subtract(cart.position());
               initialOrientation = Direction.fromYRot(Mth.atan2(diff.z, diff.x) * 180.0 / Math.PI);
            }

            OrientedContraptionEntity entity = OrientedContraptionEntity.create(world, contraption, initialOrientation);
            if (couplingFound) {
               entity.setCouplingId(cart.getUUID());
            }

            entity.setPos((double)pos.getX() + 0.5, (double)pos.getY(), (double)pos.getZ() + 0.5);
            world.addFreshEntity(entity);
            entity.startRiding(cart);
            if (cart instanceof MinecartFurnace) {
               CompoundTag nbt = new CompoundTag();
               if (cart.save(nbt)) {
                  nbt.putDouble("PushZ", 0.0);
                  nbt.putDouble("PushX", 0.0);
                  cart.load(nbt);
               }
            }

            if (contraption.containsBlockBreakers()) {
               this.award(AllAdvancements.CONTRAPTION_ACTORS);
            }
         }
      }
   }

   protected void disassemble(Level world, BlockPos pos, AbstractMinecart cart) {
      if (!cart.getPassengers().isEmpty()) {
         Entity entity = (Entity)cart.getPassengers().get(0);
         if (entity instanceof OrientedContraptionEntity contraption) {
            UUID couplingId = contraption.getCouplingId();
            if (couplingId == null) {
               contraption.yaw = CartAssemblerBlock.getHorizontalDirection(this.getBlockState()).toYRot();
               this.disassembleCart(cart);
            } else {
               Couple<MinecartController> coupledCarts = contraption.getCoupledCartsIfPresent();
               if (coupledCarts != null) {
                  for (boolean current : Iterate.trueAndFalse) {
                     MinecartController minecartController = (MinecartController)coupledCarts.get(current);
                     if (minecartController.cart() != cart) {
                        BlockPos otherPos = minecartController.cart().blockPosition();
                        BlockState blockState = world.getBlockState(otherPos);
                        if (!AllBlocks.CART_ASSEMBLER.has(blockState)) {
                           return;
                        }

                        if (!CartAssemblerBlock.getActionForCart(blockState, minecartController.cart()).shouldDisassemble()) {
                           return;
                        }
                     }
                  }

                  for (boolean currentx : Iterate.trueAndFalse) {
                     ((MinecartController)coupledCarts.get(currentx)).removeConnection(currentx);
                  }

                  this.disassembleCart(cart);
               }
            }
         }
      }
   }

   protected void disassembleCart(AbstractMinecart cart) {
      cart.ejectPassengers();
      if (cart instanceof MinecartFurnace) {
         CompoundTag nbt = new CompoundTag();
         cart.saveAsPassenger(nbt);
         nbt.putDouble("PushZ", cart.getDeltaMovement().x);
         nbt.putDouble("PushX", cart.getDeltaMovement().z);
         cart.load(nbt);
      }
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      this.movementMode = new ScrollOptionBehaviour<>(
         CartAssemblerBlockEntity.CartMovementMode.class, CreateLang.translateDirect("contraptions.cart_movement_mode"), this, this.getMovementModeSlot()
      );
      behaviours.add(this.movementMode);
      this.registerAwardables(behaviours, new CreateAdvancement[]{AllAdvancements.CONTRAPTION_ACTORS});
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      AssemblyException.write(compound, registries, this.lastException);
      super.write(compound, registries, clientPacket);
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      this.lastException = AssemblyException.read(compound, registries);
      super.read(compound, registries, clientPacket);
   }

   @Override
   public AssemblyException getLastAssemblyException() {
      return this.lastException;
   }

   protected ValueBoxTransform getMovementModeSlot() {
      return new CartAssemblerBlockEntity.CartAssemblerValueBoxTransform();
   }

   public void resetTicksSinceMinecartUpdate() {
      this.ticksSinceMinecartUpdate = 0;
   }

   public void assembleNextTick(AbstractMinecart cart) {
      if (this.cartToAssemble == null) {
         this.cartToAssemble = cart;
      }
   }

   public boolean isMinecartUpdateValid() {
      return this.ticksSinceMinecartUpdate >= 8;
   }

   private class CartAssemblerValueBoxTransform extends CenteredSideValueBoxTransform {
      public CartAssemblerValueBoxTransform() {
         super((state, d) -> {
            if (d.getAxis().isVertical()) {
               return false;
            } else if (!state.hasProperty(CartAssemblerBlock.RAIL_SHAPE)) {
               return false;
            } else {
               RailShape railShape = (RailShape)state.getValue(CartAssemblerBlock.RAIL_SHAPE);
               return d.getAxis() == Axis.X == (railShape == RailShape.NORTH_SOUTH);
            }
         });
      }

      @Override
      protected Vec3 getSouthLocation() {
         return VecHelper.voxelSpace(8.0, 7.0, 17.5);
      }
   }

   public static enum CartMovementMode implements INamedIconOptions {
      ROTATE(AllIcons.I_CART_ROTATE),
      ROTATE_PAUSED(AllIcons.I_CART_ROTATE_PAUSED),
      ROTATION_LOCKED(AllIcons.I_CART_ROTATE_LOCKED);

      private String translationKey;
      private AllIcons icon;

      private CartMovementMode(AllIcons icon) {
         this.icon = icon;
         this.translationKey = "create.contraptions.cart_movement_mode." + Lang.asId(this.name());
      }

      @Override
      public AllIcons getIcon() {
         return this.icon;
      }

      @Override
      public String getTranslationKey() {
         return this.translationKey;
      }
   }
}

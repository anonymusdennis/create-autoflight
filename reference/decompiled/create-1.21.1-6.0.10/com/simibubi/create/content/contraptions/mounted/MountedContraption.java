package com.simibubi.create.content.contraptions.mounted;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.contraption.ContraptionType;
import com.simibubi.create.content.contraptions.AssemblyException;
import com.simibubi.create.content.contraptions.Contraption;
import java.util.Iterator;
import java.util.Queue;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.apache.commons.lang3.tuple.Pair;

public class MountedContraption extends Contraption {
   public CartAssemblerBlockEntity.CartMovementMode rotationMode;
   public AbstractMinecart connectedCart;

   public MountedContraption() {
      this(CartAssemblerBlockEntity.CartMovementMode.ROTATE);
   }

   public MountedContraption(CartAssemblerBlockEntity.CartMovementMode mode) {
      this.rotationMode = mode;
   }

   @Override
   public ContraptionType getType() {
      return (ContraptionType)AllContraptionTypes.MOUNTED.value();
   }

   @Override
   public boolean assemble(Level world, BlockPos pos) throws AssemblyException {
      BlockState state = world.getBlockState(pos);
      if (!state.hasProperty(CartAssemblerBlock.RAIL_SHAPE)) {
         return false;
      } else if (!this.searchMovedStructure(world, pos, null)) {
         return false;
      } else {
         Axis axis = state.getValue(CartAssemblerBlock.RAIL_SHAPE) == RailShape.EAST_WEST ? Axis.X : Axis.Z;
         this.addBlock(
            world,
            pos,
            Pair.of(
               new StructureBlockInfo(pos, (BlockState)AllBlocks.MINECART_ANCHOR.getDefaultState().setValue(BlockStateProperties.HORIZONTAL_AXIS, axis), null),
               null
            )
         );
         return this.blocks.size() != 1;
      }
   }

   @Override
   protected boolean addToInitialFrontier(Level world, BlockPos pos, Direction direction, Queue<BlockPos> frontier) {
      frontier.clear();
      frontier.add(pos.above());
      return true;
   }

   @Override
   protected Pair<StructureBlockInfo, BlockEntity> capture(Level world, BlockPos pos) {
      Pair<StructureBlockInfo, BlockEntity> pair = super.capture(world, pos);
      StructureBlockInfo capture = (StructureBlockInfo)pair.getKey();
      if (!AllBlocks.CART_ASSEMBLER.has(capture.state())) {
         return pair;
      } else {
         Pair<StructureBlockInfo, BlockEntity> anchorSwap = Pair.of(
            new StructureBlockInfo(pos, CartAssemblerBlock.createAnchor(capture.state()), null), (BlockEntity)pair.getValue()
         );
         if (!pos.equals(this.anchor) && this.connectedCart == null) {
            for (Axis axis : Iterate.axes) {
               if (!axis.isVertical() && VecHelper.onSameAxis(this.anchor, pos, axis)) {
                  for (AbstractMinecart abstractMinecartEntity : world.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos))) {
                     if (!CartAssemblerBlock.canAssembleTo(abstractMinecartEntity)) {
                        break;
                     }

                     this.connectedCart = abstractMinecartEntity;
                     this.connectedCart.setPos((double)pos.getX() + 0.5, (double)pos.getY(), (double)((float)pos.getZ() + 0.5F));
                  }
               }
            }

            return anchorSwap;
         } else {
            return anchorSwap;
         }
      }
   }

   @Override
   protected boolean movementAllowed(BlockState state, Level world, BlockPos pos) {
      return !pos.equals(this.anchor) && AllBlocks.CART_ASSEMBLER.has(state)
         ? this.testSecondaryCartAssembler(world, state, pos)
         : super.movementAllowed(state, world, pos);
   }

   protected boolean testSecondaryCartAssembler(Level world, BlockState state, BlockPos pos) {
      for (Axis axis : Iterate.axes) {
         if (!axis.isVertical() && VecHelper.onSameAxis(this.anchor, pos, axis)) {
            Iterator var8 = world.getEntitiesOfClass(AbstractMinecart.class, new AABB(pos)).iterator();
            if (var8.hasNext()) {
               AbstractMinecart abstractMinecartEntity = (AbstractMinecart)var8.next();
               if (CartAssemblerBlock.canAssembleTo(abstractMinecartEntity)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   @Override
   public CompoundTag writeNBT(Provider registries, boolean spawnPacket) {
      CompoundTag tag = super.writeNBT(registries, spawnPacket);
      NBTHelper.writeEnum(tag, "RotationMode", this.rotationMode);
      return tag;
   }

   @Override
   public void readNBT(Level world, CompoundTag nbt, boolean spawnData) {
      this.rotationMode = (CartAssemblerBlockEntity.CartMovementMode)NBTHelper.readEnum(nbt, "RotationMode", CartAssemblerBlockEntity.CartMovementMode.class);
      super.readNBT(world, nbt, spawnData);
   }

   @Override
   protected boolean customBlockPlacement(LevelAccessor world, BlockPos pos, BlockState state) {
      return AllBlocks.MINECART_ANCHOR.has(state);
   }

   @Override
   protected boolean customBlockRemoval(LevelAccessor world, BlockPos pos, BlockState state) {
      return AllBlocks.MINECART_ANCHOR.has(state);
   }

   @Override
   public boolean canBeStabilized(Direction facing, BlockPos localPos) {
      return true;
   }

   public void addExtraInventories(Entity cart) {
      if (cart instanceof Container container) {
         this.storage.attachExternal(new InvWrapper(container));
      }
   }
}

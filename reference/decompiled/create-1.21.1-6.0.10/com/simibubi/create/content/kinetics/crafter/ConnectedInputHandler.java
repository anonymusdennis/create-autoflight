package com.simibubi.create.content.kinetics.crafter;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

public class ConnectedInputHandler {
   public static boolean shouldConnect(Level world, BlockPos pos, Direction face, Direction direction) {
      BlockState refState = world.getBlockState(pos);
      if (!refState.hasProperty(HorizontalKineticBlock.HORIZONTAL_FACING)) {
         return false;
      } else {
         Direction refDirection = (Direction)refState.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
         if (direction.getAxis() == refDirection.getAxis()) {
            return false;
         } else if (face == refDirection) {
            return false;
         } else {
            BlockState neighbour = world.getBlockState(pos.relative(direction));
            return !AllBlocks.MECHANICAL_CRAFTER.has(neighbour) ? false : refDirection == neighbour.getValue(HorizontalKineticBlock.HORIZONTAL_FACING);
         }
      }
   }

   public static void toggleConnection(Level world, BlockPos pos, BlockPos pos2) {
      MechanicalCrafterBlockEntity crafter1 = CrafterHelper.getCrafter(world, pos);
      MechanicalCrafterBlockEntity crafter2 = CrafterHelper.getCrafter(world, pos2);
      if (crafter1 != null && crafter2 != null) {
         BlockPos controllerPos1 = crafter1.getBlockPos().offset((Vec3i)crafter1.input.data.get(0));
         BlockPos controllerPos2 = crafter2.getBlockPos().offset((Vec3i)crafter2.input.data.get(0));
         if (!controllerPos1.equals(controllerPos2)) {
            if (!crafter1.input.isController) {
               crafter1 = CrafterHelper.getCrafter(world, controllerPos1);
            }

            if (!crafter2.input.isController) {
               crafter2 = CrafterHelper.getCrafter(world, controllerPos2);
            }

            if (crafter1 != null && crafter2 != null) {
               connectControllers(world, crafter1, crafter2);
               world.setBlock(crafter1.getBlockPos(), crafter1.getBlockState(), 3);
               crafter1.setChanged();
               crafter1.connectivityChanged();
               crafter2.setChanged();
               crafter2.connectivityChanged();
            }
         } else {
            MechanicalCrafterBlockEntity controller = CrafterHelper.getCrafter(world, controllerPos1);
            Set<BlockPos> positions = controller.input.data.stream().<BlockPos>map(controllerPos1::offset).collect(Collectors.toSet());
            List<BlockPos> frontier = new LinkedList<>();
            List<BlockPos> splitGroup = new ArrayList<>();
            frontier.add(pos2);
            positions.remove(pos2);
            positions.remove(pos);

            while (!frontier.isEmpty()) {
               BlockPos current = frontier.remove(0);

               for (Direction direction : Iterate.directions) {
                  BlockPos next = current.relative(direction);
                  if (positions.remove(next)) {
                     splitGroup.add(next);
                     frontier.add(next);
                  }
               }
            }

            initAndAddAll(world, crafter1, positions);
            initAndAddAll(world, crafter2, splitGroup);
            crafter1.setChanged();
            crafter1.connectivityChanged();
            crafter2.setChanged();
            crafter2.connectivityChanged();
         }
      }
   }

   public static void initAndAddAll(Level world, MechanicalCrafterBlockEntity crafter, Collection<BlockPos> positions) {
      crafter.input = new ConnectedInputHandler.ConnectedInput();
      positions.forEach(splitPos -> modifyAndUpdate(world, splitPos, input -> {
            input.attachTo(crafter.getBlockPos(), splitPos);
            crafter.input.data.add(splitPos.subtract(crafter.getBlockPos()));
         }));
   }

   public static void connectControllers(Level world, MechanicalCrafterBlockEntity crafter1, MechanicalCrafterBlockEntity crafter2) {
      crafter1.input.data.forEach(offset -> {
         BlockPos connectedPos = crafter1.getBlockPos().offset(offset);
         modifyAndUpdate(world, connectedPos, input -> {
         });
      });
      crafter2.input.data.forEach(offset -> {
         if (!offset.equals(BlockPos.ZERO)) {
            BlockPos connectedPos = crafter2.getBlockPos().offset(offset);
            modifyAndUpdate(world, connectedPos, input -> {
               input.attachTo(crafter1.getBlockPos(), connectedPos);
               crafter1.input.data.add(BlockPos.ZERO.subtract((Vec3i)input.data.get(0)));
            });
         }
      });
      crafter2.input.attachTo(crafter1.getBlockPos(), crafter2.getBlockPos());
      crafter1.input.data.add(BlockPos.ZERO.subtract((Vec3i)crafter2.input.data.get(0)));
   }

   private static void modifyAndUpdate(Level world, BlockPos pos, Consumer<ConnectedInputHandler.ConnectedInput> callback) {
      if (world.getBlockEntity(pos) instanceof MechanicalCrafterBlockEntity crafter) {
         callback.accept(crafter.input);
         crafter.setChanged();
         crafter.connectivityChanged();
      }
   }

   public static class ConnectedInput {
      boolean isController;
      List<BlockPos> data = Collections.synchronizedList(new ArrayList<>());

      public ConnectedInput() {
         this.isController = true;
         this.data.add(BlockPos.ZERO);
      }

      public void attachTo(BlockPos controllerPos, BlockPos myPos) {
         this.isController = false;
         this.data.clear();
         this.data.add(controllerPos.subtract(myPos));
      }

      public IItemHandler getItemHandler(Level world, BlockPos pos) {
         List<MechanicalCrafterBlockEntity.Inventory> inventories = this.getInventories(world, pos);
         return new CombinedInvWrapper(inventories.toArray(IItemHandlerModifiable[]::new));
      }

      public List<MechanicalCrafterBlockEntity.Inventory> getInventories(Level world, BlockPos pos) {
         if (!this.isController) {
            BlockPos controllerPos = pos.offset((Vec3i)this.data.get(0));
            ConnectedInputHandler.ConnectedInput input = CrafterHelper.getInput(world, controllerPos);
            return input != this && input != null && input.isController ? input.getInventories(world, controllerPos) : List.of();
         } else {
            Direction facing = Direction.SOUTH;
            BlockState blockState = world.getBlockState(pos);
            if (blockState.hasProperty(MechanicalCrafterBlock.HORIZONTAL_FACING)) {
               facing = (Direction)blockState.getValue(MechanicalCrafterBlock.HORIZONTAL_FACING);
            }

            AxisDirection axisDirection = facing.getAxisDirection();
            Axis compareAxis = facing.getClockWise().getAxis();
            Comparator<BlockPos> invOrdering = (p1, p2) -> {
               int compareY = -Integer.compare(p1.getY(), p2.getY());
               int modifier = axisDirection.getStep() * (compareAxis == Axis.Z ? -1 : 1);
               int c1 = compareAxis.choose(p1.getX(), p1.getY(), p1.getZ());
               int c2 = compareAxis.choose(p2.getX(), p2.getY(), p2.getZ());
               return compareY != 0 ? compareY : modifier * Integer.compare(c1, c2);
            };
            return this.data
               .stream()
               .sorted(invOrdering)
               .map(l -> CrafterHelper.getCrafter(world, pos.offset(l)))
               .filter(Objects::nonNull)
               .map(MechanicalCrafterBlockEntity::getInventory)
               .collect(Collectors.toList());
         }
      }

      public void write(CompoundTag nbt) {
         nbt.putBoolean("Controller", this.isController);
         ListTag list = new ListTag();
         this.data.forEach(pos -> {
            CompoundTag data = new CompoundTag();
            data.putInt("X", pos.getX());
            data.putInt("Y", pos.getY());
            data.putInt("Z", pos.getZ());
            list.add(data);
         });
         nbt.put("Data", list);
      }

      public void read(CompoundTag nbt) {
         this.isController = nbt.getBoolean("Controller");
         this.data = NBTHelper.readCompoundList(nbt.getList("Data", 10), c -> new BlockPos(c.getInt("X"), c.getInt("Y"), c.getInt("Z")));
         if (this.data.isEmpty()) {
            this.isController = true;
            this.data.add(BlockPos.ZERO);
         }
      }
   }
}

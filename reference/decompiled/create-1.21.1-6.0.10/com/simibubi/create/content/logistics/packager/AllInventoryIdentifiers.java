package com.simibubi.create.content.logistics.packager;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.packager.InventoryIdentifier;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.Property;

public class AllInventoryIdentifiers {
   public static void registerDefaults() {
      InventoryIdentifier.REGISTRY
         .registerProvider(SimpleRegistry.Provider.forBlockTag(AllTags.AllBlockTags.SINGLE_BLOCK_INVENTORIES.tag, AllInventoryIdentifiers::single));
      InventoryIdentifier.REGISTRY.registerProvider(block -> {
         Collection<Property<?>> properties = block.getStateDefinition().getProperties();
         return properties.contains(ChestBlock.TYPE) && properties.contains(ChestBlock.FACING) ? AllInventoryIdentifiers::chest : null;
      });
      InventoryIdentifier.REGISTRY.registerProvider(block -> block instanceof WorldlyContainerHolder ? AllInventoryIdentifiers::worldlyContainerBlock : null);
      InventoryIdentifier.REGISTRY
         .register(
            (Block)AllBlocks.ITEM_VAULT.get(),
            (level, state, face) -> level.getBlockEntity(face.getPos()) instanceof ItemVaultBlockEntity vault ? vault.getInvId() : null
         );
   }

   private static InventoryIdentifier single(Level level, BlockState state, BlockFace face) {
      return new InventoryIdentifier.Single(face.getPos());
   }

   private static InventoryIdentifier chest(Level level, BlockState state, BlockFace face) {
      ChestType type = (ChestType)state.getValue(ChestBlock.TYPE);
      if (type != ChestType.SINGLE) {
         Direction toOther = ChestBlock.getConnectedDirection(state);
         BlockPos otherPos = face.getPos().relative(toOther);
         BlockState otherState = level.getBlockState(otherPos);
         if (otherState.is(state.getBlock()) && ChestBlock.getConnectedDirection(otherState) == toOther.getOpposite()) {
            return new InventoryIdentifier.Pair(face.getPos(), otherPos);
         }
      }

      return new InventoryIdentifier.Single(face.getPos());
   }

   private static InventoryIdentifier worldlyContainerBlock(Level level, BlockState state, BlockFace face) {
      WorldlyContainerHolder holder = (WorldlyContainerHolder)state.getBlock();
      WorldlyContainer container = holder.getContainer(state, level, face.getPos());
      return ofWorldlyContainer(container, face);
   }

   private static InventoryIdentifier ofWorldlyContainer(WorldlyContainer container, BlockFace face) {
      Direction side = face.getFace();
      int[] slots = container.getSlotsForFace(side);
      Set<Direction> directions = EnumSet.of(side);

      for (Direction direction : Iterate.directions) {
         if (direction != side) {
            int[] faceSlots = container.getSlotsForFace(direction);
            if (Arrays.equals(slots, faceSlots)) {
               directions.add(direction);
            }
         }
      }

      return new InventoryIdentifier.MultiFace(face.getPos(), directions);
   }

   public static InventoryIdentifier fallback(Level level, BlockState state, BlockFace face) {
      return level.getBlockEntity(face.getPos()) instanceof WorldlyContainer container ? ofWorldlyContainer(container, face) : null;
   }
}

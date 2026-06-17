package com.simibubi.create.api.packager;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.logistics.packager.AllInventoryIdentifiers;
import java.util.Set;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface InventoryIdentifier {
   SimpleRegistry<Block, InventoryIdentifier.Finder> REGISTRY = SimpleRegistry.create();

   boolean contains(BlockFace var1);

   @Nullable
   static InventoryIdentifier get(Level level, BlockFace face) {
      BlockState state = level.getBlockState(face.getPos());
      InventoryIdentifier.Finder finder = REGISTRY.get(state);
      InventoryIdentifier.Finder toQuery = finder != null ? finder : AllInventoryIdentifiers::fallback;
      return toQuery.find(level, state, face);
   }

   public static record Bounds(BoundingBox bounds) implements InventoryIdentifier {
      @Override
      public boolean contains(BlockFace face) {
         return this.bounds.isInside(face.getPos());
      }
   }

   @FunctionalInterface
   public interface Finder {
      @Nullable
      InventoryIdentifier find(Level var1, BlockState var2, BlockFace var3);
   }

   public static record MultiFace(BlockPos pos, Set<Direction> sides) implements InventoryIdentifier {
      @Override
      public boolean contains(BlockFace face) {
         return this.pos.equals(face.getPos()) && this.sides.contains(face.getFace());
      }
   }

   public static record Pair(BlockPos first, BlockPos second) implements InventoryIdentifier {
      public Pair(BlockPos first, BlockPos second) {
         boolean isFirstLower = first.compareTo(second) < 0;
         this.first = isFirstLower ? first : second;
         this.second = isFirstLower ? second : first;
      }

      @Override
      public boolean contains(BlockFace face) {
         BlockPos pos = face.getPos();
         return this.first.equals(pos) || this.second.equals(pos);
      }
   }

   public static record Single(BlockPos pos) implements InventoryIdentifier {
      @Override
      public boolean contains(BlockFace face) {
         return this.pos.equals(face.getPos());
      }
   }
}

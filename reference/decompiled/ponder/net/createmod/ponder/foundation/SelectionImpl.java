package net.createmod.ponder.foundation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class SelectionImpl {
   public static Selection of(BoundingBox bb) {
      return new SelectionImpl.Simple(bb);
   }

   private static class Compound implements Selection {
      private final Set<BlockPos> posSet;
      @Nullable
      private Vec3 center;

      public Compound(SelectionImpl.Simple initial) {
         this.posSet = new HashSet<>();
         this.add(initial);
      }

      private Compound(Set<BlockPos> template) {
         this.posSet = new HashSet<>(template);
      }

      public boolean test(BlockPos t) {
         return this.posSet.contains(t);
      }

      @Override
      public Selection add(Selection other) {
         other.forEach(p -> this.posSet.add(p.immutable()));
         this.center = null;
         return this;
      }

      @Override
      public Selection substract(Selection other) {
         other.forEach(p -> this.posSet.remove(p.immutable()));
         this.center = null;
         return this;
      }

      @Override
      public Outline.OutlineParams makeOutline(Outliner outliner, Object slot) {
         return outliner.showCluster(slot, this.posSet);
      }

      @Override
      public Vec3 getCenter() {
         return this.center == null ? (this.center = this.evalCenter()) : this.center;
      }

      private Vec3 evalCenter() {
         Vec3 center = Vec3.ZERO;
         if (this.posSet.isEmpty()) {
            return center;
         } else {
            for (BlockPos blockPos : this.posSet) {
               center = center.add(Vec3.atLowerCornerOf(blockPos));
            }

            center = center.scale((double)(1.0F / (float)this.posSet.size()));
            return center.add(new Vec3(0.5, 0.5, 0.5));
         }
      }

      @Override
      public Selection copy() {
         return new SelectionImpl.Compound(this.posSet);
      }

      @Override
      public Iterator<BlockPos> iterator() {
         return this.posSet.iterator();
      }
   }

   private static class Simple implements Selection {
      private final BoundingBox bb;
      private final AABB aabb;
      private final Iterable<BlockPos> iterable;

      public Simple(BoundingBox bb) {
         this.bb = bb;
         this.aabb = new AABB(
            (double)bb.minX(), (double)bb.minY(), (double)bb.minZ(), (double)(bb.maxX() + 1), (double)(bb.maxY() + 1), (double)(bb.maxZ() + 1)
         );
         this.iterable = BlockPos.betweenClosed(
            Math.min(bb.minX(), bb.maxX()),
            Math.min(bb.minY(), bb.maxY()),
            Math.min(bb.minZ(), bb.maxZ()),
            Math.max(bb.minX(), bb.maxX()),
            Math.max(bb.minY(), bb.maxY()),
            Math.max(bb.minZ(), bb.maxZ())
         );
      }

      public boolean test(BlockPos t) {
         return this.bb.isInside(t);
      }

      @Override
      public Selection add(Selection other) {
         return new SelectionImpl.Compound(this).add(other);
      }

      @Override
      public Selection substract(Selection other) {
         return new SelectionImpl.Compound(this).substract(other);
      }

      @Override
      public Vec3 getCenter() {
         return this.aabb.getCenter();
      }

      @Override
      public Outline.OutlineParams makeOutline(Outliner outliner, Object slot) {
         return outliner.showAABB(slot, this.aabb);
      }

      @Override
      public Selection copy() {
         return new SelectionImpl.Simple(new BoundingBox(this.bb.minX(), this.bb.minY(), this.bb.minZ(), this.bb.maxX(), this.bb.maxY(), this.bb.maxZ()));
      }

      @Override
      public Iterator<BlockPos> iterator() {
         return this.iterable.iterator();
      }
   }
}

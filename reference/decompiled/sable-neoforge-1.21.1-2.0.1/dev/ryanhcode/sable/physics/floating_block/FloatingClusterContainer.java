package dev.ryanhcode.sable.physics.floating_block;

import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class FloatingClusterContainer {
   public List<FloatingBlockCluster> clusters = new ArrayList<>();
   private final Long2ObjectMap<BlockState> addedBlocks = new Long2ObjectOpenHashMap();
   private final Long2ObjectMap<BlockState> removedBlocks = new Long2ObjectOpenHashMap();
   public final Vector3d positionOffset = new Vector3d();
   public final Quaterniond rotationOffset = new Quaterniond();
   public final Vector3d velocity = new Vector3d();
   public final Vector3d angularVelocity = new Vector3d();

   public boolean needsTicking() {
      return !this.addedBlocks.isEmpty() || !this.clusters.isEmpty();
   }

   public void processBlockChanges(Vector3dc centerOfMass) {
      ObjectIterator var2 = this.removedBlocks.entrySet().iterator();

      while (var2.hasNext()) {
         Entry<Long, BlockState> entry = (Entry<Long, BlockState>)var2.next();
         BlockPos blockPos = BlockPos.of(entry.getKey());
         Vector3d pos = new Vector3d((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5).sub(centerOfMass);
         this.removeFloatingBlock(entry.getValue(), pos);
      }

      var2 = this.addedBlocks.entrySet().iterator();

      while (var2.hasNext()) {
         Entry<Long, BlockState> entry = (Entry<Long, BlockState>)var2.next();
         BlockPos blockPos = BlockPos.of(entry.getKey());
         Vector3d pos = new Vector3d((double)blockPos.getX() + 0.5, (double)blockPos.getY() + 0.5, (double)blockPos.getZ() + 0.5).sub(centerOfMass);
         this.addFloatingBlock(entry.getValue(), pos);
      }

      this.addedBlocks.clear();
      this.removedBlocks.clear();
   }

   public void addFloatingBlock(BlockState state, Vector3d pos) {
      FloatingBlockMaterial material = PhysicsBlockPropertyHelper.getFloatingMaterial(state);

      assert material != null : "Floating Material desync on adding";

      FloatingBlockCluster foundCluster = null;

      for (FloatingBlockCluster cluster : this.clusters) {
         if (cluster.getMaterial().equals(material)) {
            foundCluster = cluster;
            break;
         }
      }

      if (foundCluster == null) {
         foundCluster = new FloatingBlockCluster(material);
         this.clusters.add(foundCluster);
      }

      double scale = PhysicsBlockPropertyHelper.getFloatingScale(state);
      foundCluster.getBlockData().addFloatingBlock(pos, scale);
   }

   public void removeFloatingBlock(BlockState state, Vector3d pos) {
      FloatingBlockMaterial material = PhysicsBlockPropertyHelper.getFloatingMaterial(state);

      assert material != null : "Floating Material desync on removing";

      FloatingBlockCluster foundCluster = null;

      for (FloatingBlockCluster cluster : this.clusters) {
         if (cluster.getMaterial().equals(material)) {
            foundCluster = cluster;
            break;
         }
      }

      if (foundCluster != null) {
         double scale = PhysicsBlockPropertyHelper.getFloatingScale(state);
         foundCluster.getBlockData().removeFloatingBlock(pos, scale);
         if (foundCluster.getBlockData().blockCount == 0) {
            this.clusters.remove(foundCluster);
         }
      }
   }

   public void queueAddFloatingBlock(BlockState state, BlockPos pos) {
      long longKey = pos.asLong();
      if (PhysicsBlockPropertyHelper.getFloatingMaterial(state) != null && !this.removedBlocks.remove(longKey, state)) {
         this.addedBlocks.put(longKey, state);
      }
   }

   public void queueRemoveFloatingBlock(BlockState state, BlockPos pos) {
      long longKey = pos.asLong();
      if (PhysicsBlockPropertyHelper.getFloatingMaterial(state) != null && !this.addedBlocks.remove(longKey, state)) {
         this.removedBlocks.put(longKey, state);
      }
   }
}

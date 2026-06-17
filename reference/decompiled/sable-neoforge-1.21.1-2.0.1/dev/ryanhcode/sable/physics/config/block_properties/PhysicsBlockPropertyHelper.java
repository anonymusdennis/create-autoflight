package dev.ryanhcode.sable.physics.config.block_properties;

import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PhysicsBlockPropertyHelper {
   public static double getMass(BlockGetter level, BlockPos pos, BlockState state) {
      boolean solid = VoxelNeighborhoodState.isSolid(level, pos, state);
      return !solid
         ? 0.0
         : ((BlockStateExtension)state)
            .<Double>sable$getProperty((PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>)PhysicsBlockPropertyTypes.MASS.get());
   }

   @Nullable
   public static Vec3 getInertia(BlockGetter level, BlockPos pos, BlockState state) {
      boolean solid = VoxelNeighborhoodState.isSolid(level, pos, state);
      return !solid
         ? null
         : ((BlockStateExtension)state).sable$getProperty((PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Vec3>)PhysicsBlockPropertyTypes.INERTIA.get());
   }

   public static double getFriction(BlockState state) {
      return ((BlockStateExtension)state)
         .<Double>sable$getProperty((PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>)PhysicsBlockPropertyTypes.FRICTION.get());
   }

   public static double getVolume(BlockState state) {
      return ((BlockStateExtension)state)
         .<Double>sable$getProperty((PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>)PhysicsBlockPropertyTypes.VOLUME.get());
   }

   public static double getRestitution(BlockState state) {
      return ((BlockStateExtension)state)
         .<Double>sable$getProperty((PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>)PhysicsBlockPropertyTypes.RESTITUTION.get());
   }

   public static double getFloatingScale(BlockState state) {
      return ((BlockStateExtension)state)
         .<Double>sable$getProperty((PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>)PhysicsBlockPropertyTypes.FLOATING_SCALE.get());
   }

   public static FloatingBlockMaterial getFloatingMaterial(BlockState state) {
      ResourceLocation location = ((BlockStateExtension)state)
         .sable$getProperty((PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<ResourceLocation>)PhysicsBlockPropertyTypes.FLOATING_MATERIAL.get());
      return location == null ? null : FloatingBlockMaterialDataHandler.allMaterials.get(location);
   }
}

package dev.eriksonn.aeronautics.index;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint;
import dev.eriksonn.aeronautics.Aeronautics;
import dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class AeroArmInteractionPoints {
   private static <T extends ArmInteractionPointType> void register(String name, T type) {
      Registry.register(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, Aeronautics.path(name), type);
   }

   public static void init() {
   }

   static {
      register("mounted_potato_cannon_point", new AeroArmInteractionPoints.MountedPotatoCannonType());
   }

   public static class MountedPotatoCannonPoint extends DepositOnlyArmInteractionPoint {
      public MountedPotatoCannonPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
         return this.cachedState.hasBlockEntity() && this.level.getBlockEntity(this.pos) instanceof MountedPotatoCannonBlockEntity sbe
            ? sbe.getInventory().insertSlot(stack, 0, simulate)
            : super.insert(armBlockEntity, stack, simulate);
      }
   }

   public static class MountedPotatoCannonType extends ArmInteractionPointType {
      public boolean canCreatePoint(Level var1, BlockPos var2, BlockState var3) {
         return AeroBlocks.MOUNTED_POTATO_CANNON.has(var1.getBlockState(var2));
      }

      @Nullable
      public ArmInteractionPoint createPoint(Level var1, BlockPos var2, BlockState var3) {
         return new AeroArmInteractionPoints.MountedPotatoCannonPoint(this, var1, var2, var3);
      }
   }
}

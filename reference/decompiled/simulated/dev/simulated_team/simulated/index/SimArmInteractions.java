package dev.simulated_team.simulated.index;

import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.DepotPoint;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SimArmInteractions {
   private static <T extends ArmInteractionPointType> void register(String name, T type) {
      Registry.register(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, Simulated.path(name), type);
   }

   public static void init() {
   }

   static {
      register("portable_engine", new SimArmInteractions.PortableEngineType());
      register("navigation_table", new SimArmInteractions.NavTableType());
   }

   public static class NavTablePoint extends DepotPoint {
      public NavTablePoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }
   }

   public static class NavTableType extends ArmInteractionPointType {
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return SimBlocks.NAVIGATION_TABLE.has(state);
      }

      @Nullable
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new SimArmInteractions.NavTablePoint(this, level, pos, state);
      }
   }

   public static class PortableEngineInteractionPoint extends DepotPoint {
      public PortableEngineInteractionPoint(ArmInteractionPointType type, Level level, BlockPos pos, BlockState state) {
         super(type, level, pos, state);
      }

      public ItemStack insert(ArmBlockEntity armBlockEntity, ItemStack stack, boolean simulate) {
         return this.cachedState.hasBlockEntity() && this.level.getBlockEntity(this.pos) instanceof PortableEngineBlockEntity sbe
            ? sbe.inventory.insertSlot(stack, 0, simulate)
            : super.insert(armBlockEntity, stack, simulate);
      }
   }

   public static class PortableEngineType extends ArmInteractionPointType {
      public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
         return SimBlocks.PORTABLE_ENGINES.contains(state.getBlock());
      }

      @Nullable
      public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
         return new SimArmInteractions.PortableEngineInteractionPoint(this, level, pos, state);
      }
   }
}

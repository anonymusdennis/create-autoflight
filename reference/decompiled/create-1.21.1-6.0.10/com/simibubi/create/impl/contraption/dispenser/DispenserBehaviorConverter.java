package com.simibubi.create.impl.contraption.dispenser;

import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.contraption.dispenser.DefaultMountedDispenseBehavior;
import com.simibubi.create.api.contraption.dispenser.MountedDispenseBehavior;
import com.simibubi.create.api.contraption.dispenser.MountedProjectileDispenseBehavior;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.foundation.mixin.accessor.DispenserBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public enum DispenserBehaviorConverter implements SimpleRegistry.Provider<Item, MountedDispenseBehavior> {
   INSTANCE;

   @Nullable
   public MountedDispenseBehavior get(Item item) {
      DispenseItemBehavior vanilla = getDispenseMethod(new ItemStack(item));
      if (vanilla == null) {
         return null;
      } else if (vanilla.getClass() == DefaultDispenseItemBehavior.class) {
         return null;
      } else if (AllTags.AllItemTags.DISPENSE_BEHAVIOR_WRAP_BLACKLIST.matches(item)) {
         return null;
      } else {
         return (MountedDispenseBehavior)(vanilla instanceof ProjectileDispenseBehavior projectile
            ? MountedProjectileDispenseBehavior.of(projectile)
            : new DispenserBehaviorConverter.FallbackBehavior(item, vanilla));
      }
   }

   @Override
   public void onRegister(Runnable invalidate) {
      NeoForge.EVENT_BUS.addListener(event -> {
         if (event.shouldUpdateStaticData()) {
            invalidate.run();
         }
      });
   }

   @Nullable
   private static DispenseItemBehavior getDispenseMethod(ItemStack stack) {
      MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
      return server == null ? null : ((DispenserBlockAccessor)Blocks.DISPENSER).create$callGetDispenseMethod(server.getLevel(Level.OVERWORLD), stack);
   }

   private static final class FallbackBehavior extends DefaultMountedDispenseBehavior {
      private final Item item;
      private final DispenseItemBehavior wrapped;
      private boolean hasErrored;

      private FallbackBehavior(Item item, DispenseItemBehavior wrapped) {
         this.item = item;
         this.wrapped = wrapped;
      }

      @Override
      protected ItemStack execute(ItemStack stack, MovementContext context, BlockPos pos, Vec3 facing) {
         if (this.hasErrored) {
            return stack;
         } else {
            MinecraftServer server = context.world.getServer();
            ServerLevel serverLevel = server != null ? server.getLevel(context.world.dimension()) : null;
            Direction nearestFacing = MountedDispenseBehavior.getClosestFacingDirection(facing);
            BlockState state = context.state;
            if (state.hasProperty(BlockStateProperties.FACING)) {
               state = (BlockState)state.setValue(BlockStateProperties.FACING, nearestFacing);
            }

            BlockSource source = new BlockSource(serverLevel, pos, state, null);

            try {
               return this.wrapped.dispense(source, stack.copy());
            } catch (NullPointerException var13) {
               ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(this.item);
               String message = "Error dispensing item '" + itemId + "' from contraption, not doing that anymore";
               Create.LOGGER.error(message, var13);
               this.hasErrored = true;
               return stack;
            }
         }
      }
   }
}

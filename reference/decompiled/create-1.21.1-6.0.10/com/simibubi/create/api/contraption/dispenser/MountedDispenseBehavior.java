package com.simibubi.create.api.contraption.dispenser;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.impl.contraption.dispenser.DispenserBehaviorConverter;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;

@FunctionalInterface
public interface MountedDispenseBehavior {
   SimpleRegistry<Item, MountedDispenseBehavior> REGISTRY = (SimpleRegistry<Item, MountedDispenseBehavior>)Util.make(() -> {
      SimpleRegistry<Item, MountedDispenseBehavior> registry = SimpleRegistry.create();
      registry.registerProvider(DispenserBehaviorConverter.INSTANCE);
      return registry;
   });

   ItemStack dispense(ItemStack var1, MovementContext var2, BlockPos var3);

   static Vec3 getDispenserNormal(MovementContext ctx) {
      Direction facing = (Direction)ctx.state.getValue(DispenserBlock.FACING);
      Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
      return ctx.rotation.apply(normal).normalize();
   }

   static Direction getClosestFacingDirection(Vec3 facing) {
      return Direction.getNearest(facing.x, facing.y, facing.z);
   }

   static void placeItemInInventory(ItemStack stack, MovementContext context, BlockPos pos) {
      ItemStack toInsert = stack.copy();
      ItemStack remainder = ItemHandlerHelper.insertItem(context.getItemStorage(), toInsert, false);
      if (!remainder.isEmpty()) {
         CombinedInvWrapper contraption = context.contraption.getStorage().getAllItems();
         ItemStack newRemainder = ItemHandlerHelper.insertItem(contraption, remainder, false);
         if (!newRemainder.isEmpty()) {
            DefaultMountedDispenseBehavior.INSTANCE.dispense(remainder, context, pos);
         }
      }
   }
}

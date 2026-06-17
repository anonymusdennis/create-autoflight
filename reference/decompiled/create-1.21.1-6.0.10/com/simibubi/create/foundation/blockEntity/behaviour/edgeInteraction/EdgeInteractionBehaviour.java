package com.simibubi.create.foundation.blockEntity.behaviour.edgeInteraction;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class EdgeInteractionBehaviour extends BlockEntityBehaviour {
   public static final BehaviourType<EdgeInteractionBehaviour> TYPE = new BehaviourType<>();
   EdgeInteractionBehaviour.ConnectionCallback connectionCallback;
   EdgeInteractionBehaviour.ConnectivityPredicate connectivityPredicate;
   Predicate<Item> requiredItem;

   public EdgeInteractionBehaviour(SmartBlockEntity be, EdgeInteractionBehaviour.ConnectionCallback callback) {
      super(be);
      this.connectionCallback = callback;
      this.requiredItem = item -> true;
      this.connectivityPredicate = (world, pos, face, face2) -> true;
   }

   public EdgeInteractionBehaviour connectivity(EdgeInteractionBehaviour.ConnectivityPredicate pred) {
      this.connectivityPredicate = pred;
      return this;
   }

   public EdgeInteractionBehaviour require(Item required) {
      return this.require((Predicate<Item>)(item -> item == required));
   }

   public EdgeInteractionBehaviour require(Predicate<Item> predicate) {
      this.requiredItem = predicate;
      return this;
   }

   @Override
   public BehaviourType<?> getType() {
      return TYPE;
   }

   @FunctionalInterface
   public interface ConnectionCallback {
      void apply(Level var1, BlockPos var2, BlockPos var3);
   }

   @FunctionalInterface
   public interface ConnectivityPredicate {
      boolean test(Level var1, BlockPos var2, Direction var3, Direction var4);
   }
}

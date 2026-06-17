package dev.simulated_team.simulated.util.click_interactions;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public interface InteractCallback {
   @NotNull
   static InteractCallback.Result filterInteract(
      InteractCallback clickInteraction, InteractCallback.Input input, int modifiers, int action, InteractCallback.KeyMappings associatedMappings
   ) {
      if (input.matches(associatedMappings.attack)) {
         return clickInteraction.onAttack(modifiers, action, associatedMappings.attack);
      } else if (input.matches(associatedMappings.middle)) {
         return clickInteraction.onPick(modifiers, action, associatedMappings.middle);
      } else {
         return input.matches(associatedMappings.use) ? clickInteraction.onUse(modifiers, action, associatedMappings.use) : InteractCallback.Result.empty();
      }
   }

   default InteractCallback.Result onPick(int modifiers, int action, KeyMapping middleKey) {
      return InteractCallback.Result.empty();
   }

   default InteractCallback.Result onAttack(int modifiers, int action, KeyMapping leftKey) {
      return InteractCallback.Result.empty();
   }

   default InteractCallback.Result onUse(int modifiers, int action, KeyMapping rightKey) {
      return InteractCallback.Result.empty();
   }

   default InteractCallback.Result onScroll(double deltaX, double deltaY) {
      return InteractCallback.Result.empty();
   }

   default InteractCallback.Result onMouseMove(double yaw, double pitch) {
      return InteractCallback.Result.empty();
   }

   default void clientTick(Level level, LocalPlayer player) {
   }

   public static record Input(boolean mouse, int key, int scanCode) {
      public static InteractCallback.Input mouse(int key) {
         return new InteractCallback.Input(true, key, -1);
      }

      public static InteractCallback.Input key(int key, int scanCode) {
         return new InteractCallback.Input(false, key, scanCode);
      }

      public boolean matches(KeyMapping mapping) {
         return this.mouse ? mapping.matchesMouse(this.key) : mapping.matches(this.key, this.scanCode);
      }
   }

   public static record KeyMappings(KeyMapping use, KeyMapping attack, KeyMapping middle) {
      private static final InteractCallback.KeyMappings MAPPINGS = populateMappings();

      public static InteractCallback.KeyMappings getMappings() {
         return MAPPINGS;
      }

      private static InteractCallback.KeyMappings populateMappings() {
         Options options = Minecraft.getInstance().options;
         return new InteractCallback.KeyMappings(options.keyUse, options.keyAttack, options.keyPickItem);
      }
   }

   public static record Result(boolean cancelled) {
      private static final InteractCallback.Result EMPTY = new InteractCallback.Result(false);

      public static InteractCallback.Result empty() {
         return EMPTY;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj.getClass() != this.getClass()) {
            return false;
         } else {
            InteractCallback.Result otherEvent = (InteractCallback.Result)obj;
            return otherEvent.cancelled == this.cancelled;
         }
      }
   }
}

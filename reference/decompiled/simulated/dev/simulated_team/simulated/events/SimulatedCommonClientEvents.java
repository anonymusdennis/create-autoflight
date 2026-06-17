package dev.simulated_team.simulated.events;

import dev.ryanhcode.sable.util.SableDistUtil;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.client.BlockPropertiesTooltip;
import dev.simulated_team.simulated.content.blocks.redstone.linked_typewriter.LinkedTypewriterInteractionHandler;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ZiplineClientManager;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverClientGripHandler;
import dev.simulated_team.simulated.content.end_sea.EndSeaRenderer;
import dev.simulated_team.simulated.content.items.rope.RopeItem.ClientRopeItemHandler;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffRenderHandler;
import dev.simulated_team.simulated.index.SimClickInteractions;
import dev.simulated_team.simulated.index.SimKeys;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager;
import dev.simulated_team.simulated.util.hold_interaction.HoldTipManager;
import foundry.veil.api.client.render.MatrixStack;
import foundry.veil.api.event.VeilRenderLevelStageEvent.Stage;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;

public class SimulatedCommonClientEvents {
   public static void onAfterMouseInput(int button, int modifiers, int action) {
   }

   public static InteractCallback.Result onBeforeMouseInput(InteractCallback.Input input, int modifiers, int action) {
      Minecraft mc = Minecraft.getInstance();
      InteractCallback.KeyMappings mappings = InteractCallback.KeyMappings.getMappings();
      if (mc.screen != null) {
         return InteractCallback.Result.empty();
      } else {
         for (InteractCallback interactCallback : SimClickInteractions.CLICK_INTERACTION_ENTRIES) {
            InteractCallback.Result returnEvent = InteractCallback.filterInteract(interactCallback, input, modifiers, action, mappings);
            if (!InteractCallback.Result.empty().equals(returnEvent)) {
               return returnEvent;
            }
         }

         return InteractCallback.Result.empty();
      }
   }

   public static InteractCallback.Result onMouseMove(double yaw, double pitch) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.screen != null) {
         return InteractCallback.Result.empty();
      } else {
         for (InteractCallback interactCallback : SimClickInteractions.CLICK_INTERACTION_ENTRIES) {
            InteractCallback.Result returnEvent = interactCallback.onMouseMove(yaw, pitch);
            if (!InteractCallback.Result.empty().equals(returnEvent)) {
               return returnEvent;
            }
         }

         return InteractCallback.Result.empty();
      }
   }

   public static void onRenderLevelStage(
      Stage stage,
      LevelRenderer levelRenderer,
      BufferSource bufferSource,
      MatrixStack matrixStack,
      Matrix4fc matrix4fc,
      Matrix4fc matrix4fc1,
      int i,
      DeltaTracker deltaTracker,
      Camera camera,
      Frustum frustum
   ) {
      PhysicsStaffRenderHandler.renderSelectionBox(stage, levelRenderer, bufferSource, matrixStack, matrix4fc, matrix4fc1, i, deltaTracker, camera, frustum);
   }

   public static void onAfterKeyPress(int key, int scanCode, int action, int modifiers) {
      LinkedTypewriterInteractionHandler.onKeyPress(key, scanCode, action, modifiers);
   }

   public static InteractCallback.Result onMouseScroll(double deltaX, double deltaY) {
      if (Minecraft.getInstance().screen == null) {
         for (InteractCallback interactCallback : SimClickInteractions.CLICK_INTERACTION_ENTRIES) {
            InteractCallback.Result result = interactCallback.onScroll(deltaX, deltaY);
            if (!InteractCallback.Result.empty().equals(result)) {
               return result;
            }
         }
      }

      return InteractCallback.Result.empty();
   }

   public static void renderOverlays(GuiGraphics graphics, float pt) {
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      HoldInteractionManager.renderOverlay(graphics, width, height);
   }

   public static void preClientTick(Minecraft instance) {
      ThrottleLeverClientGripHandler.clearNearbyThrottleLevers();
      double delta = 0.0;
      if (SimKeys.SCROLL_UP.isPressed()) {
         delta++;
      }

      if (SimKeys.SCROLL_DOWN.isPressed()) {
         delta--;
      }

      if (delta != 0.0) {
         onMouseScroll(0.0, delta);
      }
   }

   public static void postClientTick(Minecraft instance) {
      SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.tick();
      if (instance.player != null && instance.level != null) {
         Level level = SableDistUtil.getClientLevel();
         LocalPlayer player = (LocalPlayer)SimDistUtil.getClientPlayer();
         SimulatedClient.MERGING_GLUE_ITEM_HANDLER.clientTick(level, player);
         ClientRopeItemHandler.tick();
         ZiplineClientManager.tick();
         LinkedTypewriterInteractionHandler.tick();
         if (instance.screen != null) {
            HoldInteractionManager.stop();
         }

         for (InteractCallback interactCallback : SimClickInteractions.CLICK_INTERACTION_ENTRIES) {
            interactCallback.clientTick(level, player);
         }

         HoldInteractionManager.tick(level, player);
         HoldTipManager.tick();
         EndSeaRenderer.tick();
         SimulatedClient.PLUNGER_LAUNCHER_RENDER_HANDLER.tick();
      }
   }

   @Nullable
   public static InteractionResult onRightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitResult) {
      return HoldInteractionManager.isActive() ? InteractionResult.FAIL : null;
   }

   public static void appendTooltip(ItemStack stack, TooltipFlag iTooltipFlag, @Nullable Player player, List<Component> itemTooltip) {
      BlockPropertiesTooltip.Condition propertiesCondition = (BlockPropertiesTooltip.Condition)SimConfigService.INSTANCE
         .client()
         .itemConfig
         .displayProperties
         .get();
      if (BlockPropertiesTooltip.shouldShowTooltip(propertiesCondition, iTooltipFlag, player)) {
         BlockPropertiesTooltip.appendTooltip(stack, iTooltipFlag, player, itemTooltip);
      }
   }

   public static void useItemOnAirEvent(Level level, Player player, ItemStack itemStack, InteractionHand hand) {
      SimulatedClient.MERGING_GLUE_ITEM_HANDLER.resetWhenShiftRC(player, itemStack);
   }

   public static boolean useItemMappingTriggered() {
      return HoldInteractionManager.isActive();
   }

   public static boolean useItemOnBlockEvent(Level level, Player player, ItemStack itemStack, InteractionHand hand) {
      return SimulatedClient.MERGING_GLUE_ITEM_HANDLER.onItemUseBlock(level, player, itemStack, hand);
   }
}

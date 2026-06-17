package dev.eriksonn.aeronautics.api.levitite_blend_crystallization;

import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.eriksonn.aeronautics.network.packets.LevititeCatalystCrystallizationPacket;
import dev.eriksonn.aeronautics.util.CatalyzerHelper;
import dev.simulated_team.simulated.util.SimDistUtil;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback;
import dev.simulated_team.simulated.util.click_interactions.InteractCallback.Result;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;

public class LevititeClientCatalyzerHandler implements InteractCallback {
   @NotNull
   private static ClipContext gatherContext(Player player) {
      Vec3 origin = player.getEyePosition();
      Vec3 target = RaycastHelper.getTraceTarget(player, player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), origin);
      return new ClipContext(origin, target, Block.COLLIDER, Fluid.SOURCE_ONLY, player);
   }

   public Result onUse(int modifiers, int action, KeyMapping rightKey) {
      if (action == 1) {
         LocalPlayer player = (LocalPlayer)SimDistUtil.getClientPlayer();
         Level level = player.level();
         InteractionHand hand = InteractionHand.MAIN_HAND;
         ItemStack catalyzer = player.getItemInHand(hand);
         if (!CatalyzerHelper.isCatalyzer(catalyzer)) {
            return Result.empty();
         }

         ClipContext context = gatherContext(player);
         BlockHitResult ray = level.clip(context);
         if (ray.getType() != Type.MISS && level.getFluidState(ray.getBlockPos()).getType() == LevititeBlendHelper.getFluid()) {
            VeilPacketManager.server().sendPacket(new CustomPacketPayload[]{new LevititeCatalystCrystallizationPacket(ray.getBlockPos(), hand)});
            player.swing(hand);
            return new Result(true);
         }
      }

      return Result.empty();
   }
}

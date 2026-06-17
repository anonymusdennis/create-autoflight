package dev.simulated_team.simulated.util.hold_interaction;

import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.simulated_team.simulated.content.blocks.behaviour.HoldTipBehaviour;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class HoldTipManager {
   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      HitResult target = mc.hitResult;
      if (target != null && target instanceof BlockHitResult result) {
         ClientLevel world = mc.level;
         BlockPos pos = result.getBlockPos();
         if (world.getBlockEntity(pos) instanceof SmartBlockEntity sbe) {
            for (BlockEntityBehaviour blockEntityBehaviour : sbe.getAllBehaviours()) {
               if (blockEntityBehaviour instanceof HoldTipBehaviour) {
                  HoldTipBehaviour behaviour = (HoldTipBehaviour)blockEntityBehaviour;
                  MutableComponent hoverTip = behaviour.getHoverTip(mc.player, pos, sbe.getBlockState());
                  if (hoverTip != null) {
                     List<MutableComponent> tip = new ArrayList<>();
                     tip.add(Component.literal(""));
                     tip.add(hoverTip);
                     CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
                  }
               }
            }
         }
      }
   }
}

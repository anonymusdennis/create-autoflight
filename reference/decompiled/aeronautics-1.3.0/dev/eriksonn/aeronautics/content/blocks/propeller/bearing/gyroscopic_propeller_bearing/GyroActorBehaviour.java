package dev.eriksonn.aeronautics.content.blocks.propeller.bearing.gyroscopic_propeller_bearing;

import dev.eriksonn.aeronautics.content.blocks.propeller.behaviour.PropellerActorBehaviour;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class GyroActorBehaviour<T extends GyroscopicPropellerBearingBlockEntity> extends PropellerActorBehaviour {
   public GyroActorBehaviour(T be) {
      super(be, be);
   }

   @Override
   public void additionalTooltipInfo(List<Component> tooltip, boolean isPlayerSneaking) {
      double gravStrength = DimensionPhysicsData.getGravity(this.getWorld(), JOMLConversion.toJOML(this.getPos().getCenter())).length();
      MutableComponent canLiftComponent = AeroLang.kilopixelGram(Math.abs(this.propeller.getScaledThrust()) / gravStrength)
         .style(ChatFormatting.AQUA)
         .component();
      AeroLang.translate("propeller.can_lift", canLiftComponent).style(ChatFormatting.GRAY).forGoggles(tooltip, 1);
   }
}

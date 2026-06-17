package dev.simulated_team.simulated.util.extra_kinetics;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.simulated_team.simulated.data.SimLang;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

public interface ExtraKinetics {
   @Nullable
   KineticBlockEntity getExtraKinetics();

   boolean shouldConnectExtraKinetics();

   default String getExtraKineticsSaveName() {
      return "DEFAULT";
   }

   @FunctionalInterface
   public interface ExtraKineticsBlock {
      IRotate getExtraKineticsRotationConfiguration();
   }

   public interface ExtraKineticsBlockEntity {
      KineticBlockEntity getParentBlockEntity();

      default Component getKey() {
         return SimLang.translate("extra_kinetics.default").component();
      }
   }
}

package dev.eriksonn.aeronautics.mixin.propeller_bearing;

import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider.LiftProviderContext;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({PropellerBearingContraptionEntity.class})
public abstract class PropellerBearingContraptionEntityMixin implements KinematicContraption {
   public Map<BlockPos, LiftProviderContext> sable$liftProviders() {
      return Map.of();
   }
}

package dev.simulated_team.simulated.mixin.accessor;

import com.simibubi.create.content.contraptions.Contraption;
import java.util.List;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({Contraption.class})
public interface ContraptionAccessor {
   @Accessor("superglue")
   List<AABB> getSuperGlue();
}

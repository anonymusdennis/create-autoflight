package dev.engine_room.flywheel.impl.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Deque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({PoseStack.class})
public interface PoseStackAccessor {
   @Accessor("poseStack")
   Deque<Pose> flywheel$getPoseStack();
}

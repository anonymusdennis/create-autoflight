package dev.engine_room.flywheel.impl.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ModelPart.class})
public interface ModelPartAccessor {
   @Accessor("children")
   Map<String, ModelPart> flywheel$children();

   @Invoker("compile")
   void flywheel$compile(Pose var1, VertexConsumer var2, int var3, int var4, int var5);
}

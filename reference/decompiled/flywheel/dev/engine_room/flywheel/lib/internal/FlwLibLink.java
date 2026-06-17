package dev.engine_room.flywheel.lib.internal;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import dev.engine_room.flywheel.api.internal.DependencyInjection;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import java.util.Deque;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.slf4j.Logger;

public interface FlwLibLink {
   FlwLibLink INSTANCE = DependencyInjection.load(FlwLibLink.class, "dev.engine_room.flywheel.impl.FlwLibLinkImpl");

   Logger getLogger();

   PoseTransformStack getPoseTransformStackOf(PoseStack var1);

   Map<String, ModelPart> getModelPartChildren(ModelPart var1);

   void compileModelPart(ModelPart var1, Pose var2, VertexConsumer var3, int var4, int var5, int var6);

   Deque<Pose> getPoseStack(PoseStack var1);

   boolean isIrisLoaded();

   boolean isOptifineInstalled();

   boolean isShaderPackInUse();

   boolean isRenderingShadowPass();
}

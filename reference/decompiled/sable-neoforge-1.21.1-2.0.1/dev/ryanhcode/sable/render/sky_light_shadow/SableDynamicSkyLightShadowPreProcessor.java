package dev.ryanhcode.sable.render.sky_light_shadow;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.Context;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.MinecraftContext;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslNode;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.expression.GlslAssignmentNode;
import io.github.ocelot.glslprocessor.api.node.expression.GlslOperationNode;
import io.github.ocelot.glslprocessor.api.node.expression.GlslAssignmentNode.Operand;
import io.github.ocelot.glslprocessor.api.node.function.GlslFunctionNode;
import io.github.ocelot.glslprocessor.api.node.function.GlslInvokeFunctionNode;
import io.github.ocelot.glslprocessor.api.node.variable.GlslVariableNode;
import java.util.List;
import net.minecraft.client.renderer.RenderType;

public class SableDynamicSkyLightShadowPreProcessor implements ShaderPreProcessor {
   public static final String SAMPLER_NAME = "SableShadowSampler";
   public static final String SHADOW_VOLUME_SIZE_UNIFORM = "SableShadowVolumeSize";
   public static final String ENABLE_UNIFORM = "SableShadowsEnabled";
   public static final String SHADOW_ORIGIN_UNIFORM = "SableShadowOrigin";

   public void modify(Context ctx, GlslTree tree) throws GlslSyntaxException {
      if (SableSkyLightShadows.isEnabled()) {
         if (ctx.isSourceFile()) {
            if (ctx instanceof MinecraftContext minecraftContext) {
               List<RenderType> renderTypes = RenderType.chunkBufferLayers();
               boolean anyMatches = false;

               for (RenderType renderType : renderTypes) {
                  if (ctx.isVertex() && minecraftContext.shaderInstance().equals("rendertype_%s".formatted(renderType.name))) {
                     anyMatches = true;
                  }
               }

               if (anyMatches) {
                  GlslNodeList mainFunctionBody = ((GlslFunctionNode)tree.mainFunction().orElseThrow()).getBody();

                  assert mainFunctionBody != null;

                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform sampler2D %s;".formatted("SableShadowSampler")));
                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float %s;".formatted("SableShadowVolumeSize")));
                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float %s;".formatted("SableShadowsEnabled")));
                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform vec3 %s;".formatted("SableShadowOrigin")));

                  for (int i = 0; i < mainFunctionBody.size(); i++) {
                     if (mainFunctionBody.get(i) instanceof GlslAssignmentNode assignmentNode
                        && assignmentNode.getOperand() == Operand.EQUAL
                        && assignmentNode.getSecond() instanceof GlslOperationNode operationNode
                        && operationNode.getOperand() == io.github.ocelot.glslprocessor.api.node.expression.GlslOperationNode.Operand.MULTIPLY
                        && operationNode.getSecond() instanceof GlslInvokeFunctionNode invokeNode
                        && invokeNode.getHeader() instanceof GlslVariableNode variableNode
                        && variableNode.getName().equals("minecraft_sample_lightmap")) {
                        List<GlslNode> replacementNodes = GlslParser.parseExpressionList(
                           "\n                                    float skyLightScale;\n                                    if (%s > 0.0) {\n                                        float volumeSize = %s;\n                                        vec3 shadowOrigin = %s;\n                                        vec2 shadowUv = ((pos.xz - shadowOrigin.xz) * vec2(1.0, -1.0) + volumeSize) / (volumeSize * 2.0);\n\n                                        float sampleAverage = 0.0;\n                                        int sampleRadius = 3;\n                                        float spacing = 1.0;\n\n                                        for (int i = -sampleRadius; i <= sampleRadius; i++) {\n                                            for (int j = -sampleRadius; j <= sampleRadius; j++) {\n                                                float depthSample = texture(%s, shadowUv + vec2(i, j) * spacing / (volumeSize * 2.0)).r;\n\n                                                // TODO: Pass shadow near plane in\n                                                float depth = 0.5 + depthSample * (volumeSize - 0.5);\n\n                                                float y = shadowOrigin.y - depth;\n\n//                                                pos = Position + ChunkOffset;\n//                                                pos.y = y;\n//                                                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);\n\n                                                if (y >= pos.y) {\n                                                    float strength = max(min((y - pos.y - 2.0) / 15.0, 1.0), 0.0);\n                                                    float scale = (i + j) / float(sampleRadius);\n                                                    sampleAverage += max(1.0 - scale, 0.0) * 0.6 * strength;\n                                                }\n                                            }\n                                        }\n\n                                        sampleAverage /= float((sampleRadius * 2 + 1) * (sampleRadius * 2 + 1));\n                                        skyLightScale = smoothstep(0.0, 1.0, 1.0 - sampleAverage);\n                                    } else {\n                                        skyLightScale = 1.0;\n                                    }\n\n                                    vec2 sableLightModification = vec2(1.0, skyLightScale);\n\n                                    vertexColor = Color * minecraft_sample_lightmap(Sampler2, ivec2(UV2 * sableLightModification));\n\n"
                              .formatted("SableShadowsEnabled", "SableShadowVolumeSize", "SableShadowOrigin", "SableShadowSampler")
                        );
                        mainFunctionBody.set(i, replacementNodes.get(0));

                        for (int j = 1; j < replacementNodes.size(); j++) {
                           mainFunctionBody.add(i + j, replacementNodes.get(j));
                        }
                        break;
                     }
                  }
               }
            }
         }
      }
   }
}

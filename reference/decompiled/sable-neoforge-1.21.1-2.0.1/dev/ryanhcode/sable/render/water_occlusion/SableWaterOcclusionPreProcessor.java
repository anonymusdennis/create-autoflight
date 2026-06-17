package dev.ryanhcode.sable.render.water_occlusion;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.Context;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.MinecraftContext;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.function.GlslFunctionNode;

public class SableWaterOcclusionPreProcessor implements ShaderPreProcessor {
   public static final String CLOSE_SAMPLER_NAME = "SableCloseSampler";
   public static final String FAR_SAMPLER_NAME = "SableFarSampler";
   public static final String ENABLE_UNIFORM = "SableWaterOcclusionEnabled";

   public void modify(Context ctx, GlslTree tree) throws GlslSyntaxException {
      if (WaterOcclusionRenderer.isEnabled()) {
         if (ctx.isSourceFile()) {
            if (ctx instanceof MinecraftContext minecraftContext) {
               if (ctx.isFragment() && minecraftContext.shaderInstance().equals("rendertype_translucent")) {
                  GlslNodeList mainFunctionBody = ((GlslFunctionNode)tree.mainFunction().orElseThrow()).getBody();

                  assert mainFunctionBody != null;

                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform vec2 %s;".formatted("ScreenSize")));
                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform sampler2D %s;".formatted("SableCloseSampler")));
                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform sampler2D %s;".formatted("SableFarSampler")));
                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float %s;".formatted("SableWaterOcclusionEnabled")));
                  mainFunctionBody.add(
                     1,
                     GlslParser.parseExpression(
                        "if(%s > 0.0) {\n    float closeDepth = texture(%s, gl_FragCoord.xy / ScreenSize).r;\n    float farDepth = texture(%s, gl_FragCoord.xy / ScreenSize).r;\n    float waterDepth = gl_FragCoord.z;\n    if (waterDepth > closeDepth && waterDepth < farDepth) { discard; }\n}\n"
                           .formatted("SableWaterOcclusionEnabled", "SableCloseSampler", "SableFarSampler")
                           .trim()
                     )
                  );
               }
            }
         }
      }
   }
}

package dev.simulated_team.simulated.content.end_sea;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.Context;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.MinecraftContext;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.function.GlslFunctionNode;
import java.util.List;
import net.minecraft.client.renderer.RenderType;

public class EndSeaFadeTransformer implements ShaderPreProcessor {
   public void modify(Context ctx, GlslTree tree) throws GlslSyntaxException {
      if (ctx instanceof MinecraftContext minecraftContext) {
         List<RenderType> renderTypes = RenderType.chunkBufferLayers();
         boolean anyMatches = false;

         for (RenderType renderType : renderTypes) {
            if (ctx.isVertex() && minecraftContext.shaderInstance().equals("rendertype_%s".formatted(renderType.name))) {
               anyMatches = true;
            }
         }

         if (anyMatches) {
            tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float EndSeaCameraY;"));
            if (tree.field("NormalMat").isEmpty()) {
               tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform mat3 NormalMat;"));
            }

            renderTypes = ((GlslFunctionNode)tree.mainFunction().orElseThrow()).getBody();
            renderTypes.add(
               GlslParser.parseExpression(
                  "    if (EndSeaCameraY != 0.0) {\n        vertexColor.rgb = mix(vertexColor.rgb, vec3(0.086, 0.078, 0.109) * 2.0, clamp((-(inverse(NormalMat) * (ModelViewMat * vec4(pos, 0.0)).rgb).y - EndSeaCameraY) / 30.0, 0.0, 1.0));\n    }\n"
               )
            );
         }
      }
   }
}

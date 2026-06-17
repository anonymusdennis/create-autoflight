package dev.ryanhcode.sable.render.dynamic_shade;

import foundry.veil.Veil;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.Context;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.IncludeOverloadStrategy;
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
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;
import java.io.IOException;
import java.util.List;
import net.minecraft.client.renderer.RenderType;

public class SableDynamicDirectionalShadingPreProcessor implements ShaderPreProcessor {
   public void modify(Context ctx, GlslTree tree) throws GlslSyntaxException, IOException, LexerException {
      if (SableDynamicDirectionalShading.isEnabled()) {
         if (ctx instanceof MinecraftContext minecraftContext) {
            List<RenderType> renderTypes = RenderType.chunkBufferLayers();
            boolean anyMatches = false;

            for (RenderType renderType : renderTypes) {
               if (ctx.isVertex() && minecraftContext.shaderInstance().equals("rendertype_%s".formatted(renderType.name))) {
                  anyMatches = true;
               }
            }

            if (anyMatches) {
               ctx.include(tree, Veil.veilPath("light"), IncludeOverloadStrategy.SOURCE);
               tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float SableEnableNormalLighting;"));
               tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float SableSkyLightScale;"));
               if (tree.field("NormalMat").isEmpty()) {
                  tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform mat3 NormalMat;"));
               }

               renderTypes = ((GlslFunctionNode)tree.mainFunction().orElseThrow()).getBody();
               renderTypes.add(
                  GlslParser.parseExpression(
                     "vertexColor.rgb *= mix(vec3(1.0), vec3(block_brightness(inverse(NormalMat) * (ModelViewMat * vec4(Normal, 0.0)).xyz)), SableEnableNormalLighting);"
                  )
               );
               GlslNodeList mainFunctionBody = ((GlslFunctionNode)tree.mainFunction().orElseThrow()).getBody();

               assert mainFunctionBody != null;

               for (int i = 0; i < mainFunctionBody.size(); i++) {
                  if (mainFunctionBody.get(i) instanceof GlslAssignmentNode assignmentNode
                     && assignmentNode.getOperand() == Operand.EQUAL
                     && assignmentNode.getSecond() instanceof GlslOperationNode operationNode
                     && operationNode.getOperand() == io.github.ocelot.glslprocessor.api.node.expression.GlslOperationNode.Operand.MULTIPLY
                     && operationNode.getSecond() instanceof GlslInvokeFunctionNode invokeNode
                     && invokeNode.getHeader() instanceof GlslVariableNode variableNode
                     && variableNode.getName().equals("minecraft_sample_lightmap")) {
                     List<GlslNode> replacementNodes = GlslParser.parseExpressionList(
                        "vertexColor = Color * minecraft_sample_lightmap(Sampler2, ivec2(UV2 * vec2(1.0, SableSkyLightScale)));"
                     );
                     mainFunctionBody.set(i, replacementNodes.getFirst());

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

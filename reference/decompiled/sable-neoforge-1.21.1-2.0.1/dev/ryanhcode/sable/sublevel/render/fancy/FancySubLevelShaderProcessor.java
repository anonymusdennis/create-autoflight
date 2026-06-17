package dev.ryanhcode.sable.sublevel.render.fancy;

import dev.ryanhcode.sable.Sable;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.Context;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.IncludeOverloadStrategy;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor.VeilContext;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.grammar.GlslTypeQualifier.StorageType;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.api.node.function.GlslFunctionNode;
import io.github.ocelot.glslprocessor.api.node.variable.GlslNewFieldNode;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;
import java.io.IOException;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

public class FancySubLevelShaderProcessor implements ShaderPreProcessor {
   public static final String BUFFER_SIZE = "SABLE_TEXTURE_CACHE_SIZE";

   public void modify(Context ctx, GlslTree tree) throws IOException, GlslSyntaxException, LexerException {
      if (ctx instanceof VeilContext veilContext && veilContext.isDynamic()) {
         ResourceLocation name = Objects.requireNonNull(veilContext.name(), "name");
         if (name.getNamespace().equals("sable") && name.getPath().startsWith("dynamic_sublevel/")) {
            if (ctx.isVertex()) {
               veilContext.addDefinitionDependency("SABLE_TEXTURE_CACHE_SIZE");
               tree.getBody().removeIf(next -> {
                  if (next instanceof GlslNewFieldNode field && field.getType().getQualifiers().contains(StorageType.IN)) {
                     return true;
                  }

                  return false;
               });
               ctx.include(tree, Sable.sablePath("fancy_sublevel_vertex"), IncludeOverloadStrategy.FAIL);
               GlslNodeList body = Objects.requireNonNull(((GlslFunctionNode)tree.mainFunction().orElseThrow()).getBody());
               body.add(0, GlslParser.parseExpression("_sable_unpack()"));
            }

            return;
         }

         return;
      }
   }
}

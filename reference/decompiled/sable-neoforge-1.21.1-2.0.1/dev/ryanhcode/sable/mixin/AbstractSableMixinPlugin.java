package dev.ryanhcode.sable.mixin;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.annotation.MixinModVersionConstraint;
import dev.ryanhcode.sable.platform.SableLoaderPlatform;
import foundry.veil.Veil;
import foundry.veil.api.compat.SodiumCompat;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

public abstract class AbstractSableMixinPlugin implements IMixinConfigPlugin {
   public static final Logger LOGGER = LogUtils.getLogger();
   private final Object2BooleanMap<String> modLoadedCache = new Object2BooleanOpenHashMap();
   private boolean sodiumPresent;

   public void onLoad(String mixinPackage) {
      this.sodiumPresent = SodiumCompat.isLoaded();
      LOGGER.info("Using {} renderer mixins", this.sodiumPresent ? "Sodium" : "Vanilla");
   }

   public String getRefMapperConfig() {
      return null;
   }

   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      if (mixinClassName.startsWith("dev.ryanhcode.sable.mixin.sublevel_render.impl")) {
         return this.sodiumPresent
            ? mixinClassName.startsWith("dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium")
            : mixinClassName.startsWith("dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla");
      } else if (!mixinClassName.startsWith("dev.ryanhcode.sable.mixin.compatibility.")
         && !mixinClassName.startsWith("dev.ryanhcode.sable.neoforge.mixin.compatibility.")
         && !mixinClassName.startsWith("dev.ryanhcode.sable.fabric.mixin.compatibility.")) {
         return true;
      } else {
         String[] parts = mixinClassName.split("\\.");
         if (parts.length < 5) {
            return true;
         } else {
            String modId = parts[3].equals("mixin") ? parts[5] : parts[6];
            boolean isModLoaded = this.modLoadedCache.computeIfAbsent(modId, x -> Veil.platform().isModLoaded(modId));
            return isModLoaded && AbstractSableMixinPlugin.MixinConstraints.handleClassAnnotation(mixinClassName, modId);
         }
      }
   }

   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   public List<String> getMixins() {
      return null;
   }

   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   static class MixinConstraints {
      private static final Object2ObjectMap<String, String> MOD_VERSION_CACHE = new Object2ObjectOpenHashMap();

      static boolean handleClassAnnotation(String mixinClassName, String modId) {
         try {
            List<AnnotationNode> nodes = MixinService.getService().getBytecodeProvider().getClassNode(mixinClassName).visibleAnnotations;
            return nodes == null ? true : shouldApply(nodes, modId);
         } catch (Throwable var3) {
            throw new RuntimeException(var3);
         }
      }

      static boolean shouldApply(List<AnnotationNode> nodes, String modId) throws InvalidVersionSpecificationException {
         for (AnnotationNode node : nodes) {
            if (node.desc.equals(Type.getDescriptor(MixinModVersionConstraint.class))) {
               String range = (String)Annotations.getValue(node, "value");
               VersionRange versionRange = VersionRange.createFromVersionSpec(range);
               String modVersion = (String)MOD_VERSION_CACHE.computeIfAbsent(modId, x -> SableLoaderPlatform.INSTANCE.getModVersion(modId));
               ArtifactVersion artifactVersion = new DefaultArtifactVersion(modVersion);
               return versionRange.containsVersion(artifactVersion);
            }
         }

         return true;
      }
   }
}

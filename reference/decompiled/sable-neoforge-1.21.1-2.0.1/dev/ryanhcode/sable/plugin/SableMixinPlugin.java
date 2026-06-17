package dev.ryanhcode.sable.plugin;

import dev.ryanhcode.sable.mixin.AbstractSableMixinPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class SableMixinPlugin extends AbstractSableMixinPlugin {
   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
      super.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
      super.postApply(targetClassName, targetClass, mixinClassName, mixinInfo);
   }
}

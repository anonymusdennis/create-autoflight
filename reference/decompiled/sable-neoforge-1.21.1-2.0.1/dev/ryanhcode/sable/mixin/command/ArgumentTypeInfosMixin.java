package dev.ryanhcode.sable.mixin.command;

import com.mojang.brigadier.arguments.ArgumentType;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.command.Vec3ArgumentAbsolute;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfo.Template;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ArgumentTypeInfos.class})
public abstract class ArgumentTypeInfosMixin {
   @Shadow
   private static <A extends ArgumentType<?>, T extends Template<A>> ArgumentTypeInfo<A, T> register(
      Registry<ArgumentTypeInfo<?, ?>> arg, String string, Class<? extends A> class_, ArgumentTypeInfo<A, T> arg2
   ) {
      return null;
   }

   @Inject(
      method = {"bootstrap"},
      at = {@At("TAIL")}
   )
   private static void sable$bootstrap(Registry<ArgumentTypeInfo<?, ?>> registry, CallbackInfoReturnable<ArgumentTypeInfo<?, ?>> cir) {
      register(registry, "sable:sub_level", SubLevelArgumentType.class, new SubLevelArgumentType.Info());
      register(registry, "sable:vec3_absolute", Vec3ArgumentAbsolute.class, SingletonArgumentInfo.contextFree(Vec3ArgumentAbsolute::vec3));
   }
}

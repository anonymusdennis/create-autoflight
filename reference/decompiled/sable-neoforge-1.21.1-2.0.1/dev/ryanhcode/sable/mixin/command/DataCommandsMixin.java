package dev.ryanhcode.sable.mixin.command;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.command.data_accessor.SubLevelDataAccessor;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.function.Function;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.commands.data.DataCommands.DataProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({DataCommands.class})
public class DataCommandsMixin {
   @WrapOperation(
      method = {"<clinit>"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/google/common/collect/ImmutableList;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Lcom/google/common/collect/ImmutableList;",
         remap = false
      )}
   )
   private static <E> ImmutableList<Function<String, DataProvider>> sable$allProviders(
      E e1, E e2, E e3, Operation<ImmutableList<Function<String, DataProvider>>> original
   ) {
      ImmutableList<Function<String, DataProvider>> providers = (ImmutableList<Function<String, DataProvider>>)original.call(new Object[]{e1, e2, e3});
      ObjectArrayList<Function<String, DataProvider>> mutableList = new ObjectArrayList(providers);
      mutableList.add(SubLevelDataAccessor.PROVIDER);
      return ImmutableList.copyOf(mutableList);
   }
}

package net.createmod.ponder.api.registration;

import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceLocation;

public interface LangRegistryAccess {
   void provideLang(String var1, BiConsumer<String, String> var2);

   String getShared(ResourceLocation var1);

   String getShared(ResourceLocation var1, Object... var2);

   String getTagName(ResourceLocation var1);

   String getTagDescription(ResourceLocation var1);

   String getSpecific(ResourceLocation var1, String var2);

   String getSpecific(ResourceLocation var1, String var2, Object... var3);
}

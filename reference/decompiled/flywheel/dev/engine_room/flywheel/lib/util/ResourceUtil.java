package dev.engine_room.flywheel.lib.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class ResourceUtil {
   private static final SimpleCommandExceptionType ERROR_INVALID = new SimpleCommandExceptionType(Component.translatable("argument.id.invalid"));

   private ResourceUtil() {
   }

   public static ResourceLocation rl(String path) {
      return ResourceLocation.fromNamespaceAndPath("flywheel", path);
   }

   public static ResourceLocation parseFlywheelDefault(String location) {
      String namespace = "flywheel";
      String path = location;
      int i = location.indexOf(58);
      if (i >= 0) {
         path = location.substring(i + 1);
         if (i >= 1) {
            namespace = location.substring(0, i);
         }
      }

      return ResourceLocation.fromNamespaceAndPath(namespace, path);
   }

   public static ResourceLocation readFlywheelDefault(StringReader reader) throws CommandSyntaxException {
      int i = reader.getCursor();

      while (reader.canRead() && ResourceLocation.isAllowedInResourceLocation(reader.peek())) {
         reader.skip();
      }

      String s = reader.getString().substring(i, reader.getCursor());

      try {
         return parseFlywheelDefault(s);
      } catch (ResourceLocationException var4) {
         reader.setCursor(i);
         throw ERROR_INVALID.createWithContext(reader);
      }
   }

   public static String toDebugFileNameNoExtension(ResourceLocation resourceLocation) {
      String stringLoc = resourceLocation.toDebugFileName();
      return stringLoc.substring(0, stringLoc.lastIndexOf(46));
   }
}

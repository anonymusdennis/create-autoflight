package net.createmod.catnip.config.ui;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.net.packets.ServerboundConfigPacket;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public class ConfigHelper {
   public static final Pattern unitPattern = Pattern.compile("\\[(in .*)]");
   public static final Pattern annotationPattern = Pattern.compile("\\[@cui:([^:]*)(?::(.*))?]");
   public static final Predicate<ModConfig> isForgeConfig = c -> c != null && c.getSpec() instanceof ModConfigSpec;
   public static final Map<String, ConfigHelper.ConfigChange> changes = new HashMap<>();
   private static final LoadingCache<String, EnumMap<Type, ModConfig>> configCache = CacheBuilder.newBuilder()
      .expireAfterAccess(5L, TimeUnit.MINUTES)
      .build(new CacheLoader<String, EnumMap<Type, ModConfig>>() {
         public EnumMap<Type, ModConfig> load(@Nonnull String key) {
            return ConfigHelper.findModConfigsUncached(key);
         }
      });

   private static EnumMap<Type, ModConfig> findModConfigsUncached(String modID) {
      EnumMap<Type, ModConfig> configMap = new EnumMap<>(Type.class);

      for (ModConfig config : ModConfigs.getModConfigs(modID)) {
         configMap.put((Enum)config.getType(), config);
      }

      return configMap;
   }

   public static ModConfigSpec findModConfigSpecFor(Type type, String modID) throws NullPointerException, ClassCastException {
      return (ModConfigSpec)((ModConfig)((EnumMap)configCache.getUnchecked(modID)).get(type)).getSpec();
   }

   public static boolean hasAnyForgeConfig(String modID) {
      for (ModConfig config : ((EnumMap)configCache.getUnchecked(modID)).values()) {
         if (isForgeConfig.test(config)) {
            return true;
         }
      }

      return false;
   }

   public static <T> void setConfigValue(ConfigHelper.ConfigPath path, String value) throws ConfigHelper.InvalidValueException, ClassCastException, NullPointerException {
      ModConfigSpec spec = findModConfigSpecFor(path.getType(), path.getModID());
      List<String> pathList = Arrays.asList(path.getPath());
      ValueSpec valueSpec = (ValueSpec)spec.getSpec().getRaw(pathList);
      ConfigValue<T> configValue = (ConfigValue<T>)spec.getValues().get(pathList);
      T v = (T)ServerboundConfigPacket.deserialize(configValue.get(), value);
      if (!valueSpec.test(v)) {
         throw new ConfigHelper.InvalidValueException();
      } else {
         configValue.set(v);
         configValue.save();
      }
   }

   public static <T> void setValue(String path, ConfigValue<T> configValue, T value, @Nullable Map<String, String> annotations) {
      if (value.equals(configValue.get())) {
         changes.remove(path);
      } else {
         changes.put(path, annotations == null ? new ConfigHelper.ConfigChange(value) : new ConfigHelper.ConfigChange(value, annotations));
      }
   }

   public static <T> T getValue(String path, ConfigValue<T> configValue) {
      ConfigHelper.ConfigChange configChange = changes.get(path);
      return (T)(configChange != null ? configChange.value : configValue.get());
   }

   public static Pair<String, Map<String, String>> readMetadataFromComment(List<String> commentLines) {
      AtomicReference<String> unit = new AtomicReference<>();
      Map<String, String> annotations = new HashMap<>();
      commentLines.removeIf(line -> {
         if (line.trim().isEmpty()) {
            return true;
         } else {
            Matcher matcher = annotationPattern.matcher(line);
            if (matcher.matches()) {
               String annotation = matcher.group(1);
               String aValue = matcher.group(2);
               annotations.putIfAbsent(annotation, aValue);
               return true;
            } else {
               matcher = unitPattern.matcher(line);
               if (matcher.matches()) {
                  unit.set(matcher.group(1));
               }

               return false;
            }
         }
      });
      return Pair.of(unit.get(), annotations);
   }

   public static class ConfigChange {
      Object value;
      Map<String, String> annotations = new HashMap<>();

      ConfigChange(Object value) {
         this.value = value;
      }

      ConfigChange(Object value, Map<String, String> annotations) {
         this(value);
         this.annotations.putAll(annotations);
      }
   }

   public static class ConfigPath {
      private String modID = "ponder";
      private Type type = Type.CLIENT;
      private String[] path = new String[0];

      public static ConfigHelper.ConfigPath parse(String string) {
         ConfigHelper.ConfigPath cp = new ConfigHelper.ConfigPath();
         String p = string;
         int index = string.indexOf(":");
         if (index >= 0) {
            p = string.substring(index + 1);
            if (index >= 1) {
               cp.modID = string.substring(0, index);
            }
         }

         String[] split = p.split("\\.");

         try {
            cp.type = Type.valueOf(split[0].toUpperCase(Locale.ROOT));
         } catch (Exception var6) {
            throw new IllegalArgumentException("path must start with either 'client.', 'common.' or 'server.'");
         }

         cp.path = new String[split.length - 1];
         System.arraycopy(split, 1, cp.path, 0, cp.path.length);
         return cp;
      }

      @Override
      public String toString() {
         return this.modID + ":" + this.type.name().toLowerCase(Locale.ROOT) + "." + String.join(".", this.path);
      }

      public ConfigHelper.ConfigPath setID(String modID) {
         this.modID = modID;
         return this;
      }

      public ConfigHelper.ConfigPath setType(Type type) {
         this.type = type;
         return this;
      }

      public ConfigHelper.ConfigPath setPath(String[] path) {
         this.path = path;
         return this;
      }

      public String getModID() {
         return this.modID;
      }

      public Type getType() {
         return this.type;
      }

      public String[] getPath() {
         return this.path;
      }
   }

   public static class InvalidValueException extends Exception {
      private static final long serialVersionUID = 1L;
   }
}

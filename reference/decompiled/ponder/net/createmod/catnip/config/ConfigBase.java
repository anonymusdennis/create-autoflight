package net.createmod.catnip.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.EnumValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;

public abstract class ConfigBase {
   @Nullable
   public ModConfigSpec specification;
   protected int depth;
   protected List<ConfigBase.CValue<?, ?>> allValues = new ArrayList<>();
   protected List<ConfigBase> children = new ArrayList<>();

   public void registerAll(Builder builder) {
      for (ConfigBase.CValue<?, ?> cValue : this.allValues) {
         cValue.register(builder);
      }
   }

   public void onLoad() {
      if (!this.children.isEmpty()) {
         this.children.forEach(ConfigBase::onLoad);
      }
   }

   public void onReload() {
      if (!this.children.isEmpty()) {
         this.children.forEach(ConfigBase::onReload);
      }
   }

   public abstract String getName();

   protected ConfigBase.ConfigBool b(boolean current, String name, String... comment) {
      return new ConfigBase.ConfigBool(name, current, comment);
   }

   protected ConfigBase.ConfigFloat f(float current, float min, float max, String name, String... comment) {
      return new ConfigBase.ConfigFloat(name, current, min, max, comment);
   }

   protected ConfigBase.ConfigFloat f(float current, float min, String name, String... comment) {
      return this.f(current, min, Float.MAX_VALUE, name, comment);
   }

   protected ConfigBase.ConfigInt i(int current, int min, int max, String name, String... comment) {
      return new ConfigBase.ConfigInt(name, current, min, max, comment);
   }

   protected ConfigBase.ConfigInt i(int current, int min, String name, String... comment) {
      return this.i(current, min, Integer.MAX_VALUE, name, comment);
   }

   protected ConfigBase.ConfigInt i(int current, String name, String... comment) {
      return this.i(current, Integer.MIN_VALUE, Integer.MAX_VALUE, name, comment);
   }

   protected <T extends Enum<T>> ConfigBase.ConfigEnum<T> e(T defaultValue, String name, String... comment) {
      return new ConfigBase.ConfigEnum<>(name, defaultValue, comment);
   }

   protected ConfigBase.ConfigGroup group(int depth, String name, String... comment) {
      return new ConfigBase.ConfigGroup(name, depth, comment);
   }

   protected <T extends ConfigBase> T nested(int depth, Supplier<T> constructor, String... comment) {
      T config = (T)constructor.get();
      new ConfigBase.ConfigGroup(config.getName(), depth, comment);
      new ConfigBase.CValue(config.getName(), builder -> {
         config.depth = depth;
         config.registerAll(builder);
         if (config.depth > depth) {
            builder.pop(config.depth - depth);
         }

         return null;
      });
      this.children.add(config);
      return config;
   }

   public class CValue<V, T extends ConfigValue<V>> {
      @Nullable
      protected ConfigValue<V> value;
      protected String name;
      private final ConfigBase.IValueProvider<V, T> provider;

      public CValue(String name, ConfigBase.IValueProvider<V, T> provider, String... comment) {
         this.name = name;
         this.provider = builder -> {
            this.addComments(builder, comment);
            return provider.apply(builder);
         };
         ConfigBase.this.allValues.add(this);
      }

      public void addComments(Builder builder, String... comment) {
         if (comment.length > 0) {
            String[] comments = new String[comment.length + 1];
            comments[0] = ".";
            System.arraycopy(comment, 0, comments, 1, comment.length);
            builder.comment(comments);
         } else {
            builder.comment(".");
         }
      }

      public void register(Builder builder) {
         this.value = this.provider.apply(builder);
      }

      public V get() {
         if (this.value == null) {
            throw new AssertionError("Config " + this.getName() + " was accessed, but not registered before!");
         } else {
            return (V)this.value.get();
         }
      }

      public void set(V value) {
         if (this.value == null) {
            throw new AssertionError("Config " + this.getName() + " was accessed, but not registered before!");
         } else {
            this.value.set(value);
            this.value.save();
         }
      }

      public String getName() {
         return this.name;
      }
   }

   public class ConfigBool extends ConfigBase.CValue<Boolean, BooleanValue> {
      public ConfigBool(String name, boolean def, String... comment) {
         super(name, builder -> builder.define(name, def), comment);
      }
   }

   public class ConfigEnum<T extends Enum<T>> extends ConfigBase.CValue<T, EnumValue<T>> {
      public ConfigEnum(String name, T defaultValue, String[] comment) {
         super(name, builder -> builder.defineEnum(name, defaultValue), comment);
      }
   }

   public class ConfigFloat extends ConfigBase.CValue<Double, DoubleValue> {
      public ConfigFloat(String name, float current, float min, float max, String... comment) {
         super(name, builder -> builder.defineInRange(name, (double)current, (double)min, (double)max), comment);
      }

      public float getF() {
         return this.get().floatValue();
      }
   }

   public class ConfigGroup extends ConfigBase.CValue<Boolean, BooleanValue> {
      private final int groupDepth;
      private final String[] comment;

      public ConfigGroup(String name, int depth, String... comment) {
         super(name, builder -> null, comment);
         this.groupDepth = depth;
         this.comment = comment;
      }

      @Override
      public void register(Builder builder) {
         if (ConfigBase.this.depth > this.groupDepth) {
            builder.pop(ConfigBase.this.depth - this.groupDepth);
         }

         ConfigBase.this.depth = this.groupDepth;
         this.addComments(builder, this.comment);
         builder.push(this.getName());
         ConfigBase.this.depth++;
      }
   }

   public class ConfigInt extends ConfigBase.CValue<Integer, IntValue> {
      public ConfigInt(String name, int current, int min, int max, String... comment) {
         super(name, builder -> builder.defineInRange(name, current, min, max), comment);
      }
   }

   @FunctionalInterface
   protected interface IValueProvider<V, T extends ConfigValue<V>> extends Function<Builder, T> {
   }
}

package dev.simulated_team.simulated.api.sound;

import com.simibubi.create.AllSoundEvents.SoundEntry;
import foundry.veil.platform.registry.RegistrationProvider;
import foundry.veil.platform.registry.RegistryObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class SoundEventRegistry {
   private final Map<String, SoundDefinition> definitions = new LinkedHashMap<>();
   private final Map<String, String> subtitles = new LinkedHashMap<>();
   private final RegistrationProvider<SoundEvent> registry;
   private final String modId;

   public SoundEventRegistry(String modId) {
      this.modId = modId;
      this.registry = RegistrationProvider.get(BuiltInRegistries.SOUND_EVENT, this.modId);
   }

   public SimSoundEntry create(String name, SoundSource category, UnaryOperator<SoundEventRegistry.DefinitionBuilder> operator) {
      ResourceLocation location = this.path(name);
      this.definitions.put(name, operator.apply(new SoundEventRegistry.DefinitionBuilder(name)).build());
      RegistryObject<SoundEvent> registryObject = this.registry.register(name, () -> SoundEvent.createVariableRangeEvent(location));
      return new SimSoundEntry(location, registryObject, category);
   }

   public SimSoundEntry create(String name, UnaryOperator<SoundEventRegistry.DefinitionBuilder> operator) {
      return this.create(name, SoundSource.BLOCKS, operator);
   }

   public SoundsProvider getProvider(PackOutput output) {
      return new SoundsProvider(this.modId, output, this.definitions);
   }

   private ResourceLocation path(String path) {
      return ResourceLocation.fromNamespaceAndPath(this.modId, path);
   }

   public void provideLang(BiConsumer<String, String> consumer) {
      this.subtitles.forEach(consumer);
   }

   public class DefinitionBuilder {
      private final String name;
      private String subtitle = null;
      private final List<SoundFile> sounds = new ArrayList<>();

      private DefinitionBuilder(final String name) {
         this.name = name;
      }

      public SoundEventRegistry.DefinitionBuilder defaultSubtitle(String key) {
         this.subtitle = key;
         return this;
      }

      public SoundEventRegistry.DefinitionBuilder defaultSubtitle(String subtitle, String key) {
         SoundEventRegistry.this.subtitles.put(key, subtitle);
         return this.defaultSubtitle(key);
      }

      public SoundEventRegistry.DefinitionBuilder subtitle(String subtitle) {
         String id = SoundEventRegistry.this.modId + ".subtitle." + this.name;
         return this.defaultSubtitle(subtitle, id);
      }

      public SoundEventRegistry.DefinitionBuilder addFileVariant(ResourceLocation path, UnaryOperator<SoundEventRegistry.SoundBuilder> operator) {
         SoundEventRegistry.SoundBuilder builder = SoundEventRegistry.this.new SoundBuilder(path);
         operator.apply(builder);
         this.sounds.add(builder.build());
         return this;
      }

      public SoundEventRegistry.DefinitionBuilder addFileVariant(String path, UnaryOperator<SoundEventRegistry.SoundBuilder> operator) {
         return this.addFileVariant(SoundEventRegistry.this.path(path), operator);
      }

      public SoundEventRegistry.DefinitionBuilder addFileVariant(String path) {
         return this.addFileVariant(path, UnaryOperator.identity());
      }

      public SoundEventRegistry.DefinitionBuilder addFileVariants(String path, int count) {
         for (int i = 0; i < count; i++) {
            int n = i + 1;
            this.addFileVariant(path + "_" + n);
         }

         return this;
      }

      public SoundEventRegistry.DefinitionBuilder addEventVariant(SoundEvent event, UnaryOperator<SoundEventRegistry.SoundBuilder> operator) {
         SoundEventRegistry.SoundBuilder builder = SoundEventRegistry.this.new SoundBuilder(event.getLocation()).setType(SoundFile.Type.EVENT);
         operator.apply(builder);
         this.sounds.add(builder.build());
         return this;
      }

      public SoundEventRegistry.DefinitionBuilder addEventVariant(SimSoundEntry entry, UnaryOperator<SoundEventRegistry.SoundBuilder> operator) {
         SoundEventRegistry.SoundBuilder builder = SoundEventRegistry.this.new SoundBuilder(entry.id()).setType(SoundFile.Type.EVENT);
         operator.apply(builder);
         this.sounds.add(builder.build());
         return this;
      }

      public SoundEventRegistry.DefinitionBuilder addEventVariant(SoundEntry soundEntry, UnaryOperator<SoundEventRegistry.SoundBuilder> operator) {
         SoundEventRegistry.SoundBuilder builder = SoundEventRegistry.this.new SoundBuilder(soundEntry.getId()).setType(SoundFile.Type.EVENT);
         operator.apply(builder);
         this.sounds.add(builder.build());
         return this;
      }

      public SoundEventRegistry.DefinitionBuilder addEventVariant(SoundEvent event) {
         return this.addEventVariant(event, UnaryOperator.identity());
      }

      public SoundEventRegistry.DefinitionBuilder addEventVariant(SimSoundEntry entry) {
         return this.addEventVariant(entry, UnaryOperator.identity());
      }

      public SoundEventRegistry.DefinitionBuilder addEventVariant(SoundEntry soundEntry) {
         return this.addEventVariant(soundEntry, UnaryOperator.identity());
      }

      public SoundDefinition build() {
         return new SoundDefinition(false, Optional.ofNullable(this.subtitle), this.sounds);
      }
   }

   public class SoundBuilder {
      private final ResourceLocation name;
      private float volume = 1.0F;
      private float pitch = 1.0F;
      private int weight = 1;
      private boolean stream = false;
      private int attenuationDistance = 16;
      private boolean preload = false;
      private SoundFile.Type type = SoundFile.Type.FILE;

      private SoundBuilder(final ResourceLocation name) {
         this.name = name;
      }

      private SoundBuilder(final String name) {
         this(SoundEventRegistry.this.path(name));
      }

      public SoundEventRegistry.SoundBuilder setVolume(float volume) {
         this.volume = volume;
         return this;
      }

      public SoundEventRegistry.SoundBuilder setPitch(float pitch) {
         this.pitch = pitch;
         return this;
      }

      public SoundEventRegistry.SoundBuilder setWeight(int weight) {
         this.weight = weight;
         return this;
      }

      public SoundEventRegistry.SoundBuilder setStream(boolean stream) {
         this.stream = stream;
         return this;
      }

      public SoundEventRegistry.SoundBuilder setAttenuationDistance(int attenuationDistance) {
         this.attenuationDistance = attenuationDistance;
         return this;
      }

      public SoundEventRegistry.SoundBuilder setPreload(boolean preload) {
         this.preload = preload;
         return this;
      }

      public SoundEventRegistry.SoundBuilder setType(SoundFile.Type type) {
         this.type = type;
         return this;
      }

      public SoundFile build() {
         return new SoundFile(this.name, this.volume, this.pitch, this.weight, this.stream, this.attenuationDistance, this.preload, this.type);
      }
   }
}

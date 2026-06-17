package dev.ryanhcode.sable.physics.config.block_properties;

import com.google.common.collect.UnmodifiableIterator;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs.TagOrElementLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class PhysicsBlockPropertiesDefinitionLoader extends SimpleJsonResourceReloadListener {
   public static final String NAME = "physics_block_properties";
   public static final ResourceLocation ID = Sable.sablePath("physics_block_properties");
   protected static final Gson GSON = new Gson();
   public static final PhysicsBlockPropertiesDefinitionLoader INSTANCE = new PhysicsBlockPropertiesDefinitionLoader();
   private final ObjectList<PhysicsBlockPropertiesDefinition> definitions = new ObjectArrayList();

   private PhysicsBlockPropertiesDefinitionLoader() {
      super(GSON, "physics_block_properties");
   }

   public String getName() {
      return super.getName();
   }

   protected void apply(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
      this.definitions.clear();

      for (Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
         ResourceLocation file = entry.getKey();
         JsonElement json = entry.getValue();
         DataResult<Pair<PhysicsBlockPropertiesDefinition, JsonElement>> decoded = PhysicsBlockPropertiesDefinition.CODEC.decode(JsonOps.INSTANCE, json);
         decoded.result().ifPresent(pair -> {
            PhysicsBlockPropertiesDefinition definition = (PhysicsBlockPropertiesDefinition)pair.getFirst();
            this.definitions.add(definition);
         });
         decoded.error().ifPresent(error -> Sable.LOGGER.error("Error while loading physics block properties entry: {}", error));
      }

      this.definitions.sort(Comparator.comparingInt(PhysicsBlockPropertiesDefinition::priority));
   }

   public static void applyToBlocks(PhysicsBlockPropertiesDefinition definition) {
      TagOrElementLocation selector = definition.selector();
      ObjectArrayList<Block> blocks = new ObjectArrayList(16);
      if (selector.tag()) {
         TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, selector.id());
         Optional<Named<Block>> tagBlocks = BuiltInRegistries.BLOCK.getTag(tagKey);
         if (tagBlocks.isPresent()) {
            for (Holder<Block> blockHolder : tagBlocks.get()) {
               Block block = (Block)blockHolder.value();
               blocks.add(block);
            }
         } else {
            Sable.LOGGER.error("Failed to apply tag physics properties. Unknown tag: {}", selector.id());
         }
      } else if (BuiltInRegistries.BLOCK.containsKey(selector.id())) {
         Block block = (Block)BuiltInRegistries.BLOCK.get(selector.id());
         blocks.add(block);
      } else {
         Sable.LOGGER.error("Failed to apply tag physics properties. Unknown block: {}", selector.id());
      }

      ObjectListIterator var10 = blocks.iterator();

      while (var10.hasNext()) {
         Block block = (Block)var10.next();
         StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
         UnmodifiableIterator var13 = stateDefinition.getPossibleStates().iterator();

         while (var13.hasNext()) {
            BlockState state = (BlockState)var13.next();
            ((BlockStateExtension)state).sable$loadProperties(stateDefinition, definition);
         }
      }
   }

   public void applyAll() {
      ObjectListIterator var1 = this.definitions.iterator();

      while (var1.hasNext()) {
         PhysicsBlockPropertiesDefinition definition = (PhysicsBlockPropertiesDefinition)var1.next();
         applyToBlocks(definition);
      }
   }

   public Collection<PhysicsBlockPropertiesDefinition> getDefinitions() {
      return this.definitions;
   }
}

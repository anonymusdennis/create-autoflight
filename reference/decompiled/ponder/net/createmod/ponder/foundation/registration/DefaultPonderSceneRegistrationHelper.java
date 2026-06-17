package net.createmod.ponder.foundation.registration;

import java.util.Arrays;
import java.util.function.Function;
import net.createmod.ponder.api.registration.MultiSceneBuilder;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.createmod.ponder.foundation.PonderStoryBoardEntry;
import net.minecraft.resources.ResourceLocation;

public class DefaultPonderSceneRegistrationHelper implements PonderSceneRegistrationHelper<ResourceLocation> {
   protected String namespace;
   protected PonderSceneRegistry sceneRegistry;

   public DefaultPonderSceneRegistrationHelper(String namespace, PonderSceneRegistry sceneRegistry) {
      this.namespace = namespace;
      this.sceneRegistry = sceneRegistry;
   }

   public <T> GenericPonderSceneRegistrationHelper<T> withKeyFunction(Function<T, ResourceLocation> keyGen) {
      return new GenericPonderSceneRegistrationHelper<>(this, keyGen);
   }

   public StoryBoardEntry addStoryBoard(ResourceLocation component, ResourceLocation schematicLocation, PonderStoryBoard storyBoard, ResourceLocation... tags) {
      StoryBoardEntry entry = this.createStoryBoardEntry(storyBoard, schematicLocation, component);
      entry.highlightTags(tags);
      this.sceneRegistry.addStoryBoard(entry);
      return entry;
   }

   public StoryBoardEntry addStoryBoard(ResourceLocation component, String schematicPath, PonderStoryBoard storyBoard, ResourceLocation... tags) {
      return this.addStoryBoard(component, this.asLocation(schematicPath), storyBoard, tags);
   }

   public MultiSceneBuilder forComponents(ResourceLocation... components) {
      return new GenericMultiSceneBuilder<>(this, Arrays.asList(components));
   }

   @Override
   public MultiSceneBuilder forComponents(Iterable<? extends ResourceLocation> components) {
      return new GenericMultiSceneBuilder<>(this, components);
   }

   @Override
   public ResourceLocation asLocation(String path) {
      return ResourceLocation.fromNamespaceAndPath(this.namespace, path);
   }

   private PonderStoryBoardEntry createStoryBoardEntry(PonderStoryBoard storyBoard, ResourceLocation schematicLocation, ResourceLocation component) {
      return new PonderStoryBoardEntry(storyBoard, this.namespace, schematicLocation, component);
   }
}

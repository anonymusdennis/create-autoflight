package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;

public class PonderStoryBoardEntry implements StoryBoardEntry {
   private final PonderStoryBoard board;
   private final String namespace;
   private final ResourceLocation schematicLocation;
   private final ResourceLocation component;
   private final List<ResourceLocation> tags;
   private final List<StoryBoardEntry.SceneOrderingEntry> orderingEntries;

   public PonderStoryBoardEntry(PonderStoryBoard board, String namespace, ResourceLocation schematicLocation, ResourceLocation component) {
      this.board = board;
      this.namespace = namespace;
      this.schematicLocation = schematicLocation;
      this.component = component;
      this.tags = new ArrayList<>();
      this.orderingEntries = new ArrayList<>();
   }

   public PonderStoryBoardEntry(PonderStoryBoard board, String namespace, String schematicPath, ResourceLocation component) {
      this(board, namespace, ResourceLocation.fromNamespaceAndPath(namespace, schematicPath), component);
   }

   @Override
   public PonderStoryBoard getBoard() {
      return this.board;
   }

   @Override
   public String getNamespace() {
      return this.namespace;
   }

   @Override
   public ResourceLocation getSchematicLocation() {
      return this.schematicLocation;
   }

   @Override
   public ResourceLocation getComponent() {
      return this.component;
   }

   @Override
   public List<ResourceLocation> getTags() {
      return this.tags;
   }

   @Override
   public List<StoryBoardEntry.SceneOrderingEntry> getOrderingEntries() {
      return this.orderingEntries;
   }

   @Override
   public StoryBoardEntry orderBefore(String namespace, String otherSceneId) {
      this.orderingEntries.add(StoryBoardEntry.SceneOrderingEntry.before(namespace, otherSceneId));
      return this;
   }

   @Override
   public StoryBoardEntry orderAfter(String namespace, String otherSceneId) {
      this.orderingEntries.add(StoryBoardEntry.SceneOrderingEntry.after(namespace, otherSceneId));
      return this;
   }

   @Override
   public StoryBoardEntry highlightTag(ResourceLocation tag) {
      this.tags.add(tag);
      return this;
   }

   @Override
   public StoryBoardEntry highlightTags(ResourceLocation... tags) {
      Collections.addAll(this.tags, tags);
      return this;
   }

   @Override
   public StoryBoardEntry highlightAllTags() {
      this.tags.add(PonderTag.Highlight.ALL);
      return this;
   }
}

package net.createmod.ponder.api.registration;

import java.util.List;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;

public interface StoryBoardEntry {
   PonderStoryBoard getBoard();

   String getNamespace();

   ResourceLocation getSchematicLocation();

   ResourceLocation getComponent();

   List<ResourceLocation> getTags();

   List<StoryBoardEntry.SceneOrderingEntry> getOrderingEntries();

   default StoryBoardEntry orderBefore(String otherSceneId) {
      return this.orderBefore(this.getNamespace(), otherSceneId);
   }

   StoryBoardEntry orderBefore(String var1, String var2);

   default StoryBoardEntry orderAfter(String otherSceneId) {
      return this.orderAfter(this.getNamespace(), otherSceneId);
   }

   StoryBoardEntry orderAfter(String var1, String var2);

   StoryBoardEntry highlightTag(ResourceLocation var1);

   StoryBoardEntry highlightTags(ResourceLocation... var1);

   StoryBoardEntry highlightAllTags();

   public static record SceneOrderingEntry(StoryBoardEntry.SceneOrderingType type, ResourceLocation sceneId) {
      public static StoryBoardEntry.SceneOrderingEntry after(String namespace, String sceneId) {
         return new StoryBoardEntry.SceneOrderingEntry(StoryBoardEntry.SceneOrderingType.AFTER, ResourceLocation.fromNamespaceAndPath(namespace, sceneId));
      }

      public static StoryBoardEntry.SceneOrderingEntry before(String namespace, String sceneId) {
         return new StoryBoardEntry.SceneOrderingEntry(StoryBoardEntry.SceneOrderingType.BEFORE, ResourceLocation.fromNamespaceAndPath(namespace, sceneId));
      }
   }

   public static enum SceneOrderingType {
      BEFORE,
      AFTER;
   }
}

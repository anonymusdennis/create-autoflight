package net.createmod.ponder.api.registration;

import java.util.function.Consumer;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;

public interface MultiSceneBuilder {
   MultiSceneBuilder addStoryBoard(ResourceLocation var1, PonderStoryBoard var2);

   MultiSceneBuilder addStoryBoard(ResourceLocation var1, PonderStoryBoard var2, ResourceLocation... var3);

   MultiSceneBuilder addStoryBoard(ResourceLocation var1, PonderStoryBoard var2, Consumer<StoryBoardEntry> var3);

   MultiSceneBuilder addStoryBoard(String var1, PonderStoryBoard var2);

   MultiSceneBuilder addStoryBoard(String var1, PonderStoryBoard var2, ResourceLocation... var3);

   MultiSceneBuilder addStoryBoard(String var1, PonderStoryBoard var2, Consumer<StoryBoardEntry> var3);
}

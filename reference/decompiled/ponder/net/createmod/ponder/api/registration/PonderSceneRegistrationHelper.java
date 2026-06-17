package net.createmod.ponder.api.registration;

import java.util.function.Function;
import net.createmod.ponder.api.scene.PonderStoryBoard;
import net.minecraft.resources.ResourceLocation;

public interface PonderSceneRegistrationHelper<T> {
   <S> PonderSceneRegistrationHelper<S> withKeyFunction(Function<S, T> var1);

   StoryBoardEntry addStoryBoard(T var1, ResourceLocation var2, PonderStoryBoard var3, ResourceLocation... var4);

   StoryBoardEntry addStoryBoard(T var1, String var2, PonderStoryBoard var3, ResourceLocation... var4);

   MultiSceneBuilder forComponents(T... var1);

   MultiSceneBuilder forComponents(Iterable<? extends T> var1);

   ResourceLocation asLocation(String var1);
}

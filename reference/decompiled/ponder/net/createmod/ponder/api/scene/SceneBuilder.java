package net.createmod.ponder.api.scene;

import java.util.function.Consumer;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.PonderInstruction;

public interface SceneBuilder {
   OverlayInstructions overlay();

   WorldInstructions world();

   DebugInstructions debug();

   EffectInstructions effects();

   SpecialInstructions special();

   PonderScene getScene();

   void title(String var1, String var2);

   void configureBasePlate(int var1, int var2, int var3);

   void scaleSceneView(float var1);

   void removeShadow();

   void setSceneOffsetY(float var1);

   void showBasePlate();

   void addInstruction(PonderInstruction var1);

   void addInstruction(Consumer<PonderScene> var1);

   void idle(int var1);

   void idleSeconds(int var1);

   void markAsFinished();

   void setNextUpEnabled(boolean var1);

   void rotateCameraY(float var1);

   void addKeyframe();

   void addLazyKeyframe();
}

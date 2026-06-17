package net.createmod.ponder.foundation.element;

import java.util.function.Function;
import net.createmod.catnip.outliner.Outline;
import net.createmod.catnip.outliner.Outliner;
import net.createmod.ponder.foundation.PonderScene;

public class OutlinerElement extends AnimatedSceneElementBase {
   private final Function<Outliner, Outline.OutlineParams> outlinerCall;
   private int overrideColor;

   public OutlinerElement(Function<Outliner, Outline.OutlineParams> outlinerCall) {
      this.outlinerCall = outlinerCall;
      this.overrideColor = -1;
   }

   @Override
   public void tick(PonderScene scene) {
      super.tick(scene);
      if (!(this.fade.getValue() < 0.0625F)) {
         if (!(this.fade.getValue(0.0F) > this.fade.getValue(1.0F))) {
            Outline.OutlineParams params = this.outlinerCall.apply(scene.getOutliner());
            if (this.overrideColor != -1) {
               params.colored(this.overrideColor);
            }
         }
      }
   }

   public void setColor(int overrideColor) {
      this.overrideColor = overrideColor;
   }
}

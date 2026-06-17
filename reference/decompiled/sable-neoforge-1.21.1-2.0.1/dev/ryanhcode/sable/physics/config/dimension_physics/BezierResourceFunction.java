package dev.ryanhcode.sable.physics.config.dimension_physics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.util.SableCodecUtil;
import java.util.ArrayList;
import java.util.List;

public class BezierResourceFunction {
   public static final Codec<BezierResourceFunction> CODEC = BezierResourceFunction.BezierPoint.CODEC
      .listOf()
      .flatXmap(
         bezierPoints -> DataResult.success(new BezierResourceFunction(bezierPoints)),
         bezierResourceFunction -> DataResult.success(bezierResourceFunction.getPoints())
      );
   private final List<BezierResourceFunction.BezierPoint> points;

   public BezierResourceFunction(List<BezierResourceFunction.BezierPoint> points) {
      this.points = points;
   }

   public BezierResourceFunction() {
      this.points = new ArrayList<>();
   }

   public List<BezierResourceFunction.BezierPoint> getPoints() {
      return this.points;
   }

   public void addPoint(BezierResourceFunction.BezierPoint point) {
      this.points.add(point);
   }

   public int pointSize() {
      return this.points.size();
   }

   public double evaluateFunction(double position) {
      if (this.points.isEmpty()) {
         return 1.0;
      } else if (this.points.size() == 1) {
         return this.points.get(0).value;
      } else {
         int index = -1;

         for (BezierResourceFunction.BezierPoint point : this.points) {
            if (position < point.altitude()) {
               break;
            }

            index++;
         }

         if (index == -1) {
            return this.points.get(0).value;
         } else if (index >= this.points.size() - 1) {
            return this.points.get(this.points.size() - 1).value;
         } else {
            BezierResourceFunction.BezierPoint point1 = this.points.get(index);
            BezierResourceFunction.BezierPoint point2 = this.points.get(index + 1);
            double relativeX = point2.altitude - point1.altitude;
            double relativeY = point2.value - point1.value;
            double slope1 = point1.slope;
            double slope2 = point2.slope;
            double t = (position - point1.altitude) / relativeX;
            double cubicFactor = (slope1 + slope2) * relativeX - 2.0 * relativeY;
            double quadraticFactor = 3.0 * relativeY - (2.0 * slope1 + slope2) * relativeX;
            double linearFactor = relativeX * slope1;
            return Math.max(((cubicFactor * t + quadraticFactor) * t + linearFactor) * t + point1.value, 0.0);
         }
      }
   }

   public static record BezierPoint(double altitude, double value, double slope) {
      public static final Codec<BezierResourceFunction.BezierPoint> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.DOUBLE.fieldOf("altitude").forGetter(BezierResourceFunction.BezierPoint::altitude),
                  SableCodecUtil.positiveDouble(true).fieldOf("value").forGetter(BezierResourceFunction.BezierPoint::value),
                  Codec.DOUBLE.fieldOf("slope").forGetter(BezierResourceFunction.BezierPoint::slope)
               )
               .apply(instance, BezierResourceFunction.BezierPoint::new)
      );
   }
}

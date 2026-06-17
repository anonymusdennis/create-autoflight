package dev.simulated_team.simulated.ponder;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;

public class SmoothMovementUtils {
   public static FloatUnaryOperator cubicSmoothing() {
      return t -> t * t * (3.0F - 2.0F * t);
   }

   public static FloatUnaryOperator linear() {
      return t -> t;
   }

   public static FloatUnaryOperator quinticSmoothing() {
      return t -> t * t * t * (10.0F - 3.0F * t * (5.0F - 2.0F * t));
   }

   public static FloatUnaryOperator quadraticJump() {
      return t -> 4.0F * t * (1.0F - t);
   }

   public static FloatUnaryOperator quadraticRise() {
      return t -> t * t;
   }

   public static FloatUnaryOperator quadraticRiseDual() {
      return t -> t * (2.0F - t);
   }

   public static FloatUnaryOperator quadraticRiseInOut() {
      return t -> (double)t < 0.5 ? 2.0F * t * t : 2.0F * t * (2.0F - t) - 1.0F;
   }

   public static FloatUnaryOperator quadraticRiseOut() {
      return t -> t * (2.0F - t);
   }

   public static FloatUnaryOperator elasticOut() {
      double c4 = Math.PI * 2.0 / 3.0;
      return t -> (float)(Math.pow(2.0, (double)(-10.0F * t)) * Math.sin(((double)(t * 10.0F) - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0);
   }

   public static FloatUnaryOperator softElasticOut() {
      double c4 = Math.PI * 2.0 / 3.0;
      return t -> (double)t < 0.5
            ? 2.0F * t
            : (float)(Math.pow(2.0, (double)(-10.0F * t)) * Math.sin(((double)t * Math.pow((double)(t + 1.0F), 5.0) - 0.75) * (Math.PI * 2.0 / 3.0)) + 1.0);
   }

   public static FloatUnaryOperator cubicRise() {
      return t -> t * t * t;
   }

   public static FloatUnaryOperator asymptoticAcceleration(float smoothing) {
      return t -> (float)(((double)(t * smoothing) + Math.exp((double)(-smoothing * t)) - 1.0) / ((double)smoothing + Math.exp((double)(-smoothing)) - 1.0));
   }
}

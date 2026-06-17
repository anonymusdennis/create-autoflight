package dev.ryanhcode.sable.physics.config.dimension_physics;

import com.mojang.datafixers.kinds.Applicative;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

public record DimensionPhysics(
   ResourceLocation dimension,
   int priority,
   Optional<Float> universalDrag,
   Optional<Vector3f> baseGravity,
   Optional<Double> basePressure,
   Optional<BezierResourceFunction> pressureFunction,
   Optional<Vector3f> magneticNorth,
   boolean ignoreChunks
) {
   public static final Vector3f DEFAULT_GRAVITY = new Vector3f(0.0F, -11.0F, 0.0F);
   public static final Vector3f DEFAULT_MAGNETIC_NORTH = new Vector3f(0.0F, 0.0F, 0.0F);
   public static final double DEFAULT_PRESSURE = 1.0;
   private static final float DEFAULT_UNIVERSAL_DRAG = 0.09F;
   public static final Codec<DimensionPhysics> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               ResourceLocation.CODEC.fieldOf("dimension").forGetter(DimensionPhysics::dimension),
               Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("priority", 1000).forGetter(DimensionPhysics::priority),
               Codec.optionalField("universal_drag", Codec.FLOAT, false).forGetter(DimensionPhysics::universalDrag),
               Codec.optionalField("base_gravity", ExtraCodecs.VECTOR3F, false).forGetter(DimensionPhysics::baseGravity),
               Codec.optionalField("base_pressure", Codec.DOUBLE, false).forGetter(DimensionPhysics::basePressure),
               Codec.optionalField("pressure_function", BezierResourceFunction.CODEC, false).forGetter(DimensionPhysics::pressureFunction),
               Codec.optionalField("magnetic_north", ExtraCodecs.VECTOR3F, false).forGetter(DimensionPhysics::magneticNorth),
               Codec.BOOL.optionalFieldOf("ignore_chunks", false).forGetter(DimensionPhysics::ignoreChunks)
            )
            .apply(Applicative.unbox(instance), DimensionPhysics::new)
   );

   public static DimensionPhysics createDefault(Level level) {
      double seaLevel = (double)level.getSeaLevel();
      double currentAltitude = (double)level.dimensionType().minY();
      double maxAltitude = currentAltitude + (double)level.dimensionType().logicalHeight();
      double baseSlope = -0.004;
      double maxPressure = 1.5;
      double maxStep = 200.0;
      double smoothingAltitude = maxAltitude - 40.0;
      currentAltitude = Math.max(currentAltitude, Math.log(1.5) / -0.004 + seaLevel);
      BezierResourceFunction pressureFunction = new BezierResourceFunction();

      while (true) {
         double currentPressure = Math.exp(-0.004 * (currentAltitude - seaLevel));
         double currentSlope = currentPressure * -0.004;
         pressureFunction.addPoint(new BezierResourceFunction.BezierPoint(currentAltitude, currentPressure, currentSlope));
         if (currentAltitude < seaLevel && currentAltitude + 200.0 >= seaLevel) {
            currentAltitude = seaLevel;
         } else if (currentAltitude < smoothingAltitude && currentAltitude + 200.0 >= smoothingAltitude) {
            currentAltitude = smoothingAltitude;
         } else {
            if (currentAltitude >= smoothingAltitude) {
               currentPressure = pressureFunction.getPoints().get(pressureFunction.pointSize() - 1).value();
               currentSlope = -2.0 * currentPressure / (maxAltitude - smoothingAltitude);
               pressureFunction.addPoint(new BezierResourceFunction.BezierPoint(maxAltitude, 0.0, currentSlope));
               Vector3f north = level.dimensionType().natural() ? DEFAULT_MAGNETIC_NORTH : new Vector3f(0.0F, 0.0F, 0.0F);
               return new DimensionPhysics(
                  level.dimension().location(),
                  0,
                  Optional.of(0.09F),
                  Optional.of(DEFAULT_GRAVITY),
                  Optional.of(1.0),
                  Optional.of(pressureFunction),
                  Optional.of(north),
                  false
               );
            }

            currentAltitude += 200.0;
         }
      }
   }
}

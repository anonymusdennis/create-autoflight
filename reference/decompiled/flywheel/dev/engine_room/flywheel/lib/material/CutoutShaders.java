package dev.engine_room.flywheel.lib.material;

import dev.engine_room.flywheel.api.material.CutoutShader;
import dev.engine_room.flywheel.lib.util.ResourceUtil;

public final class CutoutShaders {
   public static final CutoutShader OFF = new SimpleCutoutShader(ResourceUtil.rl("cutout/off.glsl"));
   public static final CutoutShader EPSILON = new SimpleCutoutShader(ResourceUtil.rl("cutout/epsilon.glsl"));
   public static final CutoutShader ONE_TENTH = new SimpleCutoutShader(ResourceUtil.rl("cutout/one_tenth.glsl"));
   public static final CutoutShader HALF = new SimpleCutoutShader(ResourceUtil.rl("cutout/half.glsl"));

   private CutoutShaders() {
   }
}

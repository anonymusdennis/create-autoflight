package dev.ryanhcode.sable.mixin.camera.new_camera_types;

import com.llamalad7.mixinextras.lib.apache.commons.ArrayUtils;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({CameraType.class})
public class CameraTypeMixin {
   @Shadow
   @Final
   @Mutable
   private static CameraType[] $VALUES;
   @Final
   @Shadow
   @Mutable
   private static CameraType[] VALUES;

   @Invoker("<init>")
   private static CameraType create(String name, int ordinal, boolean firstPerson, boolean mirrored) {
      throw new IllegalStateException("Unreachable");
   }

   @Overwrite
   public CameraType cycle() {
      if (this == SableCameraTypes.SUB_LEVEL_VIEW) {
         return SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED;
      } else if (this == SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED) {
         return CameraType.THIRD_PERSON_FRONT;
      } else {
         return switch ((CameraType)this) {
            case FIRST_PERSON -> CameraType.THIRD_PERSON_BACK;
            case THIRD_PERSON_BACK -> SableCameraTypes.SUB_LEVEL_VIEW;
            case THIRD_PERSON_FRONT -> CameraType.FIRST_PERSON;
            default -> null;
         };
      }
   }

   static {
      CameraType subLevelView = create("SUB_LEVEL_VIEW", $VALUES.length, false, false);
      $VALUES = (CameraType[])ArrayUtils.add($VALUES, subLevelView);
      VALUES = (CameraType[])ArrayUtils.add(VALUES, subLevelView);
      CameraType subLevelViewUnlocked = create("SUB_LEVEL_VIEW_UNLOCKED", $VALUES.length, false, false);
      $VALUES = (CameraType[])ArrayUtils.add($VALUES, subLevelViewUnlocked);
      VALUES = (CameraType[])ArrayUtils.add(VALUES, subLevelViewUnlocked);
   }
}

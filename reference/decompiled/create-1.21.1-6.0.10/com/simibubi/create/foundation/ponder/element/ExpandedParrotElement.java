package com.simibubi.create.foundation.ponder.element;

import java.util.function.Supplier;
import net.createmod.ponder.api.element.ParrotPose;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.element.ParrotElementImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public class ExpandedParrotElement extends ParrotElementImpl {
   protected boolean deferConductor = false;

   protected ExpandedParrotElement(Vec3 location, Supplier<? extends ParrotPose> pose) {
      super(location, pose);
   }

   public void reset(PonderScene scene) {
      super.reset(scene);
      this.entity.getPersistentData().remove("TrainHat");
      this.deferConductor = false;
   }

   public void tick(PonderScene scene) {
      boolean wasNull = this.entity == null;
      super.tick(scene);
      if (wasNull) {
         if (this.deferConductor) {
            this.setConductor(true);
         }

         this.deferConductor = false;
      }
   }

   public void setConductor(boolean isConductor) {
      if (this.entity == null) {
         this.deferConductor = isConductor;
      } else {
         CompoundTag data = this.entity.getPersistentData();
         if (isConductor) {
            data.putBoolean("TrainHat", true);
         } else {
            data.remove("TrainHat");
         }
      }
   }
}

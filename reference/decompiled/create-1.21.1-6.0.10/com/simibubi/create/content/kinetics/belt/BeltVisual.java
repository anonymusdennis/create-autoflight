package com.simibubi.create.content.kinetics.belt;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.transform.PoseTransformStack;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.function.Consumer;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.SpriteShiftEntry;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class BeltVisual extends KineticBlockEntityVisual<BeltBlockEntity> {
   public static final float MAGIC_SCROLL_MULTIPLIER = 0.001984127F;
   public static final float SCROLL_FACTOR_DIAGONAL = 0.375F;
   public static final float SCROLL_FACTOR_OTHERWISE = 0.5F;
   public static final float SCROLL_OFFSET_BOTTOM = 0.5F;
   public static final float SCROLL_OFFSET_OTHERWISE = 0.0F;
   protected final ScrollInstance[] belts;
   @Nullable
   protected final RotatingInstance pulley;

   public BeltVisual(VisualizationContext context, BeltBlockEntity blockEntity, float partialTick) {
      super(context, blockEntity, partialTick);
      BeltPart part = (BeltPart)this.blockState.getValue(BeltBlock.PART);
      boolean start = part == BeltPart.START;
      boolean end = part == BeltPart.END;
      DyeColor color = blockEntity.color.orElse(null);
      boolean diagonal = ((BeltSlope)this.blockState.getValue(BeltBlock.SLOPE)).isDiagonal();
      this.belts = new ScrollInstance[diagonal ? 1 : 2];

      for (boolean bottom : Iterate.trueAndFalse) {
         PartialModel beltPartial = BeltRenderer.getBeltPartial(diagonal, start, end, bottom);
         SpriteShiftEntry spriteShift = BeltRenderer.getSpriteShiftEntry(color, diagonal, bottom);
         Instancer<ScrollInstance> beltModel = this.instancerProvider().instancer(AllInstanceTypes.SCROLLING, Models.partial(beltPartial));
         this.belts[bottom ? 0 : 1] = this.setup((ScrollInstance)beltModel.createInstance(), bottom, spriteShift);
         if (diagonal) {
            break;
         }
      }

      if (blockEntity.hasPulley()) {
         this.pulley = (RotatingInstance)this.instancerProvider().instancer(AllInstanceTypes.ROTATING, this.getPulleyModel()).createInstance();
         this.pulley.setup((KineticBlockEntity)this.blockEntity).setPosition(this.getVisualPosition()).setChanged();
      } else {
         this.pulley = null;
      }
   }

   public void update(float pt) {
      DyeColor color = ((BeltBlockEntity)this.blockEntity).color.orElse(null);
      boolean diagonal = ((BeltSlope)this.blockState.getValue(BeltBlock.SLOPE)).isDiagonal();
      boolean bottom = true;

      for (ScrollInstance key : this.belts) {
         this.setup(key, bottom, BeltRenderer.getSpriteShiftEntry(color, diagonal, bottom));
         bottom = false;
      }

      if (this.pulley != null) {
         this.pulley.setup((KineticBlockEntity)this.blockEntity).setChanged();
      }
   }

   public void updateLight(float partialTick) {
      this.relight(this.belts);
      if (this.pulley != null) {
         this.relight(new FlatLit[]{this.pulley});
      }
   }

   protected void _delete() {
      for (ScrollInstance key : this.belts) {
         key.delete();
      }

      if (this.pulley != null) {
         this.pulley.delete();
      }
   }

   private Model getPulleyModel() {
      Direction dir = this.getOrientation();
      return Models.partial(AllPartialModels.BELT_PULLEY, dir.getAxis(), (axis11, modelTransform1) -> {
         PoseTransformStack msr = TransformStack.of(modelTransform1);
         msr.center();
         if (axis11 == Axis.X) {
            msr.rotateYDegrees(90.0F);
         }

         if (axis11 == Axis.Y) {
            msr.rotateXDegrees(90.0F);
         }

         msr.rotateXDegrees(90.0F);
         msr.uncenter();
      });
   }

   private Direction getOrientation() {
      Direction dir = ((Direction)this.blockState.getValue(BeltBlock.HORIZONTAL_FACING)).getClockWise();
      if (this.blockState.getValue(BeltBlock.SLOPE) == BeltSlope.SIDEWAYS) {
         dir = Direction.UP;
      }

      return dir;
   }

   private ScrollInstance setup(ScrollInstance key, boolean bottom, SpriteShiftEntry spriteShift) {
      BeltSlope beltSlope = (BeltSlope)this.blockState.getValue(BeltBlock.SLOPE);
      Direction facing = (Direction)this.blockState.getValue(BeltBlock.HORIZONTAL_FACING);
      boolean diagonal = beltSlope.isDiagonal();
      boolean sideways = beltSlope == BeltSlope.SIDEWAYS;
      boolean vertical = beltSlope == BeltSlope.VERTICAL;
      boolean upward = beltSlope == BeltSlope.UPWARD;
      boolean alongX = facing.getAxis() == Axis.X;
      boolean alongZ = facing.getAxis() == Axis.Z;
      boolean downward = beltSlope == BeltSlope.DOWNWARD;
      float speed = ((BeltBlockEntity)this.blockEntity).getSpeed();
      if (facing.getAxisDirection() == AxisDirection.NEGATIVE ^ upward ^ (alongX && !diagonal || alongZ && diagonal)) {
         speed = -speed;
      }

      if (sideways && (facing == Direction.SOUTH || facing == Direction.WEST) || vertical && facing == Direction.EAST) {
         speed = -speed;
      }

      float rotX = (float)(
         (!diagonal && beltSlope != BeltSlope.HORIZONTAL ? 90 : 0) + (downward ? 180 : 0) + (sideways ? 90 : 0) + (vertical && alongZ ? 180 : 0)
      );
      float rotY = facing.toYRot()
         + (float)(diagonal ^ alongX && !downward ? 180 : 0)
         + (float)(sideways && alongZ ? 180 : 0)
         + (float)(vertical && alongX ? 90 : 0);
      float rotZ = (float)((sideways ? 90 : 0) + (vertical && alongX ? 90 : 0));
      Quaternionf q = new Quaternionf().rotationXYZ(rotX * (float) (Math.PI / 180.0), rotY * (float) (Math.PI / 180.0), rotZ * (float) (Math.PI / 180.0));
      key.setSpriteShift(spriteShift, 1.0F, diagonal ? 0.375F : 0.5F)
         .position(this.getVisualPosition())
         .rotation(q)
         .speed(0.0F, speed * 0.001984127F)
         .offset(0.0F, bottom ? 0.5F : 0.0F)
         .colorRgb(RotatingInstance.colorFromBE((KineticBlockEntity)this.blockEntity))
         .setChanged();
      return key;
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
      if (this.pulley != null) {
         consumer.accept(this.pulley);
      }

      for (ScrollInstance key : this.belts) {
         consumer.accept(key);
      }
   }
}

package com.simibubi.create.content.trains.entity;

import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.BogeySizes;
import com.simibubi.create.content.trains.bogey.BogeyStyle;
import com.simibubi.create.content.trains.graph.DimensionPalette;
import com.simibubi.create.content.trains.graph.TrackGraph;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CarriageBogey {
   public static final StreamCodec<RegistryFriendlyByteBuf, CarriageBogey> STREAM_CODEC = StreamCodec.composite(
      AbstractBogeyBlock.STREAM_CODEC,
      bogey -> bogey.type,
      ByteBufCodecs.BOOL,
      bogey -> bogey.upsideDown,
      ByteBufCodecs.COMPOUND_TAG,
      bogey -> bogey.bogeyData,
      CarriageBogey::new
   );
   public static final String UPSIDE_DOWN_KEY = "UpsideDown";
   public Carriage carriage;
   boolean isLeading;
   public CompoundTag bogeyData;
   AbstractBogeyBlock<?> type;
   boolean upsideDown;
   Couple<TravellingPoint> points;
   LerpedFloat wheelAngle;
   LerpedFloat yaw;
   LerpedFloat pitch;
   public Couple<Vec3> couplingAnchors;
   int derailAngle;

   public CarriageBogey(AbstractBogeyBlock<?> type, boolean upsideDown, CompoundTag bogeyData) {
      this(type, upsideDown, bogeyData, new TravellingPoint(), new TravellingPoint());
   }

   public CarriageBogey(AbstractBogeyBlock<?> type, boolean upsideDown, CompoundTag bogeyData, TravellingPoint point, TravellingPoint point2) {
      this.type = type;
      this.upsideDown = type.canBeUpsideDown() && upsideDown;
      point.upsideDown = this.upsideDown;
      point2.upsideDown = this.upsideDown;
      if (bogeyData == null || bogeyData.isEmpty()) {
         bogeyData = this.createBogeyData();
      }

      bogeyData.putBoolean("UpsideDown", upsideDown);
      this.bogeyData = bogeyData;
      this.points = Couple.create(point, point2);
      this.wheelAngle = LerpedFloat.angular();
      this.yaw = LerpedFloat.angular();
      this.pitch = LerpedFloat.angular();
      this.derailAngle = Create.RANDOM.nextInt(60) - 30;
      this.couplingAnchors = Couple.create(null, null);
   }

   public ResourceKey<Level> getDimension() {
      TravellingPoint leading = this.leading();
      TravellingPoint trailing = this.trailing();
      if (leading.edge == null || trailing.edge == null) {
         return null;
      } else if (!leading.edge.isInterDimensional() && !trailing.edge.isInterDimensional()) {
         ResourceKey<Level> dimension1 = leading.node1.getLocation().dimension;
         ResourceKey<Level> dimension2 = trailing.node1.getLocation().dimension;
         return dimension1.equals(dimension2) ? dimension1 : null;
      } else {
         return null;
      }
   }

   public void updateAngles(CarriageContraptionEntity entity, double distanceMoved) {
      double angleDiff = 360.0 * distanceMoved / ((Math.PI * 2) * this.type.getWheelRadius());
      float xRot = 0.0F;
      float yRot = 0.0F;
      if (this.leading().edge != null && !this.carriage.train.derailed) {
         if (!entity.level().dimension().equals(this.getDimension())) {
            yRot = -90.0F + entity.yaw;
            xRot = 0.0F;
         } else {
            Vec3 positionVec = this.leading().getPosition(this.carriage.train.graph);
            Vec3 coupledVec = this.trailing().getPosition(this.carriage.train.graph);
            double diffX = positionVec.x - coupledVec.x;
            double diffY = positionVec.y - coupledVec.y;
            double diffZ = positionVec.z - coupledVec.z;
            yRot = AngleHelper.deg(Mth.atan2(diffZ, diffX)) + 90.0F;
            xRot = AngleHelper.deg(Math.atan2(diffY, Math.sqrt(diffX * diffX + diffZ * diffZ)));
         }
      } else {
         yRot = -90.0F + entity.yaw - (float)this.derailAngle;
      }

      double newWheelAngle = ((double)this.wheelAngle.getValue() - angleDiff) % 360.0;

      for (boolean twice : Iterate.trueAndFalse) {
         if (!twice || entity.firstPositionUpdate) {
            this.wheelAngle.setValue(newWheelAngle);
            this.pitch.setValue((double)xRot);
            this.yaw.setValue((double)(-yRot));
         }
      }
   }

   public TravellingPoint leading() {
      TravellingPoint point = (TravellingPoint)this.points.getFirst();
      point.upsideDown = this.isUpsideDown();
      return point;
   }

   public TravellingPoint trailing() {
      TravellingPoint point = (TravellingPoint)this.points.getSecond();
      point.upsideDown = this.isUpsideDown();
      return point;
   }

   public double getStress() {
      if (this.getDimension() == null) {
         return 0.0;
      } else {
         return this.carriage.train.derailed
            ? 0.0
            : this.type.getWheelPointSpacing()
               - this.leading().getPosition(this.carriage.train.graph).distanceTo(this.trailing().getPosition(this.carriage.train.graph));
      }
   }

   @Nullable
   public Vec3 getAnchorPosition() {
      return this.getAnchorPosition(false);
   }

   @Nullable
   public Vec3 getAnchorPosition(boolean flipUpsideDown) {
      return this.leading().edge == null
         ? null
         : ((TravellingPoint)this.points.getFirst())
            .getPosition(this.carriage.train.graph, flipUpsideDown)
            .add(((TravellingPoint)this.points.getSecond()).getPosition(this.carriage.train.graph, flipUpsideDown))
            .scale(0.5);
   }

   public void updateCouplingAnchor(Vec3 entityPos, float entityXRot, float entityYRot, int bogeySpacing, float partialTicks, boolean leading) {
      boolean selfUpsideDown = this.isUpsideDown();
      boolean leadingUpsideDown = this.carriage.leadingBogey().isUpsideDown();
      Vec3 thisOffset = this.type.getConnectorAnchorOffset(selfUpsideDown);
      thisOffset = thisOffset.multiply(1.0, 1.0, leading ? -1.0 : 1.0);
      thisOffset = VecHelper.rotate(thisOffset, (double)this.pitch.getValue(partialTicks), Axis.X);
      thisOffset = VecHelper.rotate(thisOffset, (double)this.yaw.getValue(partialTicks), Axis.Y);
      thisOffset = VecHelper.rotate(thisOffset, (double)(-entityYRot - 90.0F), Axis.Y);
      thisOffset = VecHelper.rotate(thisOffset, (double)entityXRot, Axis.X);
      thisOffset = VecHelper.rotate(thisOffset, -180.0, Axis.Y);
      thisOffset = thisOffset.add(0.0, 0.0, leading ? 0.0 : (double)(-bogeySpacing));
      thisOffset = VecHelper.rotate(thisOffset, 180.0, Axis.Y);
      thisOffset = VecHelper.rotate(thisOffset, (double)(-entityXRot), Axis.X);
      thisOffset = VecHelper.rotate(thisOffset, (double)(entityYRot + 90.0F), Axis.Y);
      if (selfUpsideDown != leadingUpsideDown) {
         thisOffset = thisOffset.add(0.0, selfUpsideDown ? -2.0 : 2.0, 0.0);
      }

      this.couplingAnchors.set(leading, entityPos.add(thisOffset));
   }

   public CompoundTag write(DimensionPalette dimensions) {
      CompoundTag tag = new CompoundTag();
      tag.putString("Type", RegisteredObjectsHelper.getKeyOrThrow(this.type).toString());
      tag.put("Points", this.points.serializeEach(tp -> tp.write(dimensions)));
      tag.putBoolean("UpsideDown", this.upsideDown);
      this.bogeyData.putBoolean("UpsideDown", this.upsideDown);
      NBTHelper.writeResourceLocation(this.bogeyData, "BogeyStyle", this.getStyle().id);
      tag.put("BogeyData", this.bogeyData);
      return tag;
   }

   public static CarriageBogey read(CompoundTag tag, TrackGraph graph, DimensionPalette dimensions) {
      ResourceLocation location = ResourceLocation.parse(tag.getString("Type"));
      AbstractBogeyBlock<?> type = (AbstractBogeyBlock<?>)BuiltInRegistries.BLOCK.get(location);
      boolean upsideDown = tag.getBoolean("UpsideDown");
      Couple<TravellingPoint> points = Couple.deserializeEach(tag.getList("Points", 10), c -> TravellingPoint.read(c, graph, dimensions));
      CompoundTag data = tag.getCompound("BogeyData");
      return new CarriageBogey(type, upsideDown, data, (TravellingPoint)points.getFirst(), (TravellingPoint)points.getSecond());
   }

   public BogeyStyle getStyle() {
      ResourceLocation location = NBTHelper.readResourceLocation(this.bogeyData, "BogeyStyle");
      BogeyStyle style = AllBogeyStyles.BOGEY_STYLES.get(location);
      return style != null ? style : AllBogeyStyles.STANDARD;
   }

   public BogeySizes.BogeySize getSize() {
      return this.type.getSize();
   }

   private CompoundTag createBogeyData() {
      BogeyStyle style = this.type != null ? this.type.getDefaultStyle() : AllBogeyStyles.STANDARD;
      CompoundTag nbt = style.defaultData != null ? style.defaultData : new CompoundTag();
      NBTHelper.writeResourceLocation(nbt, "BogeyStyle", style.id);
      nbt.putBoolean("UpsideDown", this.isUpsideDown());
      return nbt;
   }

   void setLeading() {
      this.isLeading = true;
   }

   public boolean isUpsideDown() {
      return this.type.canBeUpsideDown() && this.upsideDown;
   }
}

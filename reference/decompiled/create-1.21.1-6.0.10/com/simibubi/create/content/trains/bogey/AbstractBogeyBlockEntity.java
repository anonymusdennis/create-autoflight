package com.simibubi.create.content.trains.bogey;

import com.simibubi.create.AllBogeyStyles;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractBogeyBlockEntity extends CachedRenderBBBlockEntity {
   public static final String BOGEY_STYLE_KEY = "BogeyStyle";
   public static final String BOGEY_DATA_KEY = "BogeyData";
   private CompoundTag bogeyData;
   LerpedFloat virtualAnimation = LerpedFloat.angular();

   public AbstractBogeyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public abstract BogeyStyle getDefaultStyle();

   public CompoundTag getBogeyData() {
      if (this.bogeyData == null || !this.bogeyData.contains("BogeyStyle")) {
         this.bogeyData = this.createBogeyData();
      }

      return this.bogeyData;
   }

   public void setBogeyData(@NotNull CompoundTag newData) {
      if (!newData.contains("BogeyStyle")) {
         ResourceLocation style = this.getDefaultStyle().id;
         NBTHelper.writeResourceLocation(newData, "BogeyStyle", style);
      }

      this.bogeyData = newData;
   }

   public void setBogeyStyle(@NotNull BogeyStyle style) {
      ResourceLocation location = style.id;
      CompoundTag data = this.getBogeyData();
      NBTHelper.writeResourceLocation(data, "BogeyStyle", location);
      this.markUpdated();
   }

   @NotNull
   public BogeyStyle getStyle() {
      CompoundTag data = this.getBogeyData();
      ResourceLocation currentStyle = NBTHelper.readResourceLocation(data, "BogeyStyle");
      BogeyStyle style = AllBogeyStyles.BOGEY_STYLES.get(currentStyle);
      if (style == null) {
         this.setBogeyStyle(this.getDefaultStyle());
         return this.getStyle();
      } else {
         return style;
      }
   }

   protected void saveAdditional(@NotNull CompoundTag tag, @NotNull Provider registries) {
      CompoundTag data = this.getBogeyData();
      if (data != null) {
         tag.put("BogeyData", data);
      }

      super.saveAdditional(tag, registries);
   }

   protected void loadAdditional(@NotNull CompoundTag tag, @NotNull Provider registries) {
      if (tag.contains("BogeyData")) {
         this.bogeyData = tag.getCompound("BogeyData");
      } else {
         this.bogeyData = this.createBogeyData();
      }

      super.loadAdditional(tag, registries);
   }

   private CompoundTag createBogeyData() {
      CompoundTag nbt = new CompoundTag();
      NBTHelper.writeResourceLocation(nbt, "BogeyStyle", this.getDefaultStyle().id);
      boolean upsideDown = false;
      if (this.getBlockState().getBlock() instanceof AbstractBogeyBlock<?> bogeyBlock) {
         upsideDown = bogeyBlock.isUpsideDown(this.getBlockState());
      }

      nbt.putBoolean("UpsideDown", upsideDown);
      return nbt;
   }

   @Override
   protected AABB createRenderBoundingBox() {
      return super.createRenderBoundingBox().inflate(2.0);
   }

   public float getVirtualAngle(float partialTicks) {
      return this.virtualAnimation.getValue(partialTicks);
   }

   public void animate(float distanceMoved) {
      BlockState blockState = this.getBlockState();
      if (blockState.getBlock() instanceof AbstractBogeyBlock<?> type) {
         double var8 = (double)(360.0F * distanceMoved) / ((Math.PI * 2) * type.getWheelRadius());
         double newWheelAngle = ((double)this.virtualAnimation.getValue() - var8) % 360.0;
         this.virtualAnimation.setValue(newWheelAngle);
      }
   }

   private void markUpdated() {
      this.setChanged();
      Level level = this.getLevel();
      if (level != null) {
         level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
      }
   }
}

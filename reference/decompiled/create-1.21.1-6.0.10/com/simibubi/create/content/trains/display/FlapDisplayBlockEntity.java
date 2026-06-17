package com.simibubi.create.content.trains.display;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.DyeHelper;
import com.simibubi.create.foundation.utility.DynamicComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class FlapDisplayBlockEntity extends KineticBlockEntity {
   public List<FlapDisplayLayout> lines;
   public boolean isController;
   public boolean isRunning;
   public int xSize;
   public int ySize;
   public DyeColor[] colour;
   public boolean[] glowingLines;
   public boolean[] manualLines;

   public FlapDisplayBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(10);
      this.isController = false;
      this.xSize = 1;
      this.ySize = 1;
      this.colour = new DyeColor[2];
      this.manualLines = new boolean[2];
      this.glowingLines = new boolean[2];
   }

   @Override
   public void initialize() {
      super.initialize();
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      this.updateControllerStatus();
   }

   public void updateControllerStatus() {
      if (!this.level.isClientSide) {
         BlockState blockState = this.getBlockState();
         if (blockState.getBlock() instanceof FlapDisplayBlock) {
            Direction leftDirection = ((Direction)blockState.getValue(FlapDisplayBlock.HORIZONTAL_FACING)).getClockWise();
            boolean shouldBeController = !(Boolean)blockState.getValue(FlapDisplayBlock.UP)
               && this.level.getBlockState(this.worldPosition.relative(leftDirection)) != blockState;
            int newXSize = 1;
            int newYSize = 1;
            if (shouldBeController) {
               for (int xOffset = 1;
                  xOffset < 32 && this.level.getBlockState(this.worldPosition.relative(leftDirection.getOpposite(), xOffset)) == blockState;
                  xOffset++
               ) {
                  newXSize++;
               }

               for (int yOffset = 0;
                  yOffset < 32
                     && this.level.getBlockState(this.worldPosition.relative(Direction.DOWN, yOffset)).getOptionalValue(FlapDisplayBlock.DOWN).orElse(false);
                  yOffset++
               ) {
                  newYSize++;
               }
            }

            if (this.isController != shouldBeController || newXSize != this.xSize || newYSize != this.ySize) {
               this.isController = shouldBeController;
               this.xSize = newXSize;
               this.ySize = newYSize;
               this.colour = Arrays.copyOf(this.colour, this.ySize * 2);
               this.glowingLines = Arrays.copyOf(this.glowingLines, this.ySize * 2);
               this.manualLines = new boolean[this.ySize * 2];
               this.lines = null;
               this.sendData();
            }
         }
      }
   }

   @Override
   public void tick() {
      super.tick();
      this.isRunning = super.isSpeedRequirementFulfilled();
      if (this.level.isClientSide && this.isRunning || this.isVirtual()) {
         int activeFlaps = 0;
         boolean instant = Math.abs(this.getSpeed()) > 128.0F;

         for (FlapDisplayLayout line : this.lines) {
            for (FlapDisplaySection section : line.getSections()) {
               activeFlaps += section.tick(instant, this.level.random);
            }
         }

         if (activeFlaps != 0) {
            float volume = Mth.clamp((float)activeFlaps / 20.0F, 0.25F, 1.5F);
            float bgVolume = Mth.clamp((float)activeFlaps / 40.0F, 0.25F, 1.0F);
            BlockPos middle = this.worldPosition.relative(this.getDirection().getClockWise(), this.xSize / 2).relative(Direction.DOWN, this.ySize / 2);
            AllSoundEvents.SCROLL_VALUE.playAt(this.level, middle, volume, 0.56F, false);
            this.level
               .playLocalSound(
                  (double)middle.getX(),
                  (double)middle.getY(),
                  (double)middle.getZ(),
                  SoundEvents.CALCITE_HIT,
                  SoundSource.BLOCKS,
                  0.35F * bgVolume,
                  1.95F,
                  false
               );
         }
      }
   }

   @Override
   protected boolean isNoisy() {
      return false;
   }

   @Override
   public boolean isSpeedRequirementFulfilled() {
      return this.isRunning;
   }

   public void applyTextManually(int lineIndex, Component componentText) {
      List<FlapDisplayLayout> lines = this.getLines();
      if (lineIndex < lines.size()) {
         FlapDisplayLayout layout = lines.get(lineIndex);
         if (!layout.isLayout("Default")) {
            layout.loadDefault(this.getMaxCharCount());
         }

         List<FlapDisplaySection> sections = layout.getSections();
         FlapDisplaySection flapDisplaySection = sections.get(0);
         if (componentText == null) {
            this.manualLines[lineIndex] = false;
            flapDisplaySection.setText(CommonComponents.EMPTY);
            this.notifyUpdate();
         } else {
            this.manualLines[lineIndex] = true;
            Component text = this.isVirtual() ? componentText : DynamicComponent.parseCustomText(this.level, this.worldPosition, componentText);
            flapDisplaySection.setText(text);
            if (this.isVirtual()) {
               flapDisplaySection.refresh(true);
            } else {
               this.notifyUpdate();
            }
         }
      }
   }

   public void setColour(int lineIndex, DyeColor color) {
      this.colour[lineIndex] = color == DyeColor.WHITE ? null : color;
      this.notifyUpdate();
   }

   public void setGlowing(int lineIndex) {
      this.glowingLines[lineIndex] = true;
      this.notifyUpdate();
   }

   public List<FlapDisplayLayout> getLines() {
      if (this.lines == null) {
         this.initDefaultSections();
      }

      return this.lines;
   }

   public void initDefaultSections() {
      this.lines = new ArrayList<>();

      for (int i = 0; i < this.ySize * 2; i++) {
         this.lines.add(new FlapDisplayLayout(this.getMaxCharCount()));
      }
   }

   public int getMaxCharCount() {
      return this.getMaxCharCount(0);
   }

   public int getMaxCharCount(int gaps) {
      return (int)(((float)this.xSize * 16.0F - 2.0F - 4.0F * (float)gaps) / 3.5F);
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putBoolean("Controller", this.isController);
      tag.putInt("XSize", this.xSize);
      tag.putInt("YSize", this.ySize);

      for (int j = 0; j < this.manualLines.length; j++) {
         if (this.manualLines[j]) {
            NBTHelper.putMarker(tag, "CustomLine" + j);
         }
      }

      for (int jx = 0; jx < this.glowingLines.length; jx++) {
         if (this.glowingLines[jx]) {
            NBTHelper.putMarker(tag, "GlowingLine" + jx);
         }
      }

      for (int jxx = 0; jxx < this.colour.length; jxx++) {
         if (this.colour[jxx] != null) {
            NBTHelper.writeEnum(tag, "Dye" + jxx, this.colour[jxx]);
         }
      }

      List<FlapDisplayLayout> lines = this.getLines();

      for (int i = 0; i < lines.size(); i++) {
         tag.put("Display" + i, lines.get(i).write(registries));
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      boolean wasActive = this.isController;
      int prevX = this.xSize;
      int prevY = this.ySize;
      this.isController = tag.getBoolean("Controller");
      this.xSize = tag.getInt("XSize");
      this.ySize = tag.getInt("YSize");
      this.manualLines = new boolean[this.ySize * 2];

      for (int i = 0; i < this.ySize * 2; i++) {
         this.manualLines[i] = tag.contains("CustomLine" + i);
      }

      this.glowingLines = new boolean[this.ySize * 2];

      for (int i = 0; i < this.ySize * 2; i++) {
         this.glowingLines[i] = tag.contains("GlowingLine" + i);
      }

      this.colour = new DyeColor[this.ySize * 2];

      for (int i = 0; i < this.ySize * 2; i++) {
         this.colour[i] = tag.contains("Dye" + i) ? (DyeColor)NBTHelper.readEnum(tag, "Dye" + i, DyeColor.class) : null;
      }

      if (clientPacket && wasActive != this.isController || prevX != this.xSize || prevY != this.ySize) {
         this.invalidateRenderBoundingBox();
         this.lines = null;
      }

      List<FlapDisplayLayout> lines = this.getLines();

      for (int i = 0; i < lines.size(); i++) {
         lines.get(i).read(tag.getCompound("Display" + i), registries);
      }
   }

   public int getLineIndexAt(double yCoord) {
      return (int)Mth.clamp(Math.floor(2.0 * ((double)this.worldPosition.getY() - yCoord + 1.0)), 0.0, (double)(this.ySize * 2));
   }

   public FlapDisplayBlockEntity getController() {
      if (this.isController) {
         return this;
      } else {
         BlockState blockState = this.getBlockState();
         if (!(blockState.getBlock() instanceof FlapDisplayBlock)) {
            return null;
         } else {
            MutableBlockPos pos = this.getBlockPos().mutable();
            Direction side = ((Direction)blockState.getValue(FlapDisplayBlock.HORIZONTAL_FACING)).getClockWise();

            for (int i = 0; i < 64; i++) {
               BlockState other = this.level.getBlockState(pos);
               if (other.getOptionalValue(FlapDisplayBlock.UP).orElse(false)) {
                  pos.move(Direction.UP);
               } else {
                  if (this.level.getBlockState(pos.relative(side)).getOptionalValue(FlapDisplayBlock.UP).orElse(true)) {
                     if (this.level.getBlockEntity(pos) instanceof FlapDisplayBlockEntity flap && flap.isController) {
                        return flap;
                     }
                     break;
                  }

                  pos.move(side);
               }
            }

            return null;
         }
      }
   }

   @Override
   protected AABB createRenderBoundingBox() {
      AABB aabb = new AABB(this.worldPosition);
      if (!this.isController) {
         return aabb;
      } else {
         Vec3i normal = this.getDirection().getClockWise().getNormal();
         return aabb.expandTowards((double)(normal.getX() * this.xSize), (double)(-this.ySize), (double)(normal.getZ() * this.xSize));
      }
   }

   public Direction getDirection() {
      return this.getBlockState().getOptionalValue(FlapDisplayBlock.HORIZONTAL_FACING).orElse(Direction.SOUTH).getOpposite();
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   public int getLineColor(int line) {
      DyeColor color = this.colour[line];
      return color == null ? -2898246 : (Integer)DyeHelper.getDyeColors(color).getFirst() | 0xFF000000;
   }

   public boolean isLineGlowing(int line) {
      return this.glowingLines[line];
   }
}

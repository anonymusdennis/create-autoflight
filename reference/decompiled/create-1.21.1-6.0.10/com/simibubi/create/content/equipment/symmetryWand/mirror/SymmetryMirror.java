package com.simibubi.create.content.equipment.symmetryWand.mirror;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class SymmetryMirror {
   public static final String EMPTY = "empty";
   public static final String PLANE = "plane";
   public static final String CROSS_PLANE = "cross_plane";
   public static final String TRIPLE_PLANE = "triple_plane";
   public static final Codec<SymmetryMirror> CODEC = RecordCodecBuilder.create(
      i -> i.group(
               Codec.INT.fieldOf("orientation_index").forGetter(SymmetryMirror::getOrientationIndex),
               Vec3.CODEC.fieldOf("position").forGetter(SymmetryMirror::getPosition),
               Codec.STRING.fieldOf("type").forGetter(SymmetryMirror::typeName),
               Codec.BOOL.fieldOf("enable").forGetter(m -> m.enable)
            )
            .apply(i, SymmetryMirror::create)
   );
   public static final StreamCodec<ByteBuf, SymmetryMirror> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.INT,
      SymmetryMirror::getOrientationIndex,
      CatnipStreamCodecs.VEC3,
      SymmetryMirror::getPosition,
      ByteBufCodecs.STRING_UTF8,
      SymmetryMirror::typeName,
      ByteBufCodecs.BOOL,
      m -> m.enable,
      SymmetryMirror::create
   );
   protected Vec3 position;
   protected StringRepresentable orientation;
   protected int orientationIndex;
   public boolean enable;

   public SymmetryMirror(Vec3 pos) {
      this.position = pos;
      this.enable = true;
      this.orientationIndex = 0;
   }

   public static List<Component> getMirrors() {
      return ImmutableList.of(
         CreateLang.translateDirect("symmetry.mirror.plane"),
         CreateLang.translateDirect("symmetry.mirror.doublePlane"),
         CreateLang.translateDirect("symmetry.mirror.triplePlane")
      );
   }

   private static SymmetryMirror create(Integer orientationIndex, Vec3 position, String type, Boolean enable) {
      SymmetryMirror element = (SymmetryMirror)(switch (type) {
         case "plane" -> new PlaneMirror(position);
         case "cross_plane" -> new CrossPlaneMirror(position);
         case "triple_plane" -> new TriplePlaneMirror(position);
         default -> new EmptyMirror(position);
      });
      element.setOrientation(orientationIndex);
      element.enable = enable;
      return element;
   }

   public StringRepresentable getOrientation() {
      return this.orientation;
   }

   public Vec3 getPosition() {
      return this.position;
   }

   public int getOrientationIndex() {
      return this.orientationIndex;
   }

   public void rotate(boolean forward) {
      this.orientationIndex += forward ? 1 : -1;
      this.setOrientation();
   }

   public void process(Map<BlockPos, BlockState> blocks) {
      Map<BlockPos, BlockState> result = new HashMap<>();

      for (BlockPos pos : blocks.keySet()) {
         result.putAll(this.process(pos, blocks.get(pos)));
      }

      blocks.putAll(result);
   }

   public abstract Map<BlockPos, BlockState> process(BlockPos var1, BlockState var2);

   protected abstract void setOrientation();

   public abstract void setOrientation(int var1);

   public abstract String typeName();

   @OnlyIn(Dist.CLIENT)
   public abstract PartialModel getModel();

   public void applyModelTransform(PoseStack ms) {
   }

   protected Vec3 getDiff(BlockPos position) {
      return this.position.scale(-1.0).add((double)position.getX(), (double)position.getY(), (double)position.getZ());
   }

   protected BlockPos getIDiff(BlockPos position) {
      Vec3 diff = this.getDiff(position);
      return new BlockPos((int)diff.x, (int)diff.y, (int)diff.z);
   }

   protected BlockState flipX(BlockState in) {
      return in.mirror(Mirror.FRONT_BACK);
   }

   protected BlockState flipY(BlockState in) {
      for (Property<?> property : in.getProperties()) {
         if (property == BlockStateProperties.HALF) {
            return (BlockState)in.cycle(property);
         }

         if (property instanceof DirectionProperty) {
            if (in.getValue(property) == Direction.DOWN) {
               return (BlockState)in.setValue(property, Direction.UP);
            }

            if (in.getValue(property) == Direction.UP) {
               return (BlockState)in.setValue(property, Direction.DOWN);
            }
         }
      }

      return in;
   }

   protected BlockState flipZ(BlockState in) {
      return in.mirror(Mirror.LEFT_RIGHT);
   }

   protected BlockState flipD1(BlockState in) {
      return in.rotate(Rotation.COUNTERCLOCKWISE_90).mirror(Mirror.FRONT_BACK);
   }

   protected BlockState flipD2(BlockState in) {
      return in.rotate(Rotation.COUNTERCLOCKWISE_90).mirror(Mirror.LEFT_RIGHT);
   }

   protected BlockPos flipX(BlockPos position) {
      BlockPos diff = this.getIDiff(position);
      return new BlockPos(position.getX() - 2 * diff.getX(), position.getY(), position.getZ());
   }

   protected BlockPos flipY(BlockPos position) {
      BlockPos diff = this.getIDiff(position);
      return new BlockPos(position.getX(), position.getY() - 2 * diff.getY(), position.getZ());
   }

   protected BlockPos flipZ(BlockPos position) {
      BlockPos diff = this.getIDiff(position);
      return new BlockPos(position.getX(), position.getY(), position.getZ() - 2 * diff.getZ());
   }

   protected BlockPos flipD2(BlockPos position) {
      BlockPos diff = this.getIDiff(position);
      return new BlockPos(position.getX() - diff.getX() + diff.getZ(), position.getY(), position.getZ() - diff.getZ() + diff.getX());
   }

   protected BlockPos flipD1(BlockPos position) {
      BlockPos diff = this.getIDiff(position);
      return new BlockPos(position.getX() - diff.getX() - diff.getZ(), position.getY(), position.getZ() - diff.getZ() - diff.getX());
   }

   public void setPosition(Vec3 pos3d) {
      this.position = pos3d;
   }

   public abstract List<Component> getAlignToolTips();

   @Override
   public final boolean equals(Object o) {
      if (this == o) {
         return true;
      } else {
         return !(o instanceof SymmetryMirror that)
            ? false
            : this.getOrientationIndex() == that.getOrientationIndex()
               && this.enable == that.enable
               && Objects.equals(this.getPosition(), that.getPosition())
               && Objects.equals(this.getOrientation(), that.getOrientation());
      }
   }

   @Override
   public int hashCode() {
      int result = Objects.hashCode(this.getPosition());
      result = 31 * result + Objects.hashCode(this.getOrientation());
      result = 31 * result + this.getOrientationIndex();
      return 31 * result + Boolean.hashCode(this.enable);
   }
}

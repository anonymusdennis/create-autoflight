package net.createmod.catnip.outliner;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.BindableTexture;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class BlockClusterOutline extends Outline {
   private final BlockClusterOutline.Cluster cluster;
   protected final Vector3f pos0Temp = new Vector3f();
   protected final Vector3f pos1Temp = new Vector3f();
   protected final Vector3f pos2Temp = new Vector3f();
   protected final Vector3f pos3Temp = new Vector3f();
   protected final Vector3f normalTemp = new Vector3f();
   protected final Vector3f originTemp = new Vector3f();

   public BlockClusterOutline(Iterable<BlockPos> positions) {
      this.cluster = new BlockClusterOutline.Cluster();
      positions.forEach(this.cluster::include);
   }

   @Override
   public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
      this.params.loadColor(this.colorTemp);
      Vector4f color = this.colorTemp;
      int lightmap = this.params.lightmap;
      boolean disableLineNormals = this.params.disableLineNormals;
      this.renderFaces(ms, buffer, camera, pt, color, lightmap);
      this.renderEdges(ms, buffer, camera, pt, color, lightmap, disableLineNormals);
   }

   protected void renderFaces(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, Vector4f color, int lightmap) {
      BindableTexture faceTexture = this.params.faceTexture;
      if (faceTexture != null) {
         if (!this.cluster.isEmpty()) {
            ms.pushPose();
            ms.translate(
               (double)this.cluster.anchor.getX() - camera.x, (double)this.cluster.anchor.getY() - camera.y, (double)this.cluster.anchor.getZ() - camera.z
            );
            Pose pose = ms.last();
            RenderType renderType = PonderRenderTypes.outlineTranslucent(faceTexture.getLocation(), true);
            VertexConsumer consumer = buffer.getLateBuffer(renderType);
            this.cluster.visibleFaces.forEach((face, axisDirection) -> {
               Direction direction = Direction.get(axisDirection, face.axis);
               BlockPos pos = face.pos;
               if (axisDirection == AxisDirection.POSITIVE) {
                  pos = pos.relative(direction.getOpposite());
               }

               this.bufferBlockFace(pose, consumer, pos, direction, color, lightmap);
            });
            ms.popPose();
         }
      }
   }

   protected void renderEdges(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, Vector4f color, int lightmap, boolean disableNormals) {
      float lineWidth = this.params.getLineWidth();
      if (lineWidth != 0.0F) {
         if (!this.cluster.isEmpty()) {
            ms.pushPose();
            ms.translate(
               (double)this.cluster.anchor.getX() - camera.x, (double)this.cluster.anchor.getY() - camera.y, (double)this.cluster.anchor.getZ() - camera.z
            );
            Pose pose = ms.last();
            VertexConsumer consumer = buffer.getBuffer(PonderRenderTypes.outlineSolid());
            this.cluster.visibleEdges.forEach(edge -> {
               BlockPos pos = edge.pos;
               Vector3f origin = this.originTemp;
               origin.set((float)pos.getX(), (float)pos.getY(), (float)pos.getZ());
               Direction direction = Direction.get(AxisDirection.POSITIVE, edge.axis);
               this.bufferCuboidLine(pose, consumer, origin, direction, 1.0F, lineWidth, color, lightmap, disableNormals);
            });
            ms.popPose();
         }
      }
   }

   public static void loadFaceData(Direction face, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3, Vector3f normal) {
      switch (face) {
         case DOWN:
            pos0.set(0.0F, 0.0F, 1.0F);
            pos1.set(0.0F, 0.0F, 0.0F);
            pos2.set(1.0F, 0.0F, 0.0F);
            pos3.set(1.0F, 0.0F, 1.0F);
            normal.set(0.0F, -1.0F, 0.0F);
            break;
         case UP:
            pos0.set(0.0F, 1.0F, 0.0F);
            pos1.set(0.0F, 1.0F, 1.0F);
            pos2.set(1.0F, 1.0F, 1.0F);
            pos3.set(1.0F, 1.0F, 0.0F);
            normal.set(0.0F, 1.0F, 0.0F);
            break;
         case NORTH:
            pos0.set(1.0F, 1.0F, 0.0F);
            pos1.set(1.0F, 0.0F, 0.0F);
            pos2.set(0.0F, 0.0F, 0.0F);
            pos3.set(0.0F, 1.0F, 0.0F);
            normal.set(0.0F, 0.0F, -1.0F);
            break;
         case SOUTH:
            pos0.set(0.0F, 1.0F, 1.0F);
            pos1.set(0.0F, 0.0F, 1.0F);
            pos2.set(1.0F, 0.0F, 1.0F);
            pos3.set(1.0F, 1.0F, 1.0F);
            normal.set(0.0F, 0.0F, 1.0F);
            break;
         case WEST:
            pos0.set(0.0F, 1.0F, 0.0F);
            pos1.set(0.0F, 0.0F, 0.0F);
            pos2.set(0.0F, 0.0F, 1.0F);
            pos3.set(0.0F, 1.0F, 1.0F);
            normal.set(-1.0F, 0.0F, 0.0F);
            break;
         case EAST:
            pos0.set(1.0F, 1.0F, 1.0F);
            pos1.set(1.0F, 0.0F, 1.0F);
            pos2.set(1.0F, 0.0F, 0.0F);
            pos3.set(1.0F, 1.0F, 0.0F);
            normal.set(1.0F, 0.0F, 0.0F);
      }
   }

   public static void addPos(float x, float y, float z, Vector3f pos0, Vector3f pos1, Vector3f pos2, Vector3f pos3) {
      pos0.add(x, y, z);
      pos1.add(x, y, z);
      pos2.add(x, y, z);
      pos3.add(x, y, z);
   }

   protected void bufferBlockFace(Pose pose, VertexConsumer consumer, BlockPos pos, Direction face, Vector4f color, int lightmap) {
      Vector3f pos0 = this.pos0Temp;
      Vector3f pos1 = this.pos1Temp;
      Vector3f pos2 = this.pos2Temp;
      Vector3f pos3 = this.pos3Temp;
      Vector3f normal = this.normalTemp;
      loadFaceData(face, pos0, pos1, pos2, pos3, normal);
      addPos(
         (float)pos.getX() + (float)face.getStepX() / 128.0F,
         (float)pos.getY() + (float)face.getStepY() / 128.0F,
         (float)pos.getZ() + (float)face.getStepZ() / 128.0F,
         pos0,
         pos1,
         pos2,
         pos3
      );
      this.bufferQuad(pose, consumer, pos0, pos1, pos2, pos3, color, lightmap, normal);
   }

   private static class Cluster {
      private BlockPos anchor;
      private final Map<BlockClusterOutline.MergeEntry, AxisDirection> visibleFaces;
      private final Set<BlockClusterOutline.MergeEntry> visibleEdges = new HashSet<>();

      public Cluster() {
         this.visibleFaces = new HashMap<>();
      }

      public boolean isEmpty() {
         return this.anchor == null;
      }

      public void include(BlockPos pos) {
         if (this.anchor == null) {
            this.anchor = pos;
         }

         pos = pos.subtract(this.anchor);

         for (Axis axis : Iterate.axes) {
            Direction direction = Direction.get(AxisDirection.POSITIVE, axis);

            for (int offset : Iterate.zeroAndOne) {
               BlockClusterOutline.MergeEntry entry = new BlockClusterOutline.MergeEntry(axis, pos.relative(direction, offset));
               if (this.visibleFaces.remove(entry) == null) {
                  this.visibleFaces.put(entry, offset == 0 ? AxisDirection.NEGATIVE : AxisDirection.POSITIVE);
               }
            }
         }

         for (Axis axis : Iterate.axes) {
            for (Axis axis2 : Iterate.axes) {
               if (axis != axis2) {
                  for (Axis axis3 : Iterate.axes) {
                     if (axis != axis3 && axis2 != axis3) {
                        Direction direction = Direction.get(AxisDirection.POSITIVE, axis2);
                        Direction direction2 = Direction.get(AxisDirection.POSITIVE, axis3);

                        for (int offsetx : Iterate.zeroAndOne) {
                           BlockPos entryPos = pos.relative(direction, offsetx);

                           for (int offset2 : Iterate.zeroAndOne) {
                              entryPos = entryPos.relative(direction2, offset2);
                              BlockClusterOutline.MergeEntry entry = new BlockClusterOutline.MergeEntry(axis, entryPos);
                              if (!this.visibleEdges.remove(entry)) {
                                 this.visibleEdges.add(entry);
                              }
                           }
                        }
                     }
                  }
                  break;
               }
            }
         }
      }
   }

   private static class MergeEntry {
      private final Axis axis;
      private final BlockPos pos;

      public MergeEntry(Axis axis, BlockPos pos) {
         this.axis = axis;
         this.pos = pos;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) {
            return true;
         } else {
            return !(o instanceof BlockClusterOutline.MergeEntry other) ? false : this.axis == other.axis && this.pos.equals(other.pos);
         }
      }

      @Override
      public int hashCode() {
         return this.pos.hashCode() * 31 + this.axis.ordinal();
      }
   }
}

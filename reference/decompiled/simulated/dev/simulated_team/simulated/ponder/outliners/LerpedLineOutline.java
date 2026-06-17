package dev.simulated_team.simulated.ponder.outliners;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.simulated_team.simulated.ponder.records.PonderLineRecord;
import net.createmod.catnip.outliner.LineOutline;
import net.createmod.catnip.render.PonderRenderTypes;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class LerpedLineOutline extends LineOutline {
   Vector3d prevStart;
   Vector3d prevEnd;

   public LerpedLineOutline(PonderLineRecord initialLine) {
      this.prevStart = JOMLConversion.toJOML(initialLine.startPos());
      this.prevEnd = JOMLConversion.toJOML(initialLine.endPos());
   }

   public LerpedLineOutline(Vec3 initialPoint) {
      this.prevStart = JOMLConversion.toJOML(initialPoint);
      this.prevEnd = JOMLConversion.toJOML(initialPoint);
   }

   public void update(Vec3 prevStart, Vec3 prevEnd, Vec3 start, Vec3 end) {
      this.prevStart = JOMLConversion.toJOML(prevStart);
      this.prevEnd = JOMLConversion.toJOML(prevEnd);
      this.set(start, end);
   }

   public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt) {
      float width = this.params.getLineWidth();
      if (width != 0.0F) {
         VertexConsumer consumer = buffer.getBuffer(PonderRenderTypes.outlineSolid());
         this.params.loadColor(this.colorTemp);
         Vector4f color = this.colorTemp;
         int lightmap = 15728880;
         boolean disableLineNormals = false;
         this.renderInner(ms, consumer, camera, pt, width, color, 15728880, false);
      }
   }

   protected void renderInner(PoseStack ms, VertexConsumer consumer, Vec3 camera, float pt, float width, Vector4f color, int lightmap, boolean disableNormals) {
      this.bufferCuboidLine(
         ms,
         consumer,
         camera,
         interpolatePoint(this.prevStart, this.start, pt),
         interpolatePoint(this.prevEnd, this.end, pt),
         width,
         color,
         lightmap,
         disableNormals
      );
   }

   public static Vector3d interpolatePoint(Vector3d current, Vector3d target, float pt) {
      return new Vector3d(Mth.lerp((double)pt, current.x, target.x), Mth.lerp((double)pt, current.y, target.y), Mth.lerp((double)pt, current.z, target.z));
   }
}

package dev.simulated_team.simulated.ponder.instructions;

import dev.simulated_team.simulated.ponder.outliners.LerpedLineOutline;
import dev.simulated_team.simulated.ponder.records.PonderLineRecord;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.instruction.TickingInstruction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WindstreamInstruction extends TickingInstruction {
   PonderLineRecord line;
   final int size;
   final PonderPalette color;
   final String slot;
   final int lerpTicks;
   final AABB bb;
   final Vec3 windDir;
   LerpedLineOutline lerpedLine;
   Vec3 oldLineStart;
   Vec3 oldLineEnd;

   public WindstreamInstruction(AABB bb, Vec3 windDir, int size, PonderPalette color, String slot, int lerpTicks, int holdTicks) {
      super(false, 2 * lerpTicks + holdTicks + 1);
      this.size = size;
      this.color = color;
      this.slot = slot;
      this.lerpTicks = lerpTicks;
      this.bb = bb;
      this.windDir = windDir;
   }

   protected void firstTick(PonderScene scene) {
      super.firstTick(scene);
      Vec3 lineBase = new Vec3(
         (this.bb.maxX - this.bb.minX) * scene.getWorld().getRandom().nextDouble() + this.bb.minX,
         (this.bb.maxY - this.bb.minY) * scene.getWorld().getRandom().nextDouble() + this.bb.minY,
         (this.bb.maxZ - this.bb.minZ) * scene.getWorld().getRandom().nextDouble() + this.bb.minZ
      );
      this.line = PonderLineRecord.withOffset(lineBase, lineBase.add(this.windDir));
      Vec3 offset = new Vec3(87.0, 0.0, 0.0);
      this.oldLineStart = lineBase.add(offset);
      this.oldLineEnd = lineBase.add(offset);
      this.lerpedLine = new LerpedLineOutline(this.line);
   }

   public void tick(PonderScene scene) {
      super.tick(scene);
      float inLerp = Mth.clamp((float)(this.totalTicks - this.remainingTicks) / (float)this.lerpTicks, 0.0F, 1.0F);
      float outLerp = Mth.clamp((float)(this.remainingTicks - 1) / (float)this.lerpTicks, 0.0F, 1.0F);
      float percentage = Math.min(inLerp, outLerp);
      Vec3 currentStartPos = this.line.startPos().subtract(this.line.endPos()).scale((double)percentage).add(this.line.endPos());
      Vec3 currentEndPos = this.line.endPos().subtract(this.line.startPos()).scale((double)percentage).add(this.line.startPos());
      this.lerpedLine
         .update(
            outLerp < inLerp ? this.line.endPos() : this.oldLineEnd,
            outLerp < inLerp ? this.oldLineStart : this.line.startPos(),
            outLerp < inLerp ? this.line.endPos() : currentEndPos,
            outLerp < inLerp ? currentStartPos : this.line.startPos()
         );
      scene.getOutliner().showOutline(this.slot, this.lerpedLine).lineWidth((float)this.size / 16.0F).colored(this.color.getColor());
      this.oldLineStart = currentStartPos;
      this.oldLineEnd = currentEndPos;
   }
}

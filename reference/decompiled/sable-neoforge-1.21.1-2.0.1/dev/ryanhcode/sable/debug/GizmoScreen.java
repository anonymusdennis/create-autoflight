package dev.ryanhcode.sable.debug;

import dev.ryanhcode.sable.SableClient;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundGizmoMoveSubLevelPacket;
import dev.ryanhcode.sable.sublevel.SubLevel;
import foundry.veil.api.network.VeilPacketManager;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class GizmoScreen extends Screen {
   private boolean dragging;
   @Nullable
   private GizmoSelection activeSelection;

   protected GizmoScreen() {
      super(Component.literal("Gizmo Mode"));
   }

   public void render(GuiGraphics guiGraphics, int i, int j, float f) {
   }

   public boolean mouseClicked(double d, double e, int i) {
      SableClientGizmoHandler gizmoHandler = SableClient.GIZMO_HANDLER;
      if (gizmoHandler.getSelection() != null) {
         this.activeSelection = gizmoHandler.getSelection();
         this.dragging = true;
      }

      return super.mouseClicked(d, e, i);
   }

   public boolean mouseReleased(double d, double e, int i) {
      this.dragging = false;
      this.activeSelection = null;
      return super.mouseReleased(d, e, i);
   }

   public boolean mouseDragged(double x, double y, int i, double f, double g) {
      SableClientGizmoHandler gizmoHandler = SableClient.GIZMO_HANDLER;
      if (this.dragging) {
         Minecraft minecraft = Minecraft.getInstance();
         ClientLevel level = minecraft.level;
         SubLevelContainer container = SubLevelContainer.getContainer(level);

         assert container != null;

         UUID subLevelID = this.activeSelection.subLevel();
         SubLevel subLevel = container.getSubLevel(subLevelID);
         if (subLevel == null) {
            this.cancel();
            return super.mouseDragged(x, y, i, f, g);
         }

         int ordinal = (this.activeSelection.axis().ordinal() + 1) % 3;
         Axis axis = Axis.VALUES[ordinal];
         Vector3d dragNormal = JOMLConversion.atLowerCornerOf(Direction.get(AxisDirection.POSITIVE, this.activeSelection.axis()).getNormal());
         Vector3d pos = JOMLConversion.toJOML(minecraft.player.getEyePosition());
         Vector3d relativePos = new Vector3d(pos).sub(subLevel.logicalPose().position());
         Vector3d planeNormal = JOMLConversion.atLowerCornerOf(Direction.get(AxisDirection.POSITIVE, axis).getNormal());
         if (relativePos.dot(planeNormal) < 0.0) {
            planeNormal.negate();
         }

         Vector3d dir = JOMLConversion.toJOML(gizmoHandler.getMouseDir());
         boolean hitsPlane = dir.dot(planeNormal) < 0.0;
         if (hitsPlane) {
            Vector3d negatedPlaneNormal = planeNormal.negate(new Vector3d());
            double d = planeNormal.dot(relativePos);
            double rayLength = d / dir.dot(negatedPlaneNormal);
            Vector3d hitPos = new Vector3d(pos).fma(rayLength, dir);
            Vector3d subLevelPos = new Vector3d(subLevel.logicalPose().position());
            subLevelPos.fma(-subLevelPos.dot(dragNormal), dragNormal, subLevelPos);
            subLevelPos.fma(hitPos.dot(dragNormal), dragNormal, subLevelPos);
            VeilPacketManager.server()
               .sendPacket(new CustomPacketPayload[]{new ServerboundGizmoMoveSubLevelPacket(this.activeSelection.subLevel(), subLevelPos)});
         }
      }

      return super.mouseDragged(x, y, i, f, g);
   }

   public boolean isPauseScreen() {
      return false;
   }

   private void cancel() {
      this.dragging = false;
      this.activeSelection = null;
   }

   public void onClose() {
      SableClient.GIZMO_HANDLER.stop();
   }
}

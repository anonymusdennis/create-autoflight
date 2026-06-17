package dev.createautoflight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.createautoflight.CreateAutoflight;
import dev.createautoflight.content.navigation.NavigationBlockEntity;
import dev.createautoflight.content.navigation.NavigationDebugSnapshot;
import dev.createautoflight.content.thrust.DynamicThrustControllerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.List;

@EventBusSubscriber(modid = CreateAutoflight.MOD_ID, value = Dist.CLIENT)
public final class NavigationDebugRenderer {
    private static final int COLOR_DEST = 0xFF00FF00;
    private static final int MAX_PATH_SEGMENTS = 64;
    private static final double RENDER_RANGE_SQ = 256.0 * 256.0;
    private static final int COLOR_WAYPOINT = 0xFFFFFF00;
    private static final int COLOR_PATH = 0xFF00FFFF;
    private static final int COLOR_CAPSULE = 0xFFFF8800;
    private static final int COLOR_BOUNDS = 0xFFFFFFFF;
    private static final int COLOR_VEL = 0xFFFF00FF;
    private static final int COLOR_APPROACH = 0xFFFF0000;

    private NavigationDebugRenderer() {}

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        if (ClientNavigationDebugCache.positions().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (mc.level.getGameTime() % 200 == 0) {
            ClientNavigationDebugCache.pruneMissing(mc.level);
        }

        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());

        for (BlockPos pos : ClientNavigationDebugCache.positions()) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (!(be instanceof NavigationBlockEntity nav)) {
                ClientNavigationDebugCache.setEnabled(pos, false);
                continue;
            }
            if (mc.player != null && mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > RENDER_RANGE_SQ) {
                continue;
            }
            NavigationDebugSnapshot snap = nav.getDebugSnapshot();
            if (snap == null || snap.mode().equals("Idle")) {
                continue;
            }
            NavigationDebugPoseHelper.LiveGeometry live = NavigationDebugPoseHelper.resolve(
                    nav, event.getPartialTick().getGameTimeDeltaPartialTick(false));
            if (live == null) {
                continue;
            }
            renderGeometry(pose, lines, live, snap, cam, nav.getSettings().getAvoidanceOffDistance());
        }

        buffers.endBatch(RenderType.lines());
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (ClientNavigationDebugCache.positions().isEmpty()
                && ClientThrustDebugCache.positions().isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        NavigationDebugSnapshot active = null;
        NavigationBlockEntity activeNav = null;
        for (BlockPos pos : ClientNavigationDebugCache.positions()) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be instanceof NavigationBlockEntity nav
                    && nav.getDebugSnapshot() != null
                    && !nav.getDebugSnapshot().mode().equals("Idle")) {
                active = nav.getDebugSnapshot();
                activeNav = nav;
                break;
            }
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        int y = 8;

        if (active != null) {
            y = drawHudLine(graphics, font, y, "Mode: " + active.mode());
            y = drawHudLine(graphics, font, y, "Approach: " + active.approachPhase());
            y = drawHudLine(graphics, font, y, "Pathfinder: " + active.pathfinderState());
            y = drawHudLine(graphics, font, y, String.format("Dist: %.1f", active.distToDest()));
            y = drawHudLine(graphics, font, y, String.format("Capsule R: %.1f", active.capsuleRadius()));
            y = drawHudLine(graphics, font, y, "Shrink: " + active.collisionShrinkActive());
            y = drawHudLine(graphics, font, y, "Brake assist: " + active.brakeAssistActive());
            y = drawHudLine(graphics, font, y, "Waypoints: " + active.pathWaypoints().size());
            if (active.closestObstacleDist() < 1e8) {
                y = drawHudLine(graphics, font, y, String.format("Closest obs: %.1f", active.closestObstacleDist()));
            }
            if (activeNav != null && activeNav.getSettings().isHelicopterMode()) {
                int maxPitch = activeNav.getSettings().getHelicopterMaxPitchDeg();
                y = drawHudLine(graphics, font, y, String.format("Heli max pitch: %d°", maxPitch));
            }
            y = drawHudLine(graphics, font, y, String.format(
                    "Target att: Pitch %.1f°  Yaw %.1f°  Roll %.1f°",
                    active.targetPitchDeg(), active.targetYawDeg(), active.targetRollDeg()
            ));
            y = drawHudLine(graphics, font, y, String.format(
                    "Current att: Pitch %.1f°  Yaw %.1f°  Roll %.1f°",
                    active.currentPitchDeg(), active.currentYawDeg(), active.currentRollDeg()
            ));
            y += 10;
        }

        if (active == null && ClientThrustDebugCache.positions().isEmpty()) {
            return;
        }

        for (BlockPos pos : ClientThrustDebugCache.positions()) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (!(be instanceof DynamicThrustControllerBlockEntity controller)) {
                continue;
            }
            y = drawHudLine(graphics, font, y, "Thrust: " + controller.getStatusText()
                    + " GB=" + controller.getStatusGearboxCount());
            y = drawHudLine(graphics, font, y, String.format(
                    "Meas Y: %.1f  Hover Y: %.1f",
                    controller.getStatusMeasuredY(), controller.getStatusHoverY()
            ));
            drawHudLine(graphics, font, y, String.format(
                    "Demand: %.1f, %.1f, %.1f",
                    controller.getStatusDemandX(), controller.getStatusDemandY(), controller.getStatusDemandZ()
            ));
            break;
        }
    }

    private static int drawHudLine(GuiGraphics graphics, Font font, int y, String text) {
        graphics.drawString(font, text, 8, y, 0xFFFFFF, true);
        return y + 10;
    }

    private static void renderGeometry(
            PoseStack pose,
            VertexConsumer lines,
            NavigationDebugPoseHelper.LiveGeometry live,
            NavigationDebugSnapshot snap,
            Vec3 cam,
            int avoidanceRadius
    ) {
        if (live.destinationWorld() != null) {
            drawMarkerCube(pose, lines, live.destinationWorld(), cam, 0.6, COLOR_DEST);
            drawBeacon(pose, lines, live.destinationWorld(), cam, COLOR_DEST);
        }
        if (live.activeWaypointWorld() != null
                && (live.destinationWorld() == null || live.activeWaypointWorld().distanceTo(live.destinationWorld()) > 0.5)) {
            drawMarkerCube(pose, lines, live.activeWaypointWorld(), cam, 0.4, COLOR_WAYPOINT);
        }

        List<Vec3> path = live.pathWaypoints();
        int pathLimit = Math.min(path.size() - 1, MAX_PATH_SEGMENTS);
        for (int i = 0; i < pathLimit; i++) {
            drawLine(pose, lines, path.get(i), path.get(i + 1), cam, COLOR_PATH);
        }

        if (live.capsuleP0() != null && live.capsuleP1() != null) {
            drawLine(pose, lines, live.capsuleP0(), live.capsuleP1(), cam, COLOR_CAPSULE);
            drawCapsuleRing(pose, lines, live.capsuleP0(), cam, live.capsuleRadius(), COLOR_CAPSULE);
            drawCapsuleRing(pose, lines, live.capsuleP1(), cam, live.capsuleRadius(), COLOR_CAPSULE);
        }

        if (live.boundsMinWorld() != null && live.boundsMaxWorld() != null) {
            drawAabb(pose, lines, live.boundsMinWorld(), live.boundsMaxWorld(), cam, COLOR_BOUNDS);
        }

        if (live.assemblyCenterWorld() != null && live.desiredVelocityWorld() != null) {
            Vec3 end = live.assemblyCenterWorld().add(live.desiredVelocityWorld());
            drawLine(pose, lines, live.assemblyCenterWorld(), end, cam, COLOR_VEL);
        }

        if (snap.collisionShrinkActive() && live.destinationWorld() != null) {
            drawApproachSphere(pose, lines, live.destinationWorld(), cam, avoidanceRadius, COLOR_APPROACH);
        }
    }

    private static void drawLine(PoseStack pose, VertexConsumer lines, Vec3 a, Vec3 b, Vec3 cam, int color) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float bl = (color & 0xFF) / 255f;
        Matrix4f mat = pose.last().pose();
        lines.addVertex(mat, (float) (a.x - cam.x), (float) (a.y - cam.y), (float) (a.z - cam.z))
                .setColor(r, g, bl, 1f).setNormal(0, 1, 0);
        lines.addVertex(mat, (float) (b.x - cam.x), (float) (b.y - cam.y), (float) (b.z - cam.z))
                .setColor(r, g, bl, 1f).setNormal(0, 1, 0);
    }

    private static void drawMarkerCube(PoseStack pose, VertexConsumer lines, Vec3 center, Vec3 cam, double half, int color) {
        Vec3 min = center.add(-half, -half, -half);
        Vec3 max = center.add(half, half, half);
        drawAabb(pose, lines, min, max, cam, color);
    }

    private static void drawAabb(PoseStack pose, VertexConsumer lines, Vec3 min, Vec3 max, Vec3 cam, int color) {
        Vec3[] corners = {
                new Vec3(min.x, min.y, min.z), new Vec3(max.x, min.y, min.z),
                new Vec3(max.x, min.y, max.z), new Vec3(min.x, min.y, max.z),
                new Vec3(min.x, max.y, min.z), new Vec3(max.x, max.y, min.z),
                new Vec3(max.x, max.y, max.z), new Vec3(min.x, max.y, max.z)
        };
        int[][] edges = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
        for (int[] e : edges) {
            drawLine(pose, lines, corners[e[0]], corners[e[1]], cam, color);
        }
    }

    private static void drawBeacon(PoseStack pose, VertexConsumer lines, Vec3 base, Vec3 cam, int color) {
        drawLine(pose, lines, base, base.add(0, 32, 0), cam, color);
    }

    private static void drawCapsuleRing(PoseStack pose, VertexConsumer lines, Vec3 center, Vec3 cam, float radius, int color) {
        int segments = 12;
        Vec3 prev = null;
        for (int i = 0; i <= segments; i++) {
            double ang = (Math.PI * 2 * i) / segments;
            Vec3 p = center.add(Math.cos(ang) * radius, 0, Math.sin(ang) * radius);
            if (prev != null) {
                drawLine(pose, lines, prev, p, cam, color);
            }
            prev = p;
        }
    }

    private static void drawApproachSphere(PoseStack pose, VertexConsumer lines, Vec3 center, Vec3 cam, int radius, int color) {
        int segments = 16;
        for (int ring = 0; ring < 3; ring++) {
            Vec3 prev = null;
            for (int i = 0; i <= segments; i++) {
                double ang = (Math.PI * 2 * i) / segments;
                Vec3 p = switch (ring) {
                    case 0 -> center.add(Math.cos(ang) * radius, 0, Math.sin(ang) * radius);
                    case 1 -> center.add(Math.cos(ang) * radius, Math.sin(ang) * radius, 0);
                    default -> center.add(0, Math.cos(ang) * radius, Math.sin(ang) * radius);
                };
                if (prev != null) {
                    drawLine(pose, lines, prev, p, cam, color);
                }
                prev = p;
            }
        }
    }
}

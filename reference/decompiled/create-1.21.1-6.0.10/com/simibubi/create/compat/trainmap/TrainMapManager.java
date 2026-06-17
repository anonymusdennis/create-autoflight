package com.simibubi.create.compat.trainmap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.CreateClient;
import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.content.trains.graph.TrackNode;
import com.simibubi.create.content.trains.graph.TrackNodeLocation;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CClient;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.FastColor.ABGR32;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TrainMapManager {
   static final int PHASE_BACKGROUND = 0;
   static final int PHASE_STRAIGHTS = 1;
   static final int PHASE_CURVES = 2;

   public static void tick() {
      ResourceKey<Level> playerDimension = Minecraft.getInstance().level.dimension();
      if (Mods.XAEROWORLDMAP.isLoaded() && XaeroTrainMap.isMapOpen(Minecraft.getInstance().screen)) {
         ResourceKey<Level> renderedDimension = XaeroTrainMap.getRenderedDimension();
         tick(renderedDimension != null ? renderedDimension : playerDimension);
      } else {
         tick(playerDimension);
      }
   }

   public static void tick(ResourceKey<Level> dimension) {
      TrainMapRenderer map = TrainMapRenderer.INSTANCE;
      if (map.trackingVersion != CreateClient.RAILWAYS.version
         || map.trackingDim != dimension
         || map.trackingTheme != AllConfigs.client().trainMapColorTheme.get()) {
         redrawAll(dimension);
      }
   }

   public static List<FormattedText> renderAndPick(GuiGraphics graphics, int mouseX, int mouseY, boolean linearFiltering, Rect2i bounds) {
      Object hoveredElement = null;
      int offScreenMargin = 32;
      bounds.setX(bounds.getX() - offScreenMargin);
      bounds.setY(bounds.getY() - offScreenMargin);
      bounds.setWidth(bounds.getWidth() + 2 * offScreenMargin);
      bounds.setHeight(bounds.getHeight() + 2 * offScreenMargin);
      TrainMapRenderer.INSTANCE.render(graphics, linearFiltering, bounds);
      hoveredElement = drawTrains(graphics, mouseX, mouseY, hoveredElement, bounds);
      hoveredElement = drawPoints(graphics, mouseX, mouseY, hoveredElement, bounds);
      graphics.bufferSource().endBatch();
      if (hoveredElement instanceof GlobalStation station) {
         return List.of(Component.literal(station.name));
      } else {
         return hoveredElement instanceof Train train ? listTrainDetails(train) : null;
      }
   }

   public static void renderToggleWidget(GuiGraphics graphics, int x, int y) {
      boolean enabled = (Boolean)AllConfigs.client().showTrainMapOverlay.get();
      if (!CreateClient.RAILWAYS.trackNetworks.isEmpty()) {
         RenderSystem.enableBlend();
         PoseStack pose = graphics.pose();
         pose.pushPose();
         pose.translate(0.0F, 0.0F, 300.0F);
         AllGuiTextures.TRAINMAP_TOGGLE_PANEL.render(graphics, x, y);
         (enabled ? AllGuiTextures.TRAINMAP_TOGGLE_ON : AllGuiTextures.TRAINMAP_TOGGLE_OFF).render(graphics, x + 18, y + 3);
         pose.popPose();
      }
   }

   public static boolean handleToggleWidgetClick(int mouseX, int mouseY, int x, int y) {
      if (!isToggleWidgetHovered(mouseX, mouseY, x, y)) {
         return false;
      } else {
         CClient config = AllConfigs.client();
         config.showTrainMapOverlay.set(!(Boolean)config.showTrainMapOverlay.get());
         return true;
      }
   }

   public static boolean isToggleWidgetHovered(int mouseX, int mouseY, int x, int y) {
      if (CreateClient.RAILWAYS.trackNetworks.isEmpty()) {
         return false;
      } else {
         return mouseX < x || mouseX >= x + AllGuiTextures.TRAINMAP_TOGGLE_PANEL.getWidth()
            ? false
            : mouseY >= y && mouseY < y + AllGuiTextures.TRAINMAP_TOGGLE_PANEL.getHeight();
      }
   }

   private static List<FormattedText> listTrainDetails(Train train) {
      List<FormattedText> output = new ArrayList<>();
      int blue = 13885148;
      int darkBlue = 9611709;
      int bright = 16773103;
      int orange = 16756064;
      TrainMapSync.TrainMapSyncEntry trainEntry = TrainMapSyncClient.currentData.get(train.id);
      if (trainEntry == null) {
         return Collections.emptyList();
      } else {
         TrainMapSync.TrainState state = trainEntry.state;
         TrainMapSync.SignalState signalState = trainEntry.signalState;
         CreateLang.text(train.name.getString()).color(bright).addTo(output);
         if (!trainEntry.ownerName.isBlank()) {
            CreateLang.translate("train_map.train_owned_by", trainEntry.ownerName).color(blue).addTo(output);
         }

         switch (state) {
            case CONDUCTOR_MISSING:
               CreateLang.translate("train_map.conductor_missing").color(orange).addTo(output);
               return output;
            case DERAILED:
               CreateLang.translate("train_map.derailed").color(orange).addTo(output);
               return output;
            case NAVIGATION_FAILED:
               CreateLang.translate("train_map.navigation_failed").color(orange).addTo(output);
               return output;
            case SCHEDULE_INTERRUPTED:
               CreateLang.translate("train_map.schedule_interrupted").color(orange).addTo(output);
               return output;
            case RUNNING_MANUALLY:
               CreateLang.translate("train_map.player_controlled").color(blue).addTo(output);
            case RUNNING:
            default:
               String currentStation = trainEntry.targetStationName;
               int targetStationDistance = trainEntry.targetStationDistance;
               if (!currentStation.isBlank()) {
                  if (targetStationDistance == 0) {
                     CreateLang.translate("train_map.train_at_station", currentStation).color(darkBlue).addTo(output);
                  } else {
                     CreateLang.translate("train_map.train_moving_to_station", currentStation, targetStationDistance).color(darkBlue).addTo(output);
                  }
               }

               if (signalState != TrainMapSync.SignalState.NOT_WAITING) {
                  boolean chainSignal = signalState == TrainMapSync.SignalState.CHAIN_SIGNAL;
                  CreateLang.translate("train_map.waiting_at_signal").color(orange).addTo(output);
                  if (signalState == TrainMapSync.SignalState.WAITING_FOR_REDSTONE) {
                     CreateLang.translate("train_map.redstone_powered").color(blue).addTo(output);
                  } else {
                     UUID waitingFor = trainEntry.waitingForTrain;
                     boolean trainFound = false;
                     if (waitingFor != null) {
                        Train trainWaitingFor = CreateClient.RAILWAYS.trains.get(waitingFor);
                        if (trainWaitingFor != null) {
                           CreateLang.translate("train_map.for_other_train", trainWaitingFor.name.getString()).color(blue).addTo(output);
                           trainFound = true;
                        }
                     }

                     if (!trainFound) {
                        if (chainSignal) {
                           CreateLang.translate("train_map.cannot_traverse_section").color(blue).addTo(output);
                        } else {
                           CreateLang.translate("train_map.section_reserved").color(blue).addTo(output);
                        }
                     }
                  }
               }

               if (trainEntry.fueled) {
                  CreateLang.translate("train_map.fuel_boosted").color(darkBlue).addTo(output);
               }

               return output;
         }
      }
   }

   private static Object drawPoints(GuiGraphics graphics, int mouseX, int mouseY, Object hoveredElement, Rect2i bounds) {
      PoseStack pose = graphics.pose();
      RenderSystem.enableDepthTest();

      for (TrackGraph graph : CreateClient.RAILWAYS.trackNetworks.values()) {
         for (GlobalStation station : graph.getPoints(EdgePointType.STATION)) {
            Couple<TrackNodeLocation> edgeLocation = station.edgeLocation;
            TrackNode node = graph.locateNode((TrackNodeLocation)edgeLocation.getFirst());
            TrackNode other = graph.locateNode((TrackNodeLocation)edgeLocation.getSecond());
            if (node != null && other != null && node.getLocation().dimension == TrainMapRenderer.INSTANCE.trackingDim) {
               TrackEdge edge = graph.getConnection(Couple.create(node, other));
               if (edge != null) {
                  double tLength = station.getLocationOn(edge);
                  double t = tLength / edge.getLength();
                  Vec3 position = edge.getPosition(graph, t);
                  int x = Mth.floor(position.x());
                  int y = Mth.floor(position.z());
                  if (bounds.contains(x, y)) {
                     Vec3 diff = edge.getDirectionAt(tLength).normalize();
                     int rotation = Mth.positiveModulo(
                        Mth.floor(0.5 + (Math.atan2(diff.z, diff.x) * 180.0F / (float)Math.PI + 90.0 + (double)(station.isPrimary(node) ? 180 : 0)) / 45.0), 8
                     );
                     AllGuiTextures sprite = AllGuiTextures.TRAINMAP_STATION_ORTHO;
                     AllGuiTextures highlightSprite = AllGuiTextures.TRAINMAP_STATION_ORTHO_HIGHLIGHT;
                     if (rotation % 2 != 0) {
                        sprite = AllGuiTextures.TRAINMAP_STATION_DIAGO;
                        highlightSprite = AllGuiTextures.TRAINMAP_STATION_DIAGO_HIGHLIGHT;
                     }

                     boolean highlight = hoveredElement == null && Math.max(Math.abs(mouseX - x), Math.abs(mouseY - y)) < 3;
                     pose.pushPose();
                     pose.translate((float)(x - 2), (float)(y - 2), 5.0F);
                     pose.translate((double)sprite.getWidth() / 2.0, (double)sprite.getHeight() / 2.0, 0.0);
                     pose.mulPose(Axis.ZP.rotationDegrees((float)(90 * (rotation / 2))));
                     pose.translate((double)(-sprite.getWidth()) / 2.0, (double)(-sprite.getHeight()) / 2.0, 0.0);
                     sprite.render(graphics, 0, 0);
                     sprite.render(graphics, 0, 0);
                     if (highlight) {
                        pose.translate(0.0F, 0.0F, 5.0F);
                        highlightSprite.render(graphics, -1, -1);
                        hoveredElement = station;
                     }

                     pose.popPose();
                  }
               }
            }
         }
      }

      return hoveredElement;
   }

   private static Object drawTrains(GuiGraphics graphics, int mouseX, int mouseY, Object hoveredElement, Rect2i bounds) {
      PoseStack pose = graphics.pose();
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      int spriteYOffset = -3;
      double time = (double)AnimationTickHolder.getTicks();
      time += (double)AnimationTickHolder.getPartialTicks();
      time -= TrainMapSyncClient.lastPacket;
      time /= 5.0;
      time = Mth.clamp(time, 0.0, 1.0);
      int[] sliceXShiftByRotationIndex = new int[]{0, 1, 2, 2, 3, -2, -2, -1};
      int[] sliceYShiftByRotationIndex = new int[]{3, 2, 2, 1, 0, 1, 2, 2};

      for (Train train : CreateClient.RAILWAYS.trains.values()) {
         TrainMapSync.TrainMapSyncEntry trainEntry = TrainMapSyncClient.currentData.get(train.id);
         if (trainEntry != null) {
            Vec3 frontPos = Vec3.ZERO;
            List<Carriage> carriages = train.carriages;
            boolean otherDim = true;
            double avgY = 0.0;

            for (int i = 0; i < carriages.size(); i++) {
               for (boolean firstBogey : Iterate.trueAndFalse) {
                  avgY += trainEntry.getPosition(i, firstBogey, time).y();
               }
            }

            avgY /= (double)(carriages.size() * 2);

            for (int i = 0; i < carriages.size(); i++) {
               Carriage carriage = carriages.get(i);
               Vec3 pos1 = trainEntry.getPosition(i, true, time);
               Vec3 pos2 = trainEntry.getPosition(i, false, time);
               ResourceKey<Level> dim = trainEntry.dimensions.get(i);
               if (dim != null
                  && dim == TrainMapRenderer.INSTANCE.trackingDim
                  && (bounds.contains(Mth.floor(pos1.x()), Mth.floor(pos1.z())) || bounds.contains(Mth.floor(pos2.x()), Mth.floor(pos2.z())))) {
                  otherDim = false;
                  if (!trainEntry.backwards && i == 0) {
                     frontPos = pos1;
                  }

                  if (trainEntry.backwards && i == train.carriages.size() - 1) {
                     frontPos = pos2;
                  }

                  Vec3 diff = pos2.subtract(pos1);
                  int size = carriage.bogeySpacing + 1;
                  Vec3 center = pos1.add(pos2).scale(0.5);
                  double pX = center.x;
                  double pY = center.z;
                  int rotation = Mth.positiveModulo(Mth.floor(0.5 + Math.atan2(diff.x, diff.z) * 180.0F / (float)Math.PI / 22.5), 8);
                  if (trainEntry.state == TrainMapSync.TrainState.DERAILED) {
                     rotation = Mth.positiveModulo((AnimationTickHolder.getTicks() / 8 + i * 3) * (i % 2 == 0 ? 1 : -1), 8);
                  }

                  AllGuiTextures sprite = AllGuiTextures.TRAINMAP_SPRITES;
                  int slices = 2;
                  if (rotation == 0 || rotation == 4) {
                     slices += Mth.floor((double)(size - 2) / 3.0 + 0.5);
                  } else if (rotation != 2 && rotation != 6) {
                     slices += Mth.floor((double)(((float)size - (5.0F - Mth.sqrt(5.0F))) / Mth.sqrt(5.0F)) + 0.5);
                  } else {
                     slices += Mth.floor((double)(((float)size - (5.0F - 2.0F * Mth.SQRT_OF_TWO)) / (2.0F * Mth.SQRT_OF_TWO)) + 0.5);
                  }

                  slices = Math.max(2, slices);
                  sprite.bind();
                  pose.pushPose();
                  float pivotX = 7.5F + (float)((slices - 3) * sliceXShiftByRotationIndex[rotation]) / 2.0F;
                  float pivotY = 6.5F + (float)((slices - 3) * sliceYShiftByRotationIndex[rotation]) / 2.0F;
                  pose.translate(pX - (double)pivotX, pY - (double)pivotY, 10.0 + avgY / 512.0 + (1024.0 + center.z() % 8192.0) / 1024.0);
                  int trainColorIndex = train.mapColorIndex;
                  int colorRow = trainColorIndex / 4;
                  int colorCol = trainColorIndex % 4;

                  for (int slice = 0; slice < slices; slice++) {
                     int row = slice == 0 ? 1 : (slice == slices - 1 ? 2 : 3);
                     int sliceShifts = slice == 0 ? 0 : (slice == slices - 1 ? slice - 2 : slice - 1);
                     int positionX = sliceShifts * sliceXShiftByRotationIndex[rotation];
                     int positionY = sliceShifts * sliceYShiftByRotationIndex[rotation] + spriteYOffset;
                     int sheetX = rotation * 16 + colorCol * 128;
                     int sheetY = row * 16 + colorRow * 64;
                     graphics.blit(sprite.location, positionX, positionY, (float)sheetX, (float)sheetY, 16, 16, sprite.getWidth(), sprite.getHeight());
                  }

                  pose.popPose();
                  int margin = 1;
                  int sizeX = 8 + (slices - 3) * sliceXShiftByRotationIndex[rotation];
                  int sizeY = 12 + (slices - 3) * sliceYShiftByRotationIndex[rotation];
                  double pXm = pX - (double)(sizeX / 2);
                  double pYm = pY - (double)(sizeY / 2) + (double)spriteYOffset;
                  if (hoveredElement == null
                     && (double)mouseX < pXm + (double)margin + (double)sizeX
                     && (double)mouseX > pXm - (double)margin
                     && (double)mouseY < pYm + (double)margin + (double)sizeY
                     && (double)mouseY > pYm - (double)margin) {
                     hoveredElement = train;
                  }
               }
            }

            if (!otherDim && trainEntry.signalState != TrainMapSync.SignalState.NOT_WAITING) {
               pose.pushPose();
               pose.translate(frontPos.x - 0.5, frontPos.z - 0.5, 20.0 + (1024.0 + frontPos.z() % 8192.0) / 1024.0);
               AllGuiTextures.TRAINMAP_SIGNAL.render(graphics, 0, -3);
               pose.popPose();
            }
         }
      }

      return hoveredElement;
   }

   public static void redrawAll(ResourceKey<Level> dimension) {
      TrainMapRenderer map = TrainMapRenderer.INSTANCE;
      map.trackingVersion = CreateClient.RAILWAYS.version;
      map.trackingDim = dimension;
      map.trackingTheme = (CClient.TrainMapTheme)AllConfigs.client().trainMapColorTheme.get();
      map.startDrawing();
      int mainColor = -8628268;
      int darkerColor = -9419907;
      int darkerColorShadow = -11917484;
      switch (map.trackingTheme) {
         case GREY:
            mainColor = -5720651;
            darkerColor = -8950164;
            darkerColorShadow = -11120562;
            break;
         case WHITE:
            mainColor = -1508871;
            darkerColor = -7826027;
            darkerColorShadow = -11120562;
      }

      List<Couple<Integer>> collisions = new ObjectArrayList();

      for (int phase = 0; phase <= 2; phase++) {
         renderPhase(map, collisions, mainColor, darkerColor, phase);
      }

      highlightYDifferences(map, collisions, mainColor, darkerColor, darkerColor, darkerColorShadow);
      map.finishDrawing();
   }

   private static void renderPhase(TrainMapRenderer map, List<Couple<Integer>> collisions, int mainColor, int darkerColor, int phase) {
      int outlineColor = -16777216;
      int portalFrameColor = -11784869;
      int portalColor = -32810;

      for (TrackGraph graph : CreateClient.RAILWAYS.trackNetworks.values()) {
         for (TrackNodeLocation nodeLocation : graph.getNodes()) {
            if (nodeLocation.dimension == map.trackingDim) {
               TrackNode node = graph.locateNode(nodeLocation);
               Map<TrackNode, TrackEdge> connectionsFrom = graph.getConnectionsFrom(node);
               int hashCode = node.hashCode();

               for (Entry<TrackNode, TrackEdge> entry : connectionsFrom.entrySet()) {
                  TrackNode other = entry.getKey();
                  TrackNodeLocation otherLocation = other.getLocation();
                  TrackEdge edge = entry.getValue();
                  BezierConnection turn = edge.getTurn();
                  if (edge.isInterDimensional()) {
                     Vec3 vec = node.getLocation().getLocation();
                     int x = Mth.floor(vec.x);
                     int z = Mth.floor(vec.z);
                     if (phase != 2) {
                        if (phase == 0) {
                           map.setPixels(x - 3, z - 2, x + 3, z + 2, outlineColor);
                           map.setPixels(x - 2, z - 3, x + 2, z + 3, outlineColor);
                        } else {
                           int a = mapYtoAlpha((double)Mth.floor(vec.y()));

                           for (int xi = x - 2; xi <= x + 2; xi++) {
                              for (int zi = z - 2; zi <= z + 2; zi++) {
                                 int alphaAt = map.alphaAt(xi, zi);
                                 if (alphaAt > 0 && alphaAt != a) {
                                    collisions.add(Couple.create(xi, zi));
                                 }

                                 int c = (xi - x) * (xi - x) + (zi - z) * (zi - z) > 2 ? portalFrameColor : portalColor;
                                 if (alphaAt <= a) {
                                    map.setPixel(xi, zi, markY(c, vec.y()));
                                 }
                              }
                           }
                        }
                     }
                  } else if (other.hashCode() <= hashCode) {
                     if (turn == null) {
                        if (phase != 2) {
                           float x1 = (float)nodeLocation.getX();
                           float z1 = (float)nodeLocation.getZ();
                           float x2 = (float)otherLocation.getX();
                           float z2 = (float)otherLocation.getZ();
                           double y1 = nodeLocation.getLocation().y();
                           double y2 = otherLocation.getLocation().y();
                           float xDiffSign = Math.signum(x2 - x1);
                           float zDiffSign = Math.signum(z2 - z1);
                           boolean diagonal = xDiffSign != 0.0F && zDiffSign != 0.0F;
                           if (xDiffSign != 0.0F) {
                              x2 = (float)((double)x2 - (double)xDiffSign * 0.25);
                              x1 = (float)((double)x1 + (double)xDiffSign * 0.25);
                           }

                           if (zDiffSign != 0.0F) {
                              z2 = (float)((double)z2 - (double)zDiffSign * 0.25);
                              z1 = (float)((double)z1 + (double)zDiffSign * 0.25);
                           }

                           x1 /= 2.0F;
                           x2 /= 2.0F;
                           z1 /= 2.0F;
                           z2 /= 2.0F;
                           int y = Mth.floor(y1);
                           int a = mapYtoAlpha((double)y);
                           if (diagonal) {
                              int z = Mth.floor(z1);
                              int x = Mth.floor(x1);

                              for (int s = 0; (float)s <= Math.abs(x1 - x2); s++) {
                                 if (phase == 0) {
                                    map.setPixels(x - 1, z, x + 1, z + 1, outlineColor);
                                    map.setPixels(x, z - 1, x, z + 2, outlineColor);
                                    x = (int)((float)x + xDiffSign);
                                    z = (int)((float)z + zDiffSign);
                                 } else {
                                    int alphaAtx = map.alphaAt(x, z);
                                    if (alphaAtx > 0 && alphaAtx != a) {
                                       collisions.add(Couple.create(x, z));
                                    }

                                    if (alphaAtx <= a) {
                                       map.setPixel(x, z, markY(mainColor, (double)y));
                                    }

                                    if (map.alphaAt(x, z + 1) < a) {
                                       map.setPixel(x, z + 1, markY(darkerColor, (double)y));
                                    }

                                    x = (int)((float)x + xDiffSign);
                                    z = (int)((float)z + zDiffSign);
                                 }
                              }
                           } else if (phase == 0) {
                              int x1i = Mth.floor(Math.min(x1, x2));
                              int z1i = Mth.floor(Math.min(z1, z2));
                              int x2i = Mth.floor(Math.max(x1, x2));
                              int z2i = Mth.floor(Math.max(z1, z2));
                              map.setPixels(x1i - 1, z1i, x2i + 1, z2i, outlineColor);
                              map.setPixels(x1i, z1i - 1, x2i, z2i + 1, outlineColor);
                           } else {
                              int z = Mth.floor(z1);
                              int x = Mth.floor(x1);
                              float diff = Math.max(Math.abs(x1 - x2), Math.abs(z1 - z2));
                              double yStep = (y2 - y1) / (double)diff;

                              for (int sx = 0; (float)sx <= diff; sx++) {
                                 int alphaAtxx = map.alphaAt(x, z);
                                 if (alphaAtxx > 0 && alphaAtxx != a) {
                                    collisions.add(Couple.create(x, z));
                                 }

                                 if (alphaAtxx <= a) {
                                    map.setPixel(x, z, markY(mainColor, (double)y));
                                 }

                                 x = (int)((float)x + xDiffSign);
                                 y = (int)((double)y + yStep);
                                 z = (int)((float)z + zDiffSign);
                              }
                           }
                        }
                     } else if (phase != 1) {
                        BlockPos origin = (BlockPos)turn.bePositions.getFirst();
                        Map<Pair<Integer, Integer>, Double> rasterise = turn.rasterise();

                        for (boolean antialias : Iterate.falseAndTrue) {
                           for (Entry<Pair<Integer, Integer>, Double> offset : rasterise.entrySet()) {
                              Pair<Integer, Integer> xz = offset.getKey();
                              int x = origin.getX() + (Integer)xz.getFirst();
                              int y = Mth.floor((double)origin.getY() + offset.getValue() + 0.5);
                              int z = origin.getZ() + (Integer)xz.getSecond();
                              if (phase == 0) {
                                 map.setPixels(x - 1, z, x + 1, z, outlineColor);
                                 map.setPixels(x, z - 1, x, z + 1, outlineColor);
                              } else {
                                 int a = mapYtoAlpha((double)y);
                                 if (!antialias) {
                                    int alphaAtxxx = map.alphaAt(x, z);
                                    if (alphaAtxxx > 0 && alphaAtxxx != a) {
                                       collisions.add(Couple.create(x, z));
                                    }

                                    if (alphaAtxxx <= a) {
                                       map.setPixel(x, z, markY(mainColor, (double)y));
                                    }
                                 } else {
                                    boolean mainColorBelowLeft = map.is(x + 1, z + 1, mainColor) && Math.abs(map.alphaAt(x + 1, z + 1) - a) <= 1;
                                    boolean mainColorBelowRight = map.is(x - 1, z + 1, mainColor) && Math.abs(map.alphaAt(x - 1, z + 1) - a) <= 1;
                                    if (mainColorBelowLeft || mainColorBelowRight) {
                                       int alphaAtxxxx = map.alphaAt(x, z + 1);
                                       if (alphaAtxxxx > 0 && alphaAtxxxx != a) {
                                          collisions.add(Couple.create(x, z));
                                       }

                                       if (alphaAtxxxx < a) {
                                          map.setPixel(x, z + 1, markY(darkerColor, (double)y));
                                          if (map.isEmpty(x + 1, z + 1)) {
                                             map.setPixel(x + 1, z + 1, outlineColor);
                                          }

                                          if (map.isEmpty(x - 1, z + 1)) {
                                             map.setPixel(x - 1, z + 1, outlineColor);
                                          }

                                          if (map.isEmpty(x, z + 2)) {
                                             map.setPixel(x, z + 2, outlineColor);
                                          }
                                       }
                                    }
                                 }
                              }
                           }

                           if (phase == 0) {
                              break;
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private static void highlightYDifferences(
      TrainMapRenderer map, List<Couple<Integer>> collisions, int mainColor, int darkerColor, int mainColorShadow, int darkerColorShadow
   ) {
      for (Couple<Integer> couple : collisions) {
         int x = (Integer)couple.getFirst();
         int z = (Integer)couple.getSecond();
         int a = map.alphaAt(x, z);
         if (a != 0) {
            for (int xi = x - 2; xi <= x + 2; xi++) {
               for (int zi = z - 2; zi <= z + 2; zi++) {
                  if (map.alphaAt(xi, zi) < a) {
                     if (map.is(xi, zi, mainColor)) {
                        map.setPixel(xi, zi, ABGR32.color(a, mainColorShadow));
                     } else if (map.is(xi, zi, darkerColor)) {
                        map.setPixel(xi, zi, ABGR32.color(a, darkerColorShadow));
                     }
                  }
               }
            }
         }
      }
   }

   private static int mapYtoAlpha(double y) {
      int minY = Minecraft.getInstance().level.getMinBuildHeight();
      return Mth.clamp(32 + Mth.floor((y - (double)minY) / 4.0), 0, 255);
   }

   private static int markY(int color, double y) {
      return ABGR32.color(mapYtoAlpha(y), color);
   }
}

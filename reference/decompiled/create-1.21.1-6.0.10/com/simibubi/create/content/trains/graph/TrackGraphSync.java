package com.simibubi.create.content.trains.graph;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.signal.EdgeGroupColor;
import com.simibubi.create.content.trains.signal.SignalEdgeGroupPacket;
import com.simibubi.create.content.trains.signal.TrackEdgePoint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public class TrackGraphSync {
   List<TrackGraphPacket> queuedPackets = new ArrayList<>();
   int rollCallIn;
   private TrackGraphSyncPacket currentGraphSyncPacket;
   private int currentPayload;

   public void serverTick() {
      this.flushGraphPacket();
      if (!this.queuedPackets.isEmpty()) {
         for (TrackGraphPacket packet : this.queuedPackets) {
            if (packet.packetDeletesGraph || Create.RAILWAYS.trackNetworks.containsKey(packet.graphId)) {
               CatnipServices.NETWORK.sendToAllClients(packet);
               this.rollCallIn = 3;
            }
         }

         this.queuedPackets.clear();
      }

      if (this.rollCallIn > 0) {
         this.rollCallIn--;
         if (this.rollCallIn <= 0) {
            this.sendRollCall();
         }
      }
   }

   public void nodeAdded(TrackGraph graph, TrackNode node) {
      this.flushGraphPacket(graph);
      this.currentGraphSyncPacket.addedNodes.put(node.getNetId(), Pair.of(node.getLocation(), node.getNormal()));
      this.currentPayload++;
   }

   public void edgeAdded(TrackGraph graph, TrackNode node1, TrackNode node2, TrackEdge edge) {
      this.flushGraphPacket(graph);
      this.currentGraphSyncPacket.addedEdges.add(Pair.of(Pair.of(Couple.create(node1.getNetId(), node2.getNetId()), edge.getTrackMaterial()), edge.getTurn()));
      this.currentPayload++;
   }

   public void pointAdded(TrackGraph graph, TrackEdgePoint point) {
      this.flushGraphPacket(graph);
      this.currentGraphSyncPacket.addedEdgePoints.add(point);
      this.currentPayload++;
   }

   public void pointRemoved(TrackGraph graph, TrackEdgePoint point) {
      this.flushGraphPacket(graph);
      this.currentGraphSyncPacket.removedEdgePoints.add(point.getId());
      this.currentPayload++;
   }

   public void nodeRemoved(TrackGraph graph, TrackNode node) {
      this.flushGraphPacket(graph);
      int nodeId = node.getNetId();
      if (this.currentGraphSyncPacket.addedNodes.remove(nodeId) == null) {
         this.currentGraphSyncPacket.removedNodes.add(nodeId);
      }

      this.currentGraphSyncPacket.addedEdges.removeIf(pair -> {
         Couple<Integer> ids = (Couple<Integer>)((Pair)pair.getFirst()).getFirst();
         return (Integer)ids.getFirst() == nodeId || (Integer)ids.getSecond() == nodeId;
      });
   }

   public void graphSplit(TrackGraph graph, Set<TrackGraph> additional) {
      this.flushGraphPacket(graph);
      additional.forEach(rg -> this.currentGraphSyncPacket.splitSubGraphs.put(rg.nodesById.keySet().stream().findFirst().get(), Pair.of(rg.netId, rg.id)));
   }

   public void graphRemoved(TrackGraph graph) {
      this.flushGraphPacket(graph);
      this.currentGraphSyncPacket.packetDeletesGraph = true;
   }

   public void sendEdgeGroups(List<UUID> ids, List<EdgeGroupColor> colors, ServerPlayer player) {
      CatnipServices.NETWORK.sendToClient(player, new SignalEdgeGroupPacket(ids, colors, true));
   }

   public void edgeGroupCreated(UUID id, EdgeGroupColor color) {
      CatnipServices.NETWORK.sendToAllClients(new SignalEdgeGroupPacket(id, color));
   }

   public void edgeGroupRemoved(UUID id) {
      CatnipServices.NETWORK.sendToAllClients(new SignalEdgeGroupPacket(ImmutableList.of(id), Collections.emptyList(), false));
   }

   public void edgeDataChanged(TrackGraph graph, TrackNode node1, TrackNode node2, TrackEdge edge) {
      this.flushGraphPacket(graph);
      this.currentGraphSyncPacket.syncEdgeData(node1, node2, edge);
      this.currentPayload++;
   }

   public void edgeDataChanged(TrackGraph graph, TrackNode node1, TrackNode node2, TrackEdge edge, TrackEdge edge2) {
      this.flushGraphPacket(graph);
      this.currentGraphSyncPacket.syncEdgeData(node1, node2, edge);
      this.currentGraphSyncPacket.syncEdgeData(node2, node1, edge2);
      this.currentPayload++;
   }

   public void sendFullGraphTo(TrackGraph graph, ServerPlayer player) {
      TrackGraphSyncPacket packet = new TrackGraphSyncPacket(graph.id, graph.netId);
      packet.fullWipe = true;
      int sent = 0;
      Set<TrackEdgePoint> sentPoints = new HashSet<>();

      for (TrackNode node : graph.nodes.values()) {
         packet.addedNodes.put(node.getNetId(), Pair.of(node.getLocation(), node.getNormal()));
         if (sent++ >= 1000) {
            sent = 0;
            packet = this.flushAndCreateNew(graph, player, packet);
         }
      }

      for (TrackNode nodex : graph.nodes.values()) {
         TrackGraphSyncPacket currentPacket = packet;
         if (graph.connectionsByNode.containsKey(nodex)) {
            for (Entry<TrackNode, TrackEdge> entry : graph.connectionsByNode.get(nodex).entrySet()) {
               TrackNode node2 = entry.getKey();
               TrackEdge edge = entry.getValue();
               Couple<Integer> key = Couple.create(nodex.getNetId(), node2.getNetId());
               currentPacket.addedEdges.add(Pair.of(Pair.of(key, edge.getTrackMaterial()), edge.getTurn()));
               currentPacket.syncEdgeData(nodex, node2, edge);

               for (TrackEdgePoint point : edge.edgeData.getPoints()) {
                  if (!sentPoints.contains(point)) {
                     sentPoints.add(point);
                     currentPacket.addedEdgePoints.add(point);
                     sent++;
                  }
               }
            }

            if (sent++ >= 1000) {
               sent = 0;
               packet = this.flushAndCreateNew(graph, player, packet);
            }
         }
      }

      for (EdgePointType<?> type : EdgePointType.TYPES.values()) {
         for (TrackEdgePoint pointx : graph.getPoints(type)) {
            if (!sentPoints.contains(pointx)) {
               sentPoints.add(pointx);
               packet.addedEdgePoints.add(pointx);
               if (sent++ >= 1000) {
                  sent = 0;
                  packet = this.flushAndCreateNew(graph, player, packet);
               }
            }
         }
      }

      if (sent > 0) {
         this.flushAndCreateNew(graph, player, packet);
      }
   }

   private void sendRollCall() {
      CatnipServices.NETWORK.sendToAllClients(TrackGraphRollCallPacket.ofServer());
   }

   private TrackGraphSyncPacket flushAndCreateNew(TrackGraph graph, ServerPlayer player, TrackGraphSyncPacket packet) {
      CatnipServices.NETWORK.sendToClient(player, packet);
      return new TrackGraphSyncPacket(graph.id, graph.netId);
   }

   private void flushGraphPacket() {
      this.flushGraphPacket(null, 0);
   }

   private void flushGraphPacket(TrackGraph graph) {
      this.flushGraphPacket(graph.id, graph.netId);
   }

   private void flushGraphPacket(@Nullable UUID graphId, int netId) {
      if (this.currentGraphSyncPacket != null) {
         if (this.currentGraphSyncPacket.graphId.equals(graphId) && this.currentPayload < 1000) {
            return;
         }

         this.queuedPackets.add(this.currentGraphSyncPacket);
         this.currentGraphSyncPacket = null;
         this.currentPayload = 0;
      }

      if (graphId != null) {
         this.currentGraphSyncPacket = new TrackGraphSyncPacket(graphId, netId);
         this.currentPayload = 0;
      }
   }
}

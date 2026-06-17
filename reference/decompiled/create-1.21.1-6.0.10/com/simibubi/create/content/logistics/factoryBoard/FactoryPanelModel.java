package com.simibubi.create.content.logistics.factoryBoard;

import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import com.simibubi.create.foundation.model.BakedQuadHelper;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;

public class FactoryPanelModel extends BakedModelWrapperWithData {
   private static final ModelProperty<FactoryPanelModel.FactoryPanelModelData> PANEL_PROPERTY = new ModelProperty();

   public FactoryPanelModel(BakedModel originalModel) {
      super(originalModel);
   }

   @Override
   protected Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
      FactoryPanelModel.FactoryPanelModelData data = new FactoryPanelModel.FactoryPanelModelData();

      for (FactoryPanelBlock.PanelSlot slot : FactoryPanelBlock.PanelSlot.values()) {
         FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(world, new FactoryPanelPosition(pos, slot));
         if (behaviour != null) {
            data.states.put(slot, behaviour.count == 0 ? FactoryPanelBlock.PanelState.PASSIVE : FactoryPanelBlock.PanelState.ACTIVE);
            data.type = behaviour.panelBE().restocker ? FactoryPanelBlock.PanelType.PACKAGER : FactoryPanelBlock.PanelType.NETWORK;
         }
      }

      data.ponder = world instanceof PonderLevel;
      return builder.with(PANEL_PROPERTY, data);
   }

   public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
      if (side == null && data.has(PANEL_PROPERTY)) {
         FactoryPanelModel.FactoryPanelModelData modelData = (FactoryPanelModel.FactoryPanelModelData)data.get(PANEL_PROPERTY);
         List<BakedQuad> quads = new ArrayList<>(super.getQuads(state, null, rand, data, renderType));

         for (FactoryPanelBlock.PanelSlot panelSlot : FactoryPanelBlock.PanelSlot.values()) {
            if (modelData.states.containsKey(panelSlot)) {
               this.addPanel(quads, state, panelSlot, modelData.type, modelData.states.get(panelSlot), rand, data, renderType, modelData.ponder);
            }
         }

         return quads;
      } else {
         return Collections.emptyList();
      }
   }

   public void addPanel(
      List<BakedQuad> quads,
      BlockState state,
      FactoryPanelBlock.PanelSlot slot,
      FactoryPanelBlock.PanelType type,
      FactoryPanelBlock.PanelState panelState,
      RandomSource rand,
      ModelData data,
      RenderType renderType,
      boolean ponder
   ) {
      PartialModel factoryPanel = panelState == FactoryPanelBlock.PanelState.PASSIVE
         ? (type == FactoryPanelBlock.PanelType.NETWORK ? AllPartialModels.FACTORY_PANEL : AllPartialModels.FACTORY_PANEL_RESTOCKER)
         : (type == FactoryPanelBlock.PanelType.NETWORK ? AllPartialModels.FACTORY_PANEL_WITH_BULB : AllPartialModels.FACTORY_PANEL_RESTOCKER_WITH_BULB);
      List<BakedQuad> quadsToAdd = factoryPanel.get().getQuads(state, null, rand, data, RenderType.solid());
      float xRot = (180.0F / (float)Math.PI) * FactoryPanelBlock.getXRot(state);
      float yRot = (180.0F / (float)Math.PI) * FactoryPanelBlock.getYRot(state);

      for (BakedQuad bakedQuad : quadsToAdd) {
         int[] vertices = bakedQuad.getVertices();
         int[] transformedVertices = Arrays.copyOf(vertices, vertices.length);
         Vec3 quadNormal = Vec3.atLowerCornerOf(bakedQuad.getDirection().getNormal());
         quadNormal = VecHelper.rotate(quadNormal, 180.0, Axis.Y);
         quadNormal = VecHelper.rotate(quadNormal, (double)(xRot + 90.0F), Axis.X);
         quadNormal = VecHelper.rotate(quadNormal, (double)yRot, Axis.Y);

         for (int i = 0; i < vertices.length / BakedQuadHelper.VERTEX_STRIDE; i++) {
            Vec3 vertex = BakedQuadHelper.getXYZ(vertices, i);
            Vec3 normal = BakedQuadHelper.getNormalXYZ(vertices, i);
            vertex = vertex.add((double)slot.xOffset * 0.5, 0.0, (double)slot.yOffset * 0.5);
            vertex = VecHelper.rotateCentered(vertex, 180.0, Axis.Y);
            vertex = VecHelper.rotateCentered(vertex, (double)(xRot + 90.0F), Axis.X);
            vertex = VecHelper.rotateCentered(vertex, (double)yRot, Axis.Y);
            normal = VecHelper.rotate(normal, 180.0, Axis.Y);
            normal = VecHelper.rotate(normal, (double)(xRot + 90.0F), Axis.X);
            normal = VecHelper.rotate(normal, (double)yRot, Axis.Y);
            BakedQuadHelper.setXYZ(transformedVertices, i, vertex);
            BakedQuadHelper.setNormalXYZ(transformedVertices, i, new Vec3(0.0, 1.0, 0.0));
         }

         Direction newNormal = Direction.fromDelta((int)Math.round(quadNormal.x), (int)Math.round(quadNormal.y), (int)Math.round(quadNormal.z));
         quads.add(new BakedQuad(transformedVertices, bakedQuad.getTintIndex(), newNormal, bakedQuad.getSprite(), !ponder && bakedQuad.isShade()));
      }
   }

   private static class FactoryPanelModelData {
      public FactoryPanelBlock.PanelType type;
      public EnumMap<FactoryPanelBlock.PanelSlot, FactoryPanelBlock.PanelState> states = new EnumMap<>(FactoryPanelBlock.PanelSlot.class);
      private boolean ponder;
   }
}

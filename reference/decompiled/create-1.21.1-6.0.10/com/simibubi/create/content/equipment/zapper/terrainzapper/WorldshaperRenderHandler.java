package com.simibubi.create.content.equipment.zapper.terrainzapper;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSpecialTextures;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public class WorldshaperRenderHandler {
   private static Supplier<Collection<BlockPos>> renderedPositions;

   public static void tick() {
      gatherSelectedBlocks();
      if (renderedPositions != null) {
         Outliner.getInstance()
            .showCluster("terrainZapper", renderedPositions.get())
            .colored(12566463)
            .disableLineNormals()
            .lineWidth(0.03125F)
            .withFaceTexture(AllSpecialTextures.CHECKERED);
      }
   }

   protected static void gatherSelectedBlocks() {
      LocalPlayer player = Minecraft.getInstance().player;
      ItemStack heldMain = player.getMainHandItem();
      ItemStack heldOff = player.getOffhandItem();
      boolean zapperInMain = AllItems.WORLDSHAPER.isIn(heldMain);
      boolean zapperInOff = AllItems.WORLDSHAPER.isIn(heldOff);
      if (!zapperInMain || heldMain.has(AllDataComponents.SHAPER_SWAP) && zapperInOff) {
         if (zapperInOff) {
            createBrushOutline(player, heldOff);
         } else {
            renderedPositions = null;
         }
      } else {
         createBrushOutline(player, heldMain);
      }
   }

   public static void createBrushOutline(LocalPlayer player, ItemStack zapper) {
      if (!zapper.has(AllDataComponents.SHAPER_BRUSH_PARAMS)) {
         renderedPositions = null;
      } else {
         Brush brush = ((TerrainBrushes)zapper.getOrDefault(AllDataComponents.SHAPER_BRUSH, TerrainBrushes.Cuboid)).get();
         PlacementOptions placement = (PlacementOptions)zapper.getOrDefault(AllDataComponents.SHAPER_PLACEMENT_OPTIONS, PlacementOptions.Merged);
         TerrainTools tool = (TerrainTools)zapper.getOrDefault(AllDataComponents.SHAPER_TOOL, TerrainTools.Fill);
         BlockPos params = (BlockPos)zapper.get(AllDataComponents.SHAPER_BRUSH_PARAMS);
         brush.set(params.getX(), params.getY(), params.getZ());
         Vec3 start = player.position().add(0.0, (double)player.getEyeHeight(), 0.0);
         Vec3 range = player.getLookAngle().scale(128.0);
         BlockHitResult raytrace = player.level().clip(new ClipContext(start, start.add(range), Block.OUTLINE, Fluid.NONE, player));
         if (raytrace != null && raytrace.getType() != Type.MISS) {
            BlockPos pos = raytrace.getBlockPos().offset(brush.getOffset(player.getLookAngle(), raytrace.getDirection(), placement));
            renderedPositions = () -> brush.addToGlobalPositions(player.level(), pos, raytrace.getDirection(), new ArrayList<>(), tool);
         } else {
            renderedPositions = null;
         }
      }
   }
}

package com.simibubi.create.content.logistics.packagerLink;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnectionHandler;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import java.util.UUID;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.tuple.Pair;

public class LogisticallyLinkedClientHandler {
   private static UUID previouslyHeldFrequency;

   public static void tick() {
      previouslyHeldFrequency = null;
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null) {
         ItemStack mainHandItem = player.getMainHandItem();
         if (mainHandItem.getItem() instanceof LogisticallyLinkedBlockItem && LogisticallyLinkedBlockItem.isTuned(mainHandItem)) {
            CompoundTag tag = ((CustomData)mainHandItem.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)).copyTag();
            if (tag.hasUUID("Freq")) {
               UUID uuid = tag.getUUID("Freq");
               previouslyHeldFrequency = uuid;

               for (LogisticallyLinkedBehaviour behaviour : LogisticallyLinkedBehaviour.getAllPresent(uuid, false, true)) {
                  SmartBlockEntity be = behaviour.blockEntity;
                  VoxelShape shape = be.getBlockState().getShape(player.level(), be.getBlockPos());
                  if (!shape.isEmpty() && player.canInteractWithBlock(be.getBlockPos(), 64.0)) {
                     for (int i = 0; i < shape.toAabbs().size(); i++) {
                        AABB aabb = (AABB)shape.toAabbs().get(i);
                        Outliner.getInstance()
                           .showAABB(Pair.of(behaviour, i), aabb.inflate(-0.0078125).move(be.getBlockPos()), 2)
                           .lineWidth(0.03125F)
                           .disableLineNormals()
                           .colored(AnimationTickHolder.getTicks() % 16 < 8 ? 7376301 : 9481677);
                     }
                  }
               }
            }
         }
      }
   }

   public static void tickPanel(FactoryPanelBehaviour fpb) {
      if (previouslyHeldFrequency != null) {
         if (previouslyHeldFrequency.equals(fpb.network)) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
               if (player.blockPosition().closerThan(fpb.getPos(), 64.0)) {
                  Outliner.getInstance()
                     .showAABB(fpb, FactoryPanelConnectionHandler.getBB(fpb.blockEntity.getBlockState(), fpb.getPanelPosition()).inflate(-0.01171875))
                     .lineWidth(0.03125F)
                     .disableLineNormals()
                     .colored(AnimationTickHolder.getTicks() % 16 < 8 ? 7376301 : 9481677);
               }
            }
         }
      }
   }
}

package com.simibubi.create.foundation.blockEntity.behaviour.filtering;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxRenderer;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class FilteringRenderer {
   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      HitResult target = mc.hitResult;
      if (target instanceof BlockHitResult result) {
         ClientLevel world = mc.level;
         BlockPos pos = result.getBlockPos();
         BlockState state = world.getBlockState(pos);
         if (!mc.player.isShiftKeyDown()) {
            if (world.getBlockEntity(pos) instanceof SmartBlockEntity sbe) {
               ItemStack var20 = mc.player.getItemInHand(InteractionHand.MAIN_HAND);

               for (BlockEntityBehaviour b : sbe.getAllBehaviours()) {
                  if (b instanceof FilteringBehaviour) {
                     FilteringBehaviour behaviour = (FilteringBehaviour)b;
                     if (behaviour instanceof SidedFilteringBehaviour) {
                        SidedFilteringBehaviour sidedFilteringBehaviour = (SidedFilteringBehaviour)behaviour;
                        behaviour = sidedFilteringBehaviour.get(result.getDirection());
                        if (behaviour == null) {
                           continue;
                        }
                     }

                     if (behaviour.isActive()) {
                        if (behaviour.slotPositioning instanceof ValueBoxTransform.Sided) {
                           ((ValueBoxTransform.Sided)behaviour.slotPositioning).fromSide(result.getDirection());
                        }

                        if (behaviour.slotPositioning.shouldRender(world, pos, state) && behaviour.mayInteract(mc.player)) {
                           ItemStack filter = behaviour.getFilter();
                           boolean isFilterSlotted = filter.getItem() instanceof FilterItem;
                           boolean showCount = behaviour.isCountVisible();
                           Component label = behaviour.getLabel();
                           boolean hit = behaviour.slotPositioning.testHit(world, pos, state, target.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
                           AABB emptyBB = new AABB(Vec3.ZERO, Vec3.ZERO);
                           AABB bb = isFilterSlotted ? emptyBB.inflate(0.45F, 0.31F, 0.2F) : emptyBB.inflate(0.25);
                           ValueBox box = new ValueBox.ItemValueBox(label, bb, pos, filter, behaviour.getCountLabelForValueBox());
                           box.passive(!hit || behaviour.bypassesInput(var20));
                           Outliner.getInstance()
                              .showOutline(Pair.of("filter" + behaviour.netId(), pos), box.transform(behaviour.slotPositioning))
                              .lineWidth(0.015625F)
                              .withFaceTexture(hit ? AllSpecialTextures.THIN_CHECKERED : null)
                              .highlightFace(result.getDirection());
                           if (hit) {
                              List<MutableComponent> tip = new ArrayList<>();
                              tip.add(label.copy());
                              tip.add(behaviour.getTip());
                              if (showCount) {
                                 tip.add(behaviour.getAmountTip());
                              }

                              CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static void renderOnBlockEntity(SmartBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
      if (be != null && !be.isRemoved()) {
         Level level = be.getLevel();
         BlockPos blockPos = be.getBlockPos();
         Iterator var8 = be.getAllBehaviours().iterator();

         while (true) {
            while (true) {
               if (!var8.hasNext()) {
                  return;
               }

               BlockEntityBehaviour b = (BlockEntityBehaviour)var8.next();
               if (b instanceof FilteringBehaviour behaviour) {
                  if (be.isVirtual()) {
                     break;
                  }

                  Entity cameraEntity = Minecraft.getInstance().cameraEntity;
                  if (cameraEntity == null || level != cameraEntity.level()) {
                     break;
                  }

                  float max = behaviour.getRenderDistance();
                  if (!(cameraEntity.position().distanceToSqr(VecHelper.getCenterOf(blockPos)) > (double)(max * max))) {
                     break;
                  }
               }
            }

            if (behaviour.isActive() && (!behaviour.getFilter().isEmpty() || behaviour instanceof SidedFilteringBehaviour)) {
               ValueBoxTransform slotPositioning = behaviour.slotPositioning;
               BlockState blockState = be.getBlockState();
               if (slotPositioning instanceof ValueBoxTransform.Sided sided) {
                  Direction side = sided.getSide();

                  for (Direction d : Iterate.directions) {
                     ItemStack filter = behaviour.getFilter(d);
                     if (!filter.isEmpty()) {
                        sided.fromSide(d);
                        if (slotPositioning.shouldRender(level, blockPos, blockState)) {
                           ms.pushPose();
                           slotPositioning.transform(level, blockPos, blockState, ms);
                           if (AllBlocks.CONTRAPTION_CONTROLS.has(blockState)) {
                              ValueBoxRenderer.renderFlatItemIntoValueBox(filter, ms, buffer, light, overlay);
                           } else {
                              ValueBoxRenderer.renderItemIntoValueBox(filter, ms, buffer, light, overlay);
                           }

                           ms.popPose();
                        }
                     }
                  }

                  sided.fromSide(side);
               } else if (slotPositioning.shouldRender(level, blockPos, blockState)) {
                  ms.pushPose();
                  slotPositioning.transform(level, blockPos, blockState, ms);
                  ValueBoxRenderer.renderItemIntoValueBox(behaviour.getFilter(), ms, buffer, light, overlay);
                  ms.popPose();
               }
            }
         }
      }
   }
}

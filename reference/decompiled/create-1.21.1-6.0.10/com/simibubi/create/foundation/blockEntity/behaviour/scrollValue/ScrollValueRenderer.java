package com.simibubi.create.foundation.blockEntity.behaviour.scrollValue;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.CreateClient;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBox;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ScrollValueRenderer {
   public static void tick() {
      Minecraft mc = Minecraft.getInstance();
      HitResult target = mc.hitResult;
      if (target != null && target instanceof BlockHitResult result) {
         ClientLevel world = mc.level;
         BlockPos pos = result.getBlockPos();
         Direction face = result.getDirection();
         boolean highlightFound = false;
         if (world.getBlockEntity(pos) instanceof SmartBlockEntity sbe) {
            for (BlockEntityBehaviour blockEntityBehaviour : sbe.getAllBehaviours()) {
               if (blockEntityBehaviour instanceof ScrollValueBehaviour) {
                  ScrollValueBehaviour behaviour = (ScrollValueBehaviour)blockEntityBehaviour;
                  if (!behaviour.isActive()) {
                     Outliner.getInstance().remove(behaviour);
                  } else {
                     ItemStack mainhandItem = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
                     boolean clipboard = behaviour.bypassesInput(mainhandItem);
                     if (!behaviour.needsWrench || AllItems.WRENCH.isIn(mainhandItem) || clipboard) {
                        boolean highlight;
                        label62: {
                           highlight = behaviour.testHit(target.getLocation()) && !clipboard && !highlightFound;
                           if (behaviour instanceof BulkScrollValueBehaviour bulkScrolling && AllKeys.ctrlDown()) {
                              Iterator var15 = bulkScrolling.getBulk().iterator();

                              while (true) {
                                 if (!var15.hasNext()) {
                                    break label62;
                                 }

                                 SmartBlockEntity smartBlockEntity = (SmartBlockEntity)var15.next();
                                 ScrollValueBehaviour other = smartBlockEntity.getBehaviour(ScrollValueBehaviour.TYPE);
                                 if (other != null) {
                                    addBox(world, smartBlockEntity.getBlockPos(), face, other, highlight);
                                 }
                              }
                           }

                           addBox(world, pos, face, behaviour, highlight);
                        }

                        if (highlight) {
                           highlightFound = true;
                           List<MutableComponent> tip = new ArrayList<>();
                           tip.add(behaviour.label.copy());
                           tip.add(CreateLang.translateDirect("gui.value_settings.hold_to_edit"));
                           CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   protected static void addBox(ClientLevel world, BlockPos pos, Direction face, ScrollValueBehaviour behaviour, boolean highlight) {
      AABB bb = new AABB(Vec3.ZERO, Vec3.ZERO).inflate(0.5).contract(0.0, 0.0, -0.5).move(0.0, 0.0, -0.125);
      Component label = behaviour.label;
      ValueBox box;
      if (behaviour instanceof ScrollOptionBehaviour) {
         box = new ValueBox.IconValueBox(label, ((ScrollOptionBehaviour)behaviour).getIconForSelected(), bb, pos);
      } else {
         box = new ValueBox.TextValueBox(label, bb, pos, Component.literal(behaviour.formatValue()));
      }

      box.passive(!highlight).wideOutline();
      Outliner.getInstance().showOutline(behaviour, box.transform(behaviour.slotPositioning)).highlightFace(face);
   }
}

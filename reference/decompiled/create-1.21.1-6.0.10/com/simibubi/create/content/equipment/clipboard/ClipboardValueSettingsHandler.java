package com.simibubi.create.content.equipment.clipboard;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.trains.track.TrackBlockOutline;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent.Block;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.EntityInteractSpecific;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem;

@EventBusSubscriber
public class ClipboardValueSettingsHandler {
   @SubscribeEvent
   @OnlyIn(Dist.CLIENT)
   public static void drawCustomBlockSelection(Block event) {
      Minecraft mc = Minecraft.getInstance();
      BlockHitResult target = event.getTarget();
      BlockPos pos = target.getBlockPos();
      BlockState blockstate = mc.level.getBlockState(pos);
      if (mc.player != null && !mc.player.isSpectator()) {
         if (mc.level.getWorldBorder().isWithinBounds(pos)) {
            if (AllBlocks.CLIPBOARD.isIn(mc.player.getMainHandItem())) {
               if (mc.level.getBlockEntity(pos) instanceof SmartBlockEntity smartBE) {
                  if (smartBE instanceof ClipboardBlockEntity || !smartBE.getAllBehaviours().stream().noneMatch(b -> {
                     if (b instanceof ClipboardCloneable cc && cc.writeToClipboard(mc.level.registryAccess(), new CompoundTag(), target.getDirection())) {
                        return true;
                     }

                     return false;
                  }) || smartBE instanceof ClipboardCloneable) {
                     VoxelShape shape = blockstate.getShape(mc.level, pos);
                     if (!shape.isEmpty()) {
                        VertexConsumer vb = event.getMultiBufferSource().getBuffer(RenderType.lines());
                        Vec3 camPos = event.getCamera().getPosition();
                        PoseStack ms = event.getPoseStack();
                        ms.pushPose();
                        ms.translate((double)pos.getX() - camPos.x, (double)pos.getY() - camPos.y, (double)pos.getZ() - camPos.z);
                        TrackBlockOutline.renderShape(shape, ms, vb, true);
                        event.setCanceled(true);
                        ms.popPose();
                     }
                  }
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static void clientTick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.hitResult instanceof BlockHitResult target) {
         if (AllBlocks.CLIPBOARD.isIn(mc.player.getMainHandItem())) {
            BlockPos pos = target.getBlockPos();
            if (mc.level.getBlockEntity(pos) instanceof SmartBlockEntity smartBE) {
               if (smartBE instanceof ClipboardBlockEntity) {
                  List<MutableComponent> tip = new ArrayList<>();
                  tip.add(CreateLang.translateDirect("clipboard.actions"));
                  tip.add(CreateLang.translateDirect("clipboard.copy_other_clipboard", Component.keybind("key.use")));
                  CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
               } else {
                  ClipboardContent content = (ClipboardContent)mc.player.getMainHandItem().get(AllDataComponents.CLIPBOARD_CONTENT);
                  if (content != null) {
                     CompoundTag tagElement;
                     boolean var10000;
                     label79: {
                        tagElement = content.copiedValues().orElse(null);
                        label60:
                        if (!smartBE.getAllBehaviours().stream().anyMatch(b -> {
                           if (b instanceof ClipboardCloneable cc && cc.writeToClipboard(mc.level.registryAccess(), new CompoundTag(), target.getDirection())) {
                              return true;
                           }

                           return false;
                        })) {
                           if (smartBE instanceof ClipboardCloneable ccbe
                              && ccbe.writeToClipboard(mc.level.registryAccess(), new CompoundTag(), target.getDirection())) {
                              break label60;
                           }

                           var10000 = false;
                           break label79;
                        }

                        var10000 = true;
                     }

                     boolean canCopy = var10000;
                     boolean canPaste = tagElement != null
                        && (
                           smartBE.getAllBehaviours()
                                 .stream()
                                 .anyMatch(
                                    b -> {
                                       if (b instanceof ClipboardCloneable cc
                                          && cc.readFromClipboard(
                                             mc.level.registryAccess(), tagElement.getCompound(cc.getClipboardKey()), mc.player, target.getDirection(), true
                                          )) {
                                          return true;
                                       }

                                       return false;
                                    }
                                 )
                              || smartBE instanceof ClipboardCloneable ccbe
                                 && ccbe.readFromClipboard(
                                    mc.level.registryAccess(), tagElement.getCompound(ccbe.getClipboardKey()), mc.player, target.getDirection(), true
                                 )
                        );
                     if (canCopy || canPaste) {
                        List<MutableComponent> tip = new ArrayList<>();
                        tip.add(CreateLang.translateDirect("clipboard.actions"));
                        if (canCopy) {
                           tip.add(CreateLang.translateDirect("clipboard.to_copy", Component.keybind("key.use")));
                        }

                        if (canPaste) {
                           tip.add(CreateLang.translateDirect("clipboard.to_paste", Component.keybind("key.attack")));
                        }

                        CreateClient.VALUE_SETTINGS_HANDLER.showHoverTip(tip);
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   public static void rightClickToCopy(RightClickBlock event) {
      interact(event, false);
   }

   @SubscribeEvent
   public static void leftClickToPaste(LeftClickBlock event) {
      interact(event, true);
   }

   private static void interact(PlayerInteractEvent event, boolean paste) {
      ItemStack itemStack = event.getItemStack();
      if (AllBlocks.CLIPBOARD.isIn(itemStack)) {
         BlockPos pos = event.getPos();
         Level world = event.getLevel();
         Player player = event.getEntity();
         if (player == null || !player.isSpectator()) {
            if (!player.isShiftKeyDown()) {
               if (world.getBlockEntity(pos) instanceof SmartBlockEntity smartBE) {
                  ClipboardContent var21 = (ClipboardContent)itemStack.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY);
                  if (smartBE instanceof ClipboardBlockEntity cbe) {
                     if (event instanceof ICancellableEvent cancellableEvent) {
                        cancellableEvent.setCanceled(true);
                        Objects.requireNonNull(event);
                        switch (event) {
                           case EntityInteractSpecific e:
                              e.setCancellationResult(InteractionResult.SUCCESS);
                              break;
                           case EntityInteract ex:
                              ex.setCancellationResult(InteractionResult.SUCCESS);
                              break;
                           case RightClickBlock exx:
                              exx.setCancellationResult(InteractionResult.SUCCESS);
                              break;
                           case RightClickItem exxx:
                              exxx.setCancellationResult(InteractionResult.SUCCESS);
                              break;
                        }
                     }

                     if (!world.isClientSide()) {
                        List<List<ClipboardEntry>> listTo = ClipboardEntry.readAll(var21);
                        List<List<ClipboardEntry>> listFrom = ClipboardEntry.readAll(cbe.components());
                        List<ClipboardEntry> toAdd = new ArrayList<>();

                        for (List<ClipboardEntry> page : listFrom) {
                           label148:
                           for (ClipboardEntry entry : page) {
                              String entryToAdd = entry.text.getString();

                              for (List<ClipboardEntry> pageTo : listTo) {
                                 for (ClipboardEntry existing : pageTo) {
                                    if (entryToAdd.equals(existing.text.getString())) {
                                       continue label148;
                                    }
                                 }
                              }

                              toAdd.add(new ClipboardEntry(entry.checked, entry.text));
                           }
                        }

                        for (ClipboardEntry entry : toAdd) {
                           List<ClipboardEntry> page = null;

                           for (List<ClipboardEntry> freePage : listTo) {
                              if (freePage.size() <= 11) {
                                 page = freePage;
                                 break;
                              }
                           }

                           if (page == null) {
                              page = new ArrayList<>();
                              listTo.add(page);
                           }

                           page.add(entry);
                           var21 = var21.setType(ClipboardOverrides.ClipboardType.WRITTEN);
                           itemStack.set(AllDataComponents.CLIPBOARD_CONTENT, var21);
                        }

                        var21 = var21.setPages(listTo);
                        itemStack.set(AllDataComponents.CLIPBOARD_CONTENT, var21);
                     }

                     player.displayClientMessage(
                        CreateLang.translate("clipboard.copied_from_clipboard", world.getBlockState(pos).getBlock().getName().withStyle(ChatFormatting.WHITE))
                           .style(ChatFormatting.GREEN)
                           .component(),
                        true
                     );
                  } else {
                     CompoundTag tag = var21.copiedValues().orElse(null);
                     if (!paste || tag != null) {
                        if (!paste) {
                           tag = new CompoundTag();
                        }

                        boolean anySuccess = false;
                        boolean anyValid = false;

                        for (BlockEntityBehaviour behaviour : smartBE.getAllBehaviours()) {
                           if (behaviour instanceof ClipboardCloneable) {
                              ClipboardCloneable cc = (ClipboardCloneable)behaviour;
                              anyValid = true;
                              String clipboardKey = cc.getClipboardKey();
                              if (paste) {
                                 anySuccess |= cc.readFromClipboard(
                                    world.registryAccess(), tag.getCompound(clipboardKey), player, event.getFace(), world.isClientSide()
                                 );
                              } else {
                                 CompoundTag compoundTag = new CompoundTag();
                                 boolean success = cc.writeToClipboard(world.registryAccess(), compoundTag, event.getFace());
                                 anySuccess |= success;
                                 if (success) {
                                    tag.put(clipboardKey, compoundTag);
                                 }
                              }
                           }
                        }

                        if (smartBE instanceof ClipboardCloneable ccbe) {
                           anyValid = true;
                           String clipboardKey = ccbe.getClipboardKey();
                           if (paste) {
                              anySuccess |= ccbe.readFromClipboard(
                                 world.registryAccess(), tag.getCompound(clipboardKey), player, event.getFace(), world.isClientSide()
                              );
                           } else {
                              CompoundTag compoundTag = new CompoundTag();
                              boolean success = ccbe.writeToClipboard(world.registryAccess(), compoundTag, event.getFace());
                              anySuccess |= success;
                              if (success) {
                                 tag.put(clipboardKey, compoundTag);
                              }
                           }
                        }

                        if (anyValid) {
                           ((ICancellableEvent)event).setCanceled(true);
                           if (event instanceof RightClickBlock rightClickBlock) {
                              rightClickBlock.setCancellationResult(InteractionResult.SUCCESS);
                           }

                           if (!world.isClientSide()) {
                              if (anySuccess) {
                                 player.displayClientMessage(
                                    CreateLang.translate(
                                          paste ? "clipboard.pasted_to" : "clipboard.copied_from",
                                          world.getBlockState(pos).getBlock().getName().withStyle(ChatFormatting.WHITE)
                                       )
                                       .style(ChatFormatting.GREEN)
                                       .component(),
                                    true
                                 );
                                 if (!paste) {
                                    var21 = var21.setType(ClipboardOverrides.ClipboardType.WRITTEN);
                                    var21 = var21.setCopiedValues(tag);
                                    itemStack.set(AllDataComponents.CLIPBOARD_CONTENT, var21);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }
}

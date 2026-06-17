package com.simibubi.create.content.schematics.client;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.client.tools.ToolType;
import com.simibubi.create.content.schematics.packet.SchematicPlacePacket;
import com.simibubi.create.content.schematics.packet.SchematicSyncPacket;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.outliner.AABBOutline;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class SchematicHandler implements Layer {
   private String displayedSchematic;
   private SchematicTransformation transformation;
   private AABB bounds;
   private boolean deployed;
   private boolean active;
   private ToolType currentTool;
   private static final int SYNC_DELAY = 10;
   private int syncCooldown;
   private int activeHotbarSlot;
   private ItemStack activeSchematicItem;
   private AABBOutline outline;
   private final SchematicRenderer[] renderers = new SchematicRenderer[3];
   private final SchematicHotbarSlotOverlay overlay = new SchematicHotbarSlotOverlay();
   private ToolSelectionScreen selectionScreen;

   public SchematicHandler() {
      this.currentTool = ToolType.DEPLOY;
      this.selectionScreen = new ToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
      this.transformation = new SchematicTransformation();
   }

   public void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gameMode.getPlayerMode() == GameType.SPECTATOR) {
         if (this.active) {
            this.active = false;
            this.syncCooldown = 0;
            this.activeHotbarSlot = 0;
            this.activeSchematicItem = null;
         }
      } else {
         if (this.activeSchematicItem != null && this.transformation != null) {
            this.transformation.tick();
         }

         LocalPlayer player = mc.player;
         ItemStack stack = this.findBlueprintInHand(player);
         if (stack == null) {
            this.active = false;
            this.syncCooldown = 0;
            if (this.activeSchematicItem != null && this.itemLost(player)) {
               this.activeHotbarSlot = 0;
               this.activeSchematicItem = null;
            }
         } else {
            if (!this.active || !((String)stack.get(AllDataComponents.SCHEMATIC_FILE)).equals(this.displayedSchematic)) {
               this.init(player, stack);
            }

            if (this.active) {
               if (this.syncCooldown > 0) {
                  this.syncCooldown--;
               }

               if (this.syncCooldown == 1) {
                  this.sync();
               }

               this.selectionScreen.update();
               this.currentTool.getTool().updateSelection();
            }
         }
      }
   }

   private void init(LocalPlayer player, ItemStack stack) {
      this.loadSettings(stack);
      this.displayedSchematic = (String)stack.get(AllDataComponents.SCHEMATIC_FILE);
      this.active = true;
      if (this.deployed) {
         this.setupRenderer();
         ToolType toolBefore = this.currentTool;
         this.selectionScreen = new ToolSelectionScreen(ToolType.getTools(player.isCreative()), this::equip);
         if (toolBefore != null) {
            this.selectionScreen.setSelectedElement(toolBefore);
            this.equip(toolBefore);
         }
      } else {
         this.selectionScreen = new ToolSelectionScreen(ImmutableList.of(ToolType.DEPLOY), this::equip);
      }
   }

   private void setupRenderer() {
      Level clientWorld = Minecraft.getInstance().level;
      StructureTemplate schematic = SchematicItem.loadSchematic(clientWorld, this.activeSchematicItem);
      Vec3i size = schematic.getSize();
      if (!size.equals(Vec3i.ZERO)) {
         SchematicLevel w = new SchematicLevel(clientWorld);
         SchematicLevel wMirroredFB = new SchematicLevel(clientWorld);
         SchematicLevel wMirroredLR = new SchematicLevel(clientWorld);
         StructurePlaceSettings placementSettings = new StructurePlaceSettings();
         BlockPos pos = BlockPos.ZERO;

         try {
            schematic.placeInWorld(w, pos, pos, placementSettings, w.getRandom(), 2);

            for (BlockEntity blockEntity : w.getBlockEntities()) {
               blockEntity.setLevel(w);
            }

            this.fixControllerBlockEntities(w);
         } catch (Exception var12) {
            Minecraft.getInstance().player.displayClientMessage(CreateLang.translate("schematic.error").component(), false);
            Create.LOGGER.error("Failed to load Schematic for Previewing", var12);
            return;
         }

         placementSettings.setMirror(Mirror.FRONT_BACK);
         pos = BlockPos.ZERO.east(size.getX() - 1);
         schematic.placeInWorld(wMirroredFB, pos, pos, placementSettings, wMirroredFB.getRandom(), 2);
         StructureTransform transform = new StructureTransform(placementSettings.getRotationPivot(), Axis.Y, Rotation.NONE, placementSettings.getMirror());

         for (BlockEntity be : wMirroredFB.getRenderedBlockEntities()) {
            transform.apply(be);
         }

         this.fixControllerBlockEntities(wMirroredFB);
         placementSettings.setMirror(Mirror.LEFT_RIGHT);
         pos = BlockPos.ZERO.south(size.getZ() - 1);
         schematic.placeInWorld(wMirroredLR, pos, pos, placementSettings, wMirroredFB.getRandom(), 2);
         transform = new StructureTransform(placementSettings.getRotationPivot(), Axis.Y, Rotation.NONE, placementSettings.getMirror());

         for (BlockEntity be : wMirroredLR.getRenderedBlockEntities()) {
            transform.apply(be);
         }

         this.fixControllerBlockEntities(wMirroredLR);
         this.renderers[0] = new SchematicRenderer(w);
         this.renderers[1] = new SchematicRenderer(wMirroredFB);
         this.renderers[2] = new SchematicRenderer(wMirroredLR);
      }
   }

   private void fixControllerBlockEntities(SchematicLevel level) {
      for (BlockEntity blockEntity : level.getBlockEntities()) {
         if (blockEntity instanceof IMultiBlockEntityContainer) {
            IMultiBlockEntityContainer multiBlockEntity = (IMultiBlockEntityContainer)blockEntity;
            BlockPos lastKnown = multiBlockEntity.getLastKnownPos();
            BlockPos current = blockEntity.getBlockPos();
            if (lastKnown != null && current != null && !multiBlockEntity.isController() && !lastKnown.equals(current)) {
               BlockPos newControllerPos = multiBlockEntity.getController().offset(current.subtract(lastKnown));
               if (multiBlockEntity instanceof SmartBlockEntity sbe) {
                  sbe.markVirtual();
               }

               multiBlockEntity.setController(newControllerPos);
            }
         }
      }
   }

   public void render(PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera) {
      if (this.active) {
         boolean present = this.activeSchematicItem != null;
         if (present) {
            ms.pushPose();
            this.currentTool.getTool().renderTool(ms, buffer, camera);
            ms.popPose();
            ms.pushPose();
            this.transformation.applyTransformations(ms, camera);
            if (this.deployed) {
               float pt = AnimationTickHolder.getPartialTicks();
               boolean lr = this.transformation.getScaleLR().getValue(pt) < 0.0F;
               boolean fb = this.transformation.getScaleFB().getValue(pt) < 0.0F;
               if (lr && !fb && this.renderers[2] != null) {
                  this.renderers[2].render(ms, buffer);
               } else if (fb && !lr && this.renderers[1] != null) {
                  this.renderers[1].render(ms, buffer);
               } else if (this.renderers[0] != null) {
                  this.renderers[0].render(ms, buffer);
               }
            }

            this.currentTool.getTool().renderOnSchematic(ms, buffer);
            ms.popPose();
         }
      }
   }

   public void updateRenderers() {
      for (SchematicRenderer renderer : this.renderers) {
         if (renderer != null) {
            renderer.update();
         }
      }
   }

   public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
      Minecraft mc = Minecraft.getInstance();
      if (!mc.options.hideGui && this.active) {
         if (this.activeSchematicItem != null) {
            this.overlay.renderOn(guiGraphics, this.activeHotbarSlot);
         }

         this.currentTool
            .getTool()
            .renderOverlay(mc.gui, guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false), guiGraphics.guiWidth(), guiGraphics.guiHeight());
         this.selectionScreen.renderPassive(guiGraphics, deltaTracker.getGameTimeDeltaPartialTick(false));
      }
   }

   public boolean onMouseInput(int button, boolean pressed) {
      if (!this.active) {
         return false;
      } else if (pressed && button == 1) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player.isShiftKeyDown()) {
            return false;
         } else {
            if (mc.hitResult instanceof BlockHitResult blockRayTraceResult) {
               BlockState clickedBlock = mc.level.getBlockState(blockRayTraceResult.getBlockPos());
               if (AllBlocks.SCHEMATICANNON.has(clickedBlock)) {
                  return false;
               }

               if (AllBlocks.DEPLOYER.has(clickedBlock)) {
                  return false;
               }
            }

            return this.currentTool.getTool().handleRightClick();
         }
      } else {
         return false;
      }
   }

   public void onKeyInput(int key, boolean pressed) {
      if (this.active) {
         if (AllKeys.TOOL_MENU.doesModifierAndCodeMatch(key)) {
            if (pressed && !this.selectionScreen.focused) {
               this.selectionScreen.focused = true;
            }

            if (!pressed && this.selectionScreen.focused) {
               this.selectionScreen.focused = false;
               this.selectionScreen.onClose();
            }
         }
      }
   }

   public boolean mouseScrolled(double delta) {
      if (!this.active) {
         return false;
      } else if (this.selectionScreen.focused) {
         this.selectionScreen.cycle((int)Math.signum(delta));
         return true;
      } else {
         return AllKeys.ctrlDown() ? this.currentTool.getTool().handleMouseWheel(delta) : false;
      }
   }

   private ItemStack findBlueprintInHand(Player player) {
      ItemStack stack = player.getMainHandItem();
      if (!AllItems.SCHEMATIC.isIn(stack)) {
         return null;
      } else if (!stack.has(AllDataComponents.SCHEMATIC_FILE)) {
         return null;
      } else {
         this.activeSchematicItem = stack;
         this.activeHotbarSlot = player.getInventory().selected;
         return stack;
      }
   }

   private boolean itemLost(Player player) {
      for (int i = 0; i < Inventory.getSelectionSize(); i++) {
         if (!player.getInventory().getItem(i).is(this.activeSchematicItem.getItem())
            && ItemStack.matches(player.getInventory().getItem(i), this.activeSchematicItem)) {
            return false;
         }
      }

      return true;
   }

   public void markDirty() {
      this.syncCooldown = 10;
   }

   public void sync() {
      if (this.activeSchematicItem != null) {
         CatnipServices.NETWORK
            .sendToServer(new SchematicSyncPacket(this.activeHotbarSlot, this.transformation.toSettings(), this.transformation.getAnchor(), this.deployed));
      }
   }

   public void equip(ToolType tool) {
      this.currentTool = tool;
      this.currentTool.getTool().init();
   }

   public void loadSettings(ItemStack blueprint) {
      StructurePlaceSettings settings = SchematicItem.getSettings(blueprint);
      this.transformation = new SchematicTransformation();
      this.deployed = (Boolean)blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false);
      BlockPos anchor = (BlockPos)blueprint.getOrDefault(AllDataComponents.SCHEMATIC_ANCHOR, BlockPos.ZERO);
      Vec3i size = (Vec3i)blueprint.get(AllDataComponents.SCHEMATIC_BOUNDS);
      if (size != null) {
         this.bounds = new AABB(0.0, 0.0, 0.0, (double)size.getX(), (double)size.getY(), (double)size.getZ());
         this.outline = new AABBOutline(this.bounds);
         this.outline.getParams().colored(6850245).lineWidth(0.0625F);
         this.transformation.init(anchor, settings, this.bounds);
      }
   }

   public void deploy() {
      if (!this.deployed) {
         List<ToolType> tools = ToolType.getTools(Minecraft.getInstance().player.isCreative());
         this.selectionScreen = new ToolSelectionScreen(tools, this::equip);
      }

      this.deployed = true;
      this.setupRenderer();
   }

   public String getCurrentSchematicName() {
      return this.displayedSchematic != null ? this.displayedSchematic : "-";
   }

   public void printInstantly() {
      CatnipServices.NETWORK.sendToServer(new SchematicPlacePacket(this.activeSchematicItem.copy()));
      this.activeSchematicItem.set(AllDataComponents.SCHEMATIC_DEPLOYED, false);
      SchematicInstances.clearHash(this.activeSchematicItem);
      this.active = false;
      this.markDirty();
   }

   public boolean isActive() {
      return this.active;
   }

   public AABB getBounds() {
      return this.bounds;
   }

   public SchematicTransformation getTransformation() {
      return this.transformation;
   }

   public boolean isDeployed() {
      return this.deployed;
   }

   public ItemStack getActiveSchematicItem() {
      return this.activeSchematicItem;
   }

   public AABBOutline getOutline() {
      return this.outline;
   }
}

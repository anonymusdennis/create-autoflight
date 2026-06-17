package dev.simulated_team.simulated.content.blocks.nameplate;

import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimStats;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NameplateBlockEntity extends SmartBlockEntity implements ClipboardCloneable {
   protected boolean glowing;
   protected boolean waxed;
   private DyeColor textColor = DyeColor.BLACK;
   private String name;
   private SubLevel connectedSubLevel;
   private BlockPos controllerPos;
   private BlockPos supportingPos = null;
   private boolean controller = false;
   private int controllerWidth = -1;

   public NameplateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.name = null;
      this.controllerPos = pos;
   }

   private static boolean checkNameplate(DyeColor color, Direction facing, BlockState state) {
      if (state.getBlock() instanceof NameplateBlock npb && npb.getColor() == color && state.getValue(NameplateBlock.FACING) == facing) {
         return true;
      }

      return false;
   }

   public static boolean canPlayerReach(NameplateBlockEntity be, Player player) {
      return getClosestDistance(be, player.getEyePosition()) < player.blockInteractionRange() + 4.0;
   }

   public void initialize() {
      super.initialize();
      DyeColor color = this.getColor();
      Direction facing = (Direction)this.getBlockState().getValue(NameplateBlock.FACING);
      this.checkAndUpdateController(color, facing);
      this.connectedSubLevel = Sable.HELPER.getContaining(this);
      if (this.connectedSubLevel != null && this.allowsEditing()) {
         this.name = this.connectedSubLevel.getName();
      }
   }

   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         DyeColor color = this.getColor();
         Direction facing = (Direction)this.getBlockState().getValue(NameplateBlock.FACING);
         if (this.controller && (this.controllerWidth == -1 || this.controllerWidth == 0)) {
            this.updateNameplates(color, facing);
         }
      }
   }

   public void lazyTick() {
      super.lazyTick();
      if (!this.level.isClientSide
         && this.controller
         && this.allowsEditing()
         && this.connectedSubLevel != null
         && !Objects.equals(this.connectedSubLevel.getName(), this.name)) {
         this.setName(this.connectedSubLevel.getName(), true, null);
      }
   }

   public void checkAndUpdateController(DyeColor color, Direction facing) {
      BlockPos leftPos;
      boolean wasController;
      BlockState leftState;
      boolean var10001;
      label31: {
         leftPos = this.getBlockPos().offset(facing.getClockWise(Axis.Y).getNormal());
         wasController = this.controller;
         leftState = this.getLevel().getBlockState(leftPos);
         if (leftState.getBlock() instanceof NameplateBlock npb && npb.getColor() == color && leftState.getValue(NameplateBlock.FACING) == facing) {
            var10001 = true;
            break label31;
         }

         var10001 = false;
      }

      this.controller = !var10001;
      if (wasController && !this.controller && checkNameplate(color, facing, leftState)) {
         NameplateBlockEntity leftBE = (NameplateBlockEntity)this.getLevel().getBlockEntity(leftPos);
         this.moveController(leftBE);
         leftBE.checkAndUpdateController(color, facing);
      }

      if (this.controller) {
         this.controllerPos = this.getBlockPos();
         this.updateNameplates(color, facing);
         this.invalidateRenderBoundingBox();
      }

      this.notifyUpdate();
   }

   public void updateNameplates(DyeColor color, Direction facing) {
      int preControllerWidth = this.controllerWidth;
      this.controllerWidth = 1;

      for (MutableBlockPos p = this.getBlockPos().mutable();
         checkNameplate(color, facing, this.level.getBlockState(p.setWithOffset(p, facing.getCounterClockWise(Axis.Y))));
         this.controllerWidth++
      ) {
         this.transferData((NameplateBlockEntity)this.getLevel().getBlockEntity(p));
      }

      this.invalidateRenderBoundingBox();
      if (this.controllerWidth != preControllerWidth) {
         this.notifyUpdate();
      }
   }

   private void transferData(NameplateBlockEntity namePlate) {
      namePlate.resetData();
      namePlate.controllerPos = this.controllerPos;
      namePlate.name = this.getName();
      namePlate.textColor = this.textColor;
      namePlate.glowing = this.glowing;
      namePlate.invalidateRenderBoundingBox();
      namePlate.sendData();
   }

   private void moveController(NameplateBlockEntity other) {
      other.controller = true;
      other.glowing = this.glowing;
      other.setName(this.getName(), false, null);
      other.setTextColor(this.textColor, false);
      this.resetData();
   }

   public static double getClosestDistance(NameplateBlockEntity nbe, Vec3 point) {
      if (!nbe.controller) {
         return getClosestDistance(nbe.findController(), point);
      } else {
         Vec3i dir = ((Direction)nbe.getBlockState().getValue(NameplateBlock.FACING)).getCounterClockWise().getNormal();
         Vec3 A = nbe.getBlockPos().getCenter();
         Vec3 B = A.add((double)(dir.getX() * nbe.controllerWidth), (double)(dir.getY() * nbe.controllerWidth), (double)(dir.getZ() * nbe.controllerWidth));
         SubLevel subLevel = Sable.HELPER.getContaining(nbe);
         if (subLevel != null) {
            A = subLevel.logicalPose().transformPosition(A);
            B = subLevel.logicalPose().transformPosition(B);
         }

         Vec3 v = B.subtract(A);
         Vec3 u = A.subtract(point);
         double t = Math.clamp(-v.dot(u) / v.dot(v), 0.0, 1.0);
         Vec3 closest = A.add(v.scale(t));
         return point.distanceTo(closest);
      }
   }

   public boolean allowsEditing() {
      return !this.waxed;
   }

   private void resetData() {
      this.controller = false;
      this.name = null;
      this.glowing = false;
      this.controllerWidth = -1;
   }

   private DyeColor getColor() {
      return ((NameplateBlock)this.getBlockState().getBlock()).getColor();
   }

   public DyeColor getTextColor() {
      return this.textColor;
   }

   public void setTextColor(DyeColor textColor, boolean updateNameplates) {
      if (this.controller) {
         this.textColor = textColor;
         if (updateNameplates) {
            this.updateNameplates(this.getColor(), (Direction)this.getBlockState().getValue(NameplateBlock.FACING));
         }
      }
   }

   public int getDarkColor(DyeColor textColor) {
      int i = textColor.getTextColor();
      if (i == DyeColor.BLACK.getTextColor() && this.glowing) {
         return -988212;
      } else {
         double d = 0.4;
         int j = (int)((double)ARGB32.red(i) * 0.4);
         int k = (int)((double)ARGB32.green(i) * 0.4);
         int l = (int)((double)ARGB32.blue(i) * 0.4);
         return ARGB32.color(0, j, k, l);
      }
   }

   public NameplateBlockEntity findController() {
      if (!this.controller) {
         if (this.getLevel().getBlockEntity(this.controllerPos) instanceof NameplateBlockEntity nbe) {
            nbe.controller = true;
            return nbe;
         }

         this.controller = true;
      }

      return this;
   }

   public boolean isController() {
      return this.getBlockPos().equals(this.controllerPos);
   }

   public int getControllerWidth() {
      return this.controllerWidth;
   }

   public String getName() {
      if (this.connectedSubLevel != null && this.connectedSubLevel.getName() != null && this.allowsEditing()) {
         return this.connectedSubLevel.getName();
      } else {
         return this.name == null ? "" : this.name;
      }
   }

   public void setName(String name, boolean updateNameplates, @Nullable Player player) {
      this.name = name;
      if (this.connectedSubLevel != null && !this.waxed && !Objects.equals(this.connectedSubLevel.getName(), name)) {
         this.connectedSubLevel.setName(name);
         if (player != null) {
            SimStats.SIMULATED_CONTRAPTIONS_NAMED.awardTo(player);
            SimAdvancements.I_DECLARE_THEE.awardTo(player);
         }
      }

      if (updateNameplates) {
         this.updateNameplates(this.getColor(), (Direction)this.getBlockState().getValue(NameplateBlock.FACING));
         this.sendData();
      }
   }

   public static boolean hasSupport(NameplateBlockEntity nbe) {
      if (!nbe.controller) {
         return hasSupport(nbe.findController());
      } else {
         Direction facing = (Direction)nbe.getBlockState().getValue(NameplateBlock.FACING);
         if (nbe.supportingPos != null) {
            if (nbe.level.getBlockState(nbe.supportingPos).is(nbe.getBlockState().getBlock())
               && NameplateBlock.hasBackSupport(facing, nbe.level, nbe.supportingPos)) {
               return true;
            }

            nbe.supportingPos = null;
         }

         Direction next = facing.getCounterClockWise();
         MutableBlockPos pos = new MutableBlockPos();
         pos.set(nbe.getBlockPos());

         for (int i = 0; i < nbe.controllerWidth; i++) {
            if (NameplateBlock.hasBackSupport(facing, nbe.level, pos)) {
               nbe.supportingPos = pos.immutable();
               break;
            }

            pos.move(next);
         }

         return nbe.supportingPos != null;
      }
   }

   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      tag.putInt("TextColor", this.textColor.getId());
      tag.putBoolean("Glow", this.glowing);
      tag.putBoolean("Waxed", this.waxed);
      if (this.name != null) {
         tag.putString("Name", this.name);
      }

      if (this.controller) {
         tag.putInt("Width", this.controllerWidth);
      } else {
         tag.put("ControllerPos", NbtUtils.writeBlockPos(this.controllerPos));
      }
   }

   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.textColor = DyeColor.byId(tag.getInt("TextColor"));
      this.glowing = tag.getBoolean("Glow");
      this.waxed = tag.getBoolean("Waxed");
      if (tag.contains("Name")) {
         this.name = tag.getString("Name");
      }

      if (tag.contains("ControllerPos")) {
         this.controller = false;
         this.controllerPos = (BlockPos)NbtUtils.readBlockPos(tag, "ControllerPos").get();
      } else {
         this.controller = true;
         this.controllerPos = this.getBlockPos();
         this.controllerWidth = tag.getInt("Width");
      }

      if (clientPacket) {
         this.invalidateRenderBoundingBox();
      }
   }

   protected AABB createRenderBoundingBox() {
      if (!this.controller) {
         return new AABB(this.getBlockPos());
      } else {
         Direction facing = (Direction)this.getBlockState().getValue(NameplateBlock.FACING);
         Vec3i off = facing.getCounterClockWise(Axis.Y).getNormal();
         return AABB.encapsulatingFullBlocks(this.getBlockPos(), this.getBlockPos().offset(off.multiply(this.controllerWidth - 1)));
      }
   }

   public String getClipboardKey() {
      return "Name";
   }

   public boolean writeToClipboard(@NotNull Provider var1, CompoundTag tag, Direction var3) {
      NameplateBlockEntity controller = this.findController();
      tag.putString("StoredName", controller.getName());
      tag.putInt("TextColor", controller.textColor.getId());
      return true;
   }

   public boolean readFromClipboard(@NotNull Provider var1, CompoundTag tag, Player player, Direction var4, boolean simulate) {
      NameplateBlockEntity controller = this.findController();
      if (!controller.allowsEditing()) {
         return false;
      } else if (!tag.contains("StoredName")) {
         return false;
      } else if (simulate) {
         return true;
      } else {
         controller.setName(tag.getString("StoredName"), true, player);
         controller.textColor = DyeColor.byId(tag.getInt("TextColor"));
         this.sendData();
         return true;
      }
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }
}

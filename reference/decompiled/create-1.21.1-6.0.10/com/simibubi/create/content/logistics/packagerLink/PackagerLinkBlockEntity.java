package com.simibubi.create.content.logistics.packagerLink;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.packager.IdentifiedInventory;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packager.repackager.RepackagerBlockEntity;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.redstone.displayLink.LinkWithBulbBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

public class PackagerLinkBlockEntity extends LinkWithBulbBlockEntity {
   public LogisticallyLinkedBehaviour behaviour;
   public UUID placedBy;
   private static final Map<BlockState, Vec3> bulbOffsets = new HashMap<>();

   public PackagerLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(10);
      this.placedBy = null;
   }

   public InventorySummary fetchSummaryFromPackager(@Nullable IdentifiedInventory ignoredHandler) {
      PackagerBlockEntity packager = this.getPackager();
      if (packager == null) {
         return InventorySummary.EMPTY;
      } else {
         return packager.isTargetingSameInventory(ignoredHandler) ? InventorySummary.EMPTY : packager.getAvailableItems();
      }
   }

   public void playEffect() {
      AllSoundEvents.STOCK_LINK.playAt(this.level, this.worldPosition, 0.75F, 1.25F, false);
      Vec3 vec3 = Vec3.atCenterOf(this.worldPosition);
      BlockState state = this.getBlockState();
      float f = 1.0F;
      AttachFace face = state.getOptionalValue(PackagerLinkBlock.FACE).orElse(AttachFace.FLOOR);
      if (face != AttachFace.FLOOR) {
         f = -1.0F;
      }

      if (face == AttachFace.WALL) {
         vec3 = vec3.add(0.0, 0.25, 0.0);
      }

      vec3 = vec3.add(Vec3.atLowerCornerOf(state.getOptionalValue(PackagerLinkBlock.FACING).orElse(Direction.SOUTH).getNormal()).scale((double)f * 0.125));
      this.pulse();
      this.level.addParticle(new WiFiParticle.Data(), vec3.x, vec3.y, vec3.z, 1.0, face == AttachFace.CEILING ? -1.0 : 1.0, 1.0);
   }

   public Pair<PackagerBlockEntity, PackagingRequest> processRequest(
      ItemStack stack,
      int amount,
      String address,
      int linkIndex,
      MutableBoolean finalLink,
      int orderId,
      @Nullable PackageOrderWithCrafts context,
      @Nullable IdentifiedInventory ignoredHandler
   ) {
      PackagerBlockEntity packager = this.getPackager();
      if (packager == null) {
         return null;
      } else if (packager.isTargetingSameInventory(ignoredHandler)) {
         return null;
      } else {
         InventorySummary summary = packager.getAvailableItems();
         int availableCount = summary.getCountOf(stack);
         if (availableCount == 0) {
            return null;
         } else {
            int toWithdraw = Math.min(amount, availableCount);
            return Pair.of(packager, PackagingRequest.create(stack, toWithdraw, address, linkIndex, finalLink, 0, orderId, context));
         }
      }
   }

   @Override
   protected void write(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.write(tag, registries, clientPacket);
      if (this.placedBy != null) {
         tag.putUUID("PlacedBy", this.placedBy);
      }
   }

   @Override
   protected void read(CompoundTag tag, Provider registries, boolean clientPacket) {
      super.read(tag, registries, clientPacket);
      this.placedBy = tag.contains("PlacedBy") ? tag.getUUID("PlacedBy") : null;
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.behaviour = new LogisticallyLinkedBehaviour(this, true));
   }

   @Override
   public void initialize() {
      super.initialize();
      this.behaviour.redstonePowerChanged(PackagerLinkBlock.getPower(this.getBlockState(), this.level, this.worldPosition));
      PackagerBlockEntity packager = this.getPackager();
      if (packager != null) {
         packager.recheckIfLinksPresent();
      }
   }

   @Nullable
   public PackagerBlockEntity getPackager() {
      BlockState blockState = this.getBlockState();
      if (this.behaviour.redstonePower == 15) {
         return null;
      } else {
         BlockPos source = this.worldPosition.relative(PackagerLinkBlock.getConnectedDirection(blockState).getOpposite());
         if (this.level.getBlockEntity(source) instanceof PackagerBlockEntity packager) {
            return packager instanceof RepackagerBlockEntity ? null : packager;
         } else {
            return null;
         }
      }
   }

   @Override
   public Direction getBulbFacing(BlockState state) {
      return PackagerLinkBlock.getConnectedDirection(state);
   }

   @Override
   public Vec3 getBulbOffset(BlockState state) {
      return bulbOffsets.computeIfAbsent(state, s -> {
         Vec3 offset = VecHelper.voxelSpace(5.0, 6.0, 11.0);
         Vec3 wallOffset = VecHelper.voxelSpace(11.0, 6.0, 5.0);
         AttachFace face = (AttachFace)s.getValue(PackagerLinkBlock.FACE);
         Vec3 vec = face == AttachFace.WALL ? wallOffset : offset;
         float angle = AngleHelper.horizontalAngle((Direction)s.getValue(PackagerLinkBlock.FACING));
         if (face == AttachFace.CEILING) {
            angle = -angle;
         }

         if (face == AttachFace.WALL) {
            angle = 0.0F;
         }

         return VecHelper.rotateCentered(vec, (double)angle, Axis.Y);
      });
   }
}

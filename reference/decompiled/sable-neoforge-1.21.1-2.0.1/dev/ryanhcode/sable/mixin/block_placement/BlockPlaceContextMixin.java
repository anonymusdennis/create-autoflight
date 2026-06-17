package dev.ryanhcode.sable.mixin.block_placement;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockPlaceContext.class})
public abstract class BlockPlaceContextMixin extends UseOnContext {
   @Unique
   private final LevelReusedVectors sable$sink = new LevelReusedVectors();
   @Shadow
   protected boolean replaceClicked;

   public BlockPlaceContextMixin(Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
      super(pPlayer, pHand, pHitResult);
   }

   @Shadow
   public abstract BlockPos getClickedPos();

   @Redirect(
      method = {"*"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/Direction;getFacingAxis(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Direction$Axis;)Lnet/minecraft/core/Direction;"
      )
   )
   private Direction sable$getFacingAxis(Entity player, Axis axis) {
      SubLevel subLevel = Sable.HELPER.getContaining(this.getLevel(), this.getClickedPos());
      if (subLevel != null) {
         SubLevelHelper.pushEntityLocal(subLevel, player);
         Direction facingAxis = Direction.getFacingAxis(player, axis);
         SubLevelHelper.popEntityLocal(subLevel, player);
         return facingAxis;
      } else {
         return Direction.getFacingAxis(player, axis);
      }
   }

   @Redirect(
      method = {"*"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/Direction;orderedByNearest(Lnet/minecraft/world/entity/Entity;)[Lnet/minecraft/core/Direction;"
      )
   )
   private Direction[] sable$orderedByNearest(Entity player) {
      SubLevel subLevel = Sable.HELPER.getContaining(this.getLevel(), this.getClickedPos());
      if (subLevel != null) {
         SubLevelHelper.pushEntityLocal(subLevel, player);
         Direction[] nearest = Direction.orderedByNearest(player);
         SubLevelHelper.popEntityLocal(subLevel, player);
         return nearest;
      } else {
         return Direction.orderedByNearest(player);
      }
   }

   @Inject(
      method = {"canPlace"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$canPlace(CallbackInfoReturnable<Boolean> cir) {
      BlockPos clicked = this.getClickedPos();
      SubLevel subLevel = Sable.HELPER.getContaining(this.getLevel(), this.getClickedPos());
      BoundingBox3d placedBoxBoundingBox = new BoundingBox3d(clicked);
      Quaterniond placedBoxOrientation = new Quaterniond();
      Vector3d placedBoxPosition = new Vector3d((double)clicked.getX() + 0.5, (double)clicked.getY() + 0.5, (double)clicked.getZ() + 0.5);
      if (subLevel != null) {
         subLevel.logicalPose().transformPosition(placedBoxPosition);
         placedBoxOrientation.set(subLevel.logicalPose().orientation());
         placedBoxBoundingBox.transform(subLevel.logicalPose(), placedBoxBoundingBox);
      }

      for (SubLevel otherSubLevel : Sable.HELPER.getAllIntersecting(this.getLevel(), placedBoxBoundingBox)) {
         if (otherSubLevel != subLevel) {
            boolean cancelled = this.sable$intersectBlocks(cir, otherSubLevel, placedBoxBoundingBox, this.sable$sink, placedBoxPosition, placedBoxOrientation);
            if (cancelled) {
               return;
            }
         }
      }

      this.sable$intersectBlocks(cir, null, placedBoxBoundingBox, this.sable$sink, placedBoxPosition, placedBoxOrientation);
   }

   @Unique
   private boolean sable$intersectBlocks(
      CallbackInfoReturnable<Boolean> cir,
      @Nullable SubLevel otherSubLevel,
      BoundingBox3dc placedBoxBoundingBox,
      LevelReusedVectors sink,
      Vector3d placedBoxPosition,
      Quaterniond placedBoxOrientation
   ) {
      BoundingBox3d localBase = placedBoxBoundingBox.expand(0.36602540380000004, new BoundingBox3d());
      if (otherSubLevel != null) {
         localBase.transformInverse(otherSubLevel.logicalPose(), localBase);
      }

      for (BlockPos position : BlockPos.betweenClosed(
         Mth.floor(localBase.minX()),
         Mth.floor(localBase.minY()),
         Mth.floor(localBase.minZ()),
         Mth.floor(localBase.maxX()),
         Mth.floor(localBase.maxY()),
         Mth.floor(localBase.maxZ())
      )) {
         boolean replaced = this.replaceClicked || this.getLevel().getBlockState(position).canBeReplaced((BlockPlaceContext)this);
         Vector3d inWorldBoxPosition = new Vector3d((double)position.getX() + 0.5, (double)position.getY() + 0.5, (double)position.getZ() + 0.5);
         Quaterniond inWorldBoxOrientation = new Quaterniond();
         if (otherSubLevel != null) {
            inWorldBoxPosition = otherSubLevel.logicalPose().transformPosition(inWorldBoxPosition);
            inWorldBoxOrientation.set(otherSubLevel.logicalPose().orientation());
         }

         OrientedBoundingBox3d inWorldBox = new OrientedBoundingBox3d(inWorldBoxPosition, new Vector3d(1.0, 1.0, 1.0), inWorldBoxOrientation, sink);
         OrientedBoundingBox3d justPlacedBox = new OrientedBoundingBox3d(placedBoxPosition, new Vector3d(1.0, 1.0, 1.0), placedBoxOrientation, sink);
         if (!replaced && OrientedBoundingBox3d.sat(inWorldBox, justPlacedBox).lengthSquared() > 0.05) {
            cir.setReturnValue(false);
            return true;
         }
      }

      return false;
   }
}

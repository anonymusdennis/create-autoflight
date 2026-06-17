package dev.ryanhcode.sable.mixin.sculk_vibrations;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.gameevent.GameEvent.Context;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({GameEventDispatcher.class})
public class GameEventDispatcherMixin {
   @Final
   @Shadow
   private ServerLevel level;

   @Inject(
      method = {"post"},
      at = {@At(
         value = "NEW",
         target = "java/util/ArrayList"
      )}
   )
   private void sable$useBBIntersection(
      Holder<GameEvent> gameEvent,
      Vec3 pos,
      Context context,
      CallbackInfo ci,
      @Share("bb") LocalRef<BoundingBox3ic> bbRef,
      @Local(ordinal = 1) LocalIntRef x1,
      @Local(ordinal = 2) LocalIntRef y1,
      @Local(ordinal = 3) LocalIntRef z1,
      @Local(ordinal = 4) LocalIntRef x2,
      @Local(ordinal = 5) LocalIntRef y2,
      @Local(ordinal = 6) LocalIntRef z2
   ) {
      BoundingBox3ic bb = (BoundingBox3ic)bbRef.get();
      if (bb != null) {
         x1.set(SectionPos.blockToSectionCoord(bb.minX()));
         y1.set(SectionPos.blockToSectionCoord(bb.minY()));
         z1.set(SectionPos.blockToSectionCoord(bb.minZ()));
         x2.set(SectionPos.blockToSectionCoord(bb.maxX()));
         y2.set(SectionPos.blockToSectionCoord(bb.maxY()));
         z2.set(SectionPos.blockToSectionCoord(bb.maxZ()));
      }
   }

   @WrapMethod(
      method = {"post"}
   )
   private void sable$visitShipListeners(
      Holder<GameEvent> gameEvent, Vec3 pos, Context context, Operation<Void> original, @Share("bb") LocalRef<BoundingBox3ic> bbRef
   ) {
      Vec3 globalPos = Sable.HELPER.projectOutOfSubLevel(this.level, pos);
      original.call(new Object[]{gameEvent, globalPos, context});
      if (bbRef.get() == null) {
         int radius = ((GameEvent)gameEvent.value()).notificationRadius();
         BoundingBox3dc sourceBB = new BoundingBox3d(BlockPos.containing(globalPos)).expand((double)radius);
         BoundingBox3i intersection = new BoundingBox3i();
         Sable.HELPER.getAllIntersecting(this.level, sourceBB).forEach(subLevel -> {
            BoundingBox3d plotBB = new BoundingBox3d(subLevel.getPlot().getBoundingBox());
            BoundingBox3dc sourceInPlotBB = sourceBB.transformInverse(subLevel.logicalPose(), new BoundingBox3d());
            bbRef.set(intersection.set(plotBB.intersect(sourceInPlotBB)));
            original.call(new Object[]{gameEvent, globalPos, context});
         });
      }
   }
}

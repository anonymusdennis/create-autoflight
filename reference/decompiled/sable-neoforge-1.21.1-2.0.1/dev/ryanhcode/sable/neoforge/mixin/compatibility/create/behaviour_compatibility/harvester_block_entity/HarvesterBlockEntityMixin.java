package dev.ryanhcode.sable.neoforge.mixin.compatibility.create.behaviour_compatibility.harvester_block_entity;

import com.simibubi.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.harvester.HarvesterLerpedSpeed;
import dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.harvester.HarvesterTicker;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({HarvesterBlockEntity.class})
public abstract class HarvesterBlockEntityMixin extends CachedRenderBBBlockEntity implements HarvesterLerpedSpeed, BlockEntitySubLevelActor {
   @Unique
   private final LerpedFloat sable$lerpedSpeed = LerpedFloat.angular();
   @Unique
   private BlockPos sable$previousPos = BlockPos.ZERO;

   public HarvesterBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   @Override
   public void sable$clientTick() {
      double velocity = Sable.HELPER.getVelocity(this.getLevel(), JOMLConversion.atCenterOf(this.getBlockPos())).length();
      this.sable$lerpedSpeed.chase((double)this.sable$lerpedSpeed.getValue() + velocity * 5.0, 20.0, Chaser.LINEAR);
      this.sable$lerpedSpeed.tickChaser();
   }

   @Override
   public void sable$tick(ServerSubLevel subLevel) {
      ActiveSableCompanion helper = Sable.HELPER;
      Position center = this.getBlockPos().getCenter();
      BlockPos gatheredPos = helper.runIncludingSubLevels(
         this.level,
         center,
         false,
         helper.getContaining(this),
         (sublevel, pos) -> HarvesterTicker.blockEntityBehaviour.isValidCrop(this.level, pos, this.level.getBlockState(pos)) ? pos : null
      );
      if (gatheredPos == null) {
         gatheredPos = BlockPos.containing(helper.projectOutOfSubLevel(this.level, center));
      }

      if (!this.sable$previousPos.equals(gatheredPos)) {
         this.sable$previousPos = gatheredPos;
         HarvesterTicker.dummyMovementContext.update(this.level, this.getBlockPos(), this.getBlockState(), null);
         HarvesterTicker.blockEntityBehaviour.visitNewPosition(HarvesterTicker.dummyMovementContext, this.sable$previousPos);
      }
   }

   @Override
   public LerpedFloat sable$getLerpedFloat() {
      return this.sable$lerpedSpeed;
   }
}

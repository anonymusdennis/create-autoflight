package dev.ryanhcode.sable.mixin.sign_interaction;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({SignBlockEntity.class})
public abstract class SignBlockEntityMixin extends BlockEntity {
   public SignBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
      super(blockEntityType, blockPos, blockState);
   }

   @Overwrite
   public boolean isFacingFrontText(Player player) {
      BlockState state = this.getBlockState();
      if (state.getBlock() instanceof SignBlock signBlock) {
         ActiveSableCompanion helper = Sable.HELPER;
         BlockPos pos = this.getBlockPos();
         Vector3d signCenterPos = JOMLConversion.toJOML(
            signBlock.getSignHitboxCenterPosition(state).add((double)pos.getX(), (double)pos.getY(), (double)pos.getZ())
         );
         Vector3d center = helper.projectOutOfSubLevel(this.level, signCenterPos);
         Vector3d deltaDir = JOMLConversion.toJOML(player.position()).sub(center).normalize();
         float signYRot = signBlock.getYRotationDegrees(state);
         Vector3d signNormal = new Vector3d(0.0, 0.0, 1.0).rotateY(Math.toRadians((double)(-signYRot)));
         SubLevel subLevel = helper.getContaining(this.level, pos);
         if (subLevel != null) {
            subLevel.logicalPose().transformNormal(signNormal);
         }

         return signNormal.dot(deltaDir.x, deltaDir.y, deltaDir.z) > 0.0;
      } else {
         return false;
      }
   }
}

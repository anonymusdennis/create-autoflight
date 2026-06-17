package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.redstone_contact;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.redstone.contact.RedstoneContactBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class RedstoneContactBlockEntity extends SmartBlockEntity {
   public static final double CONTRAPTION_CHECK_BOUNDS = 1.0;

   public RedstoneContactBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
   }

   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   public void tick() {
      super.tick();
      if (!this.isRemoved() && this.getLevel() != null) {
         SubLevel parentSublevel = Sable.HELPER.getContaining(this);
         Direction facing = (Direction)this.getBlockState().getValue(RedstoneContactBlock.FACING);
         Vector3d facingDir = JOMLConversion.atLowerCornerOf(facing.getNormal());
         if (parentSublevel != null) {
            parentSublevel.logicalPose().transformNormal(facingDir);
         }

         Vector3d frontWorldPosition = JOMLConversion.atCenterOf(this.getBlockPos().relative(facing));
         if (parentSublevel != null) {
            parentSublevel.logicalPose().transformPosition(frontWorldPosition);
         }

         boolean found = this.checkForContactsInWorldOrSubLevel(frontWorldPosition, facing, parentSublevel, facingDir)
            || this.checkForContactsInContraption(frontWorldPosition, facingDir);
         if (found != (Boolean)this.getBlockState().getValue(RedstoneContactBlock.POWERED)) {
            if (found) {
               this.getLevel().setBlockAndUpdate(this.getBlockPos(), (BlockState)this.getBlockState().setValue(RedstoneContactBlock.POWERED, true));
            } else {
               this.getLevel().setBlockAndUpdate(this.getBlockPos(), (BlockState)this.getBlockState().setValue(RedstoneContactBlock.POWERED, false));
            }
         }
      }
   }

   private boolean checkForContactsInContraption(Vector3d frontWorldPosition, Vector3d facingDir) {
      Vec3 frontMoj = JOMLConversion.toMojang(frontWorldPosition);
      Vec3 min = frontMoj.subtract(0.5, 0.5, 0.5);
      Vec3 max = min.add(1.0, 1.0, 1.0);
      AABB searchBounds = new AABB(min, max);

      for (AbstractContraptionEntity ace : this.getLevel().getEntitiesOfClass(AbstractContraptionEntity.class, searchBounds)) {
         Vec3 contactLocalPos = ace.toLocalVector(frontMoj, 1.0F);
         StructureBlockInfo candidateBlock = (StructureBlockInfo)ace.getContraption().getBlocks().get(BlockPos.containing(contactLocalPos));
         if (candidateBlock != null) {
            BlockState otherState = candidateBlock.state();
            if (AllBlocks.REDSTONE_CONTACT.has(otherState) || AllBlocks.ELEVATOR_CONTACT.has(otherState)) {
               Direction otherFacingDirection = (Direction)otherState.getValue(RedstoneContactBlock.FACING);
               Vec3 otherFacingMoj = Vec3.atLowerCornerOf(otherFacingDirection.getNormal());
               otherFacingMoj = ace.applyRotation(otherFacingMoj, 1.0F);
               Vector3d otherFacing = JOMLConversion.toJOML(otherFacingMoj);
               if (facingDir.dot(otherFacing) < -0.95) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean checkForContactsInWorldOrSubLevel(Vector3d frontWorldPosition, Direction facing, SubLevel parentSublevel, Vector3d facingDir) {
      return Sable.HELPER
         .findIncludingSubLevels(this.getLevel(), this.getBlockPos().getCenter().relative(facing, 1.0), true, parentSublevel, (subLevel, pos) -> {
            if (subLevel != null) {
               Vector3d localFrontWorldPosition = subLevel.logicalPose().transformPositionInverse(frontWorldPosition, new Vector3d());
               Vector3d localFacingDir = subLevel.logicalPose().transformNormalInverse(facingDir, new Vector3d());
               if (this.checkForContactsInContraption(localFrontWorldPosition, localFacingDir)) {
                  return true;
               }
            }

            BlockState otherState = this.getLevel().getBlockState(pos);
            if (!AllBlocks.REDSTONE_CONTACT.has(otherState) && !AllBlocks.ELEVATOR_CONTACT.has(otherState)) {
               return false;
            } else {
               Direction otherFacing = (Direction)otherState.getValue(RedstoneContactBlock.FACING);
               Vector3d otherFacingDir = JOMLConversion.atLowerCornerOf(otherFacing.getNormal());
               if (subLevel != null) {
                  subLevel.logicalPose().transformNormal(otherFacingDir);
               }

               return facingDir.dot(otherFacingDir) < -0.99;
            }
         });
   }
}

package dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SwivelBearingPlateBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {
   private BlockPos parent;
   private UUID parentSubLevelId;
   private boolean assembling;

   public SwivelBearingPlateBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
      super(typeIn, pos, state);
   }

   public void beforeAssembly() {
      this.assembling = true;
   }

   public void remove() {
      if (!this.level.isClientSide && !this.assembling) {
         this.destroyBearing();
      }

      super.remove();
   }

   private void destroyBearing() {
      if (this.parent != null && this.getLevel().getBlockState(this.parent).is(SimBlocks.SWIVEL_BEARING)) {
         this.getLevel().destroyBlock(this.parent, false);
      }
   }

   public void setParent(SwivelBearingBlockEntity be) {
      SubLevel subLevel = Sable.HELPER.getContaining(be);
      this.parent = be.getBlockPos();
      this.parentSubLevelId = subLevel != null ? subLevel.getUniqueId() : null;
   }

   public void tick() {
      super.tick();
   }

   public float propagateRotationTo(
      KineticBlockEntity target, BlockState stateFrom, BlockState stateTo, BlockPos diff, boolean connectedViaAxes, boolean connectedViaCogs
   ) {
      return this.parent != null && target.equals(this.level.getBlockEntity(this.parent))
         ? 1.0F
         : super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
   }

   public boolean isCustomConnection(KineticBlockEntity other, BlockState state, BlockState otherState) {
      return this.parent != null && other.equals(this.level.getBlockEntity(this.parent));
   }

   public List<BlockPos> addPropagationLocations(IRotate block, BlockState state, List<BlockPos> neighbours) {
      if (this.parent != null) {
         neighbours.add(this.parent);
      }

      return super.addPropagationLocations(block, state, neighbours);
   }

   protected void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.write(compound, registries, clientPacket);
      if (this.parent != null) {
         compound.put("ParentPos", NbtUtils.writeBlockPos(this.parent));
      }

      if (this.parentSubLevelId != null) {
         compound.putUUID("ParentSubLevelId", this.parentSubLevelId);
      }
   }

   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      super.read(compound, registries, clientPacket);
      if (compound.contains("parent")) {
         this.parent = (BlockPos)NbtUtils.readBlockPos(compound, "parent").get();
      }

      if (compound.contains("ParentPos")) {
         this.parent = (BlockPos)NbtUtils.readBlockPos(compound, "ParentPos").get();
      }

      if (compound.contains("ParentSubLevelId")) {
         this.parentSubLevelId = compound.getUUID("ParentSubLevelId");
      }
   }

   public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
      if (this.parent != null && this.level.getBlockEntity(this.parent) instanceof SwivelBearingBlockEntity swivelBearingBlockEntity) {
         swivelBearingBlockEntity.updateServoCoefficients();
      }
   }

   @Nullable
   public Iterable<SubLevel> sable$getConnectionDependencies() {
      if (this.parent == null) {
         return null;
      } else {
         SubLevelContainer container = SubLevelContainer.getContainer(this.level);
         if (this.parentSubLevelId != null) {
            SubLevel subLevel = container.getSubLevel(this.parentSubLevelId);
            if (subLevel != null) {
               return List.of(subLevel);
            }
         }

         return null;
      }
   }

   public void setParentAssembleNextTick() {
      if (this.level.getBlockEntity(this.parent) instanceof SwivelBearingBlockEntity sbe) {
         sbe.assembleNextTick = true;
      }
   }

   public void fixParentLinkingWhenMoved() {
      if (!this.level.isClientSide() && this.parent != null) {
         if (this.level.getBlockEntity(this.parent) instanceof SwivelBearingBlockEntity sbe) {
            sbe.setPlatePos(this.getBlockPos());
            ServerSubLevel newSublevel = (ServerSubLevel)Sable.HELPER.getContaining(this);
            if (newSublevel != null) {
               UUID subLevelID = sbe.getSubLevelID();
               UUID newID = newSublevel.getUniqueId();
               if (newID != subLevelID) {
                  sbe.setSubLevelID(newSublevel.getUniqueId());
                  sbe.reattachConstraint(newSublevel, true);
               }
            } else {
               sbe.setSubLevelID(null);
               sbe.reattachConstraint(null, true);
            }
         }
      }
   }
}

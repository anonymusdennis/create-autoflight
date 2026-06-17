package com.simibubi.create.content.contraptions.behaviour;

import com.google.common.base.Suppliers;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MovementContext {
   public Vec3 position;
   public Vec3 motion;
   public Vec3 relativeMotion;
   public UnaryOperator<Vec3> rotation;
   public Level world;
   public BlockState state;
   public BlockPos localPos;
   public CompoundTag blockEntityData;
   public boolean stall;
   public boolean disabled;
   public boolean firstMovement;
   public CompoundTag data;
   public Contraption contraption;
   public Object temporaryData;
   private FilterItemStack filter;
   private final Supplier<MountedItemStorage> itemStorage;
   private final Supplier<MountedFluidStorage> fluidStorage;

   public MovementContext(Level world, StructureBlockInfo info, Contraption contraption) {
      this.world = world;
      this.state = info.state();
      this.blockEntityData = info.nbt();
      this.contraption = contraption;
      this.localPos = info.pos();
      this.disabled = false;
      this.firstMovement = true;
      this.motion = Vec3.ZERO;
      this.relativeMotion = Vec3.ZERO;
      this.rotation = v -> v;
      this.position = null;
      this.data = new CompoundTag();
      this.stall = false;
      this.filter = null;
      this.itemStorage = Suppliers.memoize(() -> (MountedItemStorage)contraption.getStorage().getAllItemStorages().get(this.localPos));
      this.fluidStorage = Suppliers.memoize(() -> (MountedFluidStorage)contraption.getStorage().getFluids().storages.get(this.localPos));
   }

   public float getAnimationSpeed() {
      int modifier = 1000;
      double length = -this.motion.length();
      if (this.disabled) {
         return 0.0F;
      } else if (this.world.isClientSide && this.contraption.stalled) {
         return 700.0F;
      } else {
         return Math.abs(length) < 0.001953125 ? 0.0F : (float)((int)(length * (double)modifier + 100.0 * Math.signum(length)) / 100 * 100);
      }
   }

   public static MovementContext readNBT(Level world, StructureBlockInfo info, CompoundTag nbt, Contraption contraption) {
      MovementContext context = new MovementContext(world, info, contraption);
      context.motion = VecHelper.readNBT(nbt.getList("Motion", 6));
      context.relativeMotion = VecHelper.readNBT(nbt.getList("RelativeMotion", 6));
      if (nbt.contains("Position")) {
         context.position = VecHelper.readNBT(nbt.getList("Position", 6));
      }

      context.stall = nbt.getBoolean("Stall");
      context.firstMovement = nbt.getBoolean("FirstMovement");
      context.data = nbt.getCompound("Data");
      return context;
   }

   public CompoundTag writeToNBT(CompoundTag nbt) {
      nbt.put("Motion", VecHelper.writeNBT(this.motion));
      nbt.put("RelativeMotion", VecHelper.writeNBT(this.relativeMotion));
      if (this.position != null) {
         nbt.put("Position", VecHelper.writeNBT(this.position));
      }

      nbt.putBoolean("Stall", this.stall);
      nbt.putBoolean("FirstMovement", this.firstMovement);
      nbt.put("Data", this.data.copy());
      return nbt;
   }

   public FilterItemStack getFilterFromBE() {
      return this.filter != null ? this.filter : (this.filter = FilterItemStack.of(this.world.registryAccess(), this.blockEntityData.getCompound("Filter")));
   }

   @Nullable
   public MountedItemStorage getItemStorage() {
      return this.itemStorage.get();
   }

   @Nullable
   public MountedFluidStorage getFluidStorage() {
      return this.fluidStorage.get();
   }
}

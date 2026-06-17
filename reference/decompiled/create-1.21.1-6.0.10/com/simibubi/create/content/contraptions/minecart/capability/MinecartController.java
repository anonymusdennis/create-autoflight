package com.simibubi.create.content.contraptions.minecart.capability;

import com.mojang.serialization.Codec;
import com.simibubi.create.Create;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;
import com.simibubi.create.content.contraptions.minecart.CouplingHandler;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MinecartController implements INBTSerializable<CompoundTag> {
   public static final MinecartController EMPTY = new MinecartController.Empty();
   public static final IAttachmentSerializer<CompoundTag, MinecartController> SERIALIZER = MinecartController.Type.SERIALIZER;
   private boolean needsEntryRefresh;
   private WeakReference<AbstractMinecart> weakRef;
   private Couple<Optional<MinecartController.StallData>> stallData;
   private Couple<Optional<MinecartController.CouplingData>> couplings;

   public MinecartController(AbstractMinecart minecart) {
      this.weakRef = new WeakReference<>(minecart);
      this.stallData = Couple.create(Optional::empty);
      this.couplings = Couple.create(Optional::empty);
      this.needsEntryRefresh = true;
   }

   public final boolean isEmpty() {
      return this.getType() == MinecartController.Type.EMPTY;
   }

   @NotNull
   protected MinecartController.Type getType() {
      return MinecartController.Type.NORMAL;
   }

   public void tick() {
      AbstractMinecart cart = this.cart();
      Level world = this.getWorld();
      if (cart != null && world != null) {
         if (this.needsEntryRefresh) {
            ((List)CapabilityMinecartController.queuedAdditions.get(world)).add(cart);
            this.needsEntryRefresh = false;
         }

         this.stallData.forEach(opt -> opt.ifPresent(sd -> sd.tick(cart)));
         MutableBoolean internalStall = new MutableBoolean(false);
         this.couplings.forEachWithContext((opt, main) -> opt.ifPresent(cd -> {
               UUID idOfOther = cd.idOfCart(!main);
               MinecartController otherCart = CapabilityMinecartController.getIfPresent(world, idOfOther);
               internalStall.setValue(internalStall.booleanValue() || otherCart == null || !otherCart.isPresent() || otherCart.isStalled(false));
            }));
         if (!world.isClientSide) {
            this.setStalled(internalStall.booleanValue(), true);
            this.disassemble(cart);
         }
      }
   }

   private void disassemble(AbstractMinecart cart) {
      if (!(cart instanceof Minecart)) {
         List<Entity> passengers = cart.getPassengers();
         if (!passengers.isEmpty() && passengers.getFirst() instanceof AbstractContraptionEntity) {
            Level world = cart.level();
            int i = Mth.floor(cart.getX());
            int j = Mth.floor(cart.getY());
            int k = Mth.floor(cart.getZ());
            if (world.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
               j--;
            }

            BlockPos blockpos = new BlockPos(i, j, k);
            BlockState blockstate = world.getBlockState(blockpos);
            if (cart.canUseRail()
               && blockstate.is(BlockTags.RAILS)
               && blockstate.getBlock() instanceof PoweredRailBlock
               && ((PoweredRailBlock)blockstate.getBlock()).isActivatorRail()) {
               if (cart.isVehicle()) {
                  cart.ejectPassengers();
               }

               if (cart.getHurtTime() == 0) {
                  cart.setHurtDir(-cart.getHurtDir());
                  cart.setHurtTime(10);
                  cart.setDamage(50.0F);
                  cart.hurtMarked = true;
               }
            }
         }
      }
   }

   public boolean isFullyCoupled() {
      return this.isLeadingCoupling() && this.isConnectedToCoupling();
   }

   public boolean isLeadingCoupling() {
      return ((Optional)this.couplings.get(true)).isPresent();
   }

   public boolean isConnectedToCoupling() {
      return ((Optional)this.couplings.get(false)).isPresent();
   }

   public boolean isCoupledThroughContraption() {
      for (boolean current : Iterate.trueAndFalse) {
         if (this.hasContraptionCoupling(current)) {
            return true;
         }
      }

      return false;
   }

   public boolean hasContraptionCoupling(boolean current) {
      Optional<MinecartController.CouplingData> optional = (Optional<MinecartController.CouplingData>)this.couplings.get(current);
      return optional.isPresent() && optional.get().contraption;
   }

   public float getCouplingLength(boolean leading) {
      Optional<MinecartController.CouplingData> optional = (Optional<MinecartController.CouplingData>)this.couplings.get(leading);
      return optional.<Float>map(couplingData -> couplingData.length).orElse(0.0F);
   }

   public void decouple() {
      this.couplings.forEachWithContext((opt, main) -> opt.ifPresent(cd -> {
            UUID idOfOther = cd.idOfCart(!main);
            MinecartController otherCart = CapabilityMinecartController.getIfPresent(this.getWorld(), idOfOther);
            if (otherCart != null) {
               this.removeConnection(main);
               otherCart.removeConnection(!main);
            }
         }));
   }

   public void removeConnection(boolean main) {
      if (this.hasContraptionCoupling(main) && this.getWorld() != null && !this.getWorld().isClientSide) {
         List<Entity> passengers = this.cart().getPassengers();
         if (!passengers.isEmpty()) {
            Entity entity = passengers.getFirst();
            if (entity instanceof AbstractContraptionEntity) {
               ((AbstractContraptionEntity)entity).disassemble();
            }
         }
      }

      this.couplings.set(main, Optional.empty());
      this.needsEntryRefresh |= main;
      this.sendData();
   }

   public void prepareForCoupling(boolean isLeading) {
      if (isLeading && this.isLeadingCoupling() || !isLeading && this.isConnectedToCoupling()) {
         List<MinecartController> cartsToFlip = new ArrayList<>();
         MinecartController current = this;
         boolean forward = this.isLeadingCoupling();
         int safetyCount = 1000;

         do {
            if (safetyCount-- <= 0) {
               Create.LOGGER.warn("Infinite loop in coupling iteration");
               return;
            }

            cartsToFlip.add(current);
            current = CouplingHandler.getNextInCouplingChain(this.getWorld(), current, forward);
         } while (current != null && current != EMPTY);

         for (MinecartController minecartController : cartsToFlip) {
            minecartController.couplings.forEachWithContext((opt, leading) -> opt.ifPresent(cd -> {
                  cd.flip();
                  if (cd.contraption) {
                     List<Entity> passengers = minecartController.cart().getPassengers();
                     if (!passengers.isEmpty()) {
                        Entity entity = passengers.getFirst();
                        if (entity instanceof OrientedContraptionEntity contraption) {
                           UUID couplingId = contraption.getCouplingId();
                           if (couplingId == cd.mainCartID) {
                              contraption.setCouplingId(cd.connectedCartID);
                           } else if (couplingId == cd.connectedCartID) {
                              contraption.setCouplingId(cd.mainCartID);
                           }
                        }
                     }
                  }
               }));
            minecartController.couplings = minecartController.couplings.swap();
            minecartController.needsEntryRefresh = true;
            if (minecartController != this) {
               minecartController.sendData();
            }
         }
      }
   }

   public void coupleWith(boolean isLeading, UUID coupled, float length, boolean contraption) {
      UUID mainID = isLeading ? this.cart().getUUID() : coupled;
      UUID connectedID = isLeading ? coupled : this.cart().getUUID();
      this.couplings.set(isLeading, Optional.of(new MinecartController.CouplingData(mainID, connectedID, length, contraption)));
      this.needsEntryRefresh |= isLeading;
      this.sendData();
   }

   @Nullable
   public UUID getCoupledCart(boolean asMain) {
      Optional<MinecartController.CouplingData> optional = (Optional<MinecartController.CouplingData>)this.couplings.get(asMain);
      if (optional.isEmpty()) {
         return null;
      } else {
         MinecartController.CouplingData couplingData = optional.get();
         return asMain ? couplingData.connectedCartID : couplingData.mainCartID;
      }
   }

   public boolean isStalled() {
      return this.isStalled(true) || this.isStalled(false);
   }

   private boolean isStalled(boolean internal) {
      return ((Optional)this.stallData.get(internal)).isPresent();
   }

   public void setStalledExternally(boolean stall) {
      this.setStalled(stall, false);
   }

   private void setStalled(boolean stall, boolean internal) {
      if (this.isStalled(internal) != stall) {
         AbstractMinecart cart = this.cart();
         if (cart != null) {
            if (stall && cart != null) {
               this.stallData.set(internal, Optional.of(new MinecartController.StallData(cart)));
               this.sendData();
            } else {
               if (!this.isStalled(!internal) && cart != null) {
                  ((Optional)this.stallData.get(internal)).ifPresent(data -> data.release(cart));
               }

               this.stallData.set(internal, Optional.empty());
               this.sendData();
            }
         }
      }
   }

   public void sendData() {
      this.sendData(null);
   }

   public void sendData(@Nullable AbstractMinecart cart) {
      if (cart != null) {
         this.weakRef = new WeakReference<>(cart);
         this.needsEntryRefresh = true;
      }

      if (this.getWorld() != null && !this.getWorld().isClientSide) {
         CatnipServices.NETWORK.sendToClientsTrackingEntity(this.cart(), new MinecartControllerUpdatePacket(this, this.getWorld().registryAccess()));
      }
   }

   public CompoundTag serializeNBT(@NotNull Provider provider) {
      CompoundTag compoundNBT = new CompoundTag();
      this.stallData.forEachWithContext((opt, internal) -> opt.ifPresent(sd -> compoundNBT.put(internal ? "InternalStallData" : "StallData", sd.serialize())));
      this.couplings.forEachWithContext((opt, main) -> opt.ifPresent(cd -> compoundNBT.put(main ? "MainCoupling" : "ConnectedCoupling", cd.serialize())));
      return compoundNBT;
   }

   public void deserializeNBT(@NotNull Provider provider, CompoundTag nbt) {
      Optional<MinecartController.StallData> internalSD = Optional.empty();
      Optional<MinecartController.StallData> externalSD = Optional.empty();
      Optional<MinecartController.CouplingData> mainCD = Optional.empty();
      Optional<MinecartController.CouplingData> connectedCD = Optional.empty();
      if (nbt.contains("InternalStallData")) {
         internalSD = Optional.of(MinecartController.StallData.read(nbt.getCompound("InternalStallData")));
      }

      if (nbt.contains("StallData")) {
         externalSD = Optional.of(MinecartController.StallData.read(nbt.getCompound("StallData")));
      }

      if (nbt.contains("MainCoupling")) {
         mainCD = Optional.of(MinecartController.CouplingData.read(nbt.getCompound("MainCoupling")));
      }

      if (nbt.contains("ConnectedCoupling")) {
         connectedCD = Optional.of(MinecartController.CouplingData.read(nbt.getCompound("ConnectedCoupling")));
      }

      this.stallData = Couple.create(internalSD, externalSD);
      this.couplings = Couple.create(mainCD, connectedCD);
      this.needsEntryRefresh = true;
   }

   public boolean isPresent() {
      return this.weakRef.get() != null && this.cart().isAlive();
   }

   public AbstractMinecart cart() {
      return this.weakRef.get();
   }

   @Nullable
   private Level getWorld() {
      return this.cart() == null ? null : this.cart().level();
   }

   private static class CouplingData {
      private UUID mainCartID;
      private UUID connectedCartID;
      private float length;
      private boolean contraption;

      public CouplingData(UUID mainCartID, UUID connectedCartID, float length, boolean contraption) {
         this.mainCartID = mainCartID;
         this.connectedCartID = connectedCartID;
         this.length = length;
         this.contraption = contraption;
      }

      void flip() {
         UUID swap = this.mainCartID;
         this.mainCartID = this.connectedCartID;
         this.connectedCartID = swap;
      }

      CompoundTag serialize() {
         CompoundTag nbt = new CompoundTag();
         nbt.put("Main", NbtUtils.createUUID(this.mainCartID));
         nbt.put("Connected", NbtUtils.createUUID(this.connectedCartID));
         nbt.putFloat("Length", this.length);
         nbt.putBoolean("Contraption", this.contraption);
         return nbt;
      }

      static MinecartController.CouplingData read(CompoundTag nbt) {
         UUID mainCartID = NbtUtils.loadUUID(NBTHelper.getINBT(nbt, "Main"));
         UUID connectedCartID = NbtUtils.loadUUID(NBTHelper.getINBT(nbt, "Connected"));
         float length = nbt.getFloat("Length");
         boolean contraption = nbt.getBoolean("Contraption");
         return new MinecartController.CouplingData(mainCartID, connectedCartID, length, contraption);
      }

      public UUID idOfCart(boolean main) {
         return main ? this.mainCartID : this.connectedCartID;
      }
   }

   private static class Empty extends MinecartController {
      private Empty() {
         super(null);
      }

      public Empty(AbstractMinecart minecart) {
         super(minecart);
      }

      @NotNull
      @Override
      protected MinecartController.Type getType() {
         return MinecartController.Type.EMPTY;
      }

      private static void warn() {
         Create.LOGGER.warn("Method called on EMPTY MinecartController", new Exception());
      }

      @Override
      public void tick() {
         warn();
      }

      @Override
      public boolean isFullyCoupled() {
         warn();
         return false;
      }

      @Override
      public boolean isLeadingCoupling() {
         warn();
         return false;
      }

      @Override
      public boolean isConnectedToCoupling() {
         warn();
         return false;
      }

      @Override
      public boolean isCoupledThroughContraption() {
         warn();
         return false;
      }

      @Override
      public boolean hasContraptionCoupling(boolean current) {
         warn();
         return false;
      }

      @Override
      public float getCouplingLength(boolean leading) {
         warn();
         return 0.0F;
      }

      @Override
      public void decouple() {
         warn();
      }

      @Override
      public void removeConnection(boolean main) {
         warn();
      }

      @Override
      public void prepareForCoupling(boolean isLeading) {
         warn();
      }

      @Override
      public void coupleWith(boolean isLeading, UUID coupled, float length, boolean contraption) {
         warn();
      }

      @Nullable
      @Override
      public UUID getCoupledCart(boolean asMain) {
         warn();
         return null;
      }

      @Override
      public boolean isStalled() {
         warn();
         return false;
      }

      @Override
      public void setStalledExternally(boolean stall) {
         warn();
      }

      @Override
      public void sendData() {
         super.sendData();
      }

      @Override
      public CompoundTag serializeNBT(@NotNull Provider provider) {
         return super.serializeNBT(provider);
      }

      @Override
      public void deserializeNBT(@NotNull Provider provider, CompoundTag nbt) {
         super.deserializeNBT(provider, nbt);
      }

      @Override
      public boolean isPresent() {
         return super.isPresent();
      }

      @Override
      public AbstractMinecart cart() {
         return super.cart();
      }
   }

   private static class StallData {
      Vec3 position;
      Vec3 motion;
      float yaw;
      float pitch;

      private StallData() {
      }

      StallData(AbstractMinecart entity) {
         this.position = entity.position();
         this.motion = entity.getDeltaMovement();
         this.yaw = entity.getYRot();
         this.pitch = entity.getXRot();
         this.tick(entity);
      }

      void tick(AbstractMinecart entity) {
         entity.setDeltaMovement(Vec3.ZERO);
         entity.setYRot(this.yaw);
         entity.setXRot(this.pitch);
      }

      void release(AbstractMinecart entity) {
         entity.setDeltaMovement(this.motion);
      }

      CompoundTag serialize() {
         CompoundTag nbt = new CompoundTag();
         nbt.put("Pos", VecHelper.writeNBT(this.position));
         nbt.put("Motion", VecHelper.writeNBT(this.motion));
         nbt.putFloat("Yaw", this.yaw);
         nbt.putFloat("Pitch", this.pitch);
         return nbt;
      }

      static MinecartController.StallData read(CompoundTag nbt) {
         MinecartController.StallData stallData = new MinecartController.StallData();
         stallData.position = VecHelper.readNBT(nbt.getList("Pos", 6));
         stallData.motion = VecHelper.readNBT(nbt.getList("Motion", 6));
         stallData.yaw = nbt.getFloat("Yaw");
         stallData.pitch = nbt.getFloat("Pitch");
         return stallData;
      }
   }

   protected static enum Type implements StringRepresentable {
      EMPTY(new IAttachmentSerializer<CompoundTag, MinecartController>() {
         @NotNull
         public MinecartController read(@NotNull IAttachmentHolder holder, @NotNull CompoundTag tag, @NotNull Provider provider) {
            return MinecartController.EMPTY;
         }

         public CompoundTag write(@NotNull MinecartController attachment, @NotNull Provider provider) {
            return attachment.serializeNBT(provider);
         }
      }),
      NORMAL(new IAttachmentSerializer<CompoundTag, MinecartController>() {
         @NotNull
         public MinecartController read(@NotNull IAttachmentHolder holder, @NotNull CompoundTag tag, @NotNull Provider provider) {
            MinecartController controller = new MinecartController(null);
            controller.deserializeNBT(provider, tag);
            return controller;
         }

         @Nullable
         public CompoundTag write(@NotNull MinecartController attachment, @NotNull Provider provider) {
            return attachment.serializeNBT(provider);
         }
      });

      public static final Codec<MinecartController.Type> CODEC = StringRepresentable.fromValues(MinecartController.Type::values);
      private final IAttachmentSerializer<CompoundTag, MinecartController> serializer;
      private static final IAttachmentSerializer<CompoundTag, MinecartController> SERIALIZER = new IAttachmentSerializer<CompoundTag, MinecartController>() {
         @NotNull
         public MinecartController read(@NotNull IAttachmentHolder holder, @NotNull CompoundTag tag, @NotNull Provider provider) {
            return (MinecartController)MinecartController.Type.valueOf(tag.getString("Type")).getSerializer().read(holder, tag, provider);
         }

         @Nullable
         public CompoundTag write(MinecartController attachment, @NotNull Provider provider) {
            CompoundTag tag = attachment.serializeNBT(provider);
            if (tag != null) {
               tag.putString("Type", attachment.getType().name());
            }

            return tag;
         }
      };

      private Type(IAttachmentSerializer<CompoundTag, MinecartController> serializer) {
         this.serializer = serializer;
      }

      public IAttachmentSerializer<CompoundTag, MinecartController> getSerializer() {
         return this.serializer;
      }

      @NotNull
      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}

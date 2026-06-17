package com.simibubi.create.foundation.advancement;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

public class AdvancementBehaviour extends BlockEntityBehaviour {
   public static final BehaviourType<AdvancementBehaviour> TYPE = new BehaviourType<>();
   private UUID playerId;
   private final Set<CreateAdvancement> advancements = new HashSet<>();

   public AdvancementBehaviour(SmartBlockEntity be, CreateAdvancement... advancements) {
      super(be);
      this.add(advancements);
   }

   public void add(CreateAdvancement... advancements) {
      Collections.addAll(this.advancements, advancements);
   }

   public boolean isOwnerPresent() {
      return this.playerId != null;
   }

   public void setPlayer(UUID id) {
      Player player = this.getWorld().getPlayerByUUID(id);
      if (player != null) {
         this.playerId = id;
         this.removeAwarded();
         this.blockEntity.setChanged();
      }
   }

   @Override
   public void initialize() {
      super.initialize();
      this.removeAwarded();
   }

   private void removeAwarded() {
      Player player = this.getPlayer();
      if (player != null) {
         this.advancements.removeIf(c -> c.isAlreadyAwardedTo(player));
         if (this.advancements.isEmpty()) {
            this.playerId = null;
            this.blockEntity.setChanged();
         }
      }
   }

   public void awardPlayerIfNear(CreateAdvancement advancement, int maxDistance) {
      Player player = this.getPlayer();
      if (player != null) {
         if (!(player.distanceToSqr(Vec3.atCenterOf(this.getPos())) > (double)(maxDistance * maxDistance))) {
            this.award(advancement, player);
         }
      }
   }

   public void awardPlayer(CreateAdvancement advancement) {
      Player player = this.getPlayer();
      if (player != null) {
         this.award(advancement, player);
      }
   }

   private void award(CreateAdvancement advancement, Player player) {
      if (this.advancements.contains(advancement)) {
         advancement.awardTo(player);
      }

      this.removeAwarded();
   }

   private Player getPlayer() {
      return this.playerId == null ? null : this.getWorld().getPlayerByUUID(this.playerId);
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      super.write(nbt, registries, clientPacket);
      if (this.playerId != null) {
         nbt.putUUID("Owner", this.playerId);
      }
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      super.read(nbt, registries, clientPacket);
      if (nbt.contains("Owner")) {
         this.playerId = nbt.getUUID("Owner");
      }
   }

   @Override
   public BehaviourType<?> getType() {
      return TYPE;
   }

   public static void tryAward(BlockGetter reader, BlockPos pos, CreateAdvancement advancement) {
      AdvancementBehaviour behaviour = BlockEntityBehaviour.get(reader, pos, TYPE);
      if (behaviour != null) {
         behaviour.awardPlayer(advancement);
      }
   }

   public static void setPlacedBy(Level worldIn, BlockPos pos, LivingEntity placer) {
      AdvancementBehaviour behaviour = BlockEntityBehaviour.get(worldIn, pos, TYPE);
      if (behaviour != null) {
         if (!(placer instanceof FakePlayer)) {
            if (placer instanceof ServerPlayer) {
               behaviour.setPlayer(placer.getUUID());
            }
         }
      }
   }
}

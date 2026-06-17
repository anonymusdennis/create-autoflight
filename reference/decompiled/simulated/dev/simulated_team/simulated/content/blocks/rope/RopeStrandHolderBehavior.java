package dev.simulated_team.simulated.content.blocks.rope;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import dev.simulated_team.simulated.config.server.blocks.SimBlockConfigs;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopePoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachmentPoint;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeDataPacket;
import dev.simulated_team.simulated.network.packets.rope.ClientboundRopeStoppedPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import foundry.veil.api.network.VeilPacketManager;
import foundry.veil.api.network.VeilPacketManager.PacketSink;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class RopeStrandHolderBehavior extends BlockEntityBehaviour {
   public static final BehaviourType<RopeStrandHolderBehavior> TYPE = new BehaviourType("rope_strand_holder");
   @Nullable
   private UUID attachedRopeID = null;
   private boolean strandOwner = false;
   @Nullable
   private ServerRopeStrand ownedServerStrand;
   private boolean queuedLevelAddition = false;
   @Nullable
   private ClientRopeStrand ownedClientStrand;
   public boolean renderAttached;

   public RopeStrandHolderBehavior(SmartBlockEntity be) {
      super(be);
   }

   public void tick() {
      super.tick();
      if (this.ownedServerStrand != null) {
         if (this.queuedLevelAddition) {
            this.addServerStrand(this.ownedServerStrand);
            this.queuedLevelAddition = false;
         }

         SubLevelPhysicsSystem system = this.getPhysicsSystem();
         ServerLevel level = system.getLevel();
         boolean attachmentsLoaded = this.ownedServerStrand.areAttachmentsLoaded(level);
         if (!this.ownedServerStrand.isActive() && attachmentsLoaded && system.getTicketManager().wouldBeLoaded(level, this.ownedServerStrand)) {
            system.addObject(this.ownedServerStrand);
         }

         if (this.ownedServerStrand.isActive()) {
            this.ownedServerStrand.updatePose();
         }

         this.tickStrandTrackingPlayers();
         this.destroyRopeIfAttachmentBroken();
         this.blockEntity.setChanged();
      }

      if (this.ownedClientStrand != null) {
         ClientLevelRopeManager.getOrCreate(this.getLevel()).addStrand(this.ownedClientStrand);
      }
   }

   private void tickStrandTrackingPlayers() {
      assert this.ownedServerStrand != null;

      if (this.ownedServerStrand.isActive()) {
         Set<UUID> alreadyTrackingPlayers = this.ownedServerStrand.getTrackingPlayers();
         Iterator<UUID> iter = alreadyTrackingPlayers.iterator();
         List<ServerPlayer> players = this.getStrandTrackingPlayers();

         while (iter.hasNext()) {
            UUID uuid = iter.next();
            ServerPlayer player = (ServerPlayer)this.getLevel().getPlayerByUUID(uuid);
            if (player == null || !players.contains(player)) {
               iter.remove();
            }
         }

         for (ServerPlayer player : players) {
            UUID uuid = player.getUUID();
            if (alreadyTrackingPlayers.add(uuid)) {
               this.ownedServerStrand.updatePose();
               VeilPacketManager.player(player).sendPacket(new CustomPacketPayload[]{this.makeUpdatePacket()});
            }
         }
      }
   }

   public PacketSink getStrandPacketSink() {
      List<ServerPlayer> players = this.getStrandTrackingPlayers();
      return packet -> {
         for (ServerPlayer player : players) {
            player.connection.send(packet);
         }
      };
   }

   public List<ServerPlayer> getStrandTrackingPlayers() {
      ServerLevel level = (ServerLevel)this.getLevel();
      if (level == null) {
         return List.of();
      } else {
         ChunkPos chunk = new ChunkPos(this.getPos());
         return level.getChunkSource().chunkMap.getPlayers(chunk, false);
      }
   }

   @NotNull
   public ClientboundRopeDataPacket makeUpdatePacket() {
      ServerSubLevelContainer container = (ServerSubLevelContainer)SubLevelContainer.getContainer(this.getLevel());

      assert container != null;

      SubLevelTrackingSystem trackingSystem = container.trackingSystem();
      RopeAttachment startAttachment = this.ownedServerStrand.getAttachment(RopeAttachmentPoint.START);
      RopeAttachment endAttachment = this.ownedServerStrand.getAttachment(RopeAttachmentPoint.END);
      return new ClientboundRopeDataPacket(
         trackingSystem.getInterpolationTick(),
         this.blockEntity.getBlockPos(),
         this.ownedServerStrand.getUUID(),
         new ObjectArrayList(this.ownedServerStrand.getPoints()),
         startAttachment != null ? startAttachment.blockAttachment() : null,
         endAttachment != null ? endAttachment.blockAttachment() : null
      );
   }

   public ClientboundRopeStoppedPacket makeStopPacket() {
      return new ClientboundRopeStoppedPacket(this.blockEntity.getBlockPos());
   }

   private void destroyRopeIfAttachmentBroken() {
      ServerRopeStrand strand = this.ownedServerStrand;

      assert strand != null;

      ServerLevel level = (ServerLevel)this.getLevel();
      if (strand.areAttachmentsLoaded(level)) {
         RopeAttachment endAttachment = strand.getAttachment(RopeAttachmentPoint.END);
         boolean tileDrops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
         if (endAttachment == null) {
            this.destroyRope(null, this.getAttachmentPoint(), tileDrops);
         } else {
            BlockPos blockAttachment = endAttachment.blockAttachment();
            BlockEntity blockEntity = level.getBlockEntity(blockAttachment);
            if (blockEntity == null) {
               this.destroyRope(null, blockAttachment.getCenter(), tileDrops);
            } else if (blockEntity instanceof SmartBlockEntity smartBlockEntity) {
               RopeStrandHolderBehavior holderBehavior = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(TYPE);
               if (holderBehavior == null) {
                  this.destroyRope(null, blockAttachment.getCenter(), tileDrops);
               }
            } else {
               this.destroyRope(null, blockAttachment.getCenter(), tileDrops);
            }
         }
      }
   }

   public boolean createRope(RopeStrandHolderBehavior target, boolean dropItem) {
      if (target == this) {
         return false;
      } else if (target.attachedRopeID != null) {
         return false;
      } else {
         ServerLevel level = (ServerLevel)this.getLevel();
         Vec3 localRopeStart = this.getAttachmentPoint();
         Vec3 localRopeTarget = target.getAttachmentPoint();
         SubLevel subLevelStart = Sable.HELPER.getContaining(level, localRopeStart);
         SubLevel subLevelTarget = Sable.HELPER.getContaining(level, localRopeTarget);
         Vec3 ropeStart = Sable.HELPER.projectOutOfSubLevel(level, localRopeStart);
         Vec3 ropeTarget = Sable.HELPER.projectOutOfSubLevel(level, localRopeTarget);
         SimBlockConfigs blockConfig = SimConfigService.INSTANCE.server().blocks;
         double maxRopeRange = (Double)blockConfig.maxRopeRange.get();
         if (!ropeTarget.closerThan(ropeStart, maxRopeRange)) {
            return false;
         } else {
            this.destroyRope(null, null, dropItem && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS));
            double distance = ropeTarget.distanceTo(ropeStart);
            int oneLongSegments = Mth.floor(distance);
            int points = Math.max(1, oneLongSegments + 1);
            Vec3 diff = ropeTarget.subtract(ropeStart).normalize();
            List<Vector3d> pointList = new ObjectArrayList();
            pointList.add(JOMLConversion.toJOML(ropeStart));
            double shortSegmentLength = distance - (double)oneLongSegments;

            for (int i = 0; i < points; i++) {
               pointList.add(JOMLConversion.toJOML(ropeStart.add(diff.scale((double)i + shortSegmentLength))));
            }

            ServerRopeStrand strand = new ServerRopeStrand(UUID.randomUUID(), pointList);
            strand.updateFirstSegmentExtension(shortSegmentLength);
            strand.addAttachment(
               level,
               RopeAttachmentPoint.START,
               new RopeAttachment(
                  RopeAttachmentPoint.START, Optional.ofNullable(subLevelStart).<UUID>map(SubLevel::getUniqueId).orElse(null), this.blockEntity.getBlockPos()
               )
            );
            strand.addAttachment(
               level,
               RopeAttachmentPoint.END,
               new RopeAttachment(
                  RopeAttachmentPoint.END, Optional.ofNullable(subLevelTarget).<UUID>map(SubLevel::getUniqueId).orElse(null), target.blockEntity.getBlockPos()
               )
            );
            this.strandOwner = true;
            this.attachedRopeID = strand.getUUID();
            target.strandOwner = false;
            target.attachedRopeID = strand.getUUID();
            this.addServerStrand(strand);
            this.blockEntity.notifyUpdate();
            target.blockEntity.notifyUpdate();
            return true;
         }
      }
   }

   @Nullable
   private Level getLevel() {
      return this.blockEntity.getLevel();
   }

   public SubLevelPhysicsSystem getPhysicsSystem() {
      return SubLevelContainer.getContainer((ServerLevel)this.getLevel()).physicsSystem();
   }

   public void unload() {
      Level level = this.getLevel();
      if (this.ownedServerStrand != null) {
         this.removeServerStrand(level);
      }

      if (level != null && level.isClientSide) {
         this.removeClientStrand();
      }
   }

   private void removeServerStrand(Level level) {
      if (this.ownedServerStrand.isActive()) {
         this.ownedServerStrand.updatePose();
      }

      this.getPhysicsSystem().removeObject(this.ownedServerStrand);
      ServerLevelRopeManager.getOrCreate(level).removeStrand(this.ownedServerStrand.getUUID());
      this.ownedServerStrand = null;
   }

   private void addServerStrand(ServerRopeStrand strand) {
      this.ownedServerStrand = strand;
      ServerLevelRopeManager.getOrCreate(this.getLevel()).addStrand(this.ownedServerStrand);
   }

   public void destroyRope(@Nullable ServerPlayer player, @Nullable Vec3 ropeDropPos, boolean returnItem) {
      if (this.strandOwner && this.ownedServerStrand != null) {
         Level level = this.getLevel();
         if (level != null) {
            ServerRopeStrand strand = this.getOwnedStrand();
            if (strand != null) {
               RopeAttachment target = strand.getAttachment(RopeAttachmentPoint.END);
               if (target != null) {
                  BlockPos targetBlockPos = target.blockAttachment();
                  if (level.getBlockEntity(targetBlockPos) instanceof SmartBlockEntity be) {
                     RopeStrandHolderBehavior behaviour = (RopeStrandHolderBehavior)be.getBehaviour(TYPE);
                     if (behaviour != null) {
                        behaviour.detachRope();
                        behaviour.blockEntity.notifyUpdate();
                     }
                  }
               }

               List<Vector3d> points = strand.getPoints();
               Vector3d middlePointPos = new Vector3d((Vector3dc)points.get(points.size() / 2));
               if (returnItem) {
                  ItemStack stack = new ItemStack((ItemLike)SimItems.ROPE_COUPLING.get());
                  if (player != null) {
                     if (!player.hasInfiniteMaterials() || !player.getInventory().contains(stack)) {
                        player.getInventory().placeItemBackInInventory(stack);
                     }
                  } else {
                     if (ropeDropPos != null) {
                        middlePointPos.set(ropeDropPos.x, ropeDropPos.y, ropeDropPos.z);
                     }

                     level.addFreshEntity(new ItemEntity(level, middlePointPos.x, middlePointPos.y, middlePointPos.z, stack));
                  }
               }

               for (Vector3d position : points) {
                  level.playSound(null, position.x, position.y, position.z, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.5F, 1.0F);
                  if (level instanceof ServerLevel serverLevel) {
                     serverLevel.sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, AllBlocks.ROPE.getDefaultState()),
                        position.x,
                        position.y,
                        position.z,
                        10,
                        0.1,
                        0.1,
                        0.1,
                        0.1
                     );
                  }
               }
            }

            if (this.ownedServerStrand != null) {
               this.removeServerStrand(level);
            }

            this.attachedRopeID = null;
            this.strandOwner = false;
            this.blockEntity.notifyUpdate();
         }
      }
   }

   public Vec3 getAttachmentPoint() {
      SmartBlockEntity be = this.blockEntity;
      return ((RopeStrandHolderBlockEntity)be).getAttachmentPoint(be.getBlockPos(), be.getBlockState());
   }

   public Vec3 getVisualAttachmentPoint() {
      SmartBlockEntity be = this.blockEntity;
      return ((RopeStrandHolderBlockEntity)be).getVisualAttachmentPoint(be.getBlockPos(), be.getBlockState());
   }

   public BehaviourType<?> getType() {
      return TYPE;
   }

   @Nullable
   public ServerRopeStrand getOwnedStrand() {
      return this.ownedServerStrand;
   }

   @Nullable
   public ServerRopeStrand getAttachedStrand() {
      ServerLevelRopeManager manager = ServerLevelRopeManager.getOrCreate(this.getLevel());
      return manager != null && this.attachedRopeID != null ? manager.getStrand(this.attachedRopeID) : null;
   }

   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      super.write(nbt, registries, clientPacket);
      nbt.putBoolean("OwnStrand", this.strandOwner);
      if (this.attachedRopeID != null) {
         nbt.putUUID("HasRopeAttached", this.attachedRopeID);
      }

      ServerRopeStrand strand = this.getOwnedStrand();
      if (strand != null && this.strandOwner && !clientPacket) {
         nbt.put("Strand", (Tag)ServerRopeStrand.CODEC.encodeStart(NbtOps.INSTANCE, strand).getOrThrow());
      }
   }

   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      super.read(nbt, registries, clientPacket);
      this.strandOwner = nbt.getBoolean("OwnStrand");
      if (nbt.contains("HasRopeAttached")) {
         this.attachedRopeID = nbt.getUUID("HasRopeAttached");
      } else {
         this.attachedRopeID = null;
      }

      if (clientPacket) {
         if (!this.strandOwner) {
            this.removeClientStrand();
         }
      } else if (nbt.contains("Strand")) {
         CompoundTag strandNBT = nbt.getCompound("Strand");
         if (this.ownedServerStrand == null) {
            this.loadServerStrand(strandNBT);
         }
      }
   }

   private void removeClientStrand() {
      if (this.ownedClientStrand != null) {
         ClientLevelRopeManager.getOrCreate(this.getLevel()).removeStrand(this.ownedClientStrand.getUuid());
         this.ownedClientStrand = null;
      }
   }

   public void receiveClientStrand(
      int interpolationTick, List<Vector3d> incomingPoints, UUID uuid, @Nullable BlockPos startAttachmentPos, @Nullable BlockPos endAttachmentPos
   ) {
      if (this.ownedClientStrand == null) {
         this.ownedClientStrand = new ClientRopeStrand(uuid);
      }

      ClientLevelRopeManager.getOrCreate(this.getLevel()).addStrand(this.ownedClientStrand);
      Vec3 startAttachmentPoint = this.getAttachmentPoint(startAttachmentPos);
      Vec3 endAttachmentPoint = this.getAttachmentPoint(endAttachmentPos);
      if (startAttachmentPoint != null) {
         this.ownedClientStrand.startAttachment = startAttachmentPoint;
      }

      if (endAttachmentPoint != null) {
         this.ownedClientStrand.endAttachment = endAttachmentPoint;
      }

      ObjectArrayList<ClientRopePoint> points = this.ownedClientStrand.getPoints();
      this.ownedClientStrand.setStopped(false);

      while (points.size() < incomingPoints.size()) {
         Vector3dc position = (Vector3dc)incomingPoints.get(incomingPoints.size() - points.size() - 1);
         points.addFirst(new ClientRopePoint(new Vector3d(position), new Vector3d(position), new ObjectArrayList()));
      }

      while (points.size() > incomingPoints.size()) {
         points.removeFirst();
      }

      for (int i = 0; i < incomingPoints.size(); i++) {
         Vector3d incomingPoint = incomingPoints.get(i);
         ((ClientRopePoint)points.get(i)).snapshots().add(new ClientRopePoint.Snapshot((double)interpolationTick, incomingPoint));
      }
   }

   private Vec3 getAttachmentPoint(@Nullable BlockPos attachment) {
      if (attachment == null) {
         return null;
      } else if (this.getLevel().getBlockEntity(attachment) instanceof SmartBlockEntity smartBlockEntity) {
         RopeStrandHolderBehavior holder = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(TYPE);
         return holder == null ? null : holder.getAttachmentPoint();
      } else {
         return null;
      }
   }

   private Vec3 getVisualAttachmentPoint(@Nullable BlockPos attachment) {
      if (attachment == null) {
         return null;
      } else if (this.getLevel().getBlockEntity(attachment) instanceof SmartBlockEntity smartBlockEntity) {
         RopeStrandHolderBehavior holder = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(TYPE);
         return holder == null ? null : holder.getVisualAttachmentPoint();
      } else {
         return null;
      }
   }

   private void loadServerStrand(CompoundTag tag) {
      DataResult<Pair<ServerRopeStrand, Tag>> result = ServerRopeStrand.CODEC.decode(NbtOps.INSTANCE, tag);
      ServerRopeStrand strand = (ServerRopeStrand)((Pair)result.getOrThrow()).getFirst();
      this.ownedServerStrand = strand;
      this.queuedLevelAddition = true;
   }

   public void destroy() {
      ServerRopeStrand attachedStrand = this.getAttachedStrand();
      Level level = this.getLevel();
      if (level != null && level.isClientSide) {
         this.removeClientStrand();
      }

      boolean tileDrops = level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS);
      if (!this.strandOwner && attachedStrand != null) {
         RopeAttachment startAttachment = attachedStrand.getAttachment(RopeAttachmentPoint.START);
         BlockPos blockAttachment = startAttachment.blockAttachment();
         if (Objects.requireNonNull(level).getBlockEntity(blockAttachment) instanceof SmartBlockEntity smartBlockEntity) {
            RopeStrandHolderBehavior holder = (RopeStrandHolderBehavior)smartBlockEntity.getBehaviour(TYPE);
            if (holder != null) {
               holder.destroyRope(null, this.getAttachmentPoint(), tileDrops);
               return;
            }
         }
      }

      this.destroyRope(null, this.getAttachmentPoint(), tileDrops);
   }

   public boolean isAttached() {
      return this.attachedRopeID != null;
   }

   public boolean ownsRope() {
      return this.strandOwner;
   }

   @Internal
   public void detachRope() {
      this.strandOwner = false;
      this.attachedRopeID = null;
      this.ownedServerStrand = null;
   }

   public ClientRopeStrand getClientStrand() {
      return this.ownedClientStrand;
   }

   public void giveFakeClientStrand(ClientRopeStrand strand) {
      this.strandOwner = true;
      this.ownedClientStrand = strand;
      this.attachedRopeID = strand.getUuid();
   }

   public void giveFakeClientStrand(UUID ropeUUID) {
      this.attachedRopeID = ropeUUID;
   }

   public void takeOwnedStrand(ServerRopeStrand ownedStrand) {
      this.ownedServerStrand = ownedStrand;
   }

   public void receiveClientStrandStopped() {
      if (this.ownedClientStrand != null) {
         this.ownedClientStrand.setStopped(true);
      }
   }
}

package dev.simulated_team.simulated.content.entities.diagram;

import com.simibubi.create.api.schematic.requirement.SpecialEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import com.simibubi.create.foundation.networking.ISyncPersistentData;
import com.simibubi.create.foundation.utility.IInteractionChecker;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup.PointForce;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimStats;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramDataPacket;
import dev.simulated_team.simulated.network.packets.contraption_diagram.DiagramOpenPacket;
import dev.simulated_team.simulated.util.SimColors;
import foundry.veil.api.network.VeilPacketManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class DiagramEntity extends HangingEntity implements ISyncPersistentData, IInteractionChecker, SpecialEntityItemRequirement {
   private static final Map<ResourceKey<Level>, Map<ServerSubLevel, DiagramRecordingTicket>> queuedDiagramRecordings = new WeakHashMap<>();
   protected int size;
   protected Direction verticalOrientation;
   protected DiagramConfig config;

   public static DiagramEntity create(EntityType<? extends HangingEntity> entityType, Level world) {
      return new DiagramEntity(entityType, world);
   }

   public DiagramEntity(EntityType<? extends HangingEntity> entityType, Level level) {
      super(entityType, level);
      this.size = 1;
      this.config = DiagramConfig.makeDefault(this);
   }

   public DiagramEntity(Level world, BlockPos pos, Direction facing, Direction verticalOrientation) {
      super((EntityType)SimEntityTypes.CONTRAPTION_DIAGRAM.get(), world, pos);

      for (int size = 3; size > 0; size--) {
         this.size = size;
         this.updateFacingWithBoundingBox(facing, verticalOrientation);
         if (this.survives()) {
            break;
         }
      }

      this.config = DiagramConfig.makeDefault(this);
   }

   public static void queueDiagramDataFor(SubLevel subLevel, ServerPlayer player) {
      if (subLevel instanceof ServerSubLevel serverSubLevel) {
         serverSubLevel.enableIndividualQueuedForcesTracking(true);
         Map<ServerSubLevel, DiagramRecordingTicket> map = queuedDiagramRecordings.get(serverSubLevel.getLevel().dimension());
         DiagramRecordingTicket ticket = map != null ? map.get(serverSubLevel) : null;
         if (ticket != null && !ticket.isValid()) {
            queuedDiagramRecordings.remove(serverSubLevel);
            ticket = null;
         }

         if (ticket == null) {
            List<ServerPlayer> players = new ObjectArrayList();
            ticket = new DiagramRecordingTicket(serverSubLevel, players);
            queuedDiagramRecordings.computeIfAbsent(serverSubLevel.getLevel().dimension(), x -> new Object2ObjectOpenHashMap()).put(serverSubLevel, ticket);
         }

         List<ServerPlayer> players = ticket.players();
         if (!players.contains(player)) {
            players.add(player);
         }
      }
   }

   public static void postPhysicsTick(Level level) {
      Map<ServerSubLevel, DiagramRecordingTicket> map = queuedDiagramRecordings.get(level.dimension());
      if (map != null) {
         Iterator<Entry<ServerSubLevel, DiagramRecordingTicket>> iter = map.entrySet().iterator();

         while (iter.hasNext()) {
            Entry<ServerSubLevel, DiagramRecordingTicket> entry = iter.next();
            ServerSubLevel subLevel = entry.getKey();
            DiagramRecordingTicket ticket = entry.getValue();
            if (!ticket.isValid()) {
               iter.remove();
               subLevel.enableIndividualQueuedForcesTracking(false);
            } else {
               DiagramDataPacket dataPacket = makeDiagramDataPacket(ticket.subLevel());

               for (ServerPlayer player : ticket.players()) {
                  VeilPacketManager.player(player).sendPacket(new CustomPacketPayload[]{dataPacket});
               }

               subLevel.enableIndividualQueuedForcesTracking(false);
               iter.remove();
            }
         }
      }
   }

   private static DiagramDataPacket makeDiagramDataPacket(ServerSubLevel serverSubLevel) {
      MassData massTracker = serverSubLevel.getMassTracker();
      Object2ObjectMap<ForceGroup, List<PointForce>> sentForces = new Object2ObjectOpenHashMap();
      Object2ObjectMap<ForceGroup, QueuedForceGroup> queuedForceGroups = serverSubLevel.getQueuedForceGroups();
      ServerLevel level = serverSubLevel.getLevel();
      SubLevelPhysicsSystem physicsSystem = SubLevelPhysicsSystem.get(level);
      double timeStep = 0.05 / (double)physicsSystem.getConfig().substepsPerTick;
      if (queuedForceGroups != null) {
         ObjectIterator centerOfMass = queuedForceGroups.entrySet().iterator();

         while (centerOfMass.hasNext()) {
            Entry<ForceGroup, QueuedForceGroup> entry = (Entry<ForceGroup, QueuedForceGroup>)centerOfMass.next();
            ForceGroup key = entry.getKey();
            QueuedForceGroup value = entry.getValue();
            List<PointForce> pointForces = new ObjectArrayList();

            for (PointForce pointForce : value.getRecordedPointForces()) {
               Vector3dc force = new Vector3d(pointForce.force()).div(timeStep);
               pointForces.add(new PointForce(pointForce.point(), force));
            }

            if (!pointForces.isEmpty()) {
               sentForces.put(key, pointForces);
            }
         }
      }

      Vector3dc centerOfMass = serverSubLevel.getMassTracker().getCenterOfMass();
      Pose3d pose = serverSubLevel.logicalPose();
      Vector3d localGravity = pose.transformNormalInverse(DimensionPhysicsData.getGravity(level)).mul(serverSubLevel.getMassTracker().getMass());
      sentForces.put((ForceGroup)ForceGroups.GRAVITY.get(), List.of(new PointForce(new Vector3d(centerOfMass), localGravity)));
      return new DiagramDataPacket(sentForces, massTracker.getMass());
   }

   public void remove(RemovalReason reason) {
      super.remove(reason);
   }

   public void addAdditionalSaveData(CompoundTag tag) {
      tag.putByte("Facing", (byte)this.direction.get3DDataValue());
      tag.putByte("Orientation", (byte)this.verticalOrientation.get3DDataValue());
      tag.putInt("Size", this.size);
      if (this.config != null) {
         tag.put("Config", (Tag)DiagramConfig.CODEC.encodeStart(NbtOps.INSTANCE, this.config).getOrThrow());
      }

      super.addAdditionalSaveData(tag);
   }

   public void readAdditionalSaveData(CompoundTag tag) {
      if (tag.contains("Facing", 99)) {
         this.direction = Direction.from3DDataValue(tag.getByte("Facing"));
         this.verticalOrientation = Direction.from3DDataValue(tag.getByte("Orientation"));
         this.size = tag.getInt("Size");
      } else {
         this.direction = Direction.SOUTH;
         this.verticalOrientation = Direction.DOWN;
         this.size = 1;
      }

      if (tag.contains("Config", 10)) {
         CompoundTag configTag = tag.getCompound("Config");
         this.config = (DiagramConfig)DiagramConfig.CODEC.parse(NbtOps.INSTANCE, configTag).getOrThrow();
      } else {
         this.config = DiagramConfig.makeDefault(this);
      }

      super.readAdditionalSaveData(tag);
      this.updateFacingWithBoundingBox(this.direction, this.verticalOrientation);
   }

   protected void updateFacingWithBoundingBox(Direction facing, Direction verticalOrientation) {
      Objects.requireNonNull(facing);
      this.direction = facing;
      this.verticalOrientation = verticalOrientation;
      if (facing.getAxis().isHorizontal()) {
         this.setXRot(0.0F);
         this.setYRot((float)(this.direction.get2DDataValue() * 90));
      } else {
         this.setXRot((float)(-90 * facing.getAxisDirection().getStep()));
         this.setYRot(verticalOrientation.getAxis().isHorizontal() ? 180.0F + verticalOrientation.toYRot() : 0.0F);
      }

      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
      this.recalculateBoundingBox();
   }

   public EntityDimensions getDimensions(Pose pose) {
      return super.getDimensions(pose).withEyeHeight(0.0F);
   }

   protected AABB calculateBoundingBox(BlockPos blockPos, Direction direction) {
      Vec3 pos = Vec3.atLowerCornerOf(this.getPos()).add(0.5, 0.5, 0.5).subtract(Vec3.atLowerCornerOf(direction.getNormal()).scale(0.46875));
      double d1 = pos.x;
      double d2 = pos.y;
      double d3 = pos.z;
      this.setPosRaw(d1, d2, d3);
      Axis axis = direction.getAxis();
      if (this.size == 2) {
         pos = pos.add(
               Vec3.atLowerCornerOf(axis.isHorizontal() ? direction.getCounterClockWise().getNormal() : this.verticalOrientation.getClockWise().getNormal())
                  .scale(0.5)
            )
            .add(
               Vec3.atLowerCornerOf(
                     axis.isHorizontal()
                        ? Direction.UP.getNormal()
                        : (direction == Direction.UP ? this.verticalOrientation.getNormal() : this.verticalOrientation.getOpposite().getNormal())
                  )
                  .scale(0.5)
            );
      }

      d1 = pos.x;
      d2 = pos.y;
      d3 = pos.z;
      double d4 = (double)this.getWidth();
      double d5 = (double)this.getHeight();
      double d6 = (double)this.getWidth();
      Axis direction$axis = this.direction.getAxis();
      switch (direction$axis) {
         case X:
            d4 = 1.0;
            break;
         case Y:
            d5 = 1.0;
            break;
         case Z:
            d6 = 1.0;
      }

      d4 /= 32.0;
      d5 /= 32.0;
      d6 /= 32.0;
      return new AABB(d1 - d4, d2 - d5, d3 - d6, d1 + d4, d2 + d5, d3 + d6);
   }

   public void recalculateBoundingBox() {
      if (this.direction != null && this.verticalOrientation != null) {
         this.setBoundingBox(this.calculateBoundingBox(this.pos, this.direction));
      }
   }

   public Vec3 getLightProbePosition(float partialTicks) {
      return this.position();
   }

   public boolean survives() {
      if (!this.level().noCollision(this)) {
         return false;
      } else {
         int i = Math.max(1, this.getWidth() / 16);
         int j = Math.max(1, this.getHeight() / 16);
         BlockPos blockpos = this.pos.relative(this.direction.getOpposite());
         Direction upDirection = this.direction.getAxis().isHorizontal()
            ? Direction.UP
            : (this.direction == Direction.UP ? this.verticalOrientation : this.verticalOrientation.getOpposite());
         Direction newDirection = this.direction.getAxis().isVertical() ? this.verticalOrientation.getClockWise() : this.direction.getCounterClockWise();
         MutableBlockPos blockpos$mutable = new MutableBlockPos();

         for (int k = 0; k < i; k++) {
            for (int l = 0; l < j; l++) {
               int i1 = (i - 1) / -2;
               int j1 = (j - 1) / -2;
               blockpos$mutable.set(blockpos).move(newDirection, k + i1).move(upDirection, l + j1);
               BlockState blockstate = this.level().getBlockState(blockpos$mutable);
               if (!Block.canSupportCenter(this.level(), blockpos$mutable, this.direction) && !blockstate.isSolid() && !DiodeBlock.isDiode(blockstate)) {
                  return false;
               }
            }
         }

         return this.level().getEntities(this, this.getBoundingBox(), HANGING_ENTITY).isEmpty();
      }
   }

   public int getWidth() {
      return 16 * this.size;
   }

   public int getHeight() {
      return 16 * this.size;
   }

   public void dropItem(@Nullable Entity p_110128_1_) {
      if (this.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
         this.playSound(SoundEvents.PAINTING_BREAK, 1.0F, 1.0F);
         if (p_110128_1_ instanceof Player playerentity && playerentity.getAbilities().instabuild) {
            return;
         }

         this.spawnAtLocation(SimItems.CONTRAPTION_DIAGRAM.asStack());
      }
   }

   public ItemStack getPickResult() {
      return SimItems.CONTRAPTION_DIAGRAM.asStack();
   }

   public void playPlacementSound() {
      this.playSound(SoundEvents.PAINTING_PLACE, 1.0F, 1.0F);
   }

   protected void defineSynchedData(Builder builder) {
   }

   public void moveTo(double x, double y, double z, float p_70012_7_, float p_70012_8_) {
      this.setPos(x, y, z);
   }

   @OnlyIn(Dist.CLIENT)
   public void lerpTo(double pX, double pY, double pZ, float pYRot, float pXRot, int pSteps) {
      BlockPos blockpos = this.pos.offset(BlockPos.containing(pX - this.getX(), pY - this.getY(), pZ - this.getZ()));
      this.setPos((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
   }

   public void setPos(double pX, double pY, double pZ) {
      this.setPosRaw(pX, pY, pZ);
      super.setPos(pX, pY, pZ);
   }

   public InteractionResult interactAt(Player player, Vec3 vec, InteractionHand hand) {
      if (this.level().isClientSide) {
         SubLevel subLevel = Sable.HELPER.getContaining(this);
         if (subLevel == null) {
            player.displayClientMessage(SimLang.translate("contraption_diagram.cannot_use").color(SimColors.NUH_UH_RED).component(), true);
         }
      } else {
         SubLevel subLevel = Sable.HELPER.getContaining(this);
         if (subLevel != null) {
            queueDiagramDataFor(subLevel, (ServerPlayer)player);
            VeilPacketManager.player((ServerPlayer)player).sendPacket(new CustomPacketPayload[]{new DiagramOpenPacket(this.getId(), this.config)});
            SimStats.INTERACT_WITH_CONTRAPTION_DIAGRAM.awardTo(player);
            SimAdvancements.MEASURE_ONCE_BUILD_TWICE.awardTo(player);
         }
      }

      return InteractionResult.SUCCESS;
   }

   public void onPersistentDataUpdated() {
   }

   public boolean canPlayerUse(Player player) {
      AABB box = this.getBoundingBox();
      double dx = 0.0;
      if (box.minX > player.getX()) {
         dx = box.minX - player.getX();
      } else if (player.getX() > box.maxX) {
         dx = player.getX() - box.maxX;
      }

      double dy = 0.0;
      if (box.minY > player.getY()) {
         dy = box.minY - player.getY();
      } else if (player.getY() > box.maxY) {
         dy = player.getY() - box.maxY;
      }

      double dz = 0.0;
      if (box.minZ > player.getZ()) {
         dz = box.minZ - player.getZ();
      } else if (player.getZ() > box.maxZ) {
         dz = player.getZ() - box.maxZ;
      }

      return dx * dx + dy * dy + dz * dz <= 64.0;
   }

   public ItemRequirement getRequiredItems() {
      return new ItemRequirement(ItemUseType.CONSUME, (Item)SimItems.CONTRAPTION_DIAGRAM.get());
   }

   public void setConfig(DiagramConfig config) {
      this.config = config;
   }
}

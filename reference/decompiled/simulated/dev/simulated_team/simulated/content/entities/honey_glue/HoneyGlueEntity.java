package dev.simulated_team.simulated.content.entities.honey_glue;

import com.simibubi.create.api.schematic.requirement.SpecialEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement.ItemUseType;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueSyncBoundsPacket;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HoneyGlueEntity extends Entity implements SpecialEntityItemRequirement {
   public BlockPos blockMin = null;
   public BlockPos blockMax = null;

   public static HoneyGlueEntity create(EntityType<?> entityType, Level world) {
      return new HoneyGlueEntity(entityType, world);
   }

   public HoneyGlueEntity(EntityType<?> type, Level world) {
      super(type, world);
      this.noPhysics = true;
   }

   protected void defineSynchedData(Builder builder) {
   }

   public HoneyGlueEntity(Level world, AABB boundingBox) {
      this((EntityType<?>)SimEntityTypes.HONEY_GLUE.get(), world);
      this.setBoundingBox(boundingBox);
      this.resetPositionToBounds();
      this.setBoundsAndSync(boundingBox);
   }

   public void tick() {
      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
      this.walkDistO = this.walkDist;
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      if (this.level().isClientSide) {
         this.updateClientBounds();
      } else if (this.getBoundingBox().getXsize() < 0.9F || this.getBoundingBox().getYsize() < 0.9F || this.getBoundingBox().getZsize() < 0.9F) {
         Simulated.LOGGER.warn("Removing {} ({}) due to invalid bounds!", this.getUUID(), SimLang.builder().add(this.getName()).string());
         this.discard();
      }
   }

   public void updateClientBounds() {
      if (this.blockMin != null && this.blockMax != null) {
         this.setBoundingBox(new AABB(Vec3.atLowerCornerOf(this.blockMin), Vec3.atLowerCornerOf(this.blockMax)));
      }
   }

   public void resetPositionToBounds() {
      AABB bb = this.getBoundingBox();
      this.setPosRaw(bb.getCenter().x, bb.minY, bb.getCenter().z);
   }

   public void spawnParticles() {
      AABB bb = this.getBoundingBox();
      Vec3 origin = new Vec3(bb.minX, bb.minY, bb.minZ);
      Vec3 extents = new Vec3(bb.getXsize(), bb.getYsize(), bb.getZsize());
      if (this.level() instanceof ServerLevel serverLevel) {
         label68:
         for (Axis axis : Iterate.axes) {
            AxisDirection positive = AxisDirection.POSITIVE;
            double max = axis.choose(extents.x, extents.y, extents.z);
            Vec3 normal = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis, positive).getNormal());

            for (Axis axis2 : Iterate.axes) {
               if (axis2 != axis) {
                  double max2 = axis2.choose(extents.x, extents.y, extents.z);
                  Vec3 normal2 = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis2, positive).getNormal());

                  for (Axis axis3 : Iterate.axes) {
                     if (axis3 != axis2 && axis3 != axis) {
                        double max3 = axis3.choose(extents.x, extents.y, extents.z);
                        Vec3 normal3 = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis3, positive).getNormal());

                        for (int i = 0; (double)i <= max * 2.0; i++) {
                           for (int o1 : Iterate.zeroAndOne) {
                              for (int o2 : Iterate.zeroAndOne) {
                                 Vec3 v = origin.add(normal.scale((double)((float)i / 2.0F)))
                                    .add(normal2.scale(max2 * (double)o1))
                                    .add(normal3.scale(max3 * (double)o2));
                                 serverLevel.sendParticles(
                                    new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Blocks.HONEY_BLOCK)), v.x, v.y, v.z, 1, 0.0, 0.0, 0.0, 0.0
                                 );
                              }
                           }
                        }
                        continue label68;
                     }
                  }
                  break;
               }
            }
         }
      }
   }

   protected boolean repositionEntityAfterLoad() {
      return false;
   }

   public float rotate(Rotation transformRotation) {
      AABB bb = this.getBoundingBox().move(this.position().scale(-1.0));
      if (transformRotation == Rotation.CLOCKWISE_90 || transformRotation == Rotation.COUNTERCLOCKWISE_90) {
         this.setBoundsAndSync(new AABB(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX).move(this.position()));
      }

      return super.rotate(transformRotation);
   }

   public void setPos(double x, double y, double z) {
      AABB bb = this.getBoundingBox();
      this.setPosRaw(x, y, z);
      Vec3 center = bb.getCenter();
      this.setBoundingBox(bb.move(-center.x, -bb.minY, -center.z).move(x, y, z));
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      Vec3 position = this.position();
      AABB savedBounds = this.getBoundingBox().move(position.scale(-1.0));
      compound.put("From", VecHelper.writeNBT(new Vec3(savedBounds.minX, savedBounds.minY, savedBounds.minZ)));
      compound.put("To", VecHelper.writeNBT(new Vec3(savedBounds.maxX, savedBounds.maxY, savedBounds.maxZ)));
      if (!compound.contains("Pos")) {
         compound.put("Pos", VecHelper.writeNBT(position));
      }
   }

   public void readAdditionalSaveData(@NotNull CompoundTag compound) {
      Vec3 pos = VecHelper.readNBT(compound.getList("Pos", 6));
      Vec3 from = VecHelper.readNBT(compound.getList("From", 6));
      Vec3 to = VecHelper.readNBT(compound.getList("To", 6));
      AABB bb = new AABB(from, to).move(pos);
      Level level = this.level();
      if (level.isClientSide) {
         this.setBounds(bb);
      } else {
         this.setBoundsAndSync(bb);
      }
   }

   public void thunderHit(ServerLevel serverLevel, LightningBolt lightningBolt) {
   }

   public double getEyeY() {
      return 0.0;
   }

   public void setBoundsAndSync(AABB bounds) {
      Level level = this.level();
      if (!level.isClientSide) {
         this.setBounds(bounds);
         this.syncBounds(null);
      }
   }

   public void setBoundsAndSync(AABB bounds, @Nullable Player player) {
      Level level = this.level();
      if (!level.isClientSide) {
         this.setBounds(bounds);
         this.syncBounds(player);
      }
   }

   public void syncBounds(@Nullable Player player) {
      VeilPacketManager.tracking(this)
         .sendPacket(new CustomPacketPayload[]{new HoneyGlueSyncBoundsPacket(this.getBoundingBox(), this.getId(), player != null ? player.getUUID() : null)});
   }

   public void setBounds(AABB bounds) {
      this.setBoundingBox(bounds);
      this.resetPositionToBounds();
      this.blockMin = BlockPos.containing(bounds.getMinPosition());
      this.blockMax = BlockPos.containing(bounds.getMaxPosition());
   }

   public void move(MoverType typeIn, Vec3 pos) {
      if (!this.level().isClientSide && this.isAlive() && pos.lengthSqr() > 0.0) {
         this.discard();
      }
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      return InteractionResult.PASS;
   }

   public void refreshDimensions() {
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   public ItemRequirement getRequiredItems() {
      return new ItemRequirement(ItemUseType.DAMAGE, (Item)SimItems.HONEY_GLUE.get());
   }

   public boolean isIgnoringBlockTriggers() {
      return true;
   }

   public boolean contains(BlockPos pos) {
      return this.getBoundingBox().contains(Vec3.atCenterOf(pos));
   }

   public PushReaction getPistonPushReaction() {
      return PushReaction.IGNORE;
   }

   @NotNull
   public EntityDimensions getDimensions(@NotNull Pose pose) {
      return super.getDimensions(pose).withEyeHeight(0.0F);
   }
}

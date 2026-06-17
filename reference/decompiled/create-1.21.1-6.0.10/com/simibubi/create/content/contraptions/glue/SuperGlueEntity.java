package com.simibubi.create.content.contraptions.glue;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.schematic.requirement.SpecialEntityItemRequirement;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.chassis.AbstractChassisBlock;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;

public class SuperGlueEntity extends Entity implements IEntityWithComplexSpawn, SpecialEntityItemRequirement {
   public static AABB span(BlockPos startPos, BlockPos endPos) {
      return new AABB(Vec3.atLowerCornerOf(startPos), Vec3.atLowerCornerOf(endPos)).expandTowards(1.0, 1.0, 1.0);
   }

   public static boolean isGlued(LevelAccessor level, BlockPos blockPos, Direction direction, Set<SuperGlueEntity> cached) {
      BlockPos targetPos = blockPos.relative(direction);
      if (cached != null) {
         for (SuperGlueEntity glueEntity : cached) {
            if (glueEntity.contains(blockPos) && glueEntity.contains(targetPos)) {
               return true;
            }
         }
      }

      for (SuperGlueEntity glueEntityx : level.getEntitiesOfClass(SuperGlueEntity.class, span(blockPos, targetPos).inflate(16.0))) {
         if (glueEntityx.contains(blockPos) && glueEntityx.contains(targetPos)) {
            if (cached != null) {
               cached.add(glueEntityx);
            }

            return true;
         }
      }

      return false;
   }

   public static List<SuperGlueEntity> collectCropped(Level level, AABB bb) {
      List<SuperGlueEntity> glue = new ArrayList<>();

      for (SuperGlueEntity glueEntity : level.getEntitiesOfClass(SuperGlueEntity.class, bb)) {
         AABB glueBox = glueEntity.getBoundingBox();
         AABB intersect = bb.intersect(glueBox);
         if (intersect.getXsize() * intersect.getYsize() * intersect.getZsize() != 0.0 && !Mth.equal(intersect.getSize(), 1.0)) {
            glue.add(new SuperGlueEntity(level, intersect));
         }
      }

      return glue;
   }

   public SuperGlueEntity(EntityType<?> type, Level world) {
      super(type, world);
   }

   public SuperGlueEntity(Level world, AABB boundingBox) {
      this((EntityType<?>)AllEntityTypes.SUPER_GLUE.get(), world);
      this.setBoundingBox(boundingBox);
      this.resetPositionToBB();
   }

   public void resetPositionToBB() {
      AABB bb = this.getBoundingBox();
      this.setPosRaw(bb.getCenter().x, bb.minY, bb.getCenter().z);
   }

   protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
   }

   public static boolean isValidFace(Level world, BlockPos pos, Direction direction) {
      BlockState state = world.getBlockState(pos);
      if (BlockMovementChecks.isBlockAttachedTowards(state, world, pos, direction)) {
         return true;
      } else {
         return !BlockMovementChecks.isMovementNecessary(state, world, pos) ? false : !BlockMovementChecks.isNotSupportive(state, direction);
      }
   }

   public static boolean isSideSticky(Level world, BlockPos pos, Direction direction) {
      BlockState state = world.getBlockState(pos);
      if (AllBlocks.STICKY_MECHANICAL_PISTON.has(state)) {
         return state.getValue(DirectionalKineticBlock.FACING) == direction;
      } else if (AllBlocks.STICKER.has(state)) {
         return state.getValue(DirectionalBlock.FACING) == direction;
      } else if (state.getBlock() == Blocks.SLIME_BLOCK) {
         return true;
      } else if (state.getBlock() == Blocks.HONEY_BLOCK) {
         return true;
      } else if (AllBlocks.CART_ASSEMBLER.has(state)) {
         return Direction.UP == direction;
      } else if (AllBlocks.GANTRY_CARRIAGE.has(state)) {
         return state.getValue(DirectionalKineticBlock.FACING) == direction;
      } else if (state.getBlock() instanceof BearingBlock) {
         return state.getValue(DirectionalKineticBlock.FACING) == direction;
      } else if (state.getBlock() instanceof AbstractChassisBlock) {
         BooleanProperty glueableSide = ((AbstractChassisBlock)state.getBlock()).getGlueableSide(state, direction);
         return glueableSide == null ? false : (Boolean)state.getValue(glueableSide);
      } else {
         return false;
      }
   }

   public boolean hurt(DamageSource source, float amount) {
      return false;
   }

   public void tick() {
      this.xRotO = this.getXRot();
      this.yRotO = this.getYRot();
      this.walkDistO = this.walkDist;
      this.xo = this.getX();
      this.yo = this.getY();
      this.zo = this.getZ();
      if (this.getBoundingBox().getXsize() == 0.0) {
         this.discard();
      }
   }

   public void setPos(double x, double y, double z) {
      AABB bb = this.getBoundingBox();
      this.setPosRaw(x, y, z);
      Vec3 center = bb.getCenter();
      this.setBoundingBox(bb.move(-center.x, -bb.minY, -center.z).move(x, y, z));
   }

   public void move(MoverType typeIn, Vec3 pos) {
      if (!this.level().isClientSide && this.isAlive() && pos.lengthSqr() > 0.0) {
         this.discard();
      }
   }

   public void push(double x, double y, double z) {
      if (!this.level().isClientSide && this.isAlive() && x * x + y * y + z * z > 0.0) {
         this.discard();
      }
   }

   @NotNull
   public EntityDimensions getDimensions(@NotNull Pose pose) {
      return super.getDimensions(pose).withEyeHeight(0.0F);
   }

   public void playPlaceSound() {
      AllSoundEvents.SLIME_ADDED.playFrom(this, 0.5F, 0.75F);
   }

   public void push(Entity entityIn) {
      super.push(entityIn);
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      return InteractionResult.PASS;
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      Vec3 position = this.position();
      writeBoundingBox(compound, this.getBoundingBox().move(position.scale(-1.0)));
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      Vec3 position = this.position();
      this.setBoundingBox(readBoundingBox(compound).move(position));
   }

   public static void writeBoundingBox(CompoundTag compound, AABB bb) {
      compound.put("From", VecHelper.writeNBT(new Vec3(bb.minX, bb.minY, bb.minZ)));
      compound.put("To", VecHelper.writeNBT(new Vec3(bb.maxX, bb.maxY, bb.maxZ)));
   }

   public static AABB readBoundingBox(CompoundTag compound) {
      Vec3 from = VecHelper.readNBT(compound.getList("From", 6));
      Vec3 to = VecHelper.readNBT(compound.getList("To", 6));
      return new AABB(from, to);
   }

   protected boolean repositionEntityAfterLoad() {
      return false;
   }

   public float rotate(Rotation transformRotation) {
      AABB bb = this.getBoundingBox().move(this.position().scale(-1.0));
      if (transformRotation == Rotation.CLOCKWISE_90 || transformRotation == Rotation.COUNTERCLOCKWISE_90) {
         this.setBoundingBox(new AABB(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX).move(this.position()));
      }

      return super.rotate(transformRotation);
   }

   public float mirror(Mirror transformMirror) {
      return super.mirror(transformMirror);
   }

   public void thunderHit(ServerLevel world, LightningBolt lightningBolt) {
   }

   public void refreshDimensions() {
   }

   public static Builder<?> build(Builder<?> builder) {
      return builder;
   }

   public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
      CompoundTag compound = new CompoundTag();
      this.addAdditionalSaveData(compound);
      buffer.writeNbt(compound);
   }

   public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
      this.readAdditionalSaveData(additionalData.readNbt());
   }

   @Override
   public ItemRequirement getRequiredItems() {
      return new ItemRequirement(ItemRequirement.ItemUseType.DAMAGE, (Item)AllItems.SUPER_GLUE.get());
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

   public void spawnParticles() {
      AABB bb = this.getBoundingBox();
      Vec3 origin = new Vec3(bb.minX, bb.minY, bb.minZ);
      Vec3 extents = new Vec3(bb.getXsize(), bb.getYsize(), bb.getZsize());
      if (this.level() instanceof ServerLevel slevel) {
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
                                 slevel.sendParticles(ParticleTypes.ITEM_SLIME, v.x, v.y, v.z, 1, 0.0, 0.0, 0.0, 0.0);
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
}

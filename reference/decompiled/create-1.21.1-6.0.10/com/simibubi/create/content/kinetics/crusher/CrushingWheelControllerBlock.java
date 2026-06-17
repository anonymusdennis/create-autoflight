package com.simibubi.create.content.kinetics.crusher;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

public class CrushingWheelControllerBlock extends DirectionalBlock implements IBE<CrushingWheelControllerBlockEntity> {
   public static final BooleanProperty VALID = BooleanProperty.create("valid");
   public static final MapCodec<CrushingWheelControllerBlock> CODEC = simpleCodec(CrushingWheelControllerBlock::new);

   public CrushingWheelControllerBlock(Properties p_i48440_1_) {
      super(p_i48440_1_);
   }

   public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
      return false;
   }

   public boolean addRunningEffects(BlockState state, Level world, BlockPos pos, Entity entity) {
      return true;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{VALID});
      builder.add(new Property[]{FACING});
      super.createBlockStateDefinition(builder);
   }

   public void entityInside(BlockState state, Level worldIn, BlockPos pos, Entity entityIn) {
      if ((Boolean)state.getValue(VALID)) {
         Direction facing = (Direction)state.getValue(FACING);
         Axis axis = facing.getAxis();
         this.checkEntityForProcessing(worldIn, pos, entityIn);
         this.withBlockEntityDo(worldIn, pos, be -> {
            if (be.processingEntity == entityIn) {
               entityIn.makeStuckInBlock(state, new Vec3(axis == Axis.X ? 0.05F : 0.25, axis == Axis.Y ? 0.05F : 0.25, axis == Axis.Z ? 0.05F : 0.25));
            }
         });
      }
   }

   public void checkEntityForProcessing(Level worldIn, BlockPos pos, Entity entityIn) {
      CrushingWheelControllerBlockEntity be = this.getBlockEntity(worldIn, pos);
      if (be != null) {
         if (be.crushingspeed != 0.0F) {
            CompoundTag data = entityIn.getPersistentData();
            if (!data.contains("BypassCrushingWheel") || !pos.equals(NBTHelper.readBlockPos(data, "BypassCrushingWheel"))) {
               if (!be.isOccupied()) {
                  boolean isPlayer = entityIn instanceof Player;
                  if (!isPlayer || !((Player)entityIn).isCreative()) {
                     if (!isPlayer || entityIn.level().getDifficulty() != Difficulty.PEACEFUL) {
                        be.startCrushing(entityIn);
                     }
                  }
               }
            }
         }
      }
   }

   public void updateEntityAfterFallOn(BlockGetter worldIn, Entity entityIn) {
      super.updateEntityAfterFallOn(worldIn, entityIn);
   }

   public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
      if ((Boolean)stateIn.getValue(VALID)) {
         if (rand.nextInt(1) == 0) {
            double d0 = (double)((float)pos.getX() + rand.nextFloat());
            double d1 = (double)((float)pos.getY() + rand.nextFloat());
            double d2 = (double)((float)pos.getZ() + rand.nextFloat());
            worldIn.addParticle(ParticleTypes.CRIT, d0, d1, d2, 0.0, 0.0, 0.0);
         }
      }
   }

   public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
      this.updateSpeed(stateIn, worldIn, currentPos);
      return stateIn;
   }

   public void updateSpeed(BlockState state, LevelAccessor world, BlockPos pos) {
      this.withBlockEntityDo(
         world,
         pos,
         be -> {
            if (!(Boolean)state.getValue(VALID)) {
               if (be.crushingspeed != 0.0F) {
                  be.crushingspeed = 0.0F;
                  be.sendData();
               }
            } else {
               for (Direction d : Iterate.directions) {
                  BlockState neighbour = world.getBlockState(pos.relative(d));
                  if (AllBlocks.CRUSHING_WHEEL.has(neighbour)
                     && neighbour.getValue(BlockStateProperties.AXIS) != d.getAxis()
                     && world.getBlockEntity(pos.relative(d)) instanceof CrushingWheelBlockEntity cwbe) {
                     be.crushingspeed = Math.abs(cwbe.getSpeed() / 50.0F);
                     be.sendData();
                     cwbe.award(AllAdvancements.CRUSHING_WHEEL);
                     if (Math.abs(cwbe.getSpeed()) > (float)((Integer)AllConfigs.server().kinetics.maxRotationSpeed.get() - 1)) {
                        cwbe.award(AllAdvancements.CRUSHER_MAXED);
                     }
                     break;
                  }
               }
            }
         }
      );
   }

   public VoxelShape getCollisionShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
      VoxelShape standardShape = AllShapes.CRUSHING_WHEEL_CONTROLLER_COLLISION.get((Direction)state.getValue(FACING));
      if (!(Boolean)state.getValue(VALID)) {
         return standardShape;
      } else if (!(context instanceof EntityCollisionContext)) {
         return standardShape;
      } else {
         Entity entity = ((EntityCollisionContext)context).getEntity();
         if (entity == null) {
            return standardShape;
         } else {
            CompoundTag data = entity.getPersistentData();
            if (pos.equals(NBTHelper.readBlockPos(data, "BypassCrushingWheel")) && state.getValue(FACING) != Direction.UP) {
               return Shapes.empty();
            } else {
               CrushingWheelControllerBlockEntity be = this.getBlockEntity(worldIn, pos);
               return be != null && be.processingEntity == entity ? Shapes.empty() : standardShape;
            }
         }
      }
   }

   public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
      if (state.hasBlockEntity() && state.getBlock() != newState.getBlock()) {
         this.withBlockEntityDo(worldIn, pos, be -> ItemHelper.dropContents(worldIn, pos, be.inventory));
         worldIn.removeBlockEntity(pos);
      }
   }

   @Override
   public Class<CrushingWheelControllerBlockEntity> getBlockEntityClass() {
      return CrushingWheelControllerBlockEntity.class;
   }

   @Override
   public BlockEntityType<? extends CrushingWheelControllerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends CrushingWheelControllerBlockEntity>)AllBlockEntityTypes.CRUSHING_WHEEL_CONTROLLER.get();
   }

   protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
      return false;
   }

   @NotNull
   protected MapCodec<? extends DirectionalBlock> codec() {
      return CODEC;
   }
}

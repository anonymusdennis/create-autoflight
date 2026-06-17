package dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockShapes;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

public class HotAirBurnerBlock extends Block implements IBE<HotAirBurnerBlockEntity>, IWrenchable {
   public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
   public static final EnumProperty<HotAirBurnerBlock.Variant> VARIANT = EnumProperty.create("variant", HotAirBurnerBlock.Variant.class);

   public HotAirBurnerBlock(Properties properties) {
      super(properties);
   }

   public static int getLightPower(BlockState state) {
      return state.getValue(POWERED) ? 15 : 0;
   }

   public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
      if (level.getBlockEntity(pos) instanceof HotAirBurnerBlockEntity be) {
         if (!entity.fireImmune() && (Boolean)state.getValue(POWERED) && entity instanceof LivingEntity) {
            LevelReusedVectors jomlSink = ((LevelExtension)level).sable$getJOMLSink();
            SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
            Vector3d burnerCubePos = JOMLConversion.atCenterOf(pos).add(0.0, 0.25, 0.0);
            if (subLevel != null) {
               subLevel.logicalPose().transformPosition(burnerCubePos);
            }

            AABB entityAABB = entity.getBoundingBox();
            Vector3d entityCenter = JOMLConversion.toJOML(entityAABB.getCenter());
            Vector3d sideLengths = new Vector3d(entityAABB.getXsize(), entityAABB.getYsize(), entityAABB.getZsize());
            OrientedBoundingBox3d burnerBounds = new OrientedBoundingBox3d(
               burnerCubePos,
               new Vector3d(0.625),
               (Quaterniondc)(subLevel != null ? subLevel.logicalPose().orientation() : JOMLConversion.QUAT_IDENTITY),
               jomlSink
            );
            OrientedBoundingBox3d entityBounds = new OrientedBoundingBox3d(entityCenter, sideLengths, JOMLConversion.QUAT_IDENTITY, jomlSink);
            if (OrientedBoundingBox3d.sat(burnerBounds, entityBounds).lengthSquared() > 0.0) {
               entity.hurt(level.damageSources().inFire(), (float)be.getSignalStrength() / 7.5F);
            }
         }

         super.entityInside(state, level, pos, entity);
      }
   }

   public Class<HotAirBurnerBlockEntity> getBlockEntityClass() {
      return HotAirBurnerBlockEntity.class;
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      builder.add(new Property[]{POWERED, VARIANT});
      super.createBlockStateDefinition(builder);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult
   ) {
      HotAirBurnerBlock.Variant conversion = HotAirBurnerBlock.Variant.getConversionFromItem(stack.getItem());
      if (conversion != null) {
         HotAirBurnerBlock.Variant current = (HotAirBurnerBlock.Variant)state.getValue(VARIANT);
         if (conversion != current) {
            level.setBlockAndUpdate(pos, (BlockState)state.setValue(VARIANT, conversion));
            level.playLocalSound((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), conversion.sound, SoundSource.BLOCKS, 1.0F, 1.0F, false);
            return ItemInteractionResult.SUCCESS;
         }
      }

      return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
   }

   public BlockState getStateForPlacement(BlockPlaceContext context) {
      return (BlockState)super.getStateForPlacement(context).setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
   }

   public void neighborChanged(BlockState state, Level level, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!level.isClientSide) {
         this.withBlockEntityDo(level, pos, HotAirBurnerBlockEntity::updateSignal);
         boolean previouslyPowered = (Boolean)state.getValue(POWERED);
         if (previouslyPowered != level.hasNeighborSignal(pos)) {
            level.setBlock(pos, (BlockState)state.cycle(POWERED), 2);
         }
      }
   }

   public BlockEntityType<? extends HotAirBurnerBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends HotAirBurnerBlockEntity>)AeroBlockEntityTypes.HOT_AIR_BURNER.get();
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return pContext == CollisionContext.empty() ? AeroBlockShapes.HOT_AIR_BURNER_SMOKE_CLIP : AeroBlockShapes.HOT_AIR_BURNER;
   }

   public RenderShape getRenderShape(BlockState pState) {
      return RenderShape.MODEL;
   }

   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AeroBlockShapes.HOT_AIR_BURNER_PLAYER_COLLISION;
   }

   public VoxelShape getVisualShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return AeroBlockShapes.HOT_AIR_BURNER;
   }

   public static enum Variant implements StringRepresentable {
      FIRE("fire", SoundEvents.NETHERRACK_PLACE),
      SOUL_FIRE("soulful", SoundEvents.SOUL_SAND_PLACE);

      public final String name;
      public final SoundEvent sound;

      private Variant(final String name, final SoundEvent sound) {
         this.name = name;
         this.sound = sound;
      }

      public static HotAirBurnerBlock.Variant getConversionFromItem(Item item) {
         if (item.builtInRegistryHolder().is(AeroTags.ItemTags.BURNER_FIRE)) {
            return FIRE;
         } else {
            return item.builtInRegistryHolder().is(ItemTags.SOUL_FIRE_BASE_BLOCKS) ? SOUL_FIRE : null;
         }
      }

      public String getSerializedName() {
         return this.name;
      }
   }
}

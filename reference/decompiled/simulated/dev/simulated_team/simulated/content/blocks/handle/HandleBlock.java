package dev.simulated_team.simulated.content.blocks.handle;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllTags;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.impl.contraption.BlockMovementChecksImpl;
import dev.simulated_team.simulated.content.blocks.util.AbstractDirectionalAxisBlock;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimClickInteractions;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags.Items;
import org.jetbrains.annotations.Nullable;

public class HandleBlock extends AbstractDirectionalAxisBlock implements IBE<HandleBlockEntity>, IWrenchable {
   public static final MapCodec<HandleBlock> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
               propertiesCodec(),
               DyeColor.CODEC.fieldOf("color").forGetter(HandleBlock::getColor),
               StringRepresentable.fromValues(HandleBlock.Variant::values).fieldOf("variant").forGetter(HandleBlock::getVariant)
            )
            .apply(instance, HandleBlock::new)
   );
   private static final HandleShaper SHAPER = HandleShaper.make();
   @Nullable
   private final DyeColor color;
   private final HandleBlock.Variant variant;

   public HandleBlock(Properties properties, @Nullable DyeColor dyeColor, HandleBlock.Variant variant) {
      super(properties);
      this.color = dyeColor;
      this.variant = variant;
   }

   public static boolean canInteractWithHandle(Player player) {
      ItemStack mainHandItem = player.getMainHandItem();
      return mainHandItem.isEmpty() || mainHandItem.is(AllItems.EXTENDO_GRIP);
   }

   protected ItemInteractionResult useItemOn(
      ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
   ) {
      if (AllItems.WRENCH.isIn(itemStack)) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (canInteractWithHandle(player)) {
         if (level.isClientSide && player.isLocalPlayer()) {
            SimClickInteractions.HANDLE_HANDLER.startHold(level, player, blockPos);
         }

         return ItemInteractionResult.SUCCESS;
      } else {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      }
   }

   public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
      Direction facing = ((Direction)state.getValue(FACING)).getOpposite();
      BlockPos neighbourPos = pos.relative(facing);
      BlockState neighbour = worldIn.getBlockState(neighbourPos);
      return !neighbour.getCollisionShape(worldIn, neighbourPos).isEmpty();
   }

   public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
      if (!worldIn.isClientSide) {
         Direction blockFacing = (Direction)state.getValue(FACING);
         if (fromPos.equals(pos.relative(blockFacing.getOpposite())) && !this.canSurvive(state, worldIn, pos)) {
            worldIn.destroyBlock(pos, true);
         }
      }
   }

   public VoxelShape getShape(BlockState state, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPER.get((Direction)state.getValue(FACING), (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE));
   }

   protected boolean hasAnalogOutputSignal(BlockState state) {
      return true;
   }

   protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
      if (level.getBlockEntity(pos) instanceof HandleBlockEntity be) {
         return be.hasPlayer() ? 15 : 0;
      } else {
         return 0;
      }
   }

   public BlockEntityType<? extends HandleBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends HandleBlockEntity>)SimBlockEntityTypes.HANDLE.get();
   }

   public Class<HandleBlockEntity> getBlockEntityClass() {
      return HandleBlockEntity.class;
   }

   protected MapCodec<? extends DirectionalBlock> codec() {
      return CODEC;
   }

   @Nullable
   public DyeColor getColor() {
      return this.color;
   }

   public HandleBlock.Variant getVariant() {
      return this.variant;
   }

   public static boolean isHorizontal(BlockState state) {
      Axis axis = ((Direction)state.getValue(FACING)).getAxis();
      return axis != Axis.Y && (Boolean)state.getValue(AXIS_ALONG_FIRST_COORDINATE) ^ axis == Axis.X;
   }

   static {
      BlockMovementChecksImpl.registerAttachedCheck((state, world, pos, direction) -> {
         BlockState relativeState = world.getBlockState(pos.relative(direction));
         if (state.getBlock() instanceof HandleBlock && state.getValue(FACING) == direction.getOpposite()) {
            return CheckResult.SUCCESS;
         } else {
            return relativeState.getBlock() instanceof HandleBlock && relativeState.getValue(FACING) == direction ? CheckResult.SUCCESS : CheckResult.PASS;
         }
      });
   }

   public static enum Variant implements StringRepresentable {
      IRON(Ingredient.of(Items.NUGGETS_IRON)),
      COPPER(Ingredient.of(AllTags.commonItemTag("nuggets/copper"))),
      DYED(null);

      @Nullable
      final Ingredient ingredient;

      private Variant(@Nullable final Ingredient ingredient) {
         this.ingredient = ingredient;
      }

      public String getSerializedName() {
         return this.name().toLowerCase(Locale.ROOT);
      }

      @Nullable
      public Ingredient getIngredient() {
         return this.ingredient;
      }
   }
}

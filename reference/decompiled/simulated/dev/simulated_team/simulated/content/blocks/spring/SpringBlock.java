package dev.simulated_team.simulated.content.blocks.spring;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockShapes;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.util.SimColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SpringBlock extends WrenchableDirectionalBlock implements IBE<SpringBlockEntity>, BlockSubLevelAssemblyListener, IWrenchable {
   public static final EnumProperty<SpringBlock.Size> SIZE = EnumProperty.create("size", SpringBlock.Size.class);

   public SpringBlock(Properties properties) {
      super(properties);
      this.registerDefaultState((BlockState)this.defaultBlockState().setValue(SIZE, SpringBlock.Size.MEDIUM));
   }

   protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
      super.createBlockStateDefinition(builder.add(new Property[]{SIZE}));
   }

   public ItemStack getCloneItemStack(LevelReader levelReader, BlockPos blockPos, BlockState blockState) {
      return SimItems.SPRING.asStack();
   }

   public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
      IBE.onRemove(pState, pLevel, pPos, pNewState);
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      return canAttach(pLevel, pPos, ((Direction)pState.getValue(FACING)).getOpposite());
   }

   public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
      return ((Direction)pState.getValue(FACING)).getOpposite() == pFacing && !pState.canSurvive(pLevel, pCurrentPos)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
   }

   public static boolean canAttach(LevelReader pReader, BlockPos pPos, Direction pDirection) {
      BlockPos blockpos = pPos.relative(pDirection);
      return pReader.getBlockState(blockpos).isFaceSturdy(pReader, blockpos, pDirection.getOpposite());
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return switch ((SpringBlock.Size)pState.getValue(SIZE)) {
         case SMALL -> SimBlockShapes.SMALL_SPRING.get((Direction)pState.getValue(FACING));
         case MEDIUM -> SimBlockShapes.SPRING.get((Direction)pState.getValue(FACING));
         case LARGE -> SimBlockShapes.LARGE_SPRING.get((Direction)pState.getValue(FACING));
      };
   }

   public static boolean tryAdjustSpring(Level level, BlockPos pos, Player player) {
      if (level.getBlockEntity(pos) instanceof SpringBlockEntity spring) {
         String error = spring.tryChangeLengthOrError(level, player.isShiftKeyDown() ? -0.25 : 0.25);
         if (error == null) {
            sendLengthMessage("new_length", SimColors.SUCCESS_LIME, spring, player);
            return true;
         }

         sendLengthMessage(error, SimColors.NUH_UH_RED, spring, player);
      }

      return false;
   }

   private static void sendLengthMessage(String suffix, int color, SpringBlockEntity spring, Player player) {
      SimLang.translate("spring." + suffix, String.format("%.2f", spring.desiredLength)).color(color).sendStatus(player);
   }

   public InteractionResult onWrenched(BlockState state, UseOnContext context) {
      Level level = context.getLevel();
      BlockPos pos = context.getClickedPos();
      SpringBlockEntity be = (SpringBlockEntity)this.getBlockEntity(level, pos);
      if (be == null) {
         return InteractionResult.SUCCESS;
      } else {
         SpringBlockEntity partner = be.getPairedSpring();
         if (partner == null) {
            return InteractionResult.SUCCESS;
         } else {
            BlockState partnerState = partner.getBlockState();
            BlockPos partnerPos = partner.getBlockPos();
            SpringBlock.Size size = (SpringBlock.Size)state.getValue(SIZE);
            SpringBlock.Size newSize = size.cycle();
            BlockState newState = (BlockState)state.setValue(SIZE, newSize);
            BlockState newPartnerState = (BlockState)partnerState.setValue(SIZE, newSize);
            level.setBlockAndUpdate(pos, newState);
            level.setBlockAndUpdate(partnerPos, newPartnerState);
            AllSoundEvents.WRENCH_ROTATE.playOnServer(level, pos, 1.0F, Create.RANDOM.nextFloat() + 0.5F);
            return InteractionResult.SUCCESS;
         }
      }
   }

   public Class<SpringBlockEntity> getBlockEntityClass() {
      return SpringBlockEntity.class;
   }

   public BlockEntityType<? extends SpringBlockEntity> getBlockEntityType() {
      return (BlockEntityType<? extends SpringBlockEntity>)SimBlockEntityTypes.SPRING.get();
   }

   protected void spawnAfterBreak(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, ItemStack itemStack, boolean bl) {
      super.spawnAfterBreak(blockState, serverLevel, blockPos, itemStack, bl);
   }

   public void beforeMove(ServerLevel originLevel, ServerLevel newLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
      if (newLevel.getBlockEntity(oldPos) instanceof SpringBlockEntity spring) {
         spring.assembling = true;
      }
   }

   public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
      if (newLevel.getBlockEntity(newPos) instanceof SpringBlockEntity spring) {
         SpringBlockEntity partner = spring.getPairedSpring();
         if (partner != null) {
            SubLevel subLevel = Sable.HELPER.getContaining(newLevel, newPos);
            partner.setPartnerPos(newPos, subLevel != null ? subLevel.getUniqueId() : null);
         }
      }
   }

   public static enum Size implements StringRepresentable {
      SMALL("small"),
      MEDIUM("medium"),
      LARGE("large");

      private static final SpringBlock.Size[] VALUES = values();
      private final String name;

      private Size(final String name) {
         this.name = name;
      }

      @Override
      public String toString() {
         return this.name;
      }

      public String getSerializedName() {
         return this.name;
      }

      public SpringBlock.Size cycle() {
         return VALUES[(this.ordinal() + 1) % VALUES.length];
      }
   }
}

package dev.simulated_team.simulated.content.blocks.behaviour;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class HoldTipBehaviour extends BlockEntityBehaviour {
   public static final BehaviourType<HoldTipBehaviour> TYPE = new BehaviourType();
   private HoldTipBehaviour.HoldTipGetter hoverTip;

   public HoldTipBehaviour(SmartBlockEntity be, MutableComponent hoverTip) {
      super(be);
      this.setHoverTip(hoverTip);
   }

   public HoldTipBehaviour(SmartBlockEntity be, HoldTipBehaviour.HoldTipGetter hoverTip) {
      super(be);
      this.setHoverTip(hoverTip);
   }

   public void setHoverTip(MutableComponent hoverTip) {
      this.hoverTip = (player, pos, state) -> hoverTip;
   }

   public void setHoverTip(HoldTipBehaviour.HoldTipGetter hoverTip) {
      this.hoverTip = hoverTip;
   }

   public MutableComponent getHoverTip(Player player, BlockPos pos, BlockState state) {
      return this.hoverTip.getTip(player, pos, state);
   }

   public BehaviourType<?> getType() {
      return TYPE;
   }

   @FunctionalInterface
   public interface HoldTipGetter {
      @Nullable
      MutableComponent getTip(Player var1, BlockPos var2, BlockState var3);
   }
}

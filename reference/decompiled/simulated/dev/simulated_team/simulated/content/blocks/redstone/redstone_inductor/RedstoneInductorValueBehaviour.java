package dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor;

import com.simibubi.create.content.redstone.diodes.BrassDiodeScrollValueBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RedstoneInductorValueBehaviour extends BrassDiodeScrollValueBehaviour {
   public RedstoneInductorValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
      super(label, be, slot);
   }

   public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
      return super.createBoard(player, hitResult);
   }

   public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
      BlockState blockState = this.blockEntity.getBlockState();
      if (blockState.getBlock() instanceof RedstoneInductorBlock bdb) {
         bdb.toggle(this.getWorld(), this.getPos(), blockState, player, hand);
      }
   }

   public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
      int value = valueSetting.value();

      int multiplier = switch (valueSetting.row()) {
         case 0 -> 1;
         case 1 -> 20;
         default -> 1200;
      };
      if (!valueSetting.equals(this.getValueSettings())) {
         this.playFeedbackSound(this);
      }

      int clampingValue = valueSetting.row() == 0 ? 0 : 1;
      this.setValue(Math.max(clampingValue, Math.max(clampingValue, value) * multiplier));
   }

   public MutableComponent formatSettings(ValueSettings settings) {
      BlockState blockState = this.blockEntity.getBlockState();
      Boolean inverted = (Boolean)blockState.getValue(RedstoneInductorBlock.INVERTED);
      int row = settings.row();
      int column = settings.value();
      if (row == 0 && column == 0) {
         return Component.translatable("block.simulated.redstone_inductor." + (inverted ? "invert" : "copy"));
      } else {
         return Component.literal(switch (settings.row()) {
            case 1 -> "0:" + (column < 10 ? "0" : "") + column;
            case 2 -> column + ":00";
            default -> column + "t";
         });
      }
   }
}

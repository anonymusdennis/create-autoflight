package com.simibubi.create.content.kinetics.base;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface IRotate extends IWrenchable {
   boolean hasShaftTowards(LevelReader var1, BlockPos var2, BlockState var3, Direction var4);

   Axis getRotationAxis(BlockState var1);

   default IRotate.SpeedLevel getMinimumRequiredSpeedLevel() {
      return IRotate.SpeedLevel.NONE;
   }

   default boolean hideStressImpact() {
      return false;
   }

   default boolean showCapacityWithAnnotation() {
      return false;
   }

   public static enum SpeedLevel {
      NONE(ChatFormatting.DARK_GRAY, 0, 0),
      SLOW(ChatFormatting.GREEN, 2293538, 10),
      MEDIUM(ChatFormatting.AQUA, 34047, 20),
      FAST(ChatFormatting.LIGHT_PURPLE, 16733695, 30);

      private final ChatFormatting textColor;
      private final int color;
      private final int particleSpeed;

      private SpeedLevel(ChatFormatting textColor, int color, int particleSpeed) {
         this.textColor = textColor;
         this.color = color;
         this.particleSpeed = particleSpeed;
      }

      public ChatFormatting getTextColor() {
         return this.textColor;
      }

      public int getColor() {
         return this.color;
      }

      public int getParticleSpeed() {
         return this.particleSpeed;
      }

      public float getSpeedValue() {
         switch (this) {
            case NONE:
            default:
               return 0.0F;
            case SLOW:
               return 1.0F;
            case MEDIUM:
               return ((Double)AllConfigs.server().kinetics.mediumSpeed.get()).floatValue();
            case FAST:
               return ((Double)AllConfigs.server().kinetics.fastSpeed.get()).floatValue();
         }
      }

      public static IRotate.SpeedLevel of(float speed) {
         speed = Math.abs(speed);
         if ((double)speed >= (Double)AllConfigs.server().kinetics.fastSpeed.get()) {
            return FAST;
         } else if ((double)speed >= (Double)AllConfigs.server().kinetics.mediumSpeed.get()) {
            return MEDIUM;
         } else {
            return speed >= 1.0F ? SLOW : NONE;
         }
      }

      public static LangBuilder getFormattedSpeedText(float speed, boolean overstressed) {
         IRotate.SpeedLevel speedLevel = of(speed);
         LangBuilder builder = CreateLang.text(TooltipHelper.makeProgressBar(3, speedLevel.ordinal()));
         builder.translate("tooltip.speedRequirement." + Lang.asId(speedLevel.name()), new Object[0])
            .space()
            .text("(")
            .add(CreateLang.number((double)Math.abs(speed)))
            .space()
            .translate("generic.unit.rpm", new Object[0])
            .text(")")
            .space();
         if (overstressed) {
            builder.style(ChatFormatting.DARK_GRAY).style(ChatFormatting.STRIKETHROUGH);
         } else {
            builder.style(speedLevel.getTextColor());
         }

         return builder;
      }
   }

   public static enum StressImpact {
      LOW(ChatFormatting.YELLOW, ChatFormatting.GREEN),
      MEDIUM(ChatFormatting.GOLD, ChatFormatting.YELLOW),
      HIGH(ChatFormatting.RED, ChatFormatting.GOLD),
      OVERSTRESSED(ChatFormatting.RED, ChatFormatting.RED);

      private final ChatFormatting absoluteColor;
      private final ChatFormatting relativeColor;

      private StressImpact(ChatFormatting absoluteColor, ChatFormatting relativeColor) {
         this.absoluteColor = absoluteColor;
         this.relativeColor = relativeColor;
      }

      public ChatFormatting getAbsoluteColor() {
         return this.absoluteColor;
      }

      public ChatFormatting getRelativeColor() {
         return this.relativeColor;
      }

      public static IRotate.StressImpact of(double stressPercent) {
         if (stressPercent > 1.0) {
            return OVERSTRESSED;
         } else if (stressPercent > 0.75) {
            return HIGH;
         } else {
            return stressPercent > 0.5 ? MEDIUM : LOW;
         }
      }

      public static boolean isEnabled() {
         return !(Boolean)AllConfigs.server().kinetics.disableStress.get();
      }

      public static LangBuilder getFormattedStressText(double stressPercent) {
         IRotate.StressImpact stressLevel = of(stressPercent);
         return CreateLang.text(TooltipHelper.makeProgressBar(3, Math.min(stressLevel.ordinal() + 1, 3)))
            .translate("tooltip.stressImpact." + Lang.asId(stressLevel.name()), new Object[0])
            .text(String.format(" (%s%%) ", (int)(stressPercent * 100.0)))
            .style(stressLevel.getRelativeColor());
      }
   }
}

package com.simibubi.create.content.redstone.displayLink.source;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.stream.Stream;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.Objective;

public class ScoreboardDisplaySource extends ValueListDisplaySource {
   @Override
   protected Stream<IntAttached<MutableComponent>> provideEntries(DisplayLinkContext context, int maxRows) {
      if (context.blockEntity().getLevel() instanceof ServerLevel sLevel) {
         String name = context.sourceConfig().getString("Objective");
         return this.showScoreboard(sLevel, name, maxRows);
      } else {
         return Stream.empty();
      }
   }

   protected Stream<IntAttached<MutableComponent>> showScoreboard(ServerLevel sLevel, String objectiveName, int maxRows) {
      Objective objective = sLevel.getScoreboard().getObjective(objectiveName);
      return objective == null
         ? this.notFound(objectiveName).stream()
         : sLevel.getScoreboard()
            .listPlayerScores(objective)
            .stream()
            .map(score -> IntAttached.with(score.value(), Component.literal(score.owner()).copy()))
            .sorted(IntAttached.comparator())
            .limit((long)maxRows);
   }

   private ImmutableList<IntAttached<MutableComponent>> notFound(String objective) {
      return ImmutableList.of(IntAttached.with(404, CreateLang.translateDirect("display_source.scoreboard.objective_not_found", objective)));
   }

   @Override
   protected String getTranslationKey() {
      return "scoreboard";
   }

   @Override
   public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
      if (isFirstLine) {
         builder.addTextInput(
            0,
            137,
            (e, t) -> {
               e.setValue("");
               t.withTooltip(
                  ImmutableList.of(
                     CreateLang.translateDirect("display_source.scoreboard.objective").withStyle(s -> s.withColor(5476833)),
                     CreateLang.translateDirect("gui.schedule.lmb_edit").withStyle(new ChatFormatting[]{ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC})
                  )
               );
            },
            "Objective"
         );
      } else {
         this.addFullNumberConfig(builder);
      }
   }

   @Override
   protected boolean valueFirst() {
      return false;
   }
}

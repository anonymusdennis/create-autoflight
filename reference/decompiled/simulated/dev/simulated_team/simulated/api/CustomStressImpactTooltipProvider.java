package dev.simulated_team.simulated.api;

import net.createmod.catnip.lang.LangBuilder;

public interface CustomStressImpactTooltipProvider {
   LangBuilder getCustomImpactLang();

   int getBarLength();

   int getFilledBarLength();
}

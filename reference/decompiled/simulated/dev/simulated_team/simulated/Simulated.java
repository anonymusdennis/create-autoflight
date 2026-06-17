package dev.simulated_team.simulated;

import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.events.SimulatedCommonEvents;
import dev.simulated_team.simulated.index.SimBlockEntityTypes;
import dev.simulated_team.simulated.index.SimBlockMovementChecks;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimDataComponents;
import dev.simulated_team.simulated.index.SimEntityDataSerializers;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimItemAttributeTypes;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimMenuTypes;
import dev.simulated_team.simulated.index.SimNavigationTargets;
import dev.simulated_team.simulated.index.SimParticleTypes;
import dev.simulated_team.simulated.index.SimRegistries;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.index.SimSpriteShifts;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.network.SimPacketManager;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.service.SimModCompatibilityService;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;

public final class Simulated {
   public static final String MOD_ID = "simulated";
   public static final String MOD_NAME = "Create Simulated";
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final NonNullSupplier<SimulatedRegistrate> REGISTRATE = NonNullSupplier.lazy(
      () -> (SimulatedRegistrate)new SimulatedRegistrate(path("simulated"), "simulated").defaultCreativeTab((ResourceKey)null)
   );

   public static void init() {
      setTooltips();
      SimEntityDataSerializers.register();
      getRegistrate().addDataGenerator(ProviderType.LANG, SimLang::registrateLang);
      SimRegistries.register();
      SimTags.register();
      SimBlocks.register();
      SimItems.register();
      SimBlockEntityTypes.register();
      SimParticleTypes.register();
      SimSoundEvents.init();
      SimSpriteShifts.init();
      SimPacketManager.init();
      SimEntityTypes.register();
      SimMenuTypes.register();
      SimNavigationTargets.register();
      SimDataComponents.register();
      SimItemAttributeTypes.init();
      SimulatedCommonEvents.register();
      SimBlockMovementChecks.register();
      SimAssemblyHelper.register();
      SimModCompatibilityService.initLoaded();
      SableEventPlatform.INSTANCE.onPhysicsTick(SimulatedCommonEvents::onPhysicsTick);
      SableEventPlatform.INSTANCE.onPostPhysicsTick(SimulatedCommonEvents::onPostPhysicsTick);
   }

   public static void setTooltips() {
      getRegistrate().setTooltipModifierFactory(item -> {
         Rarity rarity = item.getDefaultInstance().getRarity();
         Palette color = Palette.STANDARD_CREATE;
         if (rarity == Rarity.EPIC) {
            color = new Palette(TooltipHelper.styleFromColor(SimColors.EPIC_OURPLE), TooltipHelper.styleFromColor(rarity.color()));
         }

         return new Modifier(item, color).andThen(TooltipModifier.mapNull(KineticStats.create(item)));
      });
   }

   public static SimulatedRegistrate getRegistrate() {
      return (SimulatedRegistrate)REGISTRATE.get();
   }

   public static ResourceLocation path(String path) {
      return ResourceLocation.tryBuild("simulated", path);
   }
}

package dev.eriksonn.aeronautics;

import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.item.ItemDescription.Modifier;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.events.AeronauticsCommonEvents;
import dev.eriksonn.aeronautics.index.AeroArmorMaterials;
import dev.eriksonn.aeronautics.index.AeroBlockEntityTypes;
import dev.eriksonn.aeronautics.index.AeroBlockMovementChecks;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.eriksonn.aeronautics.index.AeroDataComponents;
import dev.eriksonn.aeronautics.index.AeroEntityTypes;
import dev.eriksonn.aeronautics.index.AeroItems;
import dev.eriksonn.aeronautics.index.AeroLevititeBlendPropagationContexts;
import dev.eriksonn.aeronautics.index.AeroLiftingGasTypes;
import dev.eriksonn.aeronautics.index.AeroRegistries;
import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.eriksonn.aeronautics.network.AeroPacketManager;
import dev.eriksonn.aeronautics.registry.AeroRegistrate;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.simulated_team.simulated.util.SimColors;
import net.createmod.catnip.lang.FontHelper.Palette;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Aeronautics {
   public static final String MOD_ID = "aeronautics";
   public static final Logger LOGGER = LoggerFactory.getLogger("aeronautics");
   private static final NonNullSupplier<AeroRegistrate> REGISTRATE = NonNullSupplier.lazy(
      () -> (AeroRegistrate)new AeroRegistrate(path("aeronautics"), "aeronautics").defaultCreativeTab((ResourceKey)null)
   );

   public static void init() {
      setTooltips();
      getRegistrate().addDataGenerator(ProviderType.LANG, AeroLang::registrateLang);
      AeroBlocks.init();
      AeroBlockEntityTypes.init();
      AeroItems.init();
      AeroEntityTypes.init();
      AeroArmorMaterials.init();
      AeroSoundEvents.init();
      AeroLiftingGasTypes.init();
      AeroBlockMovementChecks.init();
      AeroRegistries.init();
      AeroPacketManager.init();
      AeroLevititeBlendPropagationContexts.init();
      AeroDataComponents.init();
      listenCommonEvents();
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

   private static void listenCommonEvents() {
      SableEventPlatform.INSTANCE.onPhysicsTick(AeronauticsCommonEvents::physicsTick);
      SableEventPlatform.INSTANCE.onSubLevelContainerReady(AeronauticsCommonEvents::onSubLevelContainerReady);
   }

   public static AeroRegistrate getRegistrate() {
      return (AeroRegistrate)REGISTRATE.get();
   }

   public static ResourceLocation path(String path) {
      return ResourceLocation.tryBuild("aeronautics", path);
   }
}

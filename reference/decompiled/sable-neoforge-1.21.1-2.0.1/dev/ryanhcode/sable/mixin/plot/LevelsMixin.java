package dev.ryanhcode.sable.mixin.plot;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.platform.SableEventPublishPlatform;
import dev.ryanhcode.sable.platform.SablePlatform;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin({ServerLevel.class, ClientLevel.class})
public abstract class LevelsMixin extends Level implements SubLevelContainerHolder {
   @Unique
   private final SubLevelContainer sable$plotContainer = this.sable$createPlotContainer();

   protected LevelsMixin(
      WritableLevelData writableLevelData,
      ResourceKey<Level> resourceKey,
      RegistryAccess registryAccess,
      Holder<DimensionType> holder,
      Supplier<ProfilerFiller> supplier,
      boolean bl,
      boolean bl2,
      long l,
      int i
   ) {
      super(writableLevelData, resourceKey, registryAccess, holder, supplier, bl, bl2, l, i);
   }

   @Unique
   private SubLevelContainer sable$createPlotContainer() {
      if (SablePlatform.INSTANCE.isWrappedLevel(this)) {
         return null;
      } else {
         SubLevelContainer container;
         if (!this.isClientSide) {
            container = new ServerSubLevelContainer(this, SubLevelContainer.DEFAULT_LOG_SIZE_LENGTH, SubLevelContainer.DEFAULT_LOG_PLOT_SIZE, 10000, 10000);
         } else {
            container = new ClientSubLevelContainer(this, SubLevelContainer.DEFAULT_LOG_SIZE_LENGTH, SubLevelContainer.DEFAULT_LOG_PLOT_SIZE, 10000, 10000);
         }

         Sable.defaultSubLevelContainerInitializer(this, container);
         SableEventPublishPlatform.INSTANCE.onSubLevelContainerReady(this, container);
         return container;
      }
   }

   @Override
   public SubLevelContainer sable$getPlotContainer() {
      return this.sable$plotContainer;
   }
}

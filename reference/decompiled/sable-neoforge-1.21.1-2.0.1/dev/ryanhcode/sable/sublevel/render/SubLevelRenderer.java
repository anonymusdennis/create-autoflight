package dev.ryanhcode.sable.sublevel.render;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.dispatcher.ReachAroundSubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import dev.ryanhcode.sable.sublevel.render.dispatcher.VanillaSubLevelRenderDispatcher;
import foundry.veil.api.compat.SodiumCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class SubLevelRenderer {
   public static final SubLevelRenderer.SelectedRenderer DEFAULT;
   private static SubLevelRenderDispatcher dispatcher;
   private static SubLevelRenderer.SelectedRenderer selected;

   public static void setImpl(SubLevelRenderer.SelectedRenderer impl) {
      SubLevelRenderer.SelectedRenderer newImpl = !impl.isSupported() ? DEFAULT : impl;
      if (!selected.equals(newImpl)) {
         selected = newImpl;
         if (dispatcher != null) {
            dispatcher.free();
            dispatcher = null;
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
               for (ClientSubLevel sublevel : ((ClientSubLevelContainer)((SubLevelContainerHolder)level).sable$getPlotContainer()).getAllSubLevels()) {
                  sublevel.updateRenderData();
               }
            }
         }
      }
   }

   public static void free() {
      if (dispatcher != null) {
         dispatcher.free();
         dispatcher = null;
      }
   }

   public static SubLevelRenderDispatcher getDispatcher() {
      if (dispatcher == null) {
         dispatcher = selected.create();
      }

      return dispatcher;
   }

   static {
      SubLevelRenderer.SelectedRenderer impl = null;

      for (SubLevelRenderer.SelectedRenderer render : SubLevelRenderer.SelectedRenderer.values()) {
         if (render.isSupported()) {
            impl = render;
            break;
         }
      }

      if (impl == null) {
         throw new RuntimeException("Failed to find a supported sub-level renderer");
      } else {
         DEFAULT = impl;
         selected = DEFAULT;
      }
   }

   public static enum SelectedRenderer {
      VANILLA {
         @Override
         public boolean isSupported() {
            return !SodiumCompat.isLoaded();
         }

         @Override
         public SubLevelRenderDispatcher create() {
            return new VanillaSubLevelRenderDispatcher();
         }
      },
      SODIUM_REACHAROUND {
         @Override
         public boolean isSupported() {
            return SodiumCompat.isLoaded();
         }

         @Override
         public SubLevelRenderDispatcher create() {
            return new ReachAroundSubLevelRenderDispatcher();
         }
      };

      public abstract boolean isSupported();

      public abstract SubLevelRenderDispatcher create();
   }
}

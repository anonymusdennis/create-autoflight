package net.createmod.ponder.foundation.ui;

import net.createmod.catnip.gui.NavigatableSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.ponder.Ponder;
import net.minecraft.network.chat.Component;

public abstract class AbstractPonderScreen extends NavigatableSimiScreen {
   public static final String INDEX_TITLE = "ui.index_title";
   public static final String WELCOME = "ui.welcome";
   public static final String CATEGORIES = "ui.categories";
   public static final String DESCRIPTION = "ui.index_description";
   public static final String PONDERING = "ui.pondering";
   public static final String PONDERING_TAG = "ui.pondering_tag";
   public static final String IDENTIFY_MODE = "ui.identify_mode";
   public static final String IN_CHAPTER = "ui.in_chapter";
   public static final String IDENTIFY = "ui.identify";
   public static final String PREVIOUS = "ui.previous";
   public static final String CLOSE = "ui.close";
   public static final String NEXT = "ui.next";
   public static final String NEXT_UP = "ui.next_up";
   public static final String REPLAY = "ui.replay";
   public static final String SLOW_TEXT = "ui.slow_text";
   public static final String THINK_BACK = "ui.think_back";
   public static final String EXIT = "ui.exit";
   public static final String ASSOCIATED = "ui.associated";

   @Override
   protected void init() {
      super.init();
      if (this.backTrack != null) {
         this.backTrack.withCustomTheme(PonderButton.COLOR_IDLE, PonderButton.COLOR_HOVER, PonderButton.COLOR_CLICK, PonderButton.COLOR_DISABLED);
      }
   }

   @Override
   protected Component backTrackingComponent() {
      return ScreenOpener.getBackStepScreen() instanceof NavigatableSimiScreen
         ? Ponder.lang().translate("ui.think_back").component()
         : Ponder.lang().translate("ui.exit").component();
   }
}

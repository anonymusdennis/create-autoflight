package dev.ryanhcode.sable.neoforge.gametest;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;

public class TestProgressBar {
   private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Test Progress"), BossBarColor.RED, BossBarOverlay.PROGRESS);
   private final PlayerList playerList;
   private long maxItems;

   public TestProgressBar(PlayerList playerList) {
      this.playerList = playerList;
   }

   private void updateVisible() {
      for (ServerPlayer player : this.playerList.getPlayers()) {
         this.bossEvent.addPlayer(player);
      }
   }

   public void begin(long maxItems) {
      this.maxItems = maxItems;
      this.bossEvent.setName(Component.literal("Test Progress: 0 / " + maxItems));
      this.updateVisible();
   }

   public void update(long items) {
      this.updateVisible();
      this.bossEvent.setName(Component.literal("Test Progress: " + items + " / " + this.maxItems));
      this.bossEvent.setProgress((float)items / (float)this.maxItems);
   }

   public void end() {
      this.bossEvent.removeAllPlayers();
   }
}

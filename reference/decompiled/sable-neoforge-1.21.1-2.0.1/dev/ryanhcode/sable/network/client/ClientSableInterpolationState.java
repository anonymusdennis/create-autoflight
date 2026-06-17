package dev.ryanhcode.sable.network.client;

import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.network.packets.PacketReceiveMode;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ClientSableInterpolationState {
   public static final boolean RENDER_INTERPOLATION_BOUNDS = false;
   private final Minecraft minecraft = Minecraft.getInstance();
   private double mostRecentTick = -1.0;
   private boolean receivedFirstUpdate;
   private double interpolationTick;
   private double estimatedServerTickSpeed;
   private float serverMsFromLastUpdate;
   private boolean stopped = true;
   private PacketReceiveMode receivingMode = PacketReceiveMode.UNKNOWN;
   private double latestDelay;
   public double mostRecentInterpolationTick;
   public double lastInterpolationTick;

   public void tick() {
      if (this.receivedFirstUpdate) {
         float rate = this.minecraft.level.tickRateManager().tickrate();
         float expectedMsBetween = 1000.0F / rate;
         if (!this.stopped) {
            this.estimatedServerTickSpeed = Mth.lerp(
               0.05, this.estimatedServerTickSpeed, (double)(expectedMsBetween / Math.max(1.0F, this.serverMsFromLastUpdate))
            );
         }

         this.interpolationTick = this.interpolationTick + this.estimatedServerTickSpeed;
         this.interpolationTick = Mth.clamp(this.interpolationTick, this.mostRecentTick - this.getInterpolationDelay(), this.mostRecentTick + 1.5);
         this.latestDelay = this.mostRecentTick - this.interpolationTick + this.getInterpolationDelay();
         this.lastInterpolationTick = this.mostRecentInterpolationTick;
         this.mostRecentInterpolationTick = this.getTickPointer();
      }
   }

   public double getTickPointer() {
      return this.interpolationTick - this.getInterpolationDelay();
   }

   public void receiveSnapshot(ClientSubLevel clientSubLevel, int gameTick, Pose3dc data, PacketReceiveMode packetReceiveMode) {
      this.receivingMode = packetReceiveMode;
      clientSubLevel.getInterpolator().receiveSnapshot(gameTick, data);
   }

   @Internal
   public void addDebugInfo(Consumer<String> consumer) {
      consumer.accept(String.format("Delay: %.2ft", this.latestDelay));
      consumer.accept(String.format("Estimated Send-rate: %.2ft", this.estimatedServerTickSpeed));
      if (this.interpolationTick - this.getInterpolationDelay() > this.mostRecentTick) {
         consumer.accept(ChatFormatting.RED + "Past most-recent tick");
      }

      consumer.accept("Interpolation " + (this.stopped ? "stopped" : "running"));
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.getSingleplayerServer() != null) {
         consumer.accept("Networking locally");
      } else {
         consumer.accept("Networking through " + this.receivingMode.name());
      }
   }

   public double getInterpolationDelay() {
      return SableClientConfig.INTERPOLATION_DELAY.getAsDouble();
   }

   public void receiveInfo(int msSinceLast, int gameTick, boolean stopped) {
      if (!((double)gameTick < this.mostRecentTick)) {
         if (!this.receivedFirstUpdate || this.stopped && !stopped) {
            this.interpolationTick = (double)gameTick;
            this.estimatedServerTickSpeed = 1.0;
            this.receivedFirstUpdate = true;
         }

         this.stopped = stopped;
         this.mostRecentTick = (double)gameTick;
         this.serverMsFromLastUpdate = (float)msSinceLast;
      }
   }

   public boolean isStopped() {
      return this.stopped;
   }
}

package com.simibubi.create.content.trains.entity;

import com.google.common.collect.Streams;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class TrainStatus {
   Train train;
   public boolean navigation;
   public boolean track;
   public boolean conductor;
   List<TrainStatus.StatusMessage> queued = new ArrayList<>();

   public TrainStatus(Train train) {
      this.train = train;
   }

   public void failedNavigation() {
      if (!this.navigation) {
         this.displayInformation("no_path", false);
         this.navigation = true;
      }
   }

   public void failedNavigationNoTarget(String filter) {
      if (!this.navigation) {
         this.displayInformation("no_match", false, filter);
         this.navigation = true;
      }
   }

   public void failedPackageNoTarget(String address) {
      if (!this.navigation) {
         this.displayInformation("no_package_target", false, address);
         this.navigation = true;
      }
   }

   public void successfulNavigation() {
      if (this.navigation) {
         this.displayInformation("navigation_success", true);
         this.navigation = false;
      }
   }

   public void foundConductor() {
      if (this.conductor) {
         this.displayInformation("found_driver", true);
         this.conductor = false;
      }
   }

   public void missingConductor() {
      if (!this.conductor) {
         this.displayInformation("missing_driver", false);
         this.conductor = true;
      }
   }

   public void missingCorrectConductor() {
      if (!this.conductor) {
         this.displayInformation("opposite_driver", false);
         this.conductor = true;
      }
   }

   public void manualControls() {
      this.displayInformation("paused_for_manual", true);
   }

   public void failedMigration() {
      if (!this.track) {
         this.displayInformation("track_missing", false);
         this.track = true;
      }
   }

   public void highStress() {
      if (!this.track) {
         this.displayInformation("coupling_stress", false);
         this.track = true;
      }
   }

   public void doublePortal() {
      if (!this.track) {
         this.displayInformation("double_portal", false);
         this.track = true;
      }
   }

   public void endOfTrack() {
      if (!this.track) {
         this.displayInformation("end_of_track", false);
         this.track = true;
      }
   }

   public void crash() {
      Component component = Component.literal(" - ")
         .withStyle(ChatFormatting.GRAY)
         .append(CreateLang.translateDirect("train.status.collision").withStyle(st -> st.withColor(16765876)));
      List<ResourceKey<Level>> presentDimensions = this.train.getPresentDimensions();
      Stream<Component> locationComponents = presentDimensions.stream()
         .map(
            key -> Component.literal(" - ")
                  .withStyle(ChatFormatting.GRAY)
                  .append(
                     CreateLang.translateDirect(
                           "train.status.collision.where",
                           key.location().toString(),
                           this.train.getPositionInDimension((ResourceKey<Level>)key).get().toShortString()
                        )
                        .withStyle(style -> style.withColor(16765876))
                  )
         );
      this.addMessage(new TrainStatus.StatusMessage(Streams.concat(new Stream[]{Stream.of(component), locationComponents}).toArray(Component[]::new)));
   }

   public void successfulMigration() {
      if (this.track) {
         this.displayInformation("back_on_track", true);
         this.track = false;
      }
   }

   public void trackOK() {
      this.track = false;
   }

   public void tick(Level level) {
      if (!this.queued.isEmpty()) {
         LivingEntity owner = this.train.getOwner(level);
         if (owner != null) {
            if (owner instanceof Player player) {
               player.displayClientMessage(CreateLang.translateDirect("train.status", this.train.name).withStyle(ChatFormatting.GOLD), false);
               this.queued.forEach(message -> message.displayToPlayer(player));
            }

            this.queued.clear();
         }
      }
   }

   public void displayInformation(String key, boolean itsAGoodThing, Object... args) {
      MutableComponent component = Component.literal(" - ")
         .withStyle(ChatFormatting.GRAY)
         .append(CreateLang.translateDirect("train.status." + key, args).withStyle(st -> st.withColor(itsAGoodThing ? 14019778 : 16765876)));
      this.addMessage(new TrainStatus.StatusMessage(component));
   }

   public void addMessage(TrainStatus.StatusMessage message) {
      this.queued.add(message);
      if (this.queued.size() > 3) {
         this.queued.remove(0);
      }
   }

   public void newSchedule() {
      this.navigation = false;
      this.conductor = false;
   }

   public static record StatusMessage(Component... messages) {
      public void displayToPlayer(Player player) {
         Arrays.stream(this.messages).forEach(messages -> player.displayClientMessage(messages, false));
      }
   }
}

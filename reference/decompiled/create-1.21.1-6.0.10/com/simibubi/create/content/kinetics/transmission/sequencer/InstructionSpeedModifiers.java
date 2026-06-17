package com.simibubi.create.content.kinetics.transmission.sequencer;

import com.simibubi.create.foundation.utility.CreateLang;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;

public enum InstructionSpeedModifiers {
   FORWARD_FAST(2, ">>"),
   FORWARD(1, "->"),
   BACK(-1, "<-"),
   BACK_FAST(-2, "<<");

   public static final StreamCodec<ByteBuf, InstructionSpeedModifiers> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(InstructionSpeedModifiers.class);
   String translationKey;
   int value;
   Component label;

   private InstructionSpeedModifiers(int modifier, Component label) {
      this.label = label;
      this.translationKey = "gui.sequenced_gearshift.speed." + Lang.asId(this.name());
      this.value = modifier;
   }

   private InstructionSpeedModifiers(int modifier, String label) {
      this.label = Component.literal(label);
      this.translationKey = "gui.sequenced_gearshift.speed." + Lang.asId(this.name());
      this.value = modifier;
   }

   static List<Component> getOptions() {
      List<Component> options = new ArrayList<>();

      for (InstructionSpeedModifiers entry : values()) {
         options.add(CreateLang.translateDirect(entry.translationKey));
      }

      return options;
   }

   public static InstructionSpeedModifiers getByModifier(int modifier) {
      return Arrays.stream(values()).filter(speedModifier -> speedModifier.value == modifier).findAny().orElse(FORWARD);
   }
}

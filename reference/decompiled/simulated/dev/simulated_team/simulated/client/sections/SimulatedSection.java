package dev.simulated_team.simulated.client.sections;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.simulated_team.simulated.Simulated;
import foundry.veil.api.client.color.Color;
import foundry.veil.api.client.color.Colorc;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import org.jetbrains.annotations.NotNull;

public record SimulatedSection(int priority, SimulatedSection.Title title, ResourceLocation sprite, boolean animateOnHover)
   implements Comparable<SimulatedSection> {
   private static final ResourceLocation DEFAULT_BANNER = Simulated.path("default_banner");
   public static final Codec<SimulatedSection> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               ExtraCodecs.POSITIVE_INT.fieldOf("priority").orElse(0).forGetter(SimulatedSection::priority),
               SimulatedSection.Title.CODEC.fieldOf("title").forGetter(SimulatedSection::title),
               ResourceLocation.CODEC.fieldOf("sprite").orElse(DEFAULT_BANNER).forGetter(SimulatedSection::sprite),
               Codec.BOOL.fieldOf("only_animate_on_hover").orElse(false).forGetter(SimulatedSection::animateOnHover)
            )
            .apply(instance, SimulatedSection::new)
   );

   public int compareTo(@NotNull SimulatedSection other) {
      return (int)Math.signum((float)(this.priority() - other.priority()));
   }

   public static record Title(Component text, Colorc color, Optional<Colorc> secondaryColor, Colorc background) {
      public static final Codec<Colorc> COLOR_CODEC = Color.ARGB_INT_CODEC.xmap(i -> new Color(i, true), Colorc::argb);
      public static final Codec<SimulatedSection.Title> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  ComponentSerialization.CODEC.fieldOf("text").forGetter(SimulatedSection.Title::text),
                  COLOR_CODEC.fieldOf("color").orElse(new Color(-1, true)).forGetter(SimulatedSection.Title::color),
                  COLOR_CODEC.optionalFieldOf("secondary_color").forGetter(SimulatedSection.Title::secondaryColor),
                  COLOR_CODEC.fieldOf("background").orElse(new Color(-1442840576, true)).forGetter(SimulatedSection.Title::background)
               )
               .apply(instance, SimulatedSection.Title::new)
      );
   }
}

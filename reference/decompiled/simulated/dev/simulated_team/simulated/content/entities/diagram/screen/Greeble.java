package dev.simulated_team.simulated.content.entities.diagram.screen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public record Greeble(ResourceLocation texture, List<Greeble.TextureSlice> slices, int width, int height, float weight) {
   public static final Codec<Greeble> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               ResourceLocation.CODEC.fieldOf("texture").forGetter(g -> g.texture),
               Greeble.TextureSlice.CODEC.listOf().fieldOf("slices").forGetter(g -> g.slices),
               Codec.INT.fieldOf("width").forGetter(g -> g.width),
               Codec.INT.fieldOf("height").forGetter(g -> g.height),
               Codec.FLOAT.optionalFieldOf("weight", 100.0F).forGetter(g -> g.weight)
            )
            .apply(instance, Greeble::new)
   );

   public Greeble.TextureSlice random(RandomSource random) {
      return this.slices.get(random.nextInt(this.slices.size()));
   }

   public ArrayList<Greeble.TextureSlice> shuffled() {
      ArrayList<Greeble.TextureSlice> list = new ArrayList<>(this.slices());
      Collections.shuffle(list);
      return list;
   }

   public static record TextureSlice(int x, int y, int width, int height) {
      public static Codec<Greeble.TextureSlice> CODEC = Codec.INT.listOf(4, 4).xmap(Greeble.TextureSlice::new, Greeble.TextureSlice::asList);

      public TextureSlice(List<Integer> list) {
         this(list.get(0), list.get(1), list.get(2), list.get(3));
      }

      public List<Integer> asList() {
         return List.of(this.x(), this.y(), this.width(), this.height());
      }
   }
}

package com.simibubi.create.content.equipment.clipboard;

import com.mojang.serialization.Codec;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.Create;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import io.netty.buffer.ByteBuf;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.lang.Lang;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile.UncheckedModelFile;
import org.jetbrains.annotations.NotNull;

public class ClipboardOverrides {
   @OnlyIn(Dist.CLIENT)
   public static void registerModelOverridesClient(ClipboardBlockItem item) {
      ItemProperties.register(
         item,
         ClipboardOverrides.ClipboardType.ID,
         (pStack, pLevel, pEntity, pSeed) -> (float)((ClipboardContent)pStack.getOrDefault(AllDataComponents.CLIPBOARD_CONTENT, ClipboardContent.EMPTY))
               .type()
               .ordinal()
      );
   }

   public static ItemModelBuilder addOverrideModels(DataGenContext<Item, ClipboardBlockItem> c, RegistrateItemModelProvider p) {
      ItemModelBuilder builder = p.generated(c::get);

      for (ClipboardOverrides.ClipboardType type : ClipboardOverrides.ClipboardType.values()) {
         int i = type.ordinal();
         builder.override()
            .predicate(ClipboardOverrides.ClipboardType.ID, (float)i)
            .model(
               ((ItemModelBuilder)((ItemModelBuilder)p.getBuilder(c.getName() + "_" + i)).parent(new UncheckedModelFile("item/generated")))
                  .texture("layer0", Create.asResource("item/" + type.file))
            )
            .end();
      }

      return builder;
   }

   public static enum ClipboardType implements StringRepresentable {
      EMPTY("empty_clipboard"),
      WRITTEN("clipboard"),
      EDITING("clipboard_and_quill");

      public static final Codec<ClipboardOverrides.ClipboardType> CODEC = StringRepresentable.fromValues(ClipboardOverrides.ClipboardType::values);
      public static final StreamCodec<ByteBuf, ClipboardOverrides.ClipboardType> STREAM_CODEC = CatnipStreamCodecBuilders.ofEnum(
         ClipboardOverrides.ClipboardType.class
      );
      public final String file;
      public static ResourceLocation ID = Create.asResource("clipboard_type");

      private ClipboardType(String file) {
         this.file = file;
      }

      @NotNull
      public String getSerializedName() {
         return Lang.asId(this.name());
      }
   }
}

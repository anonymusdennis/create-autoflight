package dev.simulated_team.simulated.content.item_attributes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttribute;
import com.simibubi.create.content.logistics.item.filter.attribute.ItemAttributeType;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes.PhysicsBlockPropertyType;
import dev.simulated_team.simulated.index.SimItemAttributeTypes;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public record BlockBouncinessItemAttribute(double bounciness) implements ItemAttribute {
   public static final MapCodec<BlockBouncinessItemAttribute> CODEC = Codec.DOUBLE
      .xmap(BlockBouncinessItemAttribute::new, BlockBouncinessItemAttribute::bounciness)
      .fieldOf("value");
   public static final StreamCodec<ByteBuf, BlockBouncinessItemAttribute> STREAM_CODEC = ByteBufCodecs.DOUBLE
      .map(BlockBouncinessItemAttribute::new, BlockBouncinessItemAttribute::bounciness);

   public boolean appliesTo(ItemStack stack, Level world) {
      if (stack.getItem() instanceof BlockItem item) {
         BlockStateExtension extension = (BlockStateExtension)item.getBlock().defaultBlockState();
         return (Double)extension.sable$getProperty((PhysicsBlockPropertyType)PhysicsBlockPropertyTypes.FRICTION.get()) == this.bounciness();
      } else {
         return false;
      }
   }

   public ItemAttributeType getType() {
      return (ItemAttributeType)SimItemAttributeTypes.BLOCK_BOUNCINESS.get();
   }

   public String getTranslationKey() {
      return "block_bounciness";
   }

   public Object[] getTranslationParameters() {
      return new Object[]{this.bounciness()};
   }

   public static class Type implements ItemAttributeType {
      @NotNull
      public ItemAttribute createAttribute() {
         return new BlockBouncinessItemAttribute(1.0);
      }

      public List<ItemAttribute> getAllAttributes(ItemStack stack, Level level) {
         if (stack.getItem() instanceof BlockItem item) {
            BlockStateExtension extension = (BlockStateExtension)item.getBlock().defaultBlockState();
            double mass = (Double)extension.sable$getProperty((PhysicsBlockPropertyType)PhysicsBlockPropertyTypes.RESTITUTION.get());
            return List.of(new BlockBouncinessItemAttribute(mass));
         } else {
            return List.of();
         }
      }

      public MapCodec<? extends ItemAttribute> codec() {
         return BlockBouncinessItemAttribute.CODEC;
      }

      public StreamCodec<? super RegistryFriendlyByteBuf, ? extends ItemAttribute> streamCodec() {
         return BlockBouncinessItemAttribute.STREAM_CODEC;
      }
   }
}

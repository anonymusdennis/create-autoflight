package com.simibubi.create.content.equipment.armor;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllEnchantments;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.DistExecutor;
import com.simibubi.create.infrastructure.config.AllConfigs;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.api.distmarker.Dist;

public class BacktankUtil {
   private static final List<Function<LivingEntity, List<ItemStack>>> BACKTANK_SUPPLIERS = new ArrayList<>();

   public static List<ItemStack> getAllWithAir(LivingEntity entity) {
      List<ItemStack> all = new ArrayList<>();

      for (Function<LivingEntity, List<ItemStack>> supplier : BACKTANK_SUPPLIERS) {
         for (ItemStack stack : supplier.apply(entity)) {
            if (hasAirRemaining(stack)) {
               all.add(stack);
            }
         }
      }

      all.sort((a, b) -> Float.compare((float)getAir(a), (float)getAir(b)));
      return all;
   }

   public static boolean hasAirRemaining(ItemStack backtank) {
      return getAir(backtank) > 0;
   }

   public static int getAir(ItemStack backtank) {
      return Math.min((Integer)backtank.getOrDefault(AllDataComponents.BACKTANK_AIR, 0), maxAir(backtank));
   }

   public static void consumeAir(LivingEntity entity, ItemStack backtank, int i) {
      int maxAir = maxAir(backtank);
      int air = getAir(backtank);
      int newAir = Math.max(air - i, 0);
      backtank.set(AllDataComponents.BACKTANK_AIR, Math.min(newAir, maxAir));
      if (entity instanceof ServerPlayer player) {
         sendWarning(player, (float)air, (float)newAir, (float)maxAir / 10.0F);
         sendWarning(player, (float)air, (float)newAir, 1.0F);
      }
   }

   private static void sendWarning(ServerPlayer player, float air, float newAir, float threshold) {
      if (!(newAir > threshold)) {
         if (!(air <= threshold)) {
            boolean depleted = threshold == 1.0F;
            MutableComponent component = CreateLang.translateDirect(depleted ? "backtank.depleted" : "backtank.low");
            AllSoundEvents.DENY.play(player.level(), null, player.blockPosition(), 1.0F, 1.25F);
            AllSoundEvents.STEAM.play(player.level(), null, player.blockPosition(), 0.5F, 0.5F);
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 40, 10));
            player.connection
               .send(
                  new ClientboundSetSubtitleTextPacket(
                     Component.literal("⚠ ").withStyle(depleted ? ChatFormatting.RED : ChatFormatting.GOLD).append(component.withStyle(ChatFormatting.GRAY))
                  )
               );
            player.connection.send(new ClientboundSetTitleTextPacket(CommonComponents.EMPTY));
         }
      }
   }

   public static int maxAir(ItemStack backtank) {
      int enchantLevel = 0;
      ItemEnchantments enchants = backtank.getTagEnchantments();

      for (Entry<Holder<Enchantment>> entry : enchants.entrySet()) {
         if (((Holder)entry.getKey()).is(AllEnchantments.CAPACITY)) {
            enchantLevel = entry.getIntValue();
            break;
         }
      }

      return maxAir(enchantLevel);
   }

   public static int maxAir(int enchantLevel) {
      return (Integer)AllConfigs.server().equipment.airInBacktank.get() + (Integer)AllConfigs.server().equipment.enchantedBacktankCapacity.get() * enchantLevel;
   }

   public static int maxAirWithoutEnchants() {
      return (Integer)AllConfigs.server().equipment.airInBacktank.get();
   }

   public static boolean canAbsorbDamage(LivingEntity entity, int usesPerTank) {
      if (usesPerTank == 0) {
         return true;
      } else if (entity instanceof Player && ((Player)entity).isCreative()) {
         return true;
      } else {
         List<ItemStack> backtanks = getAllWithAir(entity);
         if (backtanks.isEmpty()) {
            return false;
         } else {
            int cost = Math.max(maxAirWithoutEnchants() / usesPerTank, 1);
            consumeAir(entity, backtanks.getFirst(), cost);
            return true;
         }
      }
   }

   public static boolean isBarVisible(ItemStack stack, int usesPerTank) {
      if (usesPerTank == 0) {
         return false;
      } else {
         Player player = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
         if (player == null) {
            return false;
         } else {
            List<ItemStack> backtanks = getAllWithAir(player);
            return backtanks.isEmpty() ? stack.isDamaged() : true;
         }
      }
   }

   public static int getBarWidth(ItemStack stack, int usesPerTank) {
      if (usesPerTank == 0) {
         return 13;
      } else {
         Player player = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
         if (player == null) {
            return 13;
         } else {
            List<ItemStack> backtanks = getAllWithAir(player);
            if (backtanks.isEmpty()) {
               return Math.round(13.0F - (float)stack.getDamageValue() / (float)stack.getMaxDamage() * 13.0F);
            } else if (backtanks.size() == 1) {
               return backtanks.getFirst().getItem().getBarWidth(backtanks.getFirst());
            } else {
               int sumBarWidth = backtanks.stream().map(backtank -> backtank.getItem().getBarWidth(backtank)).reduce(0, Integer::sum);
               return Math.round((float)sumBarWidth / (float)backtanks.size());
            }
         }
      }
   }

   public static int getBarColor(ItemStack stack, int usesPerTank) {
      if (usesPerTank == 0) {
         return 0;
      } else {
         Player player = DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().player);
         if (player == null) {
            return 0;
         } else {
            List<ItemStack> backtanks = getAllWithAir(player);
            return backtanks.isEmpty()
               ? Mth.hsvToRgb(Math.max(0.0F, 1.0F - (float)stack.getDamageValue() / (float)stack.getMaxDamage()) / 3.0F, 1.0F, 1.0F)
               : backtanks.get(0).getItem().getBarColor(backtanks.get(0));
         }
      }
   }

   public static void addBacktankSupplier(Function<LivingEntity, List<ItemStack>> supplier) {
      BACKTANK_SUPPLIERS.add(supplier);
   }

   static {
      addBacktankSupplier(entity -> {
         List<ItemStack> stacks = new ArrayList<>();

         for (ItemStack itemStack : entity.getArmorSlots()) {
            if (AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.matches(itemStack)) {
               stacks.add(itemStack);
            }
         }

         return stacks;
      });
   }
}

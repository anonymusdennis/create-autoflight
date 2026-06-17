package com.simibubi.create.content.fluids.potion;

import com.google.common.collect.Lists;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags;
import com.simibubi.create.foundation.fluid.FluidHelper;
import java.util.List;
import java.util.function.Consumer;
import net.createmod.catnip.data.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.DataComponentFluidIngredient;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

public class PotionFluidHandler {
   private static final Component NO_EFFECT = Component.translatable("effect.none").withStyle(ChatFormatting.GRAY);

   public static boolean isPotionItem(ItemStack stack) {
      return stack.getItem() instanceof PotionItem
         && !(stack.getCraftingRemainingItem().getItem() instanceof BucketItem)
         && !AllTags.AllItemTags.NOT_POTION.matches(stack);
   }

   public static Pair<FluidStack, ItemStack> emptyPotion(ItemStack stack, boolean simulate) {
      FluidStack fluid = getFluidFromPotionItem(stack);
      if (!simulate) {
         stack.shrink(1);
      }

      return Pair.of(fluid, new ItemStack(Items.GLASS_BOTTLE));
   }

   public static SizedFluidIngredient potionIngredient(Holder<Potion> potion, int amount) {
      FluidStack stack = FluidHelper.copyStackWithAmount(getFluidFromPotionItem(PotionContents.createItemStack(Items.POTION, potion)), amount);
      return new SizedFluidIngredient(DataComponentFluidIngredient.of(false, stack), amount);
   }

   public static FluidStack getFluidFromPotionItem(ItemStack stack) {
      PotionContents potion = (PotionContents)stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      PotionFluid.BottleType bottleTypeFromItem = bottleTypeFromItem(stack.getItem());
      if (potion.is(Potions.WATER) && potion.customEffects().isEmpty() && bottleTypeFromItem == PotionFluid.BottleType.REGULAR) {
         return new FluidStack(Fluids.WATER, 250);
      } else {
         FluidStack fluid = getFluidFromPotion(potion, bottleTypeFromItem, 250);
         fluid.set(AllDataComponents.POTION_FLUID_BOTTLE_TYPE, bottleTypeFromItem);
         return fluid;
      }
   }

   public static FluidStack getFluidFromPotion(PotionContents potionContents, PotionFluid.BottleType bottleType, int amount) {
      return potionContents.is(Potions.WATER) && bottleType == PotionFluid.BottleType.REGULAR
         ? new FluidStack(Fluids.WATER, amount)
         : PotionFluid.of(amount, potionContents, bottleType);
   }

   public static PotionFluid.BottleType bottleTypeFromItem(Item item) {
      if (item == Items.LINGERING_POTION) {
         return PotionFluid.BottleType.LINGERING;
      } else {
         return item == Items.SPLASH_POTION ? PotionFluid.BottleType.SPLASH : PotionFluid.BottleType.REGULAR;
      }
   }

   public static ItemLike itemFromBottleType(PotionFluid.BottleType type) {
      return switch (type) {
         case LINGERING -> Items.LINGERING_POTION;
         case SPLASH -> Items.SPLASH_POTION;
         default -> Items.POTION;
      };
   }

   public static int getRequiredAmountForFilledBottle(ItemStack stack, FluidStack availableFluid) {
      return 250;
   }

   public static ItemStack fillBottle(ItemStack stack, FluidStack availableFluid) {
      ItemStack potionStack = new ItemStack(
         itemFromBottleType((PotionFluid.BottleType)availableFluid.getOrDefault(AllDataComponents.POTION_FLUID_BOTTLE_TYPE, PotionFluid.BottleType.REGULAR))
      );
      potionStack.set(DataComponents.POTION_CONTENTS, (PotionContents)availableFluid.get(DataComponents.POTION_CONTENTS));
      return potionStack;
   }

   @OnlyIn(Dist.CLIENT)
   public static void addPotionTooltip(FluidStack fs, Consumer<Component> tooltipAdder, float durationFactor) {
      PotionContents contents = (PotionContents)fs.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
      Iterable<MobEffectInstance> effects = contents.getAllEffects();
      List<Pair<Holder<Attribute>, AttributeModifier>> list = Lists.newArrayList();
      boolean flag = true;

      for (MobEffectInstance mobeffectinstance : effects) {
         flag = false;
         MutableComponent mutablecomponent = Component.translatable(mobeffectinstance.getDescriptionId());
         Holder<MobEffect> holder = mobeffectinstance.getEffect();
         ((MobEffect)holder.value()).createModifiers(mobeffectinstance.getAmplifier(), (h, m) -> list.add(Pair.of(h, m)));
         if (mobeffectinstance.getAmplifier() > 0) {
            mutablecomponent.append(" ").append(Component.translatable("potion.potency." + mobeffectinstance.getAmplifier()).getString());
         }

         if (!mobeffectinstance.endsWithin(20)) {
            mutablecomponent.append(" (")
               .append(MobEffectUtil.formatDuration(mobeffectinstance, durationFactor, Minecraft.getInstance().level.tickRateManager().tickrate()))
               .append(")");
         }

         tooltipAdder.accept(mutablecomponent.withStyle(((MobEffect)holder.value()).getCategory().getTooltipFormatting()));
      }

      if (flag) {
         tooltipAdder.accept(NO_EFFECT);
      }

      if (!list.isEmpty()) {
         tooltipAdder.accept(CommonComponents.EMPTY);
         tooltipAdder.accept(Component.translatable("potion.whenDrank").withStyle(ChatFormatting.DARK_PURPLE));

         for (Pair<Holder<Attribute>, AttributeModifier> pair : list) {
            AttributeModifier attributemodifier = (AttributeModifier)pair.getSecond();
            double d1 = attributemodifier.amount();
            double d0;
            if (attributemodifier.operation() != Operation.ADD_MULTIPLIED_BASE && attributemodifier.operation() != Operation.ADD_MULTIPLIED_TOTAL) {
               d0 = attributemodifier.amount();
            } else {
               d0 = attributemodifier.amount() * 100.0;
            }

            if (d1 > 0.0) {
               tooltipAdder.accept(
                  Component.translatable(
                        "attribute.modifier.plus." + attributemodifier.operation().id(),
                        new Object[]{
                           ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d0),
                           Component.translatable(((Attribute)((Holder)pair.getFirst()).value()).getDescriptionId())
                        }
                     )
                     .withStyle(ChatFormatting.BLUE)
               );
            } else if (d1 < 0.0) {
               d0 *= -1.0;
               tooltipAdder.accept(
                  Component.translatable(
                        "attribute.modifier.take." + attributemodifier.operation().id(),
                        new Object[]{
                           ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(d0),
                           Component.translatable(((Attribute)((Holder)pair.getFirst()).value()).getDescriptionId())
                        }
                     )
                     .withStyle(ChatFormatting.RED)
               );
            }
         }
      }
   }
}

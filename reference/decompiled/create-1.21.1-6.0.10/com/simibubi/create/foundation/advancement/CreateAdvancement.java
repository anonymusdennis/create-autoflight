package com.simibubi.create.foundation.advancement;

import com.simibubi.create.Create;
import com.tterrag.registrate.util.entry.ItemProviderEntry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger.TriggerInstance;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

public class CreateAdvancement {
   static final ResourceLocation BACKGROUND = Create.asResource("textures/gui/advancements.png");
   static final String LANG = "advancement.create.";
   static final String SECRET_SUFFIX = "\n§7(Hidden Advancement)";
   private final net.minecraft.advancements.Advancement.Builder mcBuilder = net.minecraft.advancements.Advancement.Builder.advancement();
   private SimpleCreateTrigger builtinTrigger;
   private CreateAdvancement parent;
   private final CreateAdvancement.Builder createBuilder = new CreateAdvancement.Builder();
   AdvancementHolder datagenResult;
   private String id;
   private String title;
   private String description;

   public CreateAdvancement(String id, UnaryOperator<CreateAdvancement.Builder> b) {
      this.id = id;
      b.apply(this.createBuilder);
      if (!this.createBuilder.externalTrigger) {
         this.builtinTrigger = AllTriggers.addSimple(id + "_builtin");
         this.mcBuilder.addCriterion("0", this.builtinTrigger.createCriterion(this.builtinTrigger.instance()));
      }

      if (this.createBuilder.type == CreateAdvancement.TaskType.SECRET) {
         this.description = this.description + "\n§7(Hidden Advancement)";
      }

      AllAdvancements.ENTRIES.add(this);
   }

   private String titleKey() {
      return "advancement.create." + this.id;
   }

   private String descriptionKey() {
      return this.titleKey() + ".desc";
   }

   public boolean isAlreadyAwardedTo(Player player) {
      if (player instanceof ServerPlayer sp) {
         AdvancementHolder advancement = sp.getServer().getAdvancements().get(Create.asResource(this.id));
         return advancement == null ? true : sp.getAdvancements().getOrStartProgress(advancement).isDone();
      } else {
         return true;
      }
   }

   public void awardTo(Player player) {
      if (player instanceof ServerPlayer sp) {
         if (this.builtinTrigger == null) {
            throw new UnsupportedOperationException("Advancement " + this.id + " uses external Triggers, it cannot be awarded directly");
         } else {
            this.builtinTrigger.trigger(sp);
         }
      }
   }

   void save(Consumer<AdvancementHolder> t, Provider registries) {
      if (this.parent != null) {
         this.mcBuilder.parent(this.parent.datagenResult);
      }

      if (this.createBuilder.func != null) {
         this.createBuilder.icon(this.createBuilder.func.apply(registries));
      }

      this.mcBuilder
         .display(
            this.createBuilder.icon,
            Component.translatable(this.titleKey()),
            Component.translatable(this.descriptionKey()).withStyle(s -> s.withColor(14393875)),
            this.id.equals("root") ? BACKGROUND : null,
            this.createBuilder.type.advancementType,
            this.createBuilder.type.toast,
            this.createBuilder.type.announce,
            this.createBuilder.type.hide
         );
      this.datagenResult = this.mcBuilder.save(t, Create.asResource(this.id).toString());
   }

   void provideLang(BiConsumer<String, String> consumer) {
      consumer.accept(this.titleKey(), this.title);
      consumer.accept(this.descriptionKey(), this.description);
   }

   class Builder {
      private CreateAdvancement.TaskType type = CreateAdvancement.TaskType.NORMAL;
      private boolean externalTrigger;
      private int keyIndex;
      private ItemStack icon;
      private Function<Provider, ItemStack> func;

      CreateAdvancement.Builder special(CreateAdvancement.TaskType type) {
         this.type = type;
         return this;
      }

      CreateAdvancement.Builder after(CreateAdvancement other) {
         CreateAdvancement.this.parent = other;
         return this;
      }

      CreateAdvancement.Builder icon(ItemProviderEntry<?, ?> item) {
         return this.icon(item.asStack());
      }

      CreateAdvancement.Builder icon(ItemLike item) {
         return this.icon(new ItemStack(item));
      }

      CreateAdvancement.Builder icon(ItemStack stack) {
         this.icon = stack;
         return this;
      }

      CreateAdvancement.Builder icon(Function<Provider, ItemStack> func) {
         this.func = func;
         return this;
      }

      CreateAdvancement.Builder title(String title) {
         CreateAdvancement.this.title = title;
         return this;
      }

      CreateAdvancement.Builder description(String description) {
         CreateAdvancement.this.description = description;
         return this;
      }

      CreateAdvancement.Builder whenBlockPlaced(Block block) {
         return this.externalTrigger(TriggerInstance.placedBlock(block));
      }

      CreateAdvancement.Builder whenIconCollected() {
         return this.externalTrigger(net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[]{this.icon.getItem()}));
      }

      CreateAdvancement.Builder whenItemCollected(ItemProviderEntry<?, ?> item) {
         return this.whenItemCollected(item.asStack().getItem());
      }

      CreateAdvancement.Builder whenItemCollected(ItemLike itemProvider) {
         return this.externalTrigger(net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[]{itemProvider}));
      }

      CreateAdvancement.Builder whenItemCollected(TagKey<Item> tag) {
         return this.externalTrigger(
            net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(
               new ItemPredicate[]{net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(tag).build()}
            )
         );
      }

      CreateAdvancement.Builder awardedForFree() {
         return this.externalTrigger(net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[0]));
      }

      CreateAdvancement.Builder externalTrigger(Criterion<?> trigger) {
         CreateAdvancement.this.mcBuilder.addCriterion(String.valueOf(this.keyIndex), trigger);
         this.externalTrigger = true;
         this.keyIndex++;
         return this;
      }
   }

   static enum TaskType {
      SILENT(AdvancementType.TASK, false, false, false),
      NORMAL(AdvancementType.TASK, true, false, false),
      NOISY(AdvancementType.TASK, true, true, false),
      EXPERT(AdvancementType.GOAL, true, true, false),
      SECRET(AdvancementType.GOAL, true, true, true);

      private final AdvancementType advancementType;
      private final boolean toast;
      private final boolean announce;
      private final boolean hide;

      private TaskType(AdvancementType advancementType, boolean toast, boolean announce, boolean hide) {
         this.advancementType = advancementType;
         this.toast = toast;
         this.announce = announce;
         this.hide = hide;
      }
   }
}

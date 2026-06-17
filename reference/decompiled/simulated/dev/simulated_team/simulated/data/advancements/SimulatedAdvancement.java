package dev.simulated_team.simulated.data.advancements;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import dev.simulated_team.simulated.util.SimColors;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.ItemUsedOnLocationTrigger.TriggerInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public class SimulatedAdvancement {
   static final String SECRET_SUFFIX = "\n§7(Hidden Advancement)";
   private final net.minecraft.advancements.Advancement.Builder builder = net.minecraft.advancements.Advancement.Builder.advancement();
   private SimpleSimulatedTrigger builtinTrigger;
   private SimulatedAdvancement parent;
   AdvancementHolder datagenResult;
   private final ResourceLocation background;
   private final String lang;
   private final String id;
   private final String modid;
   private String title;
   private String description;

   public SimulatedAdvancement(
      String id,
      UnaryOperator<SimulatedAdvancement.Builder> b,
      ResourceLocation background,
      String modid,
      BiFunction<String, String, SimpleSimulatedTrigger> triggerHandler
   ) {
      this.id = id;
      this.modid = modid;
      this.background = background;
      this.lang = "advancement." + modid + ".";
      SimulatedAdvancement.Builder t = new SimulatedAdvancement.Builder();
      b.apply(t);
      if (!t.externalTrigger) {
         this.builtinTrigger = triggerHandler.apply(modid, id + "_builtin");
         this.builder.addCriterion("0", this.builtinTrigger.createCriterion(this.builtinTrigger.instance()));
      }

      this.builder
         .display(
            t.icon,
            Component.translatable(this.titleKey()),
            Component.translatable(this.descriptionKey()).withStyle(s -> s.withColor(SimColors.ADVANCABLE_GOLD)),
            id.equals("root") ? this.background : null,
            t.type.advancementType,
            t.type.toast,
            t.type.announce,
            t.type.hide
         );
      if (t.type == SimulatedAdvancement.TaskType.SECRET) {
         this.description = this.description + "\n§7(Hidden Advancement)";
      }
   }

   private String titleKey() {
      return this.lang + this.id;
   }

   private String descriptionKey() {
      return this.titleKey() + ".desc";
   }

   public boolean isAlreadyAwardedTo(Player player) {
      if (player instanceof ServerPlayer sp) {
         AdvancementHolder advancement = sp.getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(this.modid, this.id));
         return advancement == null ? true : sp.getAdvancements().getOrStartProgress(advancement).isDone();
      } else {
         return true;
      }
   }

   public void awardTo(Player player) {
      if (!this.isAlreadyAwardedTo(player)) {
         if (player instanceof ServerPlayer sp) {
            if (this.builtinTrigger == null) {
               throw new UnsupportedOperationException("Advancement " + this.id + " uses external Triggers, it cannot be awarded directly");
            } else {
               this.builtinTrigger.trigger(sp);
            }
         }
      }
   }

   public void awardToNearby(BlockPos pos, Level level, int ticks, double radius) {
      if (level.getGameTime() % (long)ticks == 0L) {
         this.awardToNearby(pos, level, radius);
      }
   }

   public void awardToNearby(BlockPos pos, Level level) {
      this.awardToNearby(pos, level, 10.0);
   }

   public void awardToNearby(BlockPos pos, Level level, double radius) {
      AABB aabb = new AABB(pos).inflate(radius);

      for (Player player : level.getEntitiesOfClass(Player.class, aabb)) {
         this.awardTo(player);
      }
   }

   public void save(Consumer<AdvancementHolder> t) {
      if (this.parent != null) {
         this.builder.parent(this.parent.datagenResult);
      }

      this.datagenResult = this.builder.save(t, ResourceLocation.fromNamespaceAndPath(this.modid, this.id).toString());
   }

   public void provideLang(BiConsumer<String, String> consumer) {
      consumer.accept(this.titleKey(), this.title);
      consumer.accept(this.descriptionKey(), this.description);
   }

   public class Builder {
      private SimulatedAdvancement.TaskType type = SimulatedAdvancement.TaskType.NORMAL;
      private boolean externalTrigger;
      private int keyIndex;
      private ItemStack icon;

      public SimulatedAdvancement.Builder special(SimulatedAdvancement.TaskType type) {
         this.type = type;
         return this;
      }

      public SimulatedAdvancement.Builder after(SimulatedAdvancement other) {
         SimulatedAdvancement.this.parent = other;
         return this;
      }

      public SimulatedAdvancement.Builder icon(ItemProviderEntry<?, ?> item) {
         return this.icon(item.asStack());
      }

      public SimulatedAdvancement.Builder icon(ItemLike item) {
         return this.icon(new ItemStack(item));
      }

      public SimulatedAdvancement.Builder icon(ItemStack stack) {
         this.icon = stack;
         return this;
      }

      public SimulatedAdvancement.Builder title(String title) {
         SimulatedAdvancement.this.title = title;
         return this;
      }

      public SimulatedAdvancement.Builder description(String description) {
         SimulatedAdvancement.this.description = description;
         return this;
      }

      public SimulatedAdvancement.Builder whenBlockPlaced(Block block) {
         return this.externalTrigger(TriggerInstance.placedBlock(block));
      }

      public SimulatedAdvancement.Builder whenIconCollected() {
         return this.externalTrigger(net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[]{this.icon.getItem()}));
      }

      public SimulatedAdvancement.Builder whenIconPlaced() {
         return this.icon.getItem() instanceof BlockItem blockItem
            ? this.externalTrigger(TriggerInstance.placedBlock(blockItem.getBlock()))
            : this.whenIconCollected();
      }

      public SimulatedAdvancement.Builder whenItemCollected(ItemProviderEntry<?, ?> item) {
         return this.whenItemCollected(item.asStack().getItem());
      }

      public SimulatedAdvancement.Builder whenItemCollected(ItemLike itemProvider) {
         return this.externalTrigger(net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[]{itemProvider}));
      }

      public SimulatedAdvancement.Builder whenItemCollected(TagKey<Item> tag) {
         return this.externalTrigger(
            net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(
               new ItemPredicate[]{net.minecraft.advancements.critereon.ItemPredicate.Builder.item().of(tag).build()}
            )
         );
      }

      public SimulatedAdvancement.Builder awardedForFree() {
         return this.externalTrigger(net.minecraft.advancements.critereon.InventoryChangeTrigger.TriggerInstance.hasItems(new ItemLike[0]));
      }

      public SimulatedAdvancement.Builder externalTrigger(Criterion<? extends CriterionTriggerInstance> trigger) {
         SimulatedAdvancement.this.builder.addCriterion(String.valueOf(this.keyIndex), trigger);
         this.externalTrigger = true;
         this.keyIndex++;
         return this;
      }
   }

   public static enum TaskType {
      SILENT(AdvancementType.TASK, false, false, false),
      NORMAL(AdvancementType.TASK, true, false, false),
      NOISY(AdvancementType.TASK, true, true, false),
      EXPERT(AdvancementType.GOAL, true, true, false),
      SECRET(AdvancementType.GOAL, true, true, true);

      private final AdvancementType advancementType;
      private final boolean toast;
      private final boolean announce;
      private final boolean hide;

      private TaskType(final AdvancementType advancementType, final boolean toast, final boolean announce, final boolean hide) {
         this.advancementType = advancementType;
         this.toast = toast;
         this.announce = announce;
         this.hide = hide;
      }
   }
}

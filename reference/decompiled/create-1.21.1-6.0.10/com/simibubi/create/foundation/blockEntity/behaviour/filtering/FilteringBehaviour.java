package com.simibubi.create.foundation.blockEntity.behaviour.filtering;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.NotNull;

public class FilteringBehaviour extends BlockEntityBehaviour implements ValueSettingsBehaviour {
   public static final BehaviourType<FilteringBehaviour> TYPE = new BehaviourType<>();
   public MutableComponent customLabel;
   ValueBoxTransform slotPositioning;
   boolean showCount;
   protected FilterItemStack filter = FilterItemStack.empty();
   public int count;
   public boolean upTo;
   private Predicate<ItemStack> predicate;
   private Consumer<ItemStack> callback;
   private Supplier<Boolean> isActive;
   private Supplier<Boolean> showCountPredicate;
   boolean recipeFilter;
   boolean fluidFilter;

   public FilteringBehaviour(SmartBlockEntity be, ValueBoxTransform slot) {
      super(be);
      this.slotPositioning = slot;
      this.showCount = false;
      this.callback = stack -> {
      };
      this.predicate = stack -> true;
      this.isActive = () -> true;
      this.count = 64;
      this.showCountPredicate = () -> this.showCount;
      this.recipeFilter = false;
      this.fluidFilter = false;
      this.upTo = true;
   }

   @Override
   public boolean isSafeNBT() {
      return true;
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      nbt.put("Filter", this.getFilter().saveOptional(registries));
      nbt.putInt("FilterAmount", this.count);
      nbt.putBoolean("UpTo", this.upTo);
      super.write(nbt, registries, clientPacket);
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      this.filter = FilterItemStack.of(registries, nbt.getCompound("Filter"));
      this.count = nbt.getInt("FilterAmount");
      this.upTo = nbt.getBoolean("UpTo");
      if (this.count == 0) {
         this.upTo = true;
         this.count = this.getMaxStackSize();
      }

      super.read(nbt, registries, clientPacket);
   }

   public FilteringBehaviour withCallback(Consumer<ItemStack> filterCallback) {
      this.callback = filterCallback;
      return this;
   }

   public FilteringBehaviour withPredicate(Predicate<ItemStack> filterPredicate) {
      this.predicate = filterPredicate;
      return this;
   }

   public FilteringBehaviour forRecipes() {
      this.recipeFilter = true;
      return this;
   }

   public FilteringBehaviour forFluids() {
      this.fluidFilter = true;
      return this;
   }

   public FilteringBehaviour onlyActiveWhen(Supplier<Boolean> condition) {
      this.isActive = condition;
      return this;
   }

   public FilteringBehaviour showCountWhen(Supplier<Boolean> condition) {
      this.showCountPredicate = condition;
      return this;
   }

   public FilteringBehaviour showCount() {
      this.showCount = true;
      return this;
   }

   public boolean setFilter(Direction face, ItemStack stack) {
      return this.setFilter(stack);
   }

   public void setLabel(MutableComponent label) {
      this.customLabel = label;
   }

   public boolean setFilter(ItemStack stack) {
      ItemStack filter = stack.copy();
      if (!filter.isEmpty() && !this.predicate.test(filter)) {
         return false;
      } else {
         this.filter = FilterItemStack.of(filter);
         if (!this.upTo && !stack.isEmpty()) {
            this.count = Math.min(this.count, stack.getMaxStackSize());
         }

         this.callback.accept(filter);
         this.blockEntity.setChanged();
         this.blockEntity.sendData();
         return true;
      }
   }

   @Override
   public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings settings, boolean ctrlDown) {
      if (!this.getValueSettings().equals(settings)) {
         this.count = Mth.clamp(settings.value(), 1, this.getMaxStackSize());
         this.upTo = settings.row() == 0;
         this.blockEntity.setChanged();
         this.blockEntity.sendData();
         this.playFeedbackSound(this);
      }
   }

   @Override
   public ValueSettingsBehaviour.ValueSettings getValueSettings() {
      return new ValueSettingsBehaviour.ValueSettings(this.upTo ? 0 : 1, this.count == 0 ? this.getMaxStackSize() : this.count);
   }

   @Override
   public void destroy() {
      if (this.filter.isFilterItem()) {
         Vec3 pos = VecHelper.getCenterOf(this.getPos());
         Level world = this.getWorld();
         world.addFreshEntity(new ItemEntity(world, pos.x, pos.y, pos.z, this.getFilter().copy()));
      }

      super.destroy();
   }

   @Override
   public ItemRequirement getRequiredItems() {
      return this.filter.isFilterItem() ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, this.getFilter()) : ItemRequirement.NONE;
   }

   public int getMaxStackSize() {
      return this.getMaxStackSize(this.getFilter());
   }

   public int getMaxStackSize(Direction face) {
      return this.getMaxStackSize(this.getFilter(face));
   }

   public int getMaxStackSize(ItemStack filter) {
      return filter.isEmpty() ? 64 : filter.getMaxStackSize();
   }

   public ItemStack getFilter(Direction side) {
      return this.getFilter();
   }

   public ItemStack getFilter() {
      return this.filter.item();
   }

   public boolean isCountVisible() {
      return this.showCountPredicate.get() && this.getMaxStackSize() > 1;
   }

   public boolean test(ItemStack stack) {
      return !this.isActive() || this.filter.test(this.blockEntity.getLevel(), stack);
   }

   public boolean test(FluidStack stack) {
      return !this.isActive() || this.filter.test(this.blockEntity.getLevel(), stack);
   }

   @Override
   public BehaviourType<?> getType() {
      return TYPE;
   }

   @Override
   public boolean testHit(Vec3 hit) {
      BlockState state = this.blockEntity.getBlockState();
      Vec3 localHit = hit.subtract(Vec3.atLowerCornerOf(this.blockEntity.getBlockPos()));
      return this.slotPositioning.testHit(this.getWorld(), this.getPos(), state, localHit);
   }

   public int getAmount() {
      return this.count;
   }

   public boolean anyAmount() {
      return this.count == 0;
   }

   @Override
   public boolean acceptsValueSettings() {
      return this.isCountVisible();
   }

   @Override
   public boolean isActive() {
      return this.isActive.get();
   }

   @Override
   public ValueBoxTransform getSlotPositioning() {
      return this.slotPositioning;
   }

   @Override
   public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
      int maxAmount = this.getMaxStackSize(hitResult.getDirection());
      return new ValueSettingsBoard(
         CreateLang.translateDirect("logistics.filter.extracted_amount"),
         maxAmount,
         16,
         CreateLang.translatedOptions("logistics.filter", "up_to", "exactly"),
         new ValueSettingsFormatter(this::formatValue)
      );
   }

   public MutableComponent formatValue(ValueSettingsBehaviour.ValueSettings value) {
      return value.row() == 0 && value.value() == this.getMaxStackSize()
         ? CreateLang.translateDirect("logistics.filter.any_amount_short")
         : Component.literal((value.row() == 0 ? "≤" : "=") + Math.max(1, value.value()));
   }

   @Override
   public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
      Level level = this.getWorld();
      BlockPos pos = this.getPos();
      ItemStack itemInHand = player.getItemInHand(hand);
      ItemStack toApply = itemInHand.copy();
      if (this.canShortInteract(toApply)) {
         if (!level.isClientSide()) {
            if (this.getFilter(side).getItem() instanceof FilterItem
               && (
                  !player.isCreative()
                     || ItemHelper.extract(
                           new InvWrapper(player.getInventory()), stack -> ItemStack.isSameItemSameComponents(stack, this.getFilter(side)), true
                        )
                        .isEmpty()
               )) {
               player.getInventory().placeItemBackInInventory(this.getFilter(side).copy());
            }

            if (toApply.getItem() instanceof FilterItem) {
               toApply.setCount(1);
            }

            if (!this.setFilter(side, toApply)) {
               player.displayClientMessage(CreateLang.translateDirect("logistics.filter.invalid_item"), true);
               AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1.0F, 1.0F);
            } else {
               if (!player.isCreative() && toApply.getItem() instanceof FilterItem) {
                  if (itemInHand.getCount() == 1) {
                     player.setItemInHand(hand, ItemStack.EMPTY);
                  } else {
                     itemInHand.shrink(1);
                  }
               }

               level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.25F, 0.1F);
            }
         }
      }
   }

   public boolean canShortInteract(ItemStack toApply) {
      return AllItems.WRENCH.isIn(toApply) ? false : !AllBlocks.MECHANICAL_ARM.isIn(toApply);
   }

   public MutableComponent getLabel() {
      return this.customLabel != null
         ? this.customLabel
         : CreateLang.translateDirect(this.recipeFilter ? "logistics.recipe_filter" : (this.fluidFilter ? "logistics.fluid_filter" : "logistics.filter"));
   }

   public MutableComponent getTip() {
      return CreateLang.translateDirect(this.filter.isEmpty() ? "logistics.filter.click_to_set" : "logistics.filter.click_to_replace");
   }

   public MutableComponent getAmountTip() {
      return CreateLang.translateDirect("logistics.filter.hold_to_set_amount");
   }

   public MutableComponent getCountLabelForValueBox() {
      return Component.literal(this.isCountVisible() ? (this.upTo && this.getMaxStackSize() == this.count ? "*" : String.valueOf(this.count)) : "");
   }

   @Override
   public String getClipboardKey() {
      return "Filtering";
   }

   @Override
   public boolean writeToClipboard(@NotNull Provider registries, CompoundTag tag, Direction side) {
      ValueSettingsBehaviour.super.writeToClipboard(registries, tag, side);
      ItemStack filter = this.getFilter(side);
      tag.put("Filter", filter.saveOptional(registries));
      return true;
   }

   @Override
   public boolean readFromClipboard(@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
      if (!this.mayInteract(player)) {
         return false;
      } else {
         boolean upstreamResult = ValueSettingsBehaviour.super.readFromClipboard(registries, tag, player, side, simulate);
         if (!tag.contains("Filter")) {
            return upstreamResult;
         } else if (simulate) {
            return true;
         } else if (this.getWorld().isClientSide) {
            return true;
         } else {
            ItemStack refund = ItemStack.EMPTY;
            if (this.getFilter(side).getItem() instanceof FilterItem && !player.isCreative()) {
               refund = this.getFilter(side).copy();
            }

            ItemStack copied = ItemStack.parseOptional(registries, tag.getCompound("Filter"));
            if (copied.getItem() instanceof FilterItem filterType && !player.isCreative()) {
               InvWrapper inv = new InvWrapper(player.getInventory());

               for (boolean preferStacksWithoutData : Iterate.trueAndFalse) {
                  if (refund.getItem() == filterType
                     || !ItemHelper.extract(inv, stack -> stack.getItem() == filterType && preferStacksWithoutData == stack.isComponentsPatchEmpty(), 1, false)
                        .isEmpty()) {
                     if (!refund.isEmpty() && refund.getItem() != filterType) {
                        player.getInventory().placeItemBackInInventory(refund);
                     }

                     this.setFilter(side, copied);
                     return true;
                  }
               }

               player.displayClientMessage(
                  CreateLang.translate("logistics.filter.requires_item_in_inventory", copied.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                     .style(ChatFormatting.RED)
                     .component(),
                  true
               );
               AllSoundEvents.DENY.playOnServer(player.level(), player.blockPosition(), 1.0F, 1.0F);
               return false;
            }

            if (!refund.isEmpty()) {
               player.getInventory().placeItemBackInInventory(refund);
            }

            return this.setFilter(side, copied);
         }
      }
   }

   public boolean isRecipeFilter() {
      return this.recipeFilter;
   }

   @Override
   public int netId() {
      return 1;
   }

   public float getRenderDistance() {
      return AllConfigs.client().filterItemRenderDistance.getF();
   }
}

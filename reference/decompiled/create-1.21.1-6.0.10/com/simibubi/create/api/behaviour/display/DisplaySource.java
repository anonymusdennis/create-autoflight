package com.simibubi.create.api.behaviour.display;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.AllDisplaySources;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayBoardTarget;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.content.trains.display.FlapDisplayBlockEntity;
import com.simibubi.create.content.trains.display.FlapDisplayLayout;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.createmod.catnip.nbt.NBTProcessors;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

public abstract class DisplaySource {
   public static final SimpleRegistry.Multi<Block, DisplaySource> BY_BLOCK = SimpleRegistry.Multi.create();
   public static final SimpleRegistry.Multi<BlockEntityType<?>, DisplaySource> BY_BLOCK_ENTITY = SimpleRegistry.Multi.create();
   public static final List<MutableComponent> EMPTY = ImmutableList.of(Component.empty());
   public static final MutableComponent EMPTY_LINE = Component.empty();
   public static final MutableComponent WHITESPACE = CommonComponents.space();

   public abstract List<MutableComponent> provideText(DisplayLinkContext var1, DisplayTargetStats var2);

   public void transferData(DisplayLinkContext context, DisplayTarget activeTarget, int line) {
      DisplayTargetStats stats = activeTarget.provideStats(context);
      if (activeTarget instanceof DisplayBoardTarget fddt) {
         List<List<MutableComponent>> flapDisplayText = this.provideFlapDisplayText(context, stats);
         fddt.acceptFlapText(line, flapDisplayText, context);
      }

      List<MutableComponent> text = this.provideText(context, stats);
      if (text.isEmpty()) {
         text = EMPTY;
      }

      if (activeTarget.requiresComponentSanitization()) {
         for (MutableComponent component : text) {
            if (NBTProcessors.textComponentHasClickEvent(component)) {
               return;
            }
         }
      }

      activeTarget.acceptText(line, text, context);
   }

   public void onSignalReset(DisplayLinkContext context) {
   }

   public void populateData(DisplayLinkContext context) {
   }

   public int getPassiveRefreshTicks() {
      return 100;
   }

   public boolean shouldPassiveReset() {
      return true;
   }

   protected final ResourceLocation getId() {
      return CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(this);
   }

   protected String getTranslationKey() {
      return this.getId().getPath();
   }

   public Component getName() {
      return Component.translatable(this.getId().getNamespace() + ".display_source." + this.getTranslationKey());
   }

   public void loadFlapDisplayLayout(DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay, FlapDisplayLayout layout, int lineIndex) {
      this.loadFlapDisplayLayout(context, flapDisplay, layout);
   }

   public void loadFlapDisplayLayout(DisplayLinkContext context, FlapDisplayBlockEntity flapDisplay, FlapDisplayLayout layout) {
      if (!layout.isLayout("Default")) {
         layout.loadDefault(flapDisplay.getMaxCharCount());
      }
   }

   public List<List<MutableComponent>> provideFlapDisplayText(DisplayLinkContext context, DisplayTargetStats stats) {
      return this.provideText(context, stats).stream().map(xva$0 -> Arrays.asList(xva$0)).toList();
   }

   @OnlyIn(Dist.CLIENT)
   public void initConfigurationWidgets(DisplayLinkContext context, ModularGuiLineBuilder builder, boolean isFirstLine) {
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> displaySource(RegistryEntry<DisplaySource, ? extends DisplaySource> source) {
      return builder -> (BlockBuilder)builder.onRegisterAfter(CreateRegistries.DISPLAY_SOURCE, block -> BY_BLOCK.add(block, (DisplaySource)source.get()));
   }

   @Nullable
   public static DisplaySource get(@Nullable ResourceLocation id) {
      if (id == null) {
         return null;
      } else {
         return id.getNamespace().equals("create") && AllDisplaySources.LEGACY_NAMES.containsKey(id.getPath())
            ? (DisplaySource)AllDisplaySources.LEGACY_NAMES.get(id.getPath()).get()
            : (DisplaySource)CreateBuiltInRegistries.DISPLAY_SOURCE.get(id);
      }
   }

   public static List<DisplaySource> getAll(LevelAccessor level, BlockPos pos) {
      BlockState state = level.getBlockState(pos);
      List<DisplaySource> byBlock = BY_BLOCK.get(state);
      BlockEntity be = level.getBlockEntity(pos);
      if (be == null) {
         return byBlock;
      } else {
         List<DisplaySource> byBe = BY_BLOCK_ENTITY.get(be.getType());
         if (byBlock.isEmpty()) {
            return byBe.isEmpty() ? List.of() : byBe;
         } else if (byBe.isEmpty()) {
            return byBlock;
         } else {
            List<DisplaySource> combined = new ArrayList<>(byBlock);
            combined.addAll(byBe);
            return combined;
         }
      }
   }
}

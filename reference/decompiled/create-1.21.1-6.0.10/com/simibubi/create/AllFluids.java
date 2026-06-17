package com.simibubi.create;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.simibubi.create.content.decoration.palettes.AllPaletteStoneTypes;
import com.simibubi.create.content.fluids.VirtualFluid;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.tterrag.registrate.builders.FluidBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.builders.FluidBuilder.FluidTypeFactory;
import com.tterrag.registrate.util.entry.FluidEntry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DispensibleContainerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.Tags.Fluids;
import net.neoforged.neoforge.common.Tags.Items;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Flowing;
import net.neoforged.neoforge.fluids.BaseFlowingFluid.Source;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation;
import net.neoforged.neoforge.fluids.FluidType.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class AllFluids {
   private static final CreateRegistrate REGISTRATE = Create.registrate();
   public static final FluidEntry<PotionFluid> POTION = REGISTRATE.virtualFluid(
         "potion", PotionFluid.PotionFluidType::new, PotionFluid::createSource, PotionFluid::createFlowing
      )
      .lang("Potion")
      .register();
   public static final FluidEntry<VirtualFluid> TEA = REGISTRATE.virtualFluid("tea")
      .lang("Builder's Tea")
      .tag(new TagKey[]{AllTags.AllFluidTags.TEA.tag})
      .register();
   public static final FluidEntry<Flowing> HONEY = ((FluidBuilder)((ItemBuilder)((FluidBuilder)REGISTRATE.standardFluid(
                  "honey", AllFluids.SolidRenderedPlaceableFluidType.create(15380015, () -> 0.125F * AllConfigs.client().honeyTransparencyMultiplier.getF())
               )
               .lang("Honey")
               .properties(b -> b.viscosity(2000).density(1400))
               .fluidProperties(p -> p.levelDecreasePerBlock(2).tickRate(25).slopeFindDistance(3).explosionResistance(100.0F))
               .tag(new TagKey[]{Fluids.HONEY})
               .source(Source::new)
               .block()
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW))
               .build())
            .bucket()
            .onRegister(AllFluids::registerFluidDispenseBehavior))
         .tag(new TagKey[]{Items.BUCKETS, AllTags.AllItemTags.HONEY_BUCKETS.tag})
         .build())
      .register();
   public static final FluidEntry<Flowing> CHOCOLATE = ((FluidBuilder)((ItemBuilder)((FluidBuilder)REGISTRATE.standardFluid(
                  "chocolate",
                  AllFluids.SolidRenderedPlaceableFluidType.create(6430752, () -> 0.03125F * AllConfigs.client().chocolateTransparencyMultiplier.getF())
               )
               .lang("Chocolate")
               .tag(new TagKey[]{AllTags.AllFluidTags.CHOCOLATE.tag})
               .properties(b -> b.viscosity(1500).density(1400))
               .fluidProperties(p -> p.levelDecreasePerBlock(2).tickRate(25).slopeFindDistance(3).explosionResistance(100.0F))
               .source(Source::new)
               .block()
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
               .build())
            .bucket()
            .onRegister(AllFluids::registerFluidDispenseBehavior))
         .tag(new TagKey[]{Items.BUCKETS, AllTags.AllItemTags.CHOCOLATE_BUCKETS.tag})
         .build())
      .register();
   private static final DispenseItemBehavior DEFAULT = new DefaultDispenseItemBehavior();
   private static final DispenseItemBehavior DISPENSE_FLUID = new DefaultDispenseItemBehavior() {
      protected ItemStack execute(BlockSource pSource, ItemStack pStack) {
         DispensibleContainerItem dispensibleContainerItem = (DispensibleContainerItem)pStack.getItem();
         BlockPos pos = pSource.pos().relative((Direction)pSource.state().getValue(DispenserBlock.FACING));
         Level level = pSource.level();
         return dispensibleContainerItem.emptyContents(null, level, pos, null, pStack)
            ? new ItemStack(net.minecraft.world.item.Items.BUCKET)
            : AllFluids.DEFAULT.dispense(pSource, pStack);
      }
   };

   public static void register() {
   }

   public static void registerFluidInteractions() {
      FluidInteractionRegistry.addInteraction(
         (FluidType)NeoForgeMod.LAVA_TYPE.value(),
         new InteractionInformation(
            ((Flowing)HONEY.get()).getFluidType(),
            fluidState -> fluidState.isSource()
                  ? Blocks.OBSIDIAN.defaultBlockState()
                  : ((Block)AllPaletteStoneTypes.LIMESTONE.getBaseBlock().get()).defaultBlockState()
         )
      );
      FluidInteractionRegistry.addInteraction(
         (FluidType)NeoForgeMod.LAVA_TYPE.value(),
         new InteractionInformation(
            ((Flowing)CHOCOLATE.get()).getFluidType(),
            fluidState -> fluidState.isSource()
                  ? Blocks.OBSIDIAN.defaultBlockState()
                  : ((Block)AllPaletteStoneTypes.SCORIA.getBaseBlock().get()).defaultBlockState()
         )
      );
   }

   @Nullable
   public static BlockState getLavaInteraction(FluidState fluidState) {
      Fluid fluid = fluidState.getType();
      if (fluid.isSame((Fluid)HONEY.get())) {
         return ((Block)AllPaletteStoneTypes.LIMESTONE.getBaseBlock().get()).defaultBlockState();
      } else {
         return fluid.isSame((Fluid)CHOCOLATE.get()) ? ((Block)AllPaletteStoneTypes.SCORIA.getBaseBlock().get()).defaultBlockState() : null;
      }
   }

   private static void registerFluidDispenseBehavior(BucketItem bucket) {
      DispenserBlock.registerBehavior(bucket, DISPENSE_FLUID);
   }

   static {
      REGISTRATE.setCreativeTab(AllCreativeModeTabs.BASE_CREATIVE_TAB);
   }

   private static class SolidRenderedPlaceableFluidType extends AllFluids.TintedFluidType {
      private Vector3f fogColor;
      private Supplier<Float> fogDistance;

      public static FluidTypeFactory create(int fogColor, Supplier<Float> fogDistance) {
         return (p, s, f) -> {
            AllFluids.SolidRenderedPlaceableFluidType fluidType = new AllFluids.SolidRenderedPlaceableFluidType(p, s, f);
            fluidType.fogColor = new Color(fogColor, false).asVectorF();
            fluidType.fogDistance = fogDistance;
            return fluidType;
         };
      }

      private SolidRenderedPlaceableFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
         super(properties, stillTexture, flowingTexture);
      }

      @Override
      protected int getTintColor(FluidStack stack) {
         return -1;
      }

      @Override
      public int getTintColor(FluidState state, BlockAndTintGetter world, BlockPos pos) {
         return 16777215;
      }

      @Override
      protected Vector3f getCustomFogColor() {
         return this.fogColor;
      }

      @Override
      protected float getFogDistanceModifier() {
         return this.fogDistance.get();
      }
   }

   public abstract static class TintedFluidType extends FluidType {
      protected static final int NO_TINT = -1;
      private final ResourceLocation stillTexture;
      private final ResourceLocation flowingTexture;

      public TintedFluidType(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
         super(properties);
         this.stillTexture = stillTexture;
         this.flowingTexture = flowingTexture;
      }

      public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
         consumer.accept(
            new IClientFluidTypeExtensions() {
               public ResourceLocation getStillTexture() {
                  return TintedFluidType.this.stillTexture;
               }

               public ResourceLocation getFlowingTexture() {
                  return TintedFluidType.this.flowingTexture;
               }

               public int getTintColor(FluidStack stack) {
                  return TintedFluidType.this.getTintColor(stack);
               }

               public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                  return TintedFluidType.this.getTintColor(state, getter, pos);
               }

               @NotNull
               public Vector3f modifyFogColor(
                  Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector3f fluidFogColor
               ) {
                  Vector3f customFogColor = TintedFluidType.this.getCustomFogColor();
                  return customFogColor == null ? fluidFogColor : customFogColor;
               }

               public void modifyFogRender(
                  Camera camera, FogMode mode, float renderDistance, float partialTick, float nearDistance, float farDistance, FogShape shape
               ) {
                  float modifier = TintedFluidType.this.getFogDistanceModifier();
                  float baseWaterFog = 96.0F;
                  if (modifier != 1.0F) {
                     RenderSystem.setShaderFogShape(FogShape.CYLINDER);
                     RenderSystem.setShaderFogStart(-8.0F);
                     RenderSystem.setShaderFogEnd(baseWaterFog * modifier);
                  }
               }
            }
         );
      }

      protected abstract int getTintColor(FluidStack var1);

      protected abstract int getTintColor(FluidState var1, BlockAndTintGetter var2, BlockPos var3);

      protected Vector3f getCustomFogColor() {
         return null;
      }

      protected float getFogDistanceModifier() {
         return 1.0F;
      }
   }
}

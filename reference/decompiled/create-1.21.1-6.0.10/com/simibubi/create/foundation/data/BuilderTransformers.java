package com.simibubi.create.foundation.data;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllTags;
import com.simibubi.create.Create;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.TrapdoorMovingInteraction;
import com.simibubi.create.content.contraptions.piston.MechanicalPistonGenerator;
import com.simibubi.create.content.decoration.MetalScaffoldingBlock;
import com.simibubi.create.content.decoration.MetalScaffoldingBlockItem;
import com.simibubi.create.content.decoration.MetalScaffoldingCTBehaviour;
import com.simibubi.create.content.decoration.copycat.CopycatBlock;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogCTBehaviour;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageStyles;
import com.simibubi.create.content.logistics.packager.PackagerGenerator;
import com.simibubi.create.content.logistics.tableCloth.TableClothBlockItem;
import com.simibubi.create.content.logistics.tableCloth.TableClothModel;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelItem;
import com.simibubi.create.content.trains.bogey.AbstractBogeyBlock;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.foundation.block.ItemUseOverrides;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.infrastructure.config.CStress;
import com.tterrag.registrate.builders.BlockBuilder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import com.tterrag.registrate.util.DataIngredient;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import com.tterrag.registrate.util.nullness.NonNullUnaryOperator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import net.createmod.catnip.registry.RegisteredObjectsHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTable.Builder;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.Tags.Items;
import org.jetbrains.annotations.Nullable;

public class BuilderTransformers {
   public static <B extends EncasedShaftBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedShaft(String casing, Supplier<CTSpriteShiftEntry> casingShift) {
      return builder -> (BlockBuilder)((BlockBuilder)((BlockBuilder)encasedBase(builder, () -> (ItemLike)AllBlocks.SHAFT.get())
                  .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(casingShift.get()))))
               .onRegister(
                  CreateRegistrate.casingConnectivity(
                     (block, cc) -> cc.make(block, casingShift.get(), (s, f) -> f.getAxis() != s.getValue(EncasedShaftBlock.AXIS))
                  )
               ))
            .blockstate(
               (c, p) -> BlockStateGen.axisBlock(c, p, blockState -> p.models().getExistingFile(p.modLoc("block/encased_shaft/block_" + casing)), true)
            )
            .item()
            .model(AssetLookup.customBlockItemModel("encased_shaft", "item_" + casing))
            .build();
   }

   public static <B extends StandardBogeyBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> bogey() {
      return b -> (BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::softMetal)
               .properties(p -> p.sound(SoundType.NETHERITE_BLOCK))
               .properties(p -> p.noOcclusion())
               .transform(TagGen.pickaxeOnly()))
            .blockstate((c, p) -> BlockStateGen.horizontalAxisBlock(c, p, s -> p.models().getExistingFile(p.modLoc("block/track/bogey/top"))))
            .loot((p, l) -> p.dropOther(l, (ItemLike)AllBlocks.RAILWAY_CASING.get()))
            .onRegister(block -> AbstractBogeyBlock.registerStandardBogey(RegisteredObjectsHelper.getKeyOrThrow(block)));
   }

   public static <B extends CopycatBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> copycat() {
      return b -> (BlockBuilder)b.initialProperties(SharedProperties::softMetal)
            .blockstate((c, p) -> p.simpleBlock((Block)c.get(), p.models().getExistingFile(p.mcLoc("air"))))
            .initialProperties(SharedProperties::softMetal)
            .properties(p -> p.noOcclusion().mapColor(MapColor.NONE).isValidSpawn((state, level, pos, type) -> false))
            .addLayer(() -> RenderType::solid)
            .addLayer(() -> RenderType::cutout)
            .addLayer(() -> RenderType::cutoutMipped)
            .addLayer(() -> RenderType::translucent)
            .color(() -> CopycatBlock::wrappedColor)
            .transform(TagGen.axeOrPickaxe());
   }

   public static <B extends TrapDoorBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> trapdoor(boolean orientable) {
      return b -> (BlockBuilder)((BlockBuilder)((BlockBuilder)b.blockstate((c, p) -> {
                  ModelFile bottom = AssetLookup.partialBaseModel(c, p, "bottom");
                  ModelFile top = AssetLookup.partialBaseModel(c, p, "top");
                  ModelFile open = AssetLookup.partialBaseModel(c, p, "open");
                  if (orientable) {
                     p.trapdoorBlock((TrapDoorBlock)c.get(), bottom, top, open, orientable);
                  } else {
                     BlockStateGen.uvLockedTrapdoorBlock((P)((TrapDoorBlock)c.get()), bottom, top, open).accept(c, p);
                  }
               }).transform(TagGen.pickaxeOnly()))
               .tag(new TagKey[]{BlockTags.TRAPDOORS})
               .onRegister(MovingInteractionBehaviour.interactionBehaviour(new TrapdoorMovingInteraction())))
            .item()
            .tag(new TagKey[]{ItemTags.TRAPDOORS})
            .build();
   }

   public static <B extends SlidingDoorBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> slidingDoor(String type) {
      return b -> (BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)b.initialProperties(() -> Blocks.IRON_DOOR)
                     .properties(p -> p.requiresCorrectToolForDrops().strength(3.0F, 6.0F))
                     .blockstate((c, p) -> {
                        ModelFile bottom = AssetLookup.partialBaseModel(c, p, "bottom");
                        ModelFile top = AssetLookup.partialBaseModel(c, p, "top");
                        p.doorBlock((DoorBlock)c.get(), bottom, bottom, bottom, bottom, top, top, top, top);
                     })
                     .addLayer(() -> RenderType::cutoutMipped)
                     .transform(TagGen.pickaxeOnly()))
                  .onRegister(MovingInteractionBehaviour.interactionBehaviour(new DoorMovingInteraction())))
               .onRegister(MovementBehaviour.movementBehaviour(new SlidingDoorMovementBehaviour())))
            .tag(new TagKey[]{BlockTags.DOORS})
            .tag(new TagKey[]{BlockTags.WOODEN_DOORS})
            .tag(new TagKey[]{AllTags.AllBlockTags.NON_DOUBLE_DOOR.tag})
            .loot((lr, block) -> lr.add(block, lr.createDoorTable(block)))
            .item()
            .tag(new TagKey[]{ItemTags.DOORS})
            .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
            .model((c, p) -> p.blockSprite(c, p.modLoc("item/" + type + "_door")))
            .build();
   }

   public static <B extends EncasedCogwheelBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedCogwheel(
      String casing, Supplier<CTSpriteShiftEntry> casingShift
   ) {
      return b -> encasedCogwheelBase(b, casing, casingShift, () -> (ItemLike)AllBlocks.COGWHEEL.get(), false);
   }

   public static <B extends EncasedCogwheelBlock, P> NonNullUnaryOperator<BlockBuilder<B, P>> encasedLargeCogwheel(
      String casing, Supplier<CTSpriteShiftEntry> casingShift
   ) {
      return b -> (BlockBuilder)encasedCogwheelBase(b, casing, casingShift, () -> (ItemLike)AllBlocks.LARGE_COGWHEEL.get(), true)
            .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCogCTBehaviour(casingShift.get())));
   }

   private static <B extends EncasedCogwheelBlock, P> BlockBuilder<B, P> encasedCogwheelBase(
      BlockBuilder<B, P> b, String casing, Supplier<CTSpriteShiftEntry> casingShift, Supplier<ItemLike> drop, boolean large
   ) {
      String encasedSuffix = "_encased_cogwheel_side" + (large ? "_connected" : "");
      String blockFolder = large ? "encased_large_cogwheel" : "encased_cogwheel";
      String wood = casing.equals("brass") ? "dark_oak" : "spruce";
      String gearbox = casing.equals("brass") ? "brass_gearbox" : "gearbox";
      return (BlockBuilder<B, P>)((BlockBuilder)encasedBase(b, drop)
            .addLayer(() -> RenderType::cutoutMipped)
            .onRegister(
               CreateRegistrate.casingConnectivity(
                  (block, cc) -> cc.make(
                        block,
                        casingShift.get(),
                        (s, f) -> f.getAxis() == s.getValue(EncasedCogwheelBlock.AXIS)
                              && !(Boolean)s.getValue(
                                 f.getAxisDirection() == AxisDirection.POSITIVE ? EncasedCogwheelBlock.TOP_SHAFT : EncasedCogwheelBlock.BOTTOM_SHAFT
                              )
                     )
               )
            ))
         .blockstate(
            (c, p) -> BlockStateGen.axisBlock(
                  c,
                  p,
                  blockState -> {
                     String suffix = (blockState.getValue(EncasedCogwheelBlock.TOP_SHAFT) ? "_top" : "")
                        + (blockState.getValue(EncasedCogwheelBlock.BOTTOM_SHAFT) ? "_bottom" : "");
                     String modelName = c.getName() + suffix;
                     return ((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)p.models()
                                       .withExistingParent(modelName, p.modLoc("block/" + blockFolder + "/block" + suffix)))
                                    .texture("casing", Create.asResource("block/" + casing + "_casing")))
                                 .texture("particle", Create.asResource("block/" + casing + "_casing")))
                              .texture("4", Create.asResource("block/" + gearbox)))
                           .texture("1", ResourceLocation.withDefaultNamespace("block/stripped_" + wood + "_log_top")))
                        .texture("side", Create.asResource("block/" + casing + encasedSuffix));
                  },
                  false
               )
         )
         .item()
         .model(
            (c, p) -> ((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)p.withExistingParent(
                              c.getName(), p.modLoc("block/" + blockFolder + "/item")
                           ))
                           .texture("casing", Create.asResource("block/" + casing + "_casing")))
                        .texture("particle", Create.asResource("block/" + casing + "_casing")))
                     .texture("1", ResourceLocation.withDefaultNamespace("block/stripped_" + wood + "_log_top")))
                  .texture("side", Create.asResource("block/" + casing + encasedSuffix))
         )
         .build();
   }

   private static <B extends RotatedPillarKineticBlock, P> BlockBuilder<B, P> encasedBase(BlockBuilder<B, P> b, Supplier<ItemLike> drop) {
      return ((BlockBuilder)b.initialProperties(SharedProperties::stone).properties(Properties::noOcclusion).transform(CStress.setNoImpact()))
         .loot((p, lb) -> p.dropOther(lb, drop.get()));
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> cuckooClock() {
      return b -> (BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::wooden)
               .blockstate((c, p) -> p.horizontalBlock((Block)c.get(), p.models().getExistingFile(p.modLoc("block/cuckoo_clock/block"))))
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(CStress.setImpact(1.0)))
            .item()
            .transform(ModelGen.customItemModel("cuckoo_clock", "item"));
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> ladder(String name, Supplier<DataIngredient> ingredient, MapColor color) {
      return b -> (BlockBuilder)((BlockBuilder)b.initialProperties(() -> Blocks.LADDER)
               .properties(p -> p.mapColor(color))
               .addLayer(() -> RenderType::cutout)
               .blockstate(
                  (c, p) -> p.horizontalBlock(
                        (Block)c.get(),
                        ((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)p.models().withExistingParent(c.getName(), p.modLoc("block/ladder")))
                                 .texture("0", p.modLoc("block/ladder_" + name + "_hoop")))
                              .texture("1", p.modLoc("block/ladder_" + name)))
                           .texture("particle", p.modLoc("block/ladder_" + name))
                     )
               )
               .properties(p -> p.sound(SoundType.COPPER))
               .transform(TagGen.pickaxeOnly()))
            .tag(new TagKey[]{BlockTags.CLIMBABLE})
            .item()
            .recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 2))
            .model((c, p) -> p.blockSprite(c::get, p.modLoc("block/ladder_" + name)))
            .build();
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> scaffold(
      String name,
      Supplier<DataIngredient> ingredient,
      MapColor color,
      CTSpriteShiftEntry scaffoldShift,
      CTSpriteShiftEntry scaffoldInsideShift,
      CTSpriteShiftEntry casingShift
   ) {
      return b -> (BlockBuilder)((BlockBuilder)((BlockBuilder)b.initialProperties(() -> Blocks.SCAFFOLDING)
                  .properties(p -> p.sound(SoundType.COPPER).mapColor(color))
                  .addLayer(() -> RenderType::cutout)
                  .blockstate(
                     (c, p) -> p.getVariantBuilder((Block)c.get())
                           .forAllStatesExcept(
                              s -> {
                                 String suffix = s.getValue(MetalScaffoldingBlock.BOTTOM) ? "_horizontal" : "";
                                 return ConfiguredModel.builder()
                                    .modelFile(
                                       ((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)p.models()
                                                         .withExistingParent(c.getName() + suffix, p.modLoc("block/scaffold/block" + suffix)))
                                                      .texture("top", p.modLoc("block/funnel/" + name + "_funnel_frame")))
                                                   .texture("inside", p.modLoc("block/scaffold/" + name + "_scaffold_inside")))
                                                .texture("side", p.modLoc("block/scaffold/" + name + "_scaffold")))
                                             .texture("casing", p.modLoc("block/" + name + "_casing")))
                                          .texture("particle", p.modLoc("block/scaffold/" + name + "_scaffold"))
                                    )
                                    .build();
                              },
                              new Property[]{MetalScaffoldingBlock.WATERLOGGED, MetalScaffoldingBlock.DISTANCE}
                           )
                  )
                  .onRegister(CreateRegistrate.connectedTextures(() -> new MetalScaffoldingCTBehaviour(scaffoldShift, scaffoldInsideShift, casingShift))))
               .transform(TagGen.pickaxeOnly()))
            .tag(new TagKey[]{BlockTags.CLIMBABLE})
            .item(MetalScaffoldingBlockItem::new)
            .recipe((c, p) -> p.stonecutting(ingredient.get(), RecipeCategory.DECORATIONS, c::get, 2))
            .model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/" + c.getName())))
            .build();
   }

   public static <B extends ValveHandleBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> valveHandle(@Nullable DyeColor color) {
      return b -> (BlockBuilder)((BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::copperMetal)
                  .blockstate(
                     (c, p) -> {
                        String variant = color == null ? "copper" : color.getSerializedName();
                        p.directionalBlock(
                           (Block)c.get(),
                           ((BlockModelBuilder)p.models().withExistingParent(variant + "_valve_handle", p.modLoc("block/valve_handle")))
                              .texture("3", p.modLoc("block/valve_handle/valve_handle_" + variant))
                        );
                     }
                  )
                  .tag(new TagKey[]{AllTags.AllBlockTags.BRITTLE.tag, AllTags.AllBlockTags.VALVE_HANDLES.tag})
                  .onRegister(BlockStressValues.setGeneratorSpeed(32)))
               .onRegister(ItemUseOverrides::addBlock))
            .item()
            .tag(new TagKey[]{AllTags.AllItemTags.VALVE_HANDLES.tag})
            .build();
   }

   public static <B extends CasingBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> casing(Supplier<CTSpriteShiftEntry> ct) {
      return b -> (BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::stone)
                     .properties(p -> p.sound(SoundType.WOOD))
                     .transform(TagGen.axeOrPickaxe()))
                  .blockstate((c, p) -> p.simpleBlock((Block)c.get()))
                  .onRegister(CreateRegistrate.connectedTextures(() -> new EncasedCTBehaviour(ct.get()))))
               .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get()))))
            .tag(new TagKey[]{AllTags.AllBlockTags.CASING.tag})
            .item()
            .tag(new TagKey[]{AllTags.AllItemTags.CASING.tag})
            .build();
   }

   public static <B extends CasingBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> layeredCasing(
      Supplier<CTSpriteShiftEntry> ct, Supplier<CTSpriteShiftEntry> ct2
   ) {
      return b -> (BlockBuilder)((BlockBuilder)((BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::stone).transform(TagGen.axeOrPickaxe()))
                  .blockstate(
                     (c, p) -> p.simpleBlock(
                           (Block)c.get(), p.models().cubeColumn(c.getName(), ct.get().getOriginalResourceLocation(), ct2.get().getOriginalResourceLocation())
                        )
                  )
                  .onRegister(CreateRegistrate.connectedTextures(() -> new HorizontalCTBehaviour(ct.get(), ct2.get()))))
               .onRegister(CreateRegistrate.casingConnectivity((block, cc) -> cc.makeCasing(block, ct.get()))))
            .tag(new TagKey[]{AllTags.AllBlockTags.CASING.tag})
            .item()
            .tag(new TagKey[]{AllTags.AllItemTags.CASING.tag})
            .build();
   }

   public static <B extends BeltTunnelBlock> NonNullUnaryOperator<BlockBuilder<B, CreateRegistrate>> beltTunnel(String type, ResourceLocation particleTexture) {
      String prefix = "block/tunnel/" + type + "_tunnel";
      String funnel_prefix = "block/funnel/" + type + "_funnel";
      return b -> (BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::stone)
               .addLayer(() -> RenderType::cutoutMipped)
               .properties(Properties::noOcclusion)
               .transform(TagGen.pickaxeOnly()))
            .blockstate(
               (c, p) -> p.getVariantBuilder((Block)c.get())
                     .forAllStates(
                        state -> {
                           BeltTunnelBlock.Shape shape = (BeltTunnelBlock.Shape)state.getValue(BeltTunnelBlock.SHAPE);
                           String window = shape == BeltTunnelBlock.Shape.WINDOW ? "_window" : "";
                           if (shape == BeltTunnelBlock.Shape.CLOSED) {
                              shape = BeltTunnelBlock.Shape.STRAIGHT;
                           }

                           String shapeName = shape.getSerializedName();
                           return ConfiguredModel.builder()
                              .modelFile(
                                 ((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)p.models()
                                                   .withExistingParent(prefix + "/" + shapeName, p.modLoc("block/belt_tunnel/" + shapeName)))
                                                .texture("top", p.modLoc(prefix + "_top" + window)))
                                             .texture("tunnel", p.modLoc(prefix)))
                                          .texture("direction", p.modLoc(funnel_prefix + "_neutral")))
                                       .texture("frame", p.modLoc(funnel_prefix + "_frame")))
                                    .texture("particle", particleTexture)
                              )
                              .rotationY(state.getValue(BeltTunnelBlock.HORIZONTAL_AXIS) == Axis.X ? 0 : 90)
                              .build();
                        }
                     )
            )
            .item(BeltTunnelItem::new)
            .model(
               (c, p) -> ((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)p.withExistingParent(
                                    "item/" + type + "_tunnel", p.modLoc("block/belt_tunnel/item")
                                 ))
                                 .texture("top", p.modLoc(prefix + "_top")))
                              .texture("tunnel", p.modLoc(prefix)))
                           .texture("direction", p.modLoc(funnel_prefix + "_neutral")))
                        .texture("frame", p.modLoc(funnel_prefix + "_frame")))
                     .texture("particle", particleTexture)
            )
            .build();
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> mechanicalPiston(PistonType type) {
      return b -> (BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::stone)
               .properties(p -> p.noOcclusion())
               .blockstate(new MechanicalPistonGenerator(type)::generate)
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(CStress.setImpact(4.0)))
            .item()
            .transform(ModelGen.customItemModel("mechanical_piston", type.getSerializedName(), "item"));
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> bearing(String prefix, String backTexture) {
      ResourceLocation baseBlockModelLocation = Create.asResource("block/bearing/block");
      ResourceLocation baseItemModelLocation = Create.asResource("block/bearing/item");
      ResourceLocation topTextureLocation = Create.asResource("block/bearing_top");
      ResourceLocation sideTextureLocation = Create.asResource("block/" + prefix + "_bearing_side");
      ResourceLocation backTextureLocation = Create.asResource("block/" + backTexture);
      return b -> (BlockBuilder)b.initialProperties(SharedProperties::stone)
            .properties(p -> p.noOcclusion())
            .blockstate(
               (c, p) -> p.directionalBlock(
                     (Block)c.get(),
                     ((BlockModelBuilder)((BlockModelBuilder)p.models().withExistingParent(c.getName(), baseBlockModelLocation))
                           .texture("side", sideTextureLocation))
                        .texture("back", backTextureLocation)
                  )
            )
            .item()
            .model(
               (c, p) -> ((ItemModelBuilder)((ItemModelBuilder)((ItemModelBuilder)p.withExistingParent(c.getName(), baseItemModelLocation))
                           .texture("top", topTextureLocation))
                        .texture("side", sideTextureLocation))
                     .texture("back", backTextureLocation)
            )
            .build();
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> crate(String type) {
      return b -> (BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::stone).transform(TagGen.axeOrPickaxe()))
            .blockstate(
               (c, p) -> {
                  String[] variants = new String[]{"single", "top", "bottom", "left", "right"};
                  Map<String, ModelFile> models = new HashMap<>();
                  ResourceLocation crate = p.modLoc("block/crate_" + type);
                  ResourceLocation side = p.modLoc("block/crate_" + type + "_side");
                  ResourceLocation casing = p.modLoc("block/" + type + "_casing");

                  for (String variant : variants) {
                     models.put(
                        variant,
                        ((BlockModelBuilder)((BlockModelBuilder)((BlockModelBuilder)p.models()
                                    .withExistingParent("block/crate/" + type + "/" + variant, p.modLoc("block/crate/" + variant)))
                                 .texture("crate", crate))
                              .texture("side", side))
                           .texture("casing", casing)
                     );
                  }

                  p.getVariantBuilder((Block)c.get()).forAllStates(state -> {
                     String variantx = "single";
                     return ConfiguredModel.builder().modelFile(models.get(variantx)).build();
                  });
               }
            )
            .item()
            .properties(p -> type.equals("creative") ? p.rarity(Rarity.EPIC) : p)
            .transform(ModelGen.customItemModel("crate", type, "single"));
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> backtank(Supplier<ItemLike> drop) {
      return b -> ((BlockBuilder)((BlockBuilder)b.blockstate((c, p) -> p.horizontalBlock((Block)c.getEntry(), AssetLookup.partialBaseModel(c, p)))
                  .transform(TagGen.pickaxeOnly()))
               .addLayer(() -> RenderType::cutoutMipped)
               .transform(CStress.setImpact(4.0)))
            .loot(
               (lt, block) -> {
                  Builder builder = LootTable.lootTable();
                  net.minecraft.world.level.storage.loot.predicates.LootItemCondition.Builder survivesExplosion = ExplosionCondition.survivesExplosion();
                  lt.add(
                     block,
                     builder.withPool(
                        LootPool.lootPool()
                           .when(survivesExplosion)
                           .setRolls(ConstantValue.exactly(1.0F))
                           .add(
                              LootItem.lootTableItem(drop.get())
                                 .apply(CopyComponentsFunction.copyComponents(Source.BLOCK_ENTITY).include(AllDataComponents.BACKTANK_AIR))
                           )
                     )
                  );
               }
            );
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> bell() {
      return b -> (BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::softMetal)
               .properties(p -> p.noOcclusion().sound(SoundType.ANVIL))
               .transform(TagGen.pickaxeOnly()))
            .addLayer(() -> RenderType::cutoutMipped)
            .tag(new TagKey[]{AllTags.AllBlockTags.BRITTLE.tag})
            .blockstate((c, p) -> p.horizontalBlock((Block)c.getEntry(), state -> {
                  String variant = ((BellAttachType)state.getValue(BlockStateProperties.BELL_ATTACHMENT)).getSerializedName();
                  return p.models().withExistingParent(c.getName() + "_" + variant, p.modLoc("block/bell_base/block_" + variant));
               }))
            .item()
            .model((c, p) -> p.withExistingParent(c.getName(), p.modLoc("block/" + c.getName())))
            .tag(new TagKey[]{AllTags.AllItemTags.CONTRAPTION_CONTROLLED.tag})
            .build();
   }

   public static ItemBuilder<PackageItem, CreateRegistrate> packageItem(PackageStyles.PackageStyle style) {
      String size = "_" + style.width() + "x" + style.height();
      return Create.registrate()
         .item(style.getItemId().getPath(), p -> new PackageItem(p, style))
         .properties(p -> p.stacksTo(1))
         .tag(new TagKey[]{AllTags.AllItemTags.PACKAGES.tag})
         .model(
            (c, p) -> {
               if (style.rare()) {
                  ((ItemModelBuilder)p.withExistingParent(c.getName(), p.modLoc("item/package/custom" + size)))
                     .texture("2", p.modLoc("item/package/" + style.type()));
               } else {
                  p.withExistingParent(c.getName(), p.modLoc("item/package/" + style.type() + size));
               }
            }
         )
         .lang((style.rare() ? "Rare" : style.type().substring(0, 1).toUpperCase(Locale.ROOT) + style.type().substring(1)) + " Package");
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> tableCloth(
      String name, NonNullSupplier<? extends Block> initialProps, boolean dyed
   ) {
      return b -> {
         TagKey<Block> soundTag = dyed ? BlockTags.COMBINATION_STEP_SOUND_BLOCKS : BlockTags.INSIDE_STEP_SOUND_BLOCKS;
         ItemBuilder<TableClothBlockItem, BlockBuilder<B, P>> item = ((BlockBuilder)((BlockBuilder)b.initialProperties(initialProps)
                  .addLayer(() -> RenderType::cutoutMipped)
                  .blockstate(
                     (c, p) -> p.simpleBlock(
                           (Block)c.get(),
                           ((BlockModelBuilder)p.models().withExistingParent(name + "_table_cloth", p.modLoc("block/table_cloth/block")))
                              .texture("0", p.modLoc("block/table_cloth/" + name))
                        )
                  )
                  .onRegister(CreateRegistrate.blockModel(() -> TableClothModel::new)))
               .tag(new TagKey[]{AllTags.AllBlockTags.TABLE_CLOTHS.tag, soundTag})
               .onRegisterAfter(Registries.ITEM, v -> ItemDescription.useKey(v, "block.create.table_cloth")))
            .item(TableClothBlockItem::new);
         if (dyed) {
            item.tag(new TagKey[]{AllTags.AllItemTags.DYED_TABLE_CLOTHS.tag});
         }

         return (BlockBuilder)item.model(
               (c, p) -> ((ItemModelBuilder)p.withExistingParent(name + "_table_cloth", p.modLoc("block/table_cloth/item")))
                     .texture("0", p.modLoc("block/table_cloth/" + name))
            )
            .tag(new TagKey[]{AllTags.AllItemTags.TABLE_CLOTHS.tag})
            .recipe(
               (c, p) -> ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, (ItemLike)c.get())
                     .requires((ItemLike)c.get())
                     .unlockedBy("has_" + c.getName(), RegistrateRecipeProvider.has((ItemLike)c.get()))
                     .save(p, Create.asResource("crafting/logistics/" + c.getName() + "_clear"))
            )
            .build();
      };
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> packager() {
      return b -> (BlockBuilder)((BlockBuilder)b.initialProperties(SharedProperties::softMetal)
               .properties(p -> p.noOcclusion())
               .properties(p -> p.isRedstoneConductor(($1, $2, $3) -> false))
               .properties(p -> p.mapColor(MapColor.TERRACOTTA_BLUE).sound(SoundType.NETHERITE_BLOCK))
               .transform(TagGen.pickaxeOnly()))
            .addLayer(() -> RenderType::cutoutMipped)
            .blockstate(new PackagerGenerator()::generate)
            .item()
            .model(AssetLookup::customItemModel)
            .build();
   }

   public static <B extends Block, P> NonNullUnaryOperator<BlockBuilder<B, P>> palettesIronBlock() {
      return b -> ((BlockBuilder)b.initialProperties(SharedProperties::softMetal)
               .properties(p -> p.mapColor(MapColor.COLOR_GRAY).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops())
               .transform(TagGen.pickaxeOnly()))
            .blockstate(
               (c, p) -> p.simpleBlock(
                     (Block)c.get(), p.models().cubeColumn(c.getName(), p.modLoc("block/" + c.getName()), p.modLoc("block/" + c.getName() + "_top"))
                  )
            )
            .tag(new TagKey[]{AllTags.AllBlockTags.WRENCH_PICKUP.tag})
            .recipe((c, p) -> p.stonecutting(DataIngredient.tag(Items.INGOTS_IRON), RecipeCategory.BUILDING_BLOCKS, c::get, 2))
            .simpleItem();
   }
}

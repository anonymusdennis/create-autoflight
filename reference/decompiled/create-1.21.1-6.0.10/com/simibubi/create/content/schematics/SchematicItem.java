package com.simibubi.create.content.schematics;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.client.SchematicEditScreen;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.foundation.utility.CreatePaths;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.GZIPInputStream;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public class SchematicItem extends Item {
   private static final Logger LOGGER = LogUtils.getLogger();

   public SchematicItem(Properties properties) {
      super(properties);
   }

   public static ItemStack create(Level level, String schematic, String owner) {
      ItemStack blueprint = AllItems.SCHEMATIC.asStack();
      blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED, false);
      blueprint.set(AllDataComponents.SCHEMATIC_OWNER, owner);
      blueprint.set(AllDataComponents.SCHEMATIC_FILE, schematic);
      blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR, BlockPos.ZERO);
      blueprint.set(AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE);
      blueprint.set(AllDataComponents.SCHEMATIC_MIRROR, Mirror.NONE);
      writeSize(level, blueprint);
      return blueprint;
   }

   @OnlyIn(Dist.CLIENT)
   public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flagIn) {
      if (stack.has(AllDataComponents.SCHEMATIC_FILE)) {
         tooltip.add(Component.literal(ChatFormatting.GOLD + (String)stack.get(AllDataComponents.SCHEMATIC_FILE)));
      } else {
         tooltip.add(CreateLang.translateDirect("schematic.invalid").withStyle(ChatFormatting.RED));
      }

      super.appendHoverText(stack, context, tooltip, flagIn);
   }

   public static void writeSize(Level level, ItemStack blueprint) {
      StructureTemplate t = loadSchematic(level, blueprint);
      blueprint.set(AllDataComponents.SCHEMATIC_BOUNDS, t.getSize());
      SchematicInstances.clearHash(blueprint);
   }

   public static StructurePlaceSettings getSettings(ItemStack blueprint) {
      return getSettings(blueprint, true);
   }

   public static StructurePlaceSettings getSettings(ItemStack blueprint, boolean processNBT) {
      StructurePlaceSettings settings = new StructurePlaceSettings();
      settings.setRotation((Rotation)blueprint.getOrDefault(AllDataComponents.SCHEMATIC_ROTATION, Rotation.NONE));
      settings.setMirror((Mirror)blueprint.getOrDefault(AllDataComponents.SCHEMATIC_MIRROR, Mirror.NONE));
      if (processNBT) {
         settings.addProcessor(SchematicProcessor.INSTANCE);
      }

      return settings;
   }

   public static StructureTemplate loadSchematic(Level level, ItemStack blueprint) {
      StructureTemplate t = new StructureTemplate();
      String owner = (String)blueprint.get(AllDataComponents.SCHEMATIC_OWNER);
      String schematic = (String)blueprint.get(AllDataComponents.SCHEMATIC_FILE);
      if (owner != null && schematic != null && schematic.endsWith(".nbt")) {
         Path dir;
         Path file;
         if (!level.isClientSide()) {
            dir = CreatePaths.UPLOADED_SCHEMATICS_DIR;
            file = Paths.get(owner, schematic);
         } else {
            dir = CreatePaths.SCHEMATICS_DIR;
            file = Paths.get(schematic);
         }

         Path path = dir.resolve(file).normalize();
         if (!path.startsWith(dir)) {
            return t;
         } else {
            try (DataInputStream stream = new DataInputStream(new BufferedInputStream(new GZIPInputStream(Files.newInputStream(path, StandardOpenOption.READ))))) {
               CompoundTag nbt = NbtIo.read(stream, NbtAccounter.create(536870912L));
               t.load(level.holderLookup(Registries.BLOCK), nbt);
            } catch (IOException var13) {
               LOGGER.warn("Failed to read schematic", var13);
            }

            return t;
         }
      } else {
         return t;
      }
   }

   @NotNull
   public InteractionResult useOn(UseOnContext context) {
      return context.getPlayer() != null && !this.onItemUse(context.getPlayer(), context.getHand()) ? super.useOn(context) : InteractionResult.SUCCESS;
   }

   public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
      return !this.onItemUse(playerIn, handIn)
         ? super.use(worldIn, playerIn, handIn)
         : new InteractionResultHolder(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
   }

   private boolean onItemUse(Player player, InteractionHand hand) {
      if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
         if (!player.getItemInHand(hand).has(AllDataComponents.SCHEMATIC_FILE)) {
            return false;
         } else if (!player.level().isClientSide()) {
            return true;
         } else {
            CatnipServices.PLATFORM.executeOnClientOnly(() -> this::displayBlueprintScreen);
            return true;
         }
      } else {
         return false;
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected void displayBlueprintScreen() {
      ScreenOpener.open(new SchematicEditScreen());
   }
}

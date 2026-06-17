package com.simibubi.create.foundation.utility;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class DynamicComponent {
   private JsonElement rawCustomText;
   private Component parsedCustomText;

   public void displayCustomText(Level level, BlockPos pos, String tagElement) {
      if (tagElement != null) {
         this.rawCustomText = getJsonFromString(tagElement);
         this.parsedCustomText = parseCustomText(level, pos, this.rawCustomText);
      }
   }

   public boolean sameAs(String tagElement) {
      return this.isValid() && this.rawCustomText.equals(getJsonFromString(tagElement));
   }

   public boolean isValid() {
      return this.parsedCustomText != null && this.rawCustomText != null;
   }

   public String resolve() {
      return this.parsedCustomText.getString();
   }

   public MutableComponent get() {
      return this.parsedCustomText == null ? Component.empty() : this.parsedCustomText.copy();
   }

   public void read(BlockPos pos, CompoundTag nbt, Provider registries) {
      this.rawCustomText = getJsonFromString(nbt.getString("RawCustomText"));

      try {
         this.parsedCustomText = Serializer.fromJson(nbt.getString("CustomText"), registries);
      } catch (JsonParseException var5) {
         this.parsedCustomText = null;
      }
   }

   public void write(CompoundTag nbt, Provider registries) {
      if (this.isValid()) {
         nbt.putString("RawCustomText", this.rawCustomText.toString());
         nbt.putString("CustomText", Serializer.toJson(this.parsedCustomText, registries));
      }
   }

   public static JsonElement getJsonFromString(String string) {
      try {
         return JsonParser.parseString(string);
      } catch (JsonParseException var2) {
         return null;
      }
   }

   public static Component parseCustomText(Level level, BlockPos pos, JsonElement customText) {
      if (level instanceof ServerLevel serverLevel) {
         try {
            return ComponentUtils.updateForEntity(getCommandSource(serverLevel, pos), Serializer.fromJson(customText, level.registryAccess()), null, 0);
         } catch (CommandSyntaxException | JsonParseException var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static Component parseCustomText(Level level, BlockPos pos, Component customText) {
      if (level instanceof ServerLevel serverLevel) {
         try {
            return ComponentUtils.updateForEntity(getCommandSource(serverLevel, pos), customText, null, 0);
         } catch (CommandSyntaxException | JsonParseException var5) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static CommandSourceStack getCommandSource(ServerLevel level, BlockPos pos) {
      return new CommandSourceStack(
         CommandSource.NULL, Vec3.atCenterOf(pos), Vec2.ZERO, level, 2, "create", Component.literal("create"), level.getServer(), null
      );
   }
}

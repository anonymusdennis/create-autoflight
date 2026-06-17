package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlock;
import com.simibubi.create.content.redstone.nixieTube.NixieTubeBlockEntity;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Component.Serializer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NixieTubePeripheral extends SyncedPeripheral<NixieTubeBlockEntity> {
   public NixieTubePeripheral(NixieTubeBlockEntity blockEntity) {
      super(blockEntity);
   }

   @Override
   protected void onFirstAttach() {
      super.onFirstAttach();
      Level world = this.blockEntity.getLevel();
      if (world != null) {
         NixieTubeBlock.walkNixies(world, this.blockEntity.getBlockPos(), true, (currentPos, rowPosition) -> {
            if (world.getBlockEntity(currentPos) instanceof NixieTubeBlockEntity ntbe) {
               ntbe.displayEmptyText(rowPosition);
            }
         });
      }
   }

   @Override
   protected void onLastDetach() {
      super.onLastDetach();
      Level world = this.blockEntity.getLevel();
      if (world != null) {
         BlockState state = world.getBlockState(this.blockEntity.getBlockPos());
         if (state.getBlock() instanceof NixieTubeBlock) {
            NixieTubeBlock.walkNixies(world, this.blockEntity.getBlockPos(), false, (currentPos, rowPosition) -> {
               if (world.getBlockEntity(currentPos) instanceof NixieTubeBlockEntity ntbe) {
                  NixieTubeBlock.updateDisplayedRedstoneValue(ntbe, state, true);
               }
            });
         }
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public void setText(IArguments arguments) throws LuaException {
      Level level = this.blockEntity.getLevel();
      if (level != null) {
         this.blockEntity.computerSignal = null;
         String tagElement = Serializer.toJson(Component.literal(arguments.getString(0)), level.registryAccess());
         String colour = arguments.optString(1, null);
         BlockState state = null;
         DyeColor dye = null;
         if (colour != null) {
            state = level.getBlockState(this.blockEntity.getBlockPos());
            dye = (DyeColor)LuaValues.checkEnum(1, DyeColor.class, colour.equals("grey") ? "gray" : colour);
         }

         this.changeTextNixie(tagElement, state, dye);
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public void setTextColour(String colour) throws LuaException {
      Level world = this.blockEntity.getLevel();
      if (world != null) {
         BlockState state = this.blockEntity.getLevel().getBlockState(this.blockEntity.getBlockPos());
         DyeColor dye = (DyeColor)LuaValues.checkEnum(1, DyeColor.class, colour.equals("grey") ? "gray" : colour);
         this.changeTextNixie(null, state, dye);
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public void setTextColor(String color) throws LuaException {
      this.setTextColour(color);
   }

   private void changeTextNixie(@Nullable String tagElement, @Nullable BlockState state, @Nullable DyeColor dye) {
      Level world = this.blockEntity.getLevel();
      if (world != null) {
         NixieTubeBlock.walkNixies(
            world,
            this.blockEntity.getBlockPos(),
            true,
            (currentPos, rowPosition) -> {
               if (tagElement != null) {
                  ((NixieTubeBlock)this.blockEntity.getBlockState().getBlock())
                     .withBlockEntityDo(world, currentPos, be -> be.displayCustomText(tagElement, rowPosition));
               }

               if (state != null && dye != null) {
                  world.setBlockAndUpdate(currentPos, NixieTubeBlock.withColor(state, dye));
               }
            }
         );
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public void setSignal(IArguments arguments) throws LuaException {
      if (arguments.optTable(0).isPresent()) {
         this.setSignal(this.signal().first, arguments.getTable(0));
      }

      if (arguments.optTable(1).isPresent()) {
         this.setSignal(this.signal().second, arguments.getTable(1));
      }
   }

   private void setSignal(NixieTubeBlockEntity.ComputerSignal.TubeDisplay display, @NotNull Map<?, ?> attrs) throws LuaException {
      if (attrs.containsKey("r")) {
         display.r = this.constrainByte("r", 0, 255, attrs.get("r"));
      }

      if (attrs.containsKey("g")) {
         display.g = this.constrainByte("g", 0, 255, attrs.get("g"));
      }

      if (attrs.containsKey("b")) {
         display.b = this.constrainByte("r", 0, 255, attrs.get("b"));
      }

      if (attrs.containsKey("glowWidth")) {
         display.glowWidth = this.constrainByte("glowWidth", 1, 4, attrs.get("glowWidth"));
      }

      if (attrs.containsKey("glowHeight")) {
         display.glowHeight = this.constrainByte("glowHeight", 1, 4, attrs.get("glowHeight"));
      }

      if (attrs.containsKey("blinkPeriod")) {
         display.blinkPeriod = this.constrainByte("blinkPeriod", 0, 255, attrs.get("blinkPeriod"));
      }

      if (attrs.containsKey("blinkOffTime")) {
         display.blinkOffTime = this.constrainByte("blinkOffTime", 0, 255, attrs.get("blinkOffTime"));
      }

      if (display.r == 0 && display.g == 0 && display.b == 0) {
         display.blinkPeriod = 0;
         display.blinkOffTime = 0;
      } else if (display.blinkPeriod == 0) {
         display.blinkPeriod = 1;
         display.blinkOffTime = 0;
      }

      this.blockEntity.notifyUpdate();
   }

   private byte constrainByte(String name, int min, int max, Object rawValue) throws LuaException {
      if (!(rawValue instanceof Number)) {
         throw LuaValues.badField(name, "number", LuaValues.getType(rawValue));
      } else {
         int value = ((Number)rawValue).intValue();
         if (value >= min && value <= max) {
            return (byte)value;
         } else {
            throw new LuaException("field " + name + " must be in range " + min + "-" + max);
         }
      }
   }

   private NixieTubeBlockEntity.ComputerSignal signal() {
      if (this.blockEntity.computerSignal == null) {
         this.blockEntity.computerSignal = new NixieTubeBlockEntity.ComputerSignal();
      }

      return this.blockEntity.computerSignal;
   }

   @NotNull
   public String getType() {
      return "Create_NixieTube";
   }
}

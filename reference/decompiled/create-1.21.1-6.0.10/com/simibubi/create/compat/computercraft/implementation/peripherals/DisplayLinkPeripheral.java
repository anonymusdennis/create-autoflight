package com.simibubi.create.compat.computercraft.implementation.peripherals;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.api.lua.ObjectLuaTable;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.jetbrains.annotations.NotNull;

public class DisplayLinkPeripheral extends SyncedPeripheral<DisplayLinkBlockEntity> {
   public static final String TAG_KEY = "ComputerSourceList";
   private final AtomicInteger cursorX = new AtomicInteger();
   private final AtomicInteger cursorY = new AtomicInteger();

   public DisplayLinkPeripheral(DisplayLinkBlockEntity blockEntity) {
      super(blockEntity);
   }

   @LuaFunction
   public final void setCursorPos(int x, int y) throws LuaException {
      if (x >= 1 && y >= 1) {
         this.cursorX.set(x - 1);
         this.cursorY.set(y - 1);
      } else {
         throw new LuaException("cursor position must be larger then 0");
      }
   }

   @LuaFunction
   public final Object[] getCursorPos() {
      return new Object[]{this.cursorX.get() + 1, this.cursorY.get() + 1};
   }

   @LuaFunction(
      mainThread = true
   )
   public final Object[] getSize() {
      this.blockEntity.updateGatheredData();
      DisplayTargetStats stats = this.blockEntity.activeTarget.provideStats(new DisplayLinkContext(this.blockEntity.getLevel(), this.blockEntity));
      return new Object[]{stats.maxRows(), stats.maxColumns()};
   }

   @LuaFunction
   public final boolean isColor() {
      return false;
   }

   @LuaFunction
   public final boolean isColour() {
      return false;
   }

   @LuaFunction
   public final void write(String text) {
      this.writeImpl(text);
   }

   @LuaFunction
   public final void writeBytes(IArguments args) throws LuaException {
      Object data = args.get(0);
      byte[] bytes;
      if (data instanceof String str) {
         bytes = str.getBytes(StandardCharsets.US_ASCII);
      } else {
         if (!(data instanceof Map<?, ?> map)) {
            throw LuaValues.badArgumentOf(args, 0, "string or table");
         }

         ObjectLuaTable table = new ObjectLuaTable(map);
         bytes = new byte[table.length()];

         for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)(table.getInt(i + 1) & 0xFF);
         }
      }

      this.writeImpl(new String(bytes, StandardCharsets.UTF_8));
   }

   protected final void writeImpl(String text) {
      ListTag tag = this.blockEntity.getSourceConfig().getList("ComputerSourceList", 8);
      int x = this.cursorX.get();
      int y = this.cursorY.get();

      for (int i = tag.size(); i <= y; i++) {
         tag.add(StringTag.valueOf(""));
      }

      StringBuilder builder = new StringBuilder(tag.getString(y));
      builder.append(" ".repeat(Math.max(0, x - builder.length())));
      builder.replace(x, x + text.length(), text);
      tag.set(y, StringTag.valueOf(builder.toString()));
      synchronized ((DisplayLinkBlockEntity)this.blockEntity) {
         this.blockEntity.getSourceConfig().put("ComputerSourceList", tag);
      }

      this.cursorX.set(x + text.length());
   }

   @LuaFunction
   public final void clearLine() {
      ListTag tag = this.blockEntity.getSourceConfig().getList("ComputerSourceList", 8);
      if (tag.size() > this.cursorY.get()) {
         tag.set(this.cursorY.get(), StringTag.valueOf(""));
      }

      synchronized ((DisplayLinkBlockEntity)this.blockEntity) {
         this.blockEntity.getSourceConfig().put("ComputerSourceList", tag);
      }
   }

   @LuaFunction
   public final void clear() {
      synchronized ((DisplayLinkBlockEntity)this.blockEntity) {
         this.blockEntity.getSourceConfig().put("ComputerSourceList", new ListTag());
      }
   }

   @LuaFunction(
      mainThread = true
   )
   public final void update() {
      this.blockEntity.tickSource();
   }

   @NotNull
   public String getType() {
      return "Create_DisplayLink";
   }
}

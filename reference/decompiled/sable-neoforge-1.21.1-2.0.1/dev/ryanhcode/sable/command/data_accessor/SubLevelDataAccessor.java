package dev.ryanhcode.sable.command.data_accessor;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument.NbtPath;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands.DataProvider;

public class SubLevelDataAccessor implements DataAccessor {
   public static final Function<String, DataProvider> PROVIDER = string -> new DataProvider() {
         public DataAccessor access(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
            return new SubLevelDataAccessor(SubLevelArgumentType.getSingleSubLevel(commandContext, string));
         }

         public ArgumentBuilder<CommandSourceStack, ?> wrap(
            ArgumentBuilder<CommandSourceStack, ?> argumentBuilder,
            Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> function
         ) {
            return argumentBuilder.then(Commands.literal("sub_level").then(function.apply(Commands.argument(string, SubLevelArgumentType.singleSubLevel()))));
         }
      };
   private final ServerSubLevel subLevel;

   public SubLevelDataAccessor(ServerSubLevel subLevel) {
      this.subLevel = subLevel;
   }

   public void setData(CompoundTag compoundTag) {
      this.subLevel.setUserDataTag(compoundTag);
   }

   public CompoundTag getData() {
      CompoundTag userTag = this.subLevel.getUserDataTag();
      return userTag != null ? userTag : new CompoundTag();
   }

   public Component getModifiedSuccess() {
      return Component.translatable("commands.data.sub_level.modified", new Object[]{this.subLevel.toString()});
   }

   public Component getPrintSuccess(Tag tag) {
      return Component.translatable("commands.data.sub_level.query", new Object[]{this.subLevel.toString(), NbtUtils.toPrettyComponent(tag)});
   }

   public Component getPrintSuccess(NbtPath nbtPath, double d, int i) {
      return Component.translatable(
         "commands.data.sub_level.get", new Object[]{nbtPath.asString(), this.subLevel.toString(), String.format(Locale.ROOT, "%.2f", d), i}
      );
   }
}

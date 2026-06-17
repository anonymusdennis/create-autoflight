package dev.ryanhcode.sable.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.PrimitiveIterator.OfInt;
import java.util.stream.IntStream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class SableAssembleCommands {
   public static final int DEFAULT_CONNECTED_ASSEMBLY_CAPACITY = 256000;

   public static void register(LiteralArgumentBuilder<CommandSourceStack> sableBuilder, CommandBuildContext buildContext) {
      sableBuilder.then(
         ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("assemble")
                        .then(
                           ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("shatter")
                                          .then(
                                             Commands.literal("sub_level")
                                                .then(
                                                   Commands.argument("sub_level", SubLevelArgumentType.subLevels())
                                                      .executes(SableAssembleCommands::executeShatterSubLevelCommand)
                                                )
                                          ))
                                       .then(
                                          ((LiteralArgumentBuilder)Commands.literal("connected")
                                                .executes(
                                                   ctx -> executeShatterConnected(
                                                         ctx,
                                                         BlockPos.containing(((CommandSourceStack)ctx.getSource()).getPosition().subtract(0.0, 1.0, 0.0)),
                                                         256000
                                                      )
                                                ))
                                             .then(
                                                ((RequiredArgumentBuilder)Commands.argument("from", BlockPosArgument.blockPos())
                                                      .executes(ctx -> executeShatterConnected(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "from"), 256000)))
                                                   .then(
                                                      Commands.argument("capacity", IntegerArgumentType.integer(1, 25600000))
                                                         .executes(
                                                            ctx -> executeShatterConnected(
                                                                  ctx,
                                                                  BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                                                  IntegerArgumentType.getInteger(ctx, "capacity")
                                                               )
                                                         )
                                                   )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("sphere")
                                          .then(
                                             ((RequiredArgumentBuilder)Commands.argument("radius", IntegerArgumentType.integer(0, 128))
                                                   .executes(
                                                      ctx -> executeShatterSphereCommand(
                                                            ctx, BlockPos.containing(((CommandSourceStack)ctx.getSource()).getPosition())
                                                         )
                                                   ))
                                                .then(
                                                   Commands.argument("origin", BlockPosArgument.blockPos())
                                                      .executes(ctx -> executeShatterSphereCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "origin")))
                                                )
                                          )
                                    ))
                                 .then(
                                    Commands.literal("cube")
                                       .then(
                                          ((RequiredArgumentBuilder)Commands.argument("range", IntegerArgumentType.integer(0, 128))
                                                .executes(
                                                   ctx -> executeShatterCubeCommand(
                                                         ctx, BlockPos.containing(((CommandSourceStack)ctx.getSource()).getPosition())
                                                      )
                                                ))
                                             .then(
                                                Commands.argument("origin", BlockPosArgument.blockPos())
                                                   .executes(ctx -> executeShatterCubeCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "origin")))
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("area")
                                    .then(
                                       Commands.argument("from", BlockPosArgument.blockPos())
                                          .then(Commands.argument("to", BlockPosArgument.blockPos()).executes(SableAssembleCommands::executeShatterAreaCommand))
                                    )
                              )
                        ))
                     .then(
                        Commands.literal("area")
                           .then(
                              Commands.argument("from", BlockPosArgument.blockPos())
                                 .then(Commands.argument("to", BlockPosArgument.blockPos()).executes(SableAssembleCommands::executeAssembleAreaCommand))
                           )
                     ))
                  .then(
                     ((LiteralArgumentBuilder)Commands.literal("connected")
                           .executes(
                              ctx -> executeAssembleConnectedCommand(
                                    ctx, BlockPos.containing(((CommandSourceStack)ctx.getSource()).getPosition().subtract(0.0, 1.0, 0.0)), 256000
                                 )
                           ))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("from", BlockPosArgument.blockPos())
                                 .executes(ctx -> executeAssembleConnectedCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "from"), 256000)))
                              .then(
                                 Commands.argument("capacity", IntegerArgumentType.integer(1, 25600000))
                                    .executes(
                                       ctx -> executeAssembleConnectedCommand(
                                             ctx, BlockPosArgument.getLoadedBlockPos(ctx, "from"), IntegerArgumentType.getInteger(ctx, "capacity")
                                          )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("sphere")
                     .then(
                        ((RequiredArgumentBuilder)Commands.argument("radius", IntegerArgumentType.integer(0, 256))
                              .executes(ctx -> executeAssembleSphereCommand(ctx, BlockPos.containing(((CommandSourceStack)ctx.getSource()).getPosition()))))
                           .then(
                              Commands.argument("origin", BlockPosArgument.blockPos())
                                 .executes(ctx -> executeAssembleSphereCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "origin")))
                           )
                     )
               ))
            .then(
               Commands.literal("cube")
                  .then(
                     ((RequiredArgumentBuilder)Commands.argument("range", IntegerArgumentType.integer(0, 256))
                           .executes(ctx -> executeAssembleCubeCommand(ctx, BlockPos.containing(((CommandSourceStack)ctx.getSource()).getPosition()))))
                        .then(
                           Commands.argument("origin", BlockPosArgument.blockPos())
                              .executes(ctx -> executeAssembleCubeCommand(ctx, BlockPosArgument.getLoadedBlockPos(ctx, "origin")))
                        )
                  )
            )
      );
   }

   private static int executeShatterConnected(CommandContext<CommandSourceStack> ctx, BlockPos assemblyOrigin, int assemblyCapacity) throws CommandSyntaxException {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      SubLevelAssemblyHelper.GatherResult result = SubLevelAssemblyHelper.gatherConnectedBlocks(assemblyOrigin, level, assemblyCapacity, null);
      if (result.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable(switch (result.assemblyState()) {
            case TOO_MANY_BLOCKS -> "commands.sable.sub_level.shatter.connected.too_many_blocks";
            case NO_BLOCKS -> "commands.sable.sub_level.shatter.no_blocks";
            default -> throw new IllegalStateException("Unexpected value: " + result.assemblyState());
         }, new Object[]{result.assemblyState() == SubLevelAssemblyHelper.GatherResult.State.TOO_MANY_BLOCKS ? assemblyCapacity : 0}));
         return 0;
      } else {
         int blocksShattered = shatterBlocks(result.blocks(), level);
         if (blocksShattered == 0) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.sable.sub_level.shatter.no_blocks"));
            return 0;
         } else {
            ((CommandSourceStack)ctx.getSource())
               .sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.connected.success", new Object[]{blocksShattered}), true);
            return blocksShattered;
         }
      }
   }

   private static int executeShatterSubLevelCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      Collection<ServerSubLevel> subLevels = SubLevelArgumentType.getSubLevels(ctx, "sub_level");
      if (subLevels.isEmpty()) {
         throw SableCommandHelper.ERROR_NO_SUB_LEVELS_FOUND.create();
      } else {
         IntStream shatteredAmounts = subLevels.stream().filter(subLevel -> {
            int solidBlockCount = 0;
            Iterator<BlockPos> it = BlockPos.betweenClosedStream(subLevel.getPlot().getBoundingBox().toMojang()).iterator();

            while (it.hasNext()) {
               BlockPos pos = it.next();
               if (VoxelNeighborhoodState.isSolid(level, pos, level.getBlockState(pos))) {
                  if (++solidBlockCount > 1) {
                     return true;
                  }
               }
            }

            return false;
         }).map(subLevel -> subLevel.getPlot().getBoundingBox()).mapToInt(bounds -> shatterBoundingBox(bounds, level));
         int blocksShattered = 0;
         int sublevelsShattered = 0;

         for (OfInt it = shatteredAmounts.iterator(); it.hasNext(); sublevelsShattered++) {
            int i = it.next();
            blocksShattered += i;
         }

         if (sublevelsShattered == 0) {
            ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.sable.sub_level.shatter.sub_level.only_single_block"));
            return 0;
         } else {
            int finalSublevelsShattered = sublevelsShattered;
            int finalBlocksShattered = blocksShattered;
            if (sublevelsShattered == 1) {
               ((CommandSourceStack)ctx.getSource())
                  .sendSuccess(
                     () -> Component.translatable(
                           "commands.sable.sub_level.shatter.sub_level.success",
                           new Object[]{Component.translatable("commands.sable.sub_level"), finalBlocksShattered}
                        ),
                     true
                  );
            } else {
               ((CommandSourceStack)ctx.getSource())
                  .sendSuccess(
                     () -> Component.translatable(
                           "commands.sable.sub_level.shatter.sub_level.success",
                           new Object[]{Component.translatable("commands.sable.sub_levels", new Object[]{finalSublevelsShattered}), finalBlocksShattered}
                        ),
                     true
                  );
            }

            return blocksShattered;
         }
      }
   }

   private static int executeShatterAreaCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      BoundingBox3i boundingBox = new BoundingBox3i(BlockPosArgument.getLoadedBlockPos(ctx, "from"), BlockPosArgument.getLoadedBlockPos(ctx, "to"));
      int blocksShattered = shatterBoundingBox(boundingBox, level);
      if (blocksShattered == 0) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.sable.sub_level.shatter.no_blocks"));
         return 0;
      } else {
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.region.success", new Object[]{blocksShattered}), true);
         return blocksShattered;
      }
   }

   private static int executeShatterSphereCommand(CommandContext<CommandSourceStack> ctx, BlockPos origin) {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      int radius = IntegerArgumentType.getInteger(ctx, "radius");
      BoundingBox boundingBox = BoundingBox.fromCorners(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius));
      int radiusSquared = radius * radius;
      List<BlockPos> blocks = BlockPos.betweenClosedStream(boundingBox).<BlockPos>map(BlockPos::immutable).toList();
      List<BlockPos> blocksInRadius = new ArrayList<>();

      for (BlockPos blockPos : blocks) {
         if (!(origin.distSqr(blockPos) > (double)radiusSquared)) {
            blocksInRadius.add(blockPos);
         }
      }

      int blocksShattered = shatterBlocks(blocksInRadius, level);
      if (blocksShattered == 0) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.sable.sub_level.shatter.no_blocks"));
         return 0;
      } else {
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.radius.success", new Object[]{blocksShattered}), true);
         return blocksShattered;
      }
   }

   private static int executeShatterCubeCommand(CommandContext<CommandSourceStack> ctx, BlockPos origin) {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      int radius = IntegerArgumentType.getInteger(ctx, "range");
      BoundingBox3i boundingBox = new BoundingBox3i(origin.offset(-radius, -radius, -radius), origin.offset(radius, radius, radius));
      int blocksShattered = shatterBoundingBox(boundingBox, level);
      if (blocksShattered == 0) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.sable.sub_level.shatter.no_blocks"));
         return 0;
      } else {
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.sable.sub_level.shatter.range.success", new Object[]{blocksShattered}), true);
         return blocksShattered;
      }
   }

   private static int shatterBoundingBox(BoundingBox3ic boundingBox, ServerLevel level) {
      return shatterBlocks(BlockPos.betweenClosedStream(boundingBox.toMojang()).<BlockPos>map(BlockPos::immutable).toList(), level);
   }

   private static int shatterBlocks(Collection<BlockPos> blocks, ServerLevel level) {
      for (BlockPos pos : blocks) {
         if (!VoxelNeighborhoodState.isSolid(level, pos, level.getBlockState(pos))) {
            level.destroyBlock(pos, true);
         }
      }

      int shattered = 0;

      for (BlockPos anchor : blocks) {
         if (shatterBlockToSubLevel(level, anchor)) {
            shattered++;
         }
      }

      return shattered;
   }

   private static boolean shatterBlockToSubLevel(ServerLevel level, BlockPos anchor) {
      if (!VoxelNeighborhoodState.isSolid(level, anchor, level.getBlockState(anchor))) {
         return false;
      } else {
         BoundingBox3i bounds = new BoundingBox3i(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX() + 1, anchor.getY() + 1, anchor.getZ() + 1);
         bounds.set(bounds.minX - 1, bounds.minY - 1, bounds.minZ - 1, bounds.maxX + 1, bounds.maxY + 1, bounds.maxZ + 1);
         SubLevelAssemblyHelper.assembleBlocks(level, anchor, List.of(anchor), bounds);
         return true;
      }
   }

   private static int executeAssembleAreaCommand(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      BoundingBox boundingBox = BoundingBox.fromCorners(BlockPosArgument.getLoadedBlockPos(ctx, "from"), BlockPosArgument.getLoadedBlockPos(ctx, "to"));
      List<BlockPos> blocks = BlockPos.betweenClosedStream(boundingBox).<BlockPos>map(BlockPos::immutable).toList();
      BlockPos anchor = blocks.getFirst();
      BoundingBox3i bounds = new BoundingBox3i(boundingBox);
      bounds.set(bounds.minX - 1, bounds.minY - 1, bounds.minZ - 1, bounds.maxX + 1, bounds.maxY + 1, bounds.maxZ + 1);
      ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, bounds);
      if (subLevel.getMassTracker().isInvalid()) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.sable.sub_level.assemble.no_blocks"));
         return 0;
      } else {
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.sable.sub_level.assemble.region.success", new Object[]{blocks.size()}), true);
         return 1;
      }
   }

   private static int executeAssembleCubeCommand(CommandContext<CommandSourceStack> ctx, BlockPos origin) {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      int range = IntegerArgumentType.getInteger(ctx, "range");
      BoundingBox boundingBox = BoundingBox.fromCorners(origin.offset(-range, -range, -range), origin.offset(range, range, range));
      List<BlockPos> blocks = BlockPos.betweenClosedStream(boundingBox).<BlockPos>map(BlockPos::immutable).toList();
      BlockPos anchor = blocks.getFirst();
      BoundingBox3i bounds = new BoundingBox3i(boundingBox);
      bounds.set(bounds.minX - 1, bounds.minY - 1, bounds.minZ - 1, bounds.maxX + 1, bounds.maxY + 1, bounds.maxZ + 1);
      ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, bounds);
      if (subLevel.getMassTracker().isInvalid()) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.sable.sub_level.assemble.no_blocks"));
         return 0;
      } else {
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.sable.sub_level.assemble.range.success", new Object[]{blocks.size()}), true);
         return 1;
      }
   }

   private static int executeAssembleConnectedCommand(CommandContext<CommandSourceStack> ctx, BlockPos assemblyOrigin, int assemblyCapacity) throws CommandSyntaxException {
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      SubLevelAssemblyHelper.GatherResult result = SubLevelAssemblyHelper.gatherConnectedBlocks(assemblyOrigin, level, assemblyCapacity, null);
      if (result.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS) {
         ((CommandSourceStack)ctx.getSource())
            .sendFailure(
               Component.translatable(
                  result.assemblyState().errorKey,
                  new Object[]{result.assemblyState() == SubLevelAssemblyHelper.GatherResult.State.TOO_MANY_BLOCKS ? assemblyCapacity : 0}
               )
            );
         return 0;
      } else {
         SubLevelAssemblyHelper.assembleBlocks(level, assemblyOrigin, result.blocks(), result.boundingBox());
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.sable.sub_level.assemble.connected.success", new Object[]{result.blocks().size()}), true);
         return 1;
      }
   }

   private static int executeAssembleSphereCommand(CommandContext<CommandSourceStack> ctx, BlockPos origin) {
      int radius = IntegerArgumentType.getInteger(ctx, "radius");
      ServerLevel level = ((CommandSourceStack)ctx.getSource()).getLevel();
      Set<BlockPos> blocks = new HashSet<>();
      int minX = Integer.MAX_VALUE;
      int minY = Integer.MAX_VALUE;
      int minZ = Integer.MAX_VALUE;
      int maxX = Integer.MIN_VALUE;
      int maxY = Integer.MIN_VALUE;
      int maxZ = Integer.MIN_VALUE;
      int radiusSquared = radius * radius;

      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
               if (x * x + y * y + z * z <= radiusSquared) {
                  BlockPos pos = origin.offset(x, y, z);
                  if (level.isLoaded(pos) && !level.getBlockState(pos).isAir()) {
                     blocks.add(pos);
                     minX = Math.min(minX, pos.getX());
                     minY = Math.min(minY, pos.getY());
                     minZ = Math.min(minZ, pos.getZ());
                     maxX = Math.max(maxX, pos.getX());
                     maxY = Math.max(maxY, pos.getY());
                     maxZ = Math.max(maxZ, pos.getZ());
                  }
               }
            }
         }
      }

      if (blocks.isEmpty()) {
         ((CommandSourceStack)ctx.getSource()).sendFailure(Component.translatable("commands.sable.sub_level.assemble.no_blocks"));
         return 0;
      } else {
         BoundingBox3i bounds = new BoundingBox3i(minX, minY, minZ, maxX, maxY, maxZ);
         SubLevelAssemblyHelper.assembleBlocks(level, origin, blocks, bounds);
         int finalBlocksCount = blocks.size();
         ((CommandSourceStack)ctx.getSource())
            .sendSuccess(() -> Component.translatable("commands.sable.sub_level.assemble.radius.success", new Object[]{finalBlocksCount}), true);
         return 1;
      }
   }
}

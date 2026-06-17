package com.simibubi.create.infrastructure.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class CloneCommand {
   private static final Dynamic2CommandExceptionType CLONE_TOO_BIG_EXCEPTION = new Dynamic2CommandExceptionType(
      (arg1, arg2) -> Component.translatable("commands.clone.toobig", new Object[]{arg1, arg2})
   );

   public static ArgumentBuilder<CommandSourceStack, ?> register() {
      return ((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("clone").requires(cs -> cs.hasPermission(2)))
            .then(
               Commands.argument("begin", BlockPosArgument.blockPos())
                  .then(
                     Commands.argument("end", BlockPosArgument.blockPos())
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("destination", BlockPosArgument.blockPos())
                                 .then(
                                    Commands.literal("skipBlocks")
                                       .executes(
                                          ctx -> doClone(
                                                (CommandSourceStack)ctx.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "begin"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "end"),
                                                BlockPosArgument.getLoadedBlockPos(ctx, "destination"),
                                                false
                                             )
                                       )
                                 ))
                              .executes(
                                 ctx -> doClone(
                                       (CommandSourceStack)ctx.getSource(),
                                       BlockPosArgument.getLoadedBlockPos(ctx, "begin"),
                                       BlockPosArgument.getLoadedBlockPos(ctx, "end"),
                                       BlockPosArgument.getLoadedBlockPos(ctx, "destination"),
                                       true
                                    )
                              )
                        )
                  )
            ))
         .executes(
            ctx -> {
               ((CommandSourceStack)ctx.getSource())
                  .sendSuccess(() -> Component.literal("Clones all blocks as well as super glue from the specified area to the target destination"), true);
               return 1;
            }
         );
   }

   private static int doClone(CommandSourceStack source, BlockPos begin, BlockPos end, BlockPos destination, boolean cloneBlocks) throws CommandSyntaxException {
      BoundingBox sourceArea = BoundingBox.fromCorners(begin, end);
      BlockPos destinationEnd = destination.offset(sourceArea.getLength());
      BoundingBox destinationArea = BoundingBox.fromCorners(destination, destinationEnd);
      ServerLevel world = source.getLevel();
      int i = sourceArea.getXSpan() * sourceArea.getYSpan() * sourceArea.getZSpan();
      int limit = world.getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
      if (i > limit) {
         throw CLONE_TOO_BIG_EXCEPTION.create(limit, i);
      } else if (world.hasChunksAt(begin, end) && world.hasChunksAt(destination, destinationEnd)) {
         BlockPos diffToTarget = new BlockPos(
            destinationArea.minX() - sourceArea.minX(), destinationArea.minY() - sourceArea.minY(), destinationArea.minZ() - sourceArea.minZ()
         );
         int blockPastes = cloneBlocks ? cloneBlocks(sourceArea, world, diffToTarget) : 0;
         int gluePastes = cloneGlue(sourceArea, world, diffToTarget);
         if (cloneBlocks) {
            source.sendSuccess(() -> Component.literal("Successfully cloned " + blockPastes + " Blocks"), true);
         }

         source.sendSuccess(() -> Component.literal("Successfully applied glue " + gluePastes + " times"), true);
         return blockPastes + gluePastes;
      } else {
         throw BlockPosArgument.ERROR_NOT_LOADED.create();
      }
   }

   private static int cloneGlue(BoundingBox sourceArea, ServerLevel world, BlockPos diffToTarget) {
      int gluePastes = 0;
      AABB bb = new AABB(
         (double)sourceArea.minX(),
         (double)sourceArea.minY(),
         (double)sourceArea.minZ(),
         (double)(sourceArea.maxX() + 1),
         (double)(sourceArea.maxY() + 1),
         (double)(sourceArea.maxZ() + 1)
      );

      for (SuperGlueEntity g : SuperGlueEntity.collectCropped(world, bb)) {
         g.setPos(g.position().add(Vec3.atLowerCornerOf(diffToTarget)));
         world.addFreshEntity(g);
         gluePastes++;
      }

      return gluePastes;
   }

   private static int cloneBlocks(BoundingBox sourceArea, ServerLevel world, BlockPos diffToTarget) {
      int blockPastes = 0;
      List<StructureBlockInfo> blocks = Lists.newArrayList();
      List<StructureBlockInfo> beBlocks = Lists.newArrayList();

      for (int z = sourceArea.minZ(); z <= sourceArea.maxZ(); z++) {
         for (int y = sourceArea.minY(); y <= sourceArea.maxY(); y++) {
            for (int x = sourceArea.minX(); x <= sourceArea.maxX(); x++) {
               BlockPos currentPos = new BlockPos(x, y, z);
               BlockPos newPos = currentPos.offset(diffToTarget);
               BlockInWorld cached = new BlockInWorld(world, currentPos, false);
               BlockState state = cached.getState();
               BlockEntity be = world.getBlockEntity(currentPos);
               if (be != null) {
                  CompoundTag nbt = be.saveWithFullMetadata(world.registryAccess());
                  beBlocks.add(new StructureBlockInfo(newPos, state, nbt));
               } else {
                  blocks.add(new StructureBlockInfo(newPos, state, null));
               }
            }
         }
      }

      List<StructureBlockInfo> allBlocks = Lists.newArrayList();
      allBlocks.addAll(blocks);
      allBlocks.addAll(beBlocks);
      List<StructureBlockInfo> reverse = Lists.reverse(allBlocks);

      for (StructureBlockInfo info : reverse) {
         BlockEntity be = world.getBlockEntity(info.pos());
         Clearable.tryClear(be);
         world.setBlock(info.pos(), Blocks.BARRIER.defaultBlockState(), 2);
      }

      for (StructureBlockInfo info : allBlocks) {
         if (world.setBlock(info.pos(), info.state(), 2)) {
            blockPastes++;
         }
      }

      for (StructureBlockInfo infox : beBlocks) {
         BlockEntity be = world.getBlockEntity(infox.pos());
         if (be != null && infox.nbt() != null) {
            infox.nbt().putInt("x", infox.pos().getX());
            infox.nbt().putInt("y", infox.pos().getY());
            infox.nbt().putInt("z", infox.pos().getZ());
            be.loadWithComponents(infox.nbt(), world.registryAccess());
            be.setChanged();
         }

         world.setBlock(infox.pos(), infox.state(), 2);
      }

      for (StructureBlockInfo infox : reverse) {
         world.blockUpdated(infox.pos(), infox.state().getBlock());
      }

      world.getBlockTicks().copyArea(sourceArea, diffToTarget);
      return blockPastes;
   }
}

package com.simibubi.create.content.contraptions.actors.roller;

import com.simibubi.create.AllTags;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.pulley.PulleyContraption;
import com.simibubi.create.content.contraptions.render.ActorVisual;
import com.simibubi.create.content.contraptions.render.ContraptionMatrices;
import com.simibubi.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.bogey.StandardBogeyBlock;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.CarriageBogey;
import com.simibubi.create.content.trains.entity.CarriageContraptionEntity;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.entity.TravellingPoint;
import com.simibubi.create.content.trains.graph.TrackEdge;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.simibubi.create.foundation.damageTypes.CreateDamageSources;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class RollerMovementBehaviour extends BlockBreakingMovementBehaviour {
   RollerMovementBehaviour.RollerTravellingPoint rollerScout = new RollerMovementBehaviour.RollerTravellingPoint();

   @Override
   public boolean isActive(MovementContext context) {
      return super.isActive(context)
         && !(context.contraption instanceof PulleyContraption)
         && VecHelper.isVecPointingTowards(context.relativeMotion, (Direction)context.state.getValue(RollerBlock.FACING));
   }

   @Override
   public boolean disableBlockEntityRendering() {
      return true;
   }

   @Nullable
   @Override
   public ActorVisual createVisual(VisualizationContext visualizationContext, VirtualRenderWorld simulationWorld, MovementContext movementContext) {
      return new RollerActorVisual(visualizationContext, simulationWorld, movementContext);
   }

   @Override
   public void renderInContraption(MovementContext context, VirtualRenderWorld renderWorld, ContraptionMatrices matrices, MultiBufferSource buffers) {
      if (!VisualizationManager.supportsVisualization(context.world)) {
         RollerRenderer.renderInContraption(context, renderWorld, matrices, buffers);
      }
   }

   @Override
   public Vec3 getActiveAreaOffset(MovementContext context) {
      return Vec3.atLowerCornerOf(((Direction)context.state.getValue(RollerBlock.FACING)).getNormal()).scale(0.45).subtract(0.0, 2.0, 0.0);
   }

   @Override
   protected float getBlockBreakingSpeed(MovementContext context) {
      return Mth.clamp(super.getBlockBreakingSpeed(context) * 1.5F, 0.0078125F, 16.0F);
   }

   @Override
   public boolean canBreak(Level world, BlockPos breakingPos, BlockState state) {
      for (Direction side : Iterate.directions) {
         if (world.getBlockState(breakingPos.relative(side)).is(BlockTags.PORTALS)) {
            return false;
         }
      }

      return super.canBreak(world, breakingPos, state) && !state.getCollisionShape(world, breakingPos).isEmpty() && !AllTags.AllBlockTags.TRACKS.matches(state);
   }

   @Override
   protected DamageSource getDamageSource(Level level) {
      return CreateDamageSources.roller(level);
   }

   @Override
   public void visitNewPosition(MovementContext context, BlockPos pos) {
      Level world = context.world;
      BlockState stateVisited = world.getBlockState(pos);
      if (!stateVisited.isRedstoneConductor(world, pos)) {
         this.damageEntities(context, pos, world);
      }

      if (!world.isClientSide) {
         List<BlockPos> positionsToBreak = this.getPositionsToBreak(context, pos);
         if (positionsToBreak.isEmpty()) {
            this.triggerPaver(context, pos);
         } else {
            BlockPos argMax = null;
            double max = -1.0;

            for (BlockPos toBreak : positionsToBreak) {
               float hardness = context.world.getBlockState(toBreak).getDestroySpeed(world, toBreak);
               if (!((double)hardness < max)) {
                  max = (double)hardness;
                  argMax = toBreak;
               }
            }

            if (argMax == null) {
               this.triggerPaver(context, pos);
            } else {
               context.data.put("ReferencePos", NbtUtils.writeBlockPos(pos));
               context.data.put("BreakingPos", NbtUtils.writeBlockPos(argMax));
               context.stall = true;
            }
         }
      }
   }

   @Override
   protected void onBlockBroken(MovementContext context, BlockPos pos, BlockState brokenState) {
      super.onBlockBroken(context, pos, brokenState);
      if (context.data.contains("ReferencePos")) {
         BlockPos referencePos = NBTHelper.readBlockPos(context.data, "ReferencePos");

         for (BlockPos otherPos : this.getPositionsToBreak(context, referencePos)) {
            if (!otherPos.equals(pos)) {
               this.destroyBlock(context, otherPos);
            }
         }

         this.triggerPaver(context, referencePos);
         context.data.remove("ReferencePos");
      }
   }

   @Override
   protected void destroyBlock(MovementContext context, BlockPos breakingPos) {
      BlockState blockState = context.world.getBlockState(breakingPos);
      boolean noHarvest = blockState.is(BlockTags.NEEDS_IRON_TOOL) || blockState.is(BlockTags.NEEDS_STONE_TOOL) || blockState.is(BlockTags.NEEDS_DIAMOND_TOOL);
      BlockHelper.destroyBlock(context.world, breakingPos, 1.0F, stack -> {
         if (!noHarvest && !context.world.random.nextBoolean()) {
            this.collectOrDropItem(context, stack);
         }
      });
      super.destroyBlock(context, breakingPos);
   }

   protected List<BlockPos> getPositionsToBreak(MovementContext context, BlockPos visitedPos) {
      ArrayList<BlockPos> positions = new ArrayList<>();
      RollerBlockEntity.RollingMode mode = this.getMode(context);
      if (mode != RollerBlockEntity.RollingMode.TUNNEL_PAVE) {
         return positions;
      } else {
         int startingY = 1;
         if (!this.getStateToPaveWith(context).isAir()) {
            FilterItemStack filter = context.getFilterFromBE();
            if (!ItemHelper.extract(context.contraption.getStorage().getAllItems(), stack -> filter.test(context.world, stack), 1, true).isEmpty()) {
               startingY = 0;
            }
         }

         PaveTask profileForTracks = this.createHeightProfileForTracks(context);
         if (profileForTracks == null) {
            for (int i = startingY; i <= 2; i++) {
               if (this.testBreakerTarget(context, visitedPos.above(i), i)) {
                  positions.add(visitedPos.above(i));
               }
            }

            return positions;
         } else {
            for (Couple<Integer> coords : profileForTracks.keys()) {
               float height = profileForTracks.get(coords);
               BlockPos targetPosition = BlockPos.containing(
                  (double)((Integer)coords.getFirst()).intValue(), (double)height, (double)((Integer)coords.getSecond()).intValue()
               );
               boolean shouldPlaceSlab = (double)height > Math.floor((double)height) + 0.45;
               if (startingY == 1
                  && shouldPlaceSlab
                  && context.world.getBlockState(targetPosition.above()).getOptionalValue(SlabBlock.TYPE).orElse(SlabType.DOUBLE) == SlabType.BOTTOM) {
                  startingY = 2;
               }

               for (int ix = startingY; ix <= (shouldPlaceSlab ? 3 : 2); ix++) {
                  if (this.testBreakerTarget(context, targetPosition.above(ix), ix)) {
                     positions.add(targetPosition.above(ix));
                  }
               }
            }

            return positions;
         }
      }
   }

   protected boolean testBreakerTarget(MovementContext context, BlockPos target, int columnY) {
      BlockState stateToPaveWith = this.getStateToPaveWith(context);
      BlockState stateToPaveWithAsSlab = this.getStateToPaveWithAsSlab(context);
      BlockState stateAbove = context.world.getBlockState(target);
      if (columnY == 0 && stateAbove.is(stateToPaveWith.getBlock())) {
         return false;
      } else {
         return stateToPaveWithAsSlab != null && columnY == 1 && stateAbove.is(stateToPaveWithAsSlab.getBlock())
            ? false
            : this.canBreak(context.world, target, stateAbove);
      }
   }

   @Nullable
   protected PaveTask createHeightProfileForTracks(MovementContext context) {
      if (context.contraption == null) {
         return null;
      } else if (context.contraption.entity instanceof CarriageContraptionEntity cce) {
         Carriage carriage = cce.getCarriage();
         if (carriage == null) {
            return null;
         } else {
            Train train = carriage.train;
            if (train != null && train.graph != null) {
               CarriageBogey mainBogey = (CarriageBogey)carriage.bogeys.getFirst();
               TravellingPoint point = mainBogey.trailing();
               this.rollerScout.node1 = point.node1;
               this.rollerScout.node2 = point.node2;
               this.rollerScout.edge = point.edge;
               this.rollerScout.position = point.position;
               Axis axis = Axis.X;
               StructureBlockInfo info = context.contraption.getBlocks().get(BlockPos.ZERO);
               if (info != null && info.state().hasProperty(StandardBogeyBlock.AXIS)) {
                  axis = (Axis)info.state().getValue(StandardBogeyBlock.AXIS);
               }

               Direction orientation = cce.getInitialOrientation();
               Direction rollerFacing = (Direction)context.state.getValue(RollerBlock.FACING);
               int step = orientation.getAxisDirection().getStep();
               double widthWiseOffset = (double)(axis.choose(-context.localPos.getZ(), 0, -context.localPos.getX()) * step);
               double lengthWiseOffset = (double)(axis.choose(-context.localPos.getX(), 0, context.localPos.getZ()) * step - 1);
               if (rollerFacing == orientation.getClockWise()) {
                  lengthWiseOffset++;
               }

               double distanceToTravel = 2.0;
               PaveTask heightProfile = new PaveTask(widthWiseOffset, widthWiseOffset);
               TravellingPoint.ITrackSelector steering = this.rollerScout.steer(TravellingPoint.SteerDirection.NONE, new Vec3(0.0, 1.0, 0.0));
               this.rollerScout.traversalCallback = (edge, coords) -> {
               };
               this.rollerScout.travel(train.graph, lengthWiseOffset + 1.0, steering);
               this.rollerScout.traversalCallback = (edge, coords) -> {
                  if (edge != null) {
                     if (!edge.isInterDimensional()) {
                        if (edge.node1.getLocation().dimension == context.world.dimension()) {
                           TrackPaverV2.pave(heightProfile, train.graph, edge, (Double)coords.getFirst(), (Double)coords.getSecond());
                        }
                     }
                  }
               };
               this.rollerScout.travel(train.graph, distanceToTravel, steering);

               for (Couple<Integer> entry : heightProfile.keys()) {
                  heightProfile.put((Integer)entry.getFirst(), (Integer)entry.getSecond(), (float)context.localPos.getY() + heightProfile.get(entry));
               }

               return heightProfile;
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   protected void triggerPaver(MovementContext context, BlockPos pos) {
      BlockState stateToPaveWith = this.getStateToPaveWith(context);
      BlockState stateToPaveWithAsSlab = this.getStateToPaveWithAsSlab(context);
      RollerBlockEntity.RollingMode mode = this.getMode(context);
      if (mode == RollerBlockEntity.RollingMode.TUNNEL_PAVE || !stateToPaveWith.isAir()) {
         Vec3 directionVec = Vec3.atLowerCornerOf(((Direction)context.state.getValue(RollerBlock.FACING)).getClockWise().getNormal());
         directionVec = context.rotation.apply(directionVec);
         RollerMovementBehaviour.PaveResult paveResult = RollerMovementBehaviour.PaveResult.PASS;
         int yOffset = 0;
         List<Pair<BlockPos, Boolean>> paveSet = new ArrayList<>();
         PaveTask profileForTracks = this.createHeightProfileForTracks(context);
         if (profileForTracks == null) {
            paveSet.add(Pair.of(pos, false));
         } else {
            for (Couple<Integer> coords : profileForTracks.keys()) {
               float height = profileForTracks.get(coords);
               boolean shouldPlaceSlab = (double)height > Math.floor((double)height) + 0.45;
               BlockPos targetPosition = BlockPos.containing(
                  (double)((Integer)coords.getFirst()).intValue(), (double)height, (double)((Integer)coords.getSecond()).intValue()
               );
               paveSet.add(Pair.of(targetPosition, shouldPlaceSlab));
            }
         }

         if (!paveSet.isEmpty()) {
            while (paveResult == RollerMovementBehaviour.PaveResult.PASS) {
               if (yOffset > (Integer)AllConfigs.server().kinetics.rollerFillDepth.get()) {
                  paveResult = RollerMovementBehaviour.PaveResult.FAIL;
                  break;
               }

               Set<Pair<BlockPos, Boolean>> currentLayer = new HashSet<>();
               if (mode == RollerBlockEntity.RollingMode.WIDE_FILL) {
                  for (Pair<BlockPos, Boolean> anchor : paveSet) {
                     int radius = (yOffset + 1) / 2;

                     for (int i = -radius; i <= radius; i++) {
                        for (int j = -radius; j <= radius; j++) {
                           if (BlockPos.ZERO.distManhattan(new BlockPos(i, 0, j)) <= radius) {
                              currentLayer.add(Pair.of(((BlockPos)anchor.getFirst()).offset(i, -yOffset, j), (Boolean)anchor.getSecond()));
                           }
                        }
                     }
                  }
               } else {
                  for (Pair<BlockPos, Boolean> anchor : paveSet) {
                     currentLayer.add(Pair.of(((BlockPos)anchor.getFirst()).below(yOffset), (Boolean)anchor.getSecond()));
                  }
               }

               boolean completelyBlocked = true;
               boolean anyBlockPlaced = false;

               for (Pair<BlockPos, Boolean> currentPos : currentLayer) {
                  if (stateToPaveWithAsSlab != null && yOffset == 0 && (Boolean)currentPos.getSecond()) {
                     this.tryFill(context, ((BlockPos)currentPos.getFirst()).above(), stateToPaveWithAsSlab);
                  }

                  paveResult = this.tryFill(context, (BlockPos)currentPos.getFirst(), stateToPaveWith);
                  if (paveResult != RollerMovementBehaviour.PaveResult.FAIL) {
                     completelyBlocked = false;
                  }

                  if (paveResult == RollerMovementBehaviour.PaveResult.SUCCESS) {
                     anyBlockPlaced = true;
                  }
               }

               if (anyBlockPlaced) {
                  paveResult = RollerMovementBehaviour.PaveResult.SUCCESS;
               } else if (!completelyBlocked || yOffset == 0) {
                  paveResult = RollerMovementBehaviour.PaveResult.PASS;
               }

               if (paveResult == RollerMovementBehaviour.PaveResult.SUCCESS && stateToPaveWith.getBlock() instanceof FallingBlock) {
                  paveResult = RollerMovementBehaviour.PaveResult.PASS;
               }

               if (paveResult != RollerMovementBehaviour.PaveResult.PASS || mode == RollerBlockEntity.RollingMode.TUNNEL_PAVE) {
                  break;
               }

               yOffset++;
            }

            if (paveResult == RollerMovementBehaviour.PaveResult.SUCCESS) {
               context.data.putInt("WaitingTicks", 2);
               context.data.put("LastPos", NbtUtils.writeBlockPos(pos));
               context.stall = true;
            }
         }
      }
   }

   public static BlockState getStateToPaveWith(ItemStack itemStack) {
      if (itemStack.getItem() instanceof BlockItem bi) {
         BlockState defaultBlockState = bi.getBlock().defaultBlockState();
         if (defaultBlockState.hasProperty(SlabBlock.TYPE)) {
            defaultBlockState = (BlockState)defaultBlockState.setValue(SlabBlock.TYPE, SlabType.DOUBLE);
         }

         return defaultBlockState;
      } else {
         return Blocks.AIR.defaultBlockState();
      }
   }

   protected BlockState getStateToPaveWith(MovementContext context) {
      return getStateToPaveWith(ItemStack.parseOptional(context.world.registryAccess(), context.blockEntityData.getCompound("Filter")));
   }

   protected BlockState getStateToPaveWithAsSlab(MovementContext context) {
      BlockState stateToPaveWith = this.getStateToPaveWith(context);
      if (stateToPaveWith.hasProperty(SlabBlock.TYPE)) {
         return (BlockState)stateToPaveWith.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
      } else {
         Block block = stateToPaveWith.getBlock();
         if (block == null) {
            return null;
         } else {
            ResourceLocation rl = BuiltInRegistries.BLOCK.getKey(block);
            String namespace = rl.getNamespace();
            String blockName = rl.getPath();
            int nameLength = blockName.length();
            List<String> possibleSlabLocations = new ArrayList<>();
            possibleSlabLocations.add(blockName + "_slab");
            if (blockName.endsWith("s") && nameLength > 1) {
               possibleSlabLocations.add(blockName.substring(0, nameLength - 1) + "_slab");
            }

            if (blockName.endsWith("planks") && nameLength > 7) {
               possibleSlabLocations.add(blockName.substring(0, nameLength - 7) + "_slab");
            }

            for (String locationAttempt : possibleSlabLocations) {
               Optional<Block> result = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.fromNamespaceAndPath(namespace, locationAttempt));
               if (!result.isEmpty()) {
                  return result.get().defaultBlockState();
               }
            }

            return null;
         }
      }
   }

   protected RollerBlockEntity.RollingMode getMode(MovementContext context) {
      return RollerBlockEntity.RollingMode.values()[context.blockEntityData.getInt("ScrollValue")];
   }

   protected RollerMovementBehaviour.PaveResult tryFill(MovementContext context, BlockPos targetPos, BlockState toPlace) {
      Level level = context.world;
      if (!level.isLoaded(targetPos)) {
         return RollerMovementBehaviour.PaveResult.FAIL;
      } else {
         BlockState existing = level.getBlockState(targetPos);
         if (existing.is(toPlace.getBlock())) {
            return RollerMovementBehaviour.PaveResult.PASS;
         } else if (existing.is(BlockTags.LEAVES)
            || existing.canBeReplaced()
            || existing.getCollisionShape(level, targetPos).isEmpty() && !existing.is(BlockTags.PORTALS)) {
            FilterItemStack filter = context.getFilterFromBE();
            ItemStack held = ItemHelper.extract(context.contraption.getStorage().getAllItems(), stack -> filter.test(context.world, stack), 1, false);
            if (held.isEmpty()) {
               return RollerMovementBehaviour.PaveResult.FAIL;
            } else {
               level.setBlockAndUpdate(targetPos, toPlace);
               return RollerMovementBehaviour.PaveResult.SUCCESS;
            }
         } else {
            return RollerMovementBehaviour.PaveResult.FAIL;
         }
      }
   }

   private static enum PaveResult {
      FAIL,
      PASS,
      SUCCESS;
   }

   private final class RollerTravellingPoint extends TravellingPoint {
      public BiConsumer<TrackEdge, Couple<Double>> traversalCallback;

      @Override
      protected Double edgeTraversedFrom(
         TrackGraph graph,
         boolean forward,
         TravellingPoint.IEdgePointListener edgePointListener,
         TravellingPoint.ITurnListener turnListener,
         double prevPos,
         double totalDistance
      ) {
         double from = forward ? prevPos : this.position;
         double to = forward ? this.position : prevPos;
         this.traversalCallback.accept(this.edge, Couple.create(from, to));
         return super.edgeTraversedFrom(graph, forward, edgePointListener, turnListener, prevPos, totalDistance);
      }
   }
}

package dev.ryanhcode.sable.neoforge.gametest;

import com.google.common.collect.UnmodifiableIterator;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestAssertPosException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.Vector3ic;

@GameTestHolder("sable")
public final class AssemblyTest {
   private static final Direction[] CAPABILITY_DIRECTIONS = new Direction[]{
      null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
   };

   @GameTest(
      template = "brittlebreak"
   )
   public static void testBrittleBreaking(GameTestHelper helper) {
      ServerLevel level = helper.getLevel();
      ServerSubLevelContainer plotContainer = SubLevelContainer.getContainer(level);
      if (plotContainer == null) {
         throw new IllegalStateException("Plot container not found in level");
      } else {
         SubLevelPhysicsSystem physicsSystem = plotContainer.physicsSystem();
         BlockPos min = helper.absolutePos(new BlockPos(0, 1, 0));
         BlockPos max = helper.absolutePos(new BlockPos(2, 3, 2));
         BoundingBox3i bounds = new BoundingBox3i(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
         List<BlockState> expectedStates = new ArrayList<>(bounds.volume());

         for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            expectedStates.add(level.getBlockState(pos));
         }

         ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(level, min, BlockPos.betweenClosed(min, max), bounds);
         physicsSystem.getPipeline()
            .teleport(
               subLevel,
               new Vector3d(
                  (double)min.getX() + (double)(1 + max.getX() - min.getX()) / 2.0,
                  (double)min.getY() + (double)(1 + max.getY() - min.getY()) / 2.0,
                  (double)min.getZ() + (double)(1 + max.getZ() - min.getZ()) / 2.0
               ),
               helper.getTestRotation().rotation().transformation().getNormalizedRotation(new Quaterniond())
            );
         helper.runAtTickTime(
            10L,
            () -> {
               Level plot = subLevel.getLevel();
               BoundingBox3ic sublevelBounds = subLevel.getPlot().getBoundingBox();
               Vector3ic actualSize = sublevelBounds.size(new Vector3i());
               Vector3ic expectedSize = bounds.size(new Vector3i());
               if (actualSize.equals(expectedSize)) {
                  int i = 0;

                  for (BlockPos posx : BlockPos.betweenClosed(
                     sublevelBounds.minX(), sublevelBounds.minY(), sublevelBounds.minZ(), sublevelBounds.maxX(), sublevelBounds.maxY(), sublevelBounds.maxZ()
                  )) {
                     BlockState expected = expectedStates.get(i);
                     if (!plot.getBlockState(posx).equals(expected)) {
                        throw new GameTestAssertPosException("Expected %s".formatted(expected.getBlock().getName().getString()), posx, posx, helper.getTick());
                     }

                     i++;
                  }

                  helper.succeed();
               } else {
                  helper.fail(
                     "Expected %dx%dx%d region, got %dx%dx%d"
                        .formatted(expectedSize.x(), expectedSize.y(), expectedSize.z(), actualSize.x(), actualSize.y(), actualSize.z())
                  );
               }
            }
         );
      }
   }

   @GameTest(
      template = "allblocks",
      required = false,
      manualOnly = true,
      timeoutTicks = 30000000
   )
   public static void testAllBlocks(GameTestHelper helper) {
      boolean failOnFirstError = false;
      boolean fastTest = true;
      Set<ResourceLocation> skip = Set.of(ResourceLocation.fromNamespaceAndPath("copycats", "wrapped_copycat"));
      Set<ResourceLocation> illegalInventories = Set.of(
         ResourceLocation.fromNamespaceAndPath("create_new_age", "reactor_fuel_acceptor"),
         ResourceLocation.fromNamespaceAndPath("farmersdelight", "cooking_pot")
      );
      ServerLevel level = helper.getLevel();
      ServerSubLevelContainer plotContainer = SubLevelContainer.getContainer(level);
      if (plotContainer == null) {
         throw new IllegalStateException("Plot container not found in level");
      } else {
         SubLevelPhysicsSystem physicsSystem = plotContainer.physicsSystem();
         ItemStack insertStack = new ItemStack(Items.OCELOT_SPAWN_EGG);
         BlockPos pos = helper.absolutePos(new BlockPos(2, 3, 2));
         BlockPos onPos = pos.below();
         Set<Block> invalidBlocks = new HashSet<>();
         Set<Block> failures = new HashSet<>();
         TestProgressBar progressBar = new TestProgressBar(level.getServer().getPlayerList());
         AtomicLong completedItems = new AtomicLong();
         long tick = 0L;
         long tests = 0L;

         for (Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation blockId = entry.getKey().location();
            if (!skip.contains(blockId)) {
               Block block = entry.getValue();
               UnmodifiableIterator var23 = block.getStateDefinition().getPossibleStates().iterator();

               while (var23.hasNext()) {
                  BlockState state = (BlockState)var23.next();
                  if (!state.is(Blocks.LECTERN) || !(Boolean)state.getValue(BlockStateProperties.HAS_BOOK)) {
                     boolean hasInventory = false;

                     for (Direction direction : CAPABILITY_DIRECTIONS) {
                        IItemHandler inventory = (IItemHandler)level.getCapability(ItemHandler.BLOCK, pos, state, level.getBlockEntity(pos), direction);
                        if (inventory != null) {
                           hasInventory = true;
                           break;
                        }
                     }

                     if (hasInventory || state.hasBlockEntity()) {
                        tests++;
                        helper.runAtTickTime(
                           tick += 2L,
                           () -> {
                              level.setBlock(onPos, Blocks.STONE.defaultBlockState(), 2);
                              level.setBlock(pos, state, 2);
                              if (isInvalidState(level.getBlockState(pos))) {
                                 helper.killAllEntities();
                                 invalidBlocks.add(block);
                              } else {
                                 List<Entity> startEntities = level.getEntities(EntityTypeTest.forClass(Entity.class), helper.getBounds(), Entity::isAlive);
                                 NonNullList<ItemStack>[] startingInventory = new NonNullList[CAPABILITY_DIRECTIONS.length];

                                 for (int i = 0; i < CAPABILITY_DIRECTIONS.length; i++) {
                                    Direction directionx = CAPABILITY_DIRECTIONS[i];
                                    IItemHandler inventoryx = (IItemHandler)level.getCapability(
                                       ItemHandler.BLOCK, pos, state, level.getBlockEntity(pos), directionx
                                    );
                                    if (inventoryx != null) {
                                       try {
                                          int slots = inventoryx.getSlots();
                                          if (inventoryx instanceof IItemHandlerModifiable modifiable) {
                                             for (int slot = 0; slot < slots; slot++) {
                                                try {
                                                   modifiable.setStackInSlot(slot, insertStack.copy());
                                                } catch (Throwable var24x) {
                                                   inventoryx.insertItem(slot, insertStack.copy(), false);
                                                }
                                             }
                                          } else {
                                             for (int slot = 0; slot < slots; slot++) {
                                                inventoryx.insertItem(slot, insertStack.copy(), false);
                                             }
                                          }

                                          NonNullList<ItemStack> list = NonNullList.withSize(slots, ItemStack.EMPTY);

                                          for (int slot = 0; slot < slots; slot++) {
                                             list.set(slot, inventoryx.getStackInSlot(slot).copy());
                                          }

                                          startingInventory[i] = list;
                                       } catch (Throwable var25x) {
                                          var25x.printStackTrace();
                                          helper.fail(
                                             formatBlockState(state).getString() + " failed. Unable to insert items successfully for face " + directionx, pos
                                          );
                                       }
                                    }
                                 }

                                 helper.runAfterDelay(
                                    1L,
                                    () -> {
                                       ServerSubLevel subLevel = SubLevelAssemblyHelper.assembleBlocks(
                                          level,
                                          pos,
                                          List.of(pos, onPos),
                                          new BoundingBox3i(onPos.getX(), onPos.getY(), onPos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)
                                       );
                                       physicsSystem.getPipeline()
                                          .teleport(
                                             subLevel,
                                             new Vector3d((double)pos.getX() + 0.5, (double)pos.getY() + 1.0, (double)pos.getZ() + 0.5),
                                             helper.getTestRotation().rotation().transformation().getNormalizedRotation(new Quaterniond())
                                          );
                                       BlockPos centerBlock = subLevel.getPlot().getCenterBlock();
                                       if (isInvalidState(level.getBlockState(centerBlock))) {
                                          invalidBlocks.add(block);
                                          helper.killAllEntities();
                                          SableTestHelper.removeSubLevel(plotContainer, subLevel);
                                          progressBar.update(completedItems.incrementAndGet());
                                       } else {
                                          List<Entity> resultEntities = level.getEntities(
                                             EntityTypeTest.forClass(Entity.class), helper.getBounds(), Entity::isAlive
                                          );
                                          if (startEntities.size() != resultEntities.size()) {
                                             failures.add(block);
                                             helper.killAllEntities();
                                          }

                                          if (!illegalInventories.contains(blockId)) {
                                             for (int ix = 0; ix < CAPABILITY_DIRECTIONS.length; ix++) {
                                                Direction directionxx = CAPABILITY_DIRECTIONS[ix];
                                                IItemHandler inventoryxx = (IItemHandler)level.getCapability(
                                                   ItemHandler.BLOCK, centerBlock, state, level.getBlockEntity(centerBlock), directionxx
                                                );
                                                NonNullList<ItemStack> starting = startingInventory[ix];
                                                if (inventoryxx != null) {
                                                   if (starting == null) {
                                                      String stateString = formatBlockState(state).getString();
                                                      helper.fail(stateString + " failed. Expected no inventory for face " + directionxx + ", found items");
                                                      return;
                                                   }

                                                   if (starting.size() != inventoryxx.getSlots()) {
                                                      String stateString = formatBlockState(state).getString();
                                                      helper.fail(
                                                         stateString
                                                            + " failed. Expected "
                                                            + starting.size()
                                                            + " inventory slots for face "
                                                            + directionxx
                                                            + ", found "
                                                            + inventoryxx.getSlots()
                                                      );
                                                      return;
                                                   }

                                                   try {
                                                      for (int slotx = 0; slotx < starting.size(); slotx++) {
                                                         if (!ItemStack.isSameItemSameComponents(
                                                            (ItemStack)starting.get(slotx), inventoryxx.getStackInSlot(slotx)
                                                         )) {
                                                            String stateString = formatBlockState(state).getString();
                                                            String expectedStack = ((ItemStack)starting.get(slotx)).toString();
                                                            String foundStack = inventoryxx.getStackInSlot(slotx).toString();
                                                            helper.fail(
                                                               stateString
                                                                  + " failed. Expected slot "
                                                                  + slotx
                                                                  + " for face "
                                                                  + directionxx
                                                                  + " to be "
                                                                  + expectedStack
                                                                  + " found "
                                                                  + foundStack
                                                            );
                                                            return;
                                                         }
                                                      }
                                                   } catch (GameTestAssertException var27x) {
                                                      throw var27x;
                                                   } catch (Throwable var28x) {
                                                      var28x.printStackTrace();
                                                      helper.fail(
                                                         formatBlockState(state).getString()
                                                            + " failed. Unable to get items successfully for face "
                                                            + directionxx
                                                      );
                                                   }
                                                } else if (starting != null) {
                                                   String stateString = formatBlockState(state).getString();
                                                   helper.fail(stateString + " failed. Expected inventory items for face " + directionxx + ", found none");
                                                   return;
                                                }
                                             }
                                          }

                                          SableTestHelper.removeSubLevel(plotContainer, subLevel);
                                          progressBar.update(completedItems.incrementAndGet());
                                       }
                                    }
                                 );
                              }
                           }
                        );
                        break;
                     }
                  }
               }
            }
         }

         progressBar.begin(tests);
         helper.runAtTickTime(tick + 1L, () -> {
            progressBar.end();
            if (!invalidBlocks.isEmpty()) {
               List<String> names = new ArrayList<>(invalidBlocks.size());

               for (Block blockxx : invalidBlocks) {
                  names.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(blockxx)));
               }

               String formattedLines = String.join("\n", names);
               Sable.LOGGER.info("Skipped blocks:\n{}", formattedLines);
            }

            if (!failures.isEmpty()) {
               List<String> names = new ArrayList<>(failures.size());

               for (Block blockx : failures) {
                  names.add(String.valueOf(BuiltInRegistries.BLOCK.getKey(blockx)));
               }

               String formattedLines = String.join("\n", names);
               helper.fail(failures.size() + " blocks failed.\n" + formattedLines);
            }

            helper.succeed();
         });
      }
   }

   private static Component formatBlockState(BlockState state) {
      MutableComponent name = Component.literal(String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
      Collection<Property<?>> properties = state.getProperties();
      if (!properties.isEmpty()) {
         StringBuilder propertiesString = new StringBuilder("[");

         for (Property<?> property : properties) {
            Object value = state.getValue(property);
            propertiesString.append(property.getName()).append("=").append(value).append(",");
         }

         propertiesString.setCharAt(propertiesString.length() - 1, ']');
         name.append(propertiesString.toString());
      }

      return name;
   }

   private static boolean isInvalidState(BlockState state) {
      return state.isAir() || state.getFluidState().createLegacyBlock() == state;
   }
}

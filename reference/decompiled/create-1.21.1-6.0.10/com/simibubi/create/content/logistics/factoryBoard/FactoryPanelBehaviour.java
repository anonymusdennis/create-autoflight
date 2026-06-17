package com.simibubi.create.content.logistics.factoryBoard;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.Create;
import com.simibubi.create.content.logistics.BigItemStack;
import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packager.InventorySummary;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagingRequest;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBehaviour;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedBlockItem;
import com.simibubi.create.content.logistics.packagerLink.LogisticallyLinkedClientHandler;
import com.simibubi.create.content.logistics.packagerLink.LogisticsManager;
import com.simibubi.create.content.logistics.packagerLink.RequestPromise;
import com.simibubi.create.content.logistics.packagerLink.RequestPromiseQueue;
import com.simibubi.create.content.logistics.stockTicker.PackageOrder;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.lang.invoke.StringConcatFactory;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.codecs.CatnipCodecs;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.nbt.NBTHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.Tags.Items;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;

public class FactoryPanelBehaviour extends FilteringBehaviour implements MenuProvider {
   public static final BehaviourType<FactoryPanelBehaviour> TOP_LEFT = new BehaviourType<>();
   public static final BehaviourType<FactoryPanelBehaviour> TOP_RIGHT = new BehaviourType<>();
   public static final BehaviourType<FactoryPanelBehaviour> BOTTOM_LEFT = new BehaviourType<>();
   public static final BehaviourType<FactoryPanelBehaviour> BOTTOM_RIGHT = new BehaviourType<>();
   public Map<FactoryPanelPosition, FactoryPanelConnection> targetedBy;
   public Map<BlockPos, FactoryPanelConnection> targetedByLinks;
   public Set<FactoryPanelPosition> targeting;
   public List<ItemStack> activeCraftingArrangement;
   public boolean satisfied;
   public boolean promisedSatisfied;
   public boolean waitingForNetwork;
   public String recipeAddress;
   public int recipeOutput;
   public LerpedFloat bulb;
   public FactoryPanelBlock.PanelSlot slot;
   public int promiseClearingInterval;
   public boolean forceClearPromises;
   public UUID network;
   public boolean active;
   public boolean redstonePowered;
   public RequestPromiseQueue restockerPromises;
   private boolean promisePrimedForMarkDirty;
   private int lastReportedUnloadedLinks;
   private int lastReportedLevelInStorage;
   private int lastReportedPromises;
   private int timer;

   public FactoryPanelBehaviour(FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
      super(be, new FactoryPanelSlotPositioning(slot));
      this.slot = slot;
      this.targetedBy = new HashMap<>();
      this.targetedByLinks = new HashMap<>();
      this.targeting = new HashSet<>();
      this.count = 0;
      this.satisfied = false;
      this.promisedSatisfied = false;
      this.waitingForNetwork = false;
      this.activeCraftingArrangement = List.of();
      this.recipeAddress = "";
      this.recipeOutput = 1;
      this.active = false;
      this.forceClearPromises = false;
      this.redstonePowered = false;
      this.promiseClearingInterval = -1;
      this.bulb = LerpedFloat.linear().startWithValue(0.0).chase(0.0, 0.175, Chaser.EXP);
      this.restockerPromises = new RequestPromiseQueue(be::setChanged);
      this.promisePrimedForMarkDirty = true;
      this.network = UUID.randomUUID();
      this.setLazyTickRate(40);
   }

   public void setNetwork(UUID network) {
      this.network = network;
   }

   @Nullable
   public static FactoryPanelBehaviour at(BlockAndTintGetter world, FactoryPanelConnection connection) {
      if (connection.cachedSource.get() instanceof FactoryPanelBehaviour fbe && !fbe.blockEntity.isRemoved()) {
         return fbe;
      }

      FactoryPanelBehaviour result = at(world, connection.from);
      connection.cachedSource = new WeakReference<>(result);
      return result;
   }

   @Nullable
   public static FactoryPanelBehaviour at(BlockAndTintGetter world, FactoryPanelPosition pos) {
      if (world instanceof Level l && !l.isLoaded(pos.pos())) {
         return null;
      }

      if (world.getBlockEntity(pos.pos()) instanceof FactoryPanelBlockEntity fpbe) {
         FactoryPanelBehaviour behaviour = fpbe.panels.get(pos.slot());
         return !behaviour.active ? null : behaviour;
      } else {
         return null;
      }
   }

   @Nullable
   public static FactoryPanelSupportBehaviour linkAt(BlockAndTintGetter world, FactoryPanelConnection connection) {
      if (connection.cachedSource.get() instanceof FactoryPanelSupportBehaviour fpsb && !fpsb.blockEntity.isRemoved()) {
         return fpsb;
      }

      FactoryPanelSupportBehaviour result = linkAt(world, connection.from);
      connection.cachedSource = new WeakReference<>(result);
      return result;
   }

   @Nullable
   public static FactoryPanelSupportBehaviour linkAt(BlockAndTintGetter world, FactoryPanelPosition pos) {
      if (world instanceof Level l && !l.isLoaded(pos.pos())) {
         return null;
      }

      return BlockEntityBehaviour.get(world, pos.pos(), FactoryPanelSupportBehaviour.TYPE);
   }

   public void moveTo(FactoryPanelPosition newPos, ServerPlayer player) {
      Level level = this.getWorld();
      BlockState existingState = level.getBlockState(newPos.pos());
      if (at(level, newPos) == null) {
         boolean isAddedToOtherGauge = AllBlocks.FACTORY_GAUGE.has(existingState);
         if (existingState.isAir() || isAddedToOtherGauge) {
            if (!isAddedToOtherGauge || existingState == this.blockEntity.getBlockState()) {
               if (!isAddedToOtherGauge) {
                  level.setBlock(newPos.pos(), this.blockEntity.getBlockState(), 3);
               }

               for (BlockPos blockPos : this.targetedByLinks.keySet()) {
                  if (!blockPos.closerThan(newPos.pos(), 24.0)) {
                     return;
                  }
               }

               for (FactoryPanelPosition blockPosx : this.targetedBy.keySet()) {
                  if (!blockPosx.pos().closerThan(newPos.pos(), 24.0)) {
                     return;
                  }
               }

               for (FactoryPanelPosition blockPosxx : this.targeting) {
                  if (!blockPosxx.pos().closerThan(newPos.pos(), 24.0)) {
                     return;
                  }
               }

               for (BlockPos pos : this.targetedByLinks.keySet()) {
                  FactoryPanelSupportBehaviour at = linkAt(level, new FactoryPanelPosition(pos, this.slot));
                  if (at != null) {
                     at.disconnect(this);
                  }
               }

               SmartBlockEntity oldBE = this.blockEntity;
               FactoryPanelPosition oldPos = this.getPanelPosition();
               this.moveToSlot(newPos.slot());
               if (level.getBlockEntity(newPos.pos()) instanceof FactoryPanelBlockEntity fpbe) {
                  fpbe.attachBehaviourLate(this);
                  fpbe.panels.put(this.slot, this);
                  fpbe.redraw = true;
                  fpbe.lastShape = null;
                  fpbe.notifyUpdate();
               }

               if (oldBE instanceof FactoryPanelBlockEntity fpbe) {
                  FactoryPanelBehaviour newBehaviour = new FactoryPanelBehaviour(fpbe, oldPos.slot());
                  fpbe.attachBehaviourLate(newBehaviour);
                  fpbe.panels.put(oldPos.slot(), newBehaviour);
                  fpbe.redraw = true;
                  fpbe.lastShape = null;
                  fpbe.notifyUpdate();
               }

               for (FactoryPanelPosition position : this.targeting) {
                  FactoryPanelBehaviour at = at(level, position);
                  if (at != null) {
                     FactoryPanelConnection connection = at.targetedBy.remove(oldPos);
                     connection.from = newPos;
                     at.targetedBy.put(newPos, connection);
                     at.blockEntity.sendData();
                  }
               }

               for (FactoryPanelPosition positionx : this.targetedBy.keySet()) {
                  FactoryPanelBehaviour at = at(level, positionx);
                  if (at != null) {
                     at.targeting.remove(oldPos);
                     at.targeting.add(newPos);
                  }
               }

               for (BlockPos posx : this.targetedByLinks.keySet()) {
                  FactoryPanelSupportBehaviour at = linkAt(level, new FactoryPanelPosition(posx, this.slot));
                  if (at != null) {
                     at.connect(this);
                  }
               }

               player.displayClientMessage(CreateLang.translate("factory_panel.relocated").style(ChatFormatting.GREEN).component(), true);
               player.level().playSound(null, newPos.pos(), SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
         }
      }
   }

   private void moveToSlot(FactoryPanelBlock.PanelSlot slot) {
      this.slot = slot;
      if (this.getSlotPositioning() instanceof FactoryPanelSlotPositioning fpsp) {
         fpsp.slot = slot;
      }
   }

   @Override
   public void initialize() {
      super.initialize();
      this.notifyRedstoneOutputs();
   }

   @Override
   public void tick() {
      super.tick();
      if (this.getWorld().isClientSide()) {
         if (this.blockEntity.isVirtual()) {
            this.tickStorageMonitor();
         }

         this.bulb.updateChaseTarget(!this.redstonePowered && !this.satisfied ? 0.0F : 1.0F);
         this.bulb.tickChaser();
         if (this.active) {
            this.tickOutline();
         }
      } else {
         if (!this.promisePrimedForMarkDirty) {
            this.restockerPromises.setOnChanged(this.blockEntity::setChanged);
            this.promisePrimedForMarkDirty = true;
         }

         this.tickStorageMonitor();
         this.tickRequests();
      }
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      if (!this.getWorld().isClientSide()) {
         this.checkForRedstoneInput();
      }
   }

   public void checkForRedstoneInput() {
      if (this.active) {
         boolean shouldPower = false;

         for (FactoryPanelConnection connection : this.targetedByLinks.values()) {
            if (!this.getWorld().isLoaded(connection.from.pos())) {
               return;
            }

            FactoryPanelSupportBehaviour linkAt = linkAt(this.getWorld(), connection);
            if (linkAt == null) {
               return;
            }

            shouldPower |= linkAt.shouldPanelBePowered();
         }

         if (shouldPower != this.redstonePowered) {
            this.redstonePowered = shouldPower;
            this.blockEntity.notifyUpdate();
            this.timer = 1;
         }
      }
   }

   private void notifyRedstoneOutputs() {
      for (FactoryPanelConnection connection : this.targetedByLinks.values()) {
         if (!this.getWorld().isLoaded(connection.from.pos())) {
            return;
         }

         FactoryPanelSupportBehaviour linkAt = linkAt(this.getWorld(), connection);
         if (linkAt == null || linkAt.isOutput()) {
            return;
         }

         linkAt.notifyLink();
      }
   }

   private void tickStorageMonitor() {
      ItemStack filter = this.getFilter();
      int unloadedLinkCount = this.getUnloadedLinks();
      FactoryPanelBlockEntity panelBE = this.panelBE();
      if (!panelBE.restocker && unloadedLinkCount == 0 && this.lastReportedUnloadedLinks != 0) {
         LogisticsManager.SUMMARIES.invalidate(this.network);
      }

      int inStorage = this.getLevelInStorage();
      int promised = this.getPromised();
      int demand = this.getAmount() * (this.upTo ? 1 : filter.getMaxStackSize());
      boolean shouldSatisfy = filter.isEmpty() || inStorage >= demand;
      boolean shouldPromiseSatisfy = filter.isEmpty() || inStorage + promised >= demand;
      boolean shouldWait = unloadedLinkCount > 0;
      if (this.lastReportedLevelInStorage != inStorage
         || this.lastReportedPromises != promised
         || this.lastReportedUnloadedLinks != unloadedLinkCount
         || this.satisfied != shouldSatisfy
         || this.promisedSatisfied != shouldPromiseSatisfy
         || this.waitingForNetwork != shouldWait) {
         if (!this.satisfied && shouldSatisfy && demand > 0) {
            AllSoundEvents.CONFIRM.playOnServer(this.getWorld(), this.getPos(), 0.075F, 1.0F);
            AllSoundEvents.CONFIRM_2.playOnServer(this.getWorld(), this.getPos(), 0.125F, 0.575F);
         }

         boolean notifyOutputs = this.satisfied != shouldSatisfy;
         this.lastReportedLevelInStorage = inStorage;
         this.satisfied = shouldSatisfy;
         this.lastReportedPromises = promised;
         this.promisedSatisfied = shouldPromiseSatisfy;
         this.lastReportedUnloadedLinks = unloadedLinkCount;
         this.waitingForNetwork = shouldWait;
         if (!this.getWorld().isClientSide) {
            this.blockEntity.sendData();
         }

         if (notifyOutputs) {
            this.notifyRedstoneOutputs();
         }
      }
   }

   private void tickRequests() {
      FactoryPanelBlockEntity panelBE = this.panelBE();
      if (!this.targetedBy.isEmpty() || panelBE.restocker) {
         if (panelBE.restocker) {
            this.restockerPromises.tick();
         }

         if (!this.satisfied && !this.promisedSatisfied && !this.waitingForNetwork && !this.redstonePowered) {
            if (this.timer > 0) {
               this.timer = Math.min(this.timer, this.getConfigRequestIntervalInTicks());
               this.timer--;
            } else {
               this.resetTimer();
               if (!this.recipeAddress.isBlank()) {
                  if (panelBE.restocker) {
                     this.tryRestock();
                  } else {
                     boolean failed = false;
                     Map<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> consolidated = new HashMap<>();

                     for (FactoryPanelConnection connection : this.targetedBy.values()) {
                        FactoryPanelBehaviour source = at(this.getWorld(), connection);
                        if (source == null) {
                           return;
                        }

                        ItemStack item = source.getFilter();
                        Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections> networkItemCounts = consolidated.computeIfAbsent(
                           source.network, $ -> new Object2ObjectOpenCustomHashMap(ItemStackLinkedSet.TYPE_AND_TAG)
                        );
                        networkItemCounts.computeIfAbsent(item, $ -> new FactoryPanelBehaviour.ItemStackConnections(item));
                        FactoryPanelBehaviour.ItemStackConnections existingConnections = networkItemCounts.get(item);
                        existingConnections.add(connection);
                        existingConnections.totalAmount = existingConnections.totalAmount + connection.amount;
                     }

                     Multimap<UUID, BigItemStack> toRequest = HashMultimap.create();

                     for (Entry<UUID, Map<ItemStack, FactoryPanelBehaviour.ItemStackConnections>> entry : consolidated.entrySet()) {
                        UUID network = entry.getKey();
                        InventorySummary summary = LogisticsManager.getSummaryOfNetwork(network, true);

                        for (FactoryPanelBehaviour.ItemStackConnections connections : entry.getValue().values()) {
                           if (connections.totalAmount != 0 && !connections.item.isEmpty() && summary.getCountOf(connections.item) >= connections.totalAmount) {
                              BigItemStack stack = new BigItemStack(connections.item, connections.totalAmount);
                              toRequest.put(network, stack);

                              for (FactoryPanelConnection connection : connections) {
                                 this.sendEffect(connection.from, true);
                              }
                           } else {
                              for (FactoryPanelConnection connection : connections) {
                                 this.sendEffect(connection.from, false);
                              }

                              failed = true;
                           }
                        }
                     }

                     if (!failed) {
                        Map<UUID, Collection<BigItemStack>> asMap = toRequest.asMap();
                        PackageOrderWithCrafts craftingContext = PackageOrderWithCrafts.empty();
                        List<Multimap<PackagerBlockEntity, PackagingRequest>> requests = new ArrayList<>();
                        if (!this.activeCraftingArrangement.isEmpty()) {
                           craftingContext = PackageOrderWithCrafts.singleRecipe(
                              this.activeCraftingArrangement.stream().map(stack -> new BigItemStack(stack.copyWithCount(1))).toList()
                           );
                        }

                        for (Entry<UUID, Collection<BigItemStack>> entry : asMap.entrySet()) {
                           PackageOrderWithCrafts order = new PackageOrderWithCrafts(
                              new PackageOrder(new ArrayList<>(entry.getValue())), craftingContext.orderedCrafts()
                           );
                           Multimap<PackagerBlockEntity, PackagingRequest> request = LogisticsManager.findPackagersForRequest(
                              entry.getKey(), order, null, this.recipeAddress
                           );
                           requests.add(request);
                        }

                        for (Multimap<PackagerBlockEntity, PackagingRequest> entry : requests) {
                           for (PackagerBlockEntity packager : entry.keySet()) {
                              if (packager.isTooBusyFor(LogisticallyLinkedBehaviour.RequestType.RESTOCK)) {
                                 return;
                              }
                           }
                        }

                        for (Multimap<PackagerBlockEntity, PackagingRequest> entry : requests) {
                           LogisticsManager.performPackageRequests(entry);
                        }

                        RequestPromiseQueue promises = Create.LOGISTICS.getQueuedPromises(this.network);
                        if (promises != null) {
                           promises.add(new RequestPromise(new BigItemStack(this.getFilter(), this.recipeOutput)));
                        }

                        panelBE.advancements.awardPlayer(AllAdvancements.FACTORY_GAUGE);
                     }
                  }
               }
            }
         }
      }
   }

   private void tryRestock() {
      ItemStack item = this.getFilter();
      if (!item.isEmpty()) {
         FactoryPanelBlockEntity panelBE = this.panelBE();
         PackagerBlockEntity packager = panelBE.getRestockedPackager();
         if (packager != null && packager.targetInventory.hasInventory()) {
            int availableOnNetwork = LogisticsManager.getStockOf(this.network, item, packager.targetInventory.getIdentifiedInventory());
            if (availableOnNetwork == 0) {
               this.sendEffect(this.getPanelPosition(), false);
            } else {
               int inStorage = this.getLevelInStorage();
               int promised = this.getPromised();
               int maxStackSize = item.getMaxStackSize();
               int demand = this.getAmount() * (this.upTo ? 1 : maxStackSize);
               int amountToOrder = Math.clamp(demand - promised - inStorage, 0, maxStackSize * 9);
               BigItemStack orderedItem = new BigItemStack(item, Math.min(amountToOrder, availableOnNetwork));
               PackageOrderWithCrafts order = PackageOrderWithCrafts.simple(List.of(orderedItem));
               this.sendEffect(this.getPanelPosition(), true);
               if (LogisticsManager.broadcastPackageRequest(
                  this.network, LogisticallyLinkedBehaviour.RequestType.RESTOCK, order, packager.targetInventory.getIdentifiedInventory(), this.recipeAddress
               )) {
                  this.restockerPromises.add(new RequestPromise(orderedItem));
               }
            }
         }
      }
   }

   private void sendEffect(FactoryPanelPosition fromPos, boolean success) {
      if (this.getWorld() instanceof ServerLevel serverLevel) {
         CatnipServices.NETWORK.sendToClientsAround(serverLevel, this.getPos(), 64.0, new FactoryPanelEffectPacket(fromPos, this.getPanelPosition(), success));
      }
   }

   public void addConnection(FactoryPanelPosition fromPos) {
      FactoryPanelSupportBehaviour link = linkAt(this.getWorld(), fromPos);
      if (link != null) {
         this.targetedByLinks.put(fromPos.pos(), new FactoryPanelConnection(fromPos, 1));
         link.connect(this);
         this.blockEntity.notifyUpdate();
      } else if (!this.panelBE().restocker) {
         if (this.targetedBy.size() < 9) {
            FactoryPanelBehaviour source = at(this.getWorld(), fromPos);
            if (source != null) {
               source.targeting.add(this.getPanelPosition());
               this.targetedBy.put(fromPos, new FactoryPanelConnection(fromPos, 1));
               this.blockEntity.notifyUpdate();
            }
         }
      }
   }

   public FactoryPanelPosition getPanelPosition() {
      return new FactoryPanelPosition(this.getPos(), this.slot);
   }

   public FactoryPanelBlockEntity panelBE() {
      return (FactoryPanelBlockEntity)this.blockEntity;
   }

   @Override
   public void onShortInteract(Player player, InteractionHand hand, Direction side, BlockHitResult hitResult) {
      if (!Create.LOGISTICS.mayInteract(this.network, player)) {
         player.displayClientMessage(CreateLang.translate("logistically_linked.protected").style(ChatFormatting.RED).component(), true);
      } else {
         boolean isClientSide = player.level().isClientSide;
         if (this.targeting.size() + this.targetedByLinks.size() > 0 && player.getItemInHand(hand).is(Items.TOOLS_WRENCH)) {
            int sharedMode = -1;
            boolean notifySelf = false;

            for (FactoryPanelPosition target : this.targeting) {
               FactoryPanelBehaviour at = at(this.getWorld(), target);
               if (at != null) {
                  FactoryPanelConnection connection = at.targetedBy.get(this.getPanelPosition());
                  if (connection != null) {
                     if (sharedMode == -1) {
                        sharedMode = (connection.arrowBendMode + 1) % 4;
                     }

                     connection.arrowBendMode = sharedMode;
                     if (!isClientSide) {
                        at.blockEntity.notifyUpdate();
                     }
                  }
               }
            }

            for (FactoryPanelConnection connection : this.targetedByLinks.values()) {
               if (sharedMode == -1) {
                  sharedMode = (connection.arrowBendMode + 1) % 4;
               }

               connection.arrowBendMode = sharedMode;
               if (!isClientSide) {
                  notifySelf = true;
               }
            }

            if (sharedMode != -1) {
               char[] boxes = "□□□□".toCharArray();
               boxes[sharedMode] = 9632;
               player.displayClientMessage(CreateLang.translate("factory_panel.cycled_arrow_path", new String(boxes)).component(), true);
               if (notifySelf) {
                  this.blockEntity.notifyUpdate();
               }
            }
         } else if (!isClientSide || !FactoryPanelConnectionHandler.panelClicked(this.getWorld(), player, this)) {
            ItemStack heldItem = player.getItemInHand(hand);
            if (this.getFilter().isEmpty()) {
               if (heldItem.isEmpty()) {
                  if (!isClientSide && player instanceof ServerPlayer sp) {
                     sp.openMenu(this, buf -> FactoryPanelPosition.STREAM_CODEC.encode(buf, this.getPanelPosition()));
                  }
               } else {
                  super.onShortInteract(player, hand, side, hitResult);
               }
            } else if (heldItem.getItem() instanceof LogisticallyLinkedBlockItem) {
               if (!isClientSide) {
                  LogisticallyLinkedBlockItem.assignFrequency(heldItem, player, this.network);
               }
            } else {
               if (isClientSide) {
                  CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> this.displayScreen(player));
               }
            }
         }
      }
   }

   public void enable() {
      this.active = true;
      this.blockEntity.notifyUpdate();
   }

   public void disable() {
      this.destroy();
      this.active = false;
      this.targetedBy = new HashMap<>();
      this.targeting = new HashSet<>();
      this.count = 0;
      this.satisfied = false;
      this.promisedSatisfied = false;
      this.recipeAddress = "";
      this.recipeOutput = 1;
      this.setFilter(ItemStack.EMPTY);
      this.blockEntity.notifyUpdate();
   }

   @Override
   public boolean isActive() {
      return this.active;
   }

   public boolean isMissingAddress() {
      return (!this.targetedBy.isEmpty() || this.panelBE().restocker) && this.count != 0 && this.recipeAddress.isBlank();
   }

   @Override
   public void destroy() {
      this.disconnectAll();
      super.destroy();
   }

   public void disconnectAll() {
      FactoryPanelPosition panelPosition = this.getPanelPosition();
      this.disconnectAllLinks();

      for (FactoryPanelConnection connection : this.targetedBy.values()) {
         FactoryPanelBehaviour source = at(this.getWorld(), connection);
         if (source != null) {
            source.targeting.remove(panelPosition);
            source.blockEntity.sendData();
         }
      }

      for (FactoryPanelPosition position : this.targeting) {
         FactoryPanelBehaviour target = at(this.getWorld(), position);
         if (target != null) {
            target.targetedBy.remove(panelPosition);
            target.blockEntity.sendData();
         }
      }

      this.targetedBy.clear();
      this.targeting.clear();
   }

   public void disconnectAllLinks() {
      for (FactoryPanelConnection connection : this.targetedByLinks.values()) {
         FactoryPanelSupportBehaviour source = linkAt(this.getWorld(), connection);
         if (source != null) {
            source.disconnect(this);
         }
      }

      this.targetedByLinks.clear();
   }

   public int getUnloadedLinks() {
      if (this.getWorld().isClientSide()) {
         return this.lastReportedUnloadedLinks;
      } else if (this.panelBE().restocker) {
         return this.panelBE().getRestockedPackager() == null ? 1 : 0;
      } else {
         return Create.LOGISTICS.getUnloadedLinkCount(this.network);
      }
   }

   public int getLevelInStorage() {
      if (this.blockEntity.isVirtual()) {
         return 1;
      } else if (this.getWorld().isClientSide()) {
         return this.lastReportedLevelInStorage;
      } else if (this.getFilter().isEmpty()) {
         return 0;
      } else {
         InventorySummary summary = this.getRelevantSummary();
         return summary.getCountOf(this.getFilter());
      }
   }

   private InventorySummary getRelevantSummary() {
      FactoryPanelBlockEntity panelBE = this.panelBE();
      if (!panelBE.restocker) {
         return LogisticsManager.getSummaryOfNetwork(this.network, false);
      } else {
         PackagerBlockEntity packager = panelBE.getRestockedPackager();
         return packager == null ? InventorySummary.EMPTY : packager.getAvailableItems();
      }
   }

   public int getPromised() {
      if (this.getWorld().isClientSide()) {
         return this.lastReportedPromises;
      } else {
         ItemStack item = this.getFilter();
         if (item.isEmpty()) {
            return 0;
         } else if (this.panelBE().restocker) {
            if (this.forceClearPromises) {
               this.restockerPromises.forceClear(item);
               this.resetTimerSlightly();
            }

            this.forceClearPromises = false;
            return this.restockerPromises.getTotalPromisedAndRemoveExpired(item, this.getPromiseExpiryTimeInTicks());
         } else {
            RequestPromiseQueue promises = Create.LOGISTICS.getQueuedPromises(this.network);
            if (promises == null) {
               return 0;
            } else {
               if (this.forceClearPromises) {
                  promises.forceClear(item);
                  this.resetTimerSlightly();
               }

               this.forceClearPromises = false;
               return promises.getTotalPromisedAndRemoveExpired(item, this.getPromiseExpiryTimeInTicks());
            }
         }
      }
   }

   public void resetTimer() {
      this.timer = this.getConfigRequestIntervalInTicks();
   }

   public void resetTimerSlightly() {
      this.timer = this.getConfigRequestIntervalInTicks() / 2;
   }

   private int getConfigRequestIntervalInTicks() {
      return (Integer)AllConfigs.server().logistics.factoryGaugeTimer.get();
   }

   private int getPromiseExpiryTimeInTicks() {
      if (this.promiseClearingInterval == -1) {
         return -1;
      } else {
         return this.promiseClearingInterval == 0 ? 600 : this.promiseClearingInterval * 20 * 60;
      }
   }

   @Override
   public void writeSafe(CompoundTag nbt, Provider registries) {
      if (this.active) {
         CompoundTag panelTag = new CompoundTag();
         panelTag.put("Filter", this.getFilter().saveOptional(registries));
         panelTag.putBoolean("UpTo", this.upTo);
         panelTag.putInt("FilterAmount", this.count);
         panelTag.putUUID("Freq", this.network);
         panelTag.putString("RecipeAddress", this.recipeAddress);
         panelTag.putInt("PromiseClearingInterval", -1);
         panelTag.putInt("RecipeOutput", 1);
         if (this.panelBE().restocker) {
            panelTag.put("Promises", this.restockerPromises.write(registries));
         }

         nbt.put(CreateLang.asId(this.slot.name()), panelTag);
      }
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      if (this.active) {
         CompoundTag panelTag = new CompoundTag();
         super.write(panelTag, registries, clientPacket);
         panelTag.putInt("Timer", this.timer);
         panelTag.putInt("LastLevel", this.lastReportedLevelInStorage);
         panelTag.putInt("LastPromised", this.lastReportedPromises);
         panelTag.putInt("LastUnloadedLinks", this.lastReportedUnloadedLinks);
         panelTag.putBoolean("Satisfied", this.satisfied);
         panelTag.putBoolean("PromisedSatisfied", this.promisedSatisfied);
         panelTag.putBoolean("Waiting", this.waitingForNetwork);
         panelTag.putBoolean("RedstonePowered", this.redstonePowered);
         panelTag.put("Targeting", (Tag)CatnipCodecUtils.encode(CatnipCodecs.set(FactoryPanelPosition.CODEC), registries, this.targeting).orElseThrow());
         panelTag.put(
            "TargetedBy",
            (Tag)CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), registries, new ArrayList<>(this.targetedBy.values())).orElseThrow()
         );
         panelTag.put(
            "TargetedByLinks",
            (Tag)CatnipCodecUtils.encode(Codec.list(FactoryPanelConnection.CODEC), registries, new ArrayList<>(this.targetedByLinks.values())).orElseThrow()
         );
         panelTag.putString("RecipeAddress", this.recipeAddress);
         panelTag.putInt("RecipeOutput", this.recipeOutput);
         panelTag.putInt("PromiseClearingInterval", this.promiseClearingInterval);
         panelTag.putUUID("Freq", this.network);
         panelTag.put("Craft", NBTHelper.writeItemList(this.activeCraftingArrangement, registries));
         if (this.panelBE().restocker && !clientPacket) {
            panelTag.put("Promises", this.restockerPromises.write(registries));
         }

         nbt.put(CreateLang.asId(this.slot.name()), panelTag);
      }
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      CompoundTag panelTag = nbt.getCompound(CreateLang.asId(this.slot.name()));
      if (panelTag.isEmpty()) {
         this.active = false;
      } else {
         this.active = true;
         this.filter = FilterItemStack.of(registries, panelTag.getCompound("Filter"));
         this.count = panelTag.getInt("FilterAmount");
         this.upTo = panelTag.getBoolean("UpTo");
         this.timer = panelTag.getInt("Timer");
         this.lastReportedLevelInStorage = panelTag.getInt("LastLevel");
         this.lastReportedPromises = panelTag.getInt("LastPromised");
         this.lastReportedUnloadedLinks = panelTag.getInt("LastUnloadedLinks");
         this.satisfied = panelTag.getBoolean("Satisfied");
         this.promisedSatisfied = panelTag.getBoolean("PromisedSatisfied");
         this.waitingForNetwork = panelTag.getBoolean("Waiting");
         this.redstonePowered = panelTag.getBoolean("RedstonePowered");
         this.promiseClearingInterval = panelTag.getInt("PromiseClearingInterval");
         if (panelTag.hasUUID("Freq")) {
            this.network = panelTag.getUUID("Freq");
         }

         this.targeting.clear();
         this.targeting.addAll(CatnipCodecUtils.decode(CatnipCodecs.set(FactoryPanelPosition.CODEC), registries, panelTag.get("Targeting")).orElse(Set.of()));
         this.targetedBy.clear();
         CatnipCodecUtils.decode(Codec.list(FactoryPanelConnection.CODEC), registries, panelTag.get("TargetedBy"))
            .orElse(List.of())
            .forEach(c -> this.targetedBy.put(c.from, c));
         this.targetedByLinks.clear();
         CatnipCodecUtils.decode(Codec.list(FactoryPanelConnection.CODEC), registries, panelTag.get("TargetedByLinks"))
            .orElse(List.of())
            .forEach(c -> this.targetedByLinks.put(c.from.pos(), c));
         this.activeCraftingArrangement = NBTHelper.readItemList(panelTag.getList("Craft", 10), registries);
         this.recipeAddress = panelTag.getString("RecipeAddress");
         this.recipeOutput = panelTag.getInt("RecipeOutput");
         if (nbt.getBoolean("Restocker") && !clientPacket) {
            this.restockerPromises = RequestPromiseQueue.read(panelTag.getCompound("Promises"), registries, () -> {
            });
            this.promisePrimedForMarkDirty = false;
         }
      }
   }

   @Override
   public float getRenderDistance() {
      return 64.0F;
   }

   @Override
   public MutableComponent formatValue(ValueSettingsBehaviour.ValueSettings value) {
      return value.value() == 0
         ? CreateLang.translateDirect("gui.factory_panel.inactive")
         : Component.literal(Math.max(0, value.value()) + (value.row() == 0 ? "" : "▤"));
   }

   @Override
   public boolean setFilter(ItemStack stack) {
      ItemStack filter = stack.copy();
      if (stack.getItem() instanceof FilterItem) {
         return false;
      } else {
         this.filter = FilterItemStack.of(filter);
         this.blockEntity.setChanged();
         this.blockEntity.sendData();
         return true;
      }
   }

   @Override
   public void setValueSettings(Player player, ValueSettingsBehaviour.ValueSettings settings, boolean ctrlDown) {
      if (!this.getValueSettings().equals(settings)) {
         this.count = Math.max(0, settings.value());
         this.upTo = settings.row() == 0;
         this.panelBE().redraw = true;
         this.blockEntity.setChanged();
         this.blockEntity.sendData();
         this.playFeedbackSound(this);
         this.resetTimerSlightly();
         if (!this.getWorld().isClientSide) {
            this.notifyRedstoneOutputs();
         }
      }
   }

   @Override
   public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
      int maxAmount = 100;
      return new ValueSettingsBoard(
         CreateLang.translate("factory_panel.target_amount").component(),
         maxAmount,
         10,
         List.of(
            CreateLang.translate("schedule.condition.threshold.items").component(), CreateLang.translate("schedule.condition.threshold.stacks").component()
         ),
         new ValueSettingsFormatter(this::formatValue)
      );
   }

   @Override
   public MutableComponent getLabel() {
      String key = "";
      if (!this.targetedBy.isEmpty() && this.count == 0) {
         return CreateLang.translate("gui.factory_panel.no_target_amount_set").style(ChatFormatting.RED).component();
      } else if (this.isMissingAddress()) {
         return CreateLang.translate("gui.factory_panel.address_missing").style(ChatFormatting.RED).component();
      } else {
         if (this.getFilter().isEmpty()) {
            key = "factory_panel.new_factory_task";
         } else {
            if (!this.waitingForNetwork) {
               if (this.getAmount() != 0 && !this.targetedBy.isEmpty()) {
                  key = this.getFilter().getHoverName().getString();
                  if (this.redstonePowered) {
                     key = key + " " + CreateLang.translate("factory_panel.redstone_paused").string();
                  } else if (!this.satisfied) {
                     key = key + " " + CreateLang.translate("factory_panel.in_progress").string();
                  }

                  return CreateLang.text(key).component();
               }

               return this.getFilter().getHoverName().plainCopy();
            }

            key = "factory_panel.some_links_unloaded";
         }

         return CreateLang.translate(key).component();
      }
   }

   @Override
   public ValueSettingsBehaviour.ValueSettings getValueSettings() {
      return new ValueSettingsBehaviour.ValueSettings(this.upTo ? 0 : 1, this.count);
   }

   @Override
   public MutableComponent getTip() {
      return CreateLang.translateDirect(this.filter.isEmpty() ? "logistics.filter.click_to_set" : "factory_panel.click_to_configure");
   }

   @Override
   public MutableComponent getAmountTip() {
      return CreateLang.translateDirect("factory_panel.hold_to_set_amount");
   }

   @Override
   public MutableComponent getCountLabelForValueBox() {
      if (this.filter.isEmpty()) {
         return Component.empty();
      } else if (this.waitingForNetwork) {
         return Component.literal("?");
      } else {
         int levelInStorage = this.getLevelInStorage();
         boolean inf = levelInStorage >= 1000000000;
         int inStorage = levelInStorage / (this.upTo ? 1 : this.getFilter().getMaxStackSize());
         int promised = this.getPromised();
         String stacks = this.upTo ? "" : "▤";
         return this.count == 0
            ? CreateLang.text(inf ? "  ∞" : inStorage + stacks).color(15855592).component()
            : CreateLang.text(inf ? "  ∞" : "   " + inStorage + stacks)
               .color(this.satisfied ? 14155688 : (this.promisedSatisfied ? 16764277 : 16760744))
               .add(CreateLang.text(promised == 0 ? "" : "⏶"))
               .add(CreateLang.text("/").style(ChatFormatting.WHITE))
               .add(CreateLang.text(this.count + stacks + "  ").color(15855592))
               .component();
      }
   }

   @Override
   public int netId() {
      return 2 + this.slot.ordinal();
   }

   @Override
   public boolean isCountVisible() {
      return !this.getFilter().isEmpty();
   }

   @Override
   public BehaviourType<?> getType() {
      return getTypeForSlot(this.slot);
   }

   public static BehaviourType<?> getTypeForSlot(FactoryPanelBlock.PanelSlot slot) {
      return switch (slot) {
         case BOTTOM_LEFT -> BOTTOM_LEFT;
         case TOP_LEFT -> TOP_LEFT;
         case TOP_RIGHT -> TOP_RIGHT;
         case BOTTOM_RIGHT -> BOTTOM_RIGHT;
      };
   }

   @OnlyIn(Dist.CLIENT)
   public void displayScreen(Player player) {
      if (player instanceof LocalPlayer) {
         ScreenOpener.open(new FactoryPanelScreen(this));
      }
   }

   public int getIngredientStatusColor() {
      return this.count != 0 && !this.isMissingAddress() && !this.redstonePowered
         ? (this.waitingForNetwork ? 5978939 : (this.satisfied ? 10420095 : (this.promisedSatisfied ? 2273199 : 4026045)))
         : 8947864;
   }

   @Override
   public ItemRequirement getRequiredItems() {
      return this.isActive() ? new ItemRequirement(ItemRequirement.ItemUseType.CONSUME, AllBlocks.FACTORY_GAUGE.asItem()) : ItemRequirement.NONE;
   }

   @Override
   public boolean canShortInteract(ItemStack toApply) {
      return true;
   }

   @Override
   public boolean readFromClipboard(@NotNull Provider registries, CompoundTag tag, Player player, Direction side, boolean simulate) {
      return false;
   }

   @Override
   public boolean writeToClipboard(@NotNull Provider registries, CompoundTag tag, Direction side) {
      return false;
   }

   private void tickOutline() {
      CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> LogisticallyLinkedClientHandler.tickPanel(this));
   }

   public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
      return FactoryPanelSetItemMenu.create(containerId, playerInventory, this);
   }

   public Component getDisplayName() {
      return this.blockEntity.getBlockState().getBlock().getName();
   }

   public String getFrogAddress() {
      PackagerBlockEntity packager = this.panelBE().getRestockedPackager();
      if (packager == null) {
         return null;
      } else {
         if (packager.getLevel().getBlockEntity(packager.getBlockPos().above()) instanceof FrogportBlockEntity fpbe
            && fpbe.addressFilter != null
            && !fpbe.addressFilter.isBlank()) {
            return StringConcatFactory.makeConcatWithConstants<"makeConcatWithConstants","\u0001">(fpbe.addressFilter);
         }

         return null;
      }
   }

   public static class ItemStackConnections extends ArrayList<FactoryPanelConnection> {
      public ItemStack item;
      public int totalAmount;

      public ItemStackConnections(ItemStack item) {
         this.item = item;
      }
   }
}

package com.simibubi.create.content.schematics.cannon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.item.ItemHelper;
import com.simibubi.create.foundation.mixin.accessor.ItemStackHandlerAccessor;
import com.simibubi.create.foundation.utility.BlockHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.simibubi.create.infrastructure.config.CSchematics;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.Clearable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.EmptyItemHandler;
import org.jetbrains.annotations.Nullable;

public class SchematicannonBlockEntity extends SmartBlockEntity implements MenuProvider, Clearable {
   public static final int NEIGHBOUR_CHECKING = 100;
   public static final int MAX_ANCHOR_DISTANCE = 256;
   public SchematicannonInventory inventory;
   public boolean sendUpdate;
   public boolean dontUpdateChecklist;
   public int neighbourCheckCooldown;
   public SchematicPrinter printer;
   public ItemStack missingItem;
   public boolean positionNotLoaded;
   public boolean hasCreativeCrate;
   private int printerCooldown;
   private int skipsLeft;
   private boolean blockSkipped;
   public BlockPos previousTarget;
   public LinkedHashSet<IItemHandler> attachedInventories;
   public List<LaunchedItem> flyingBlocks;
   public MaterialChecklist checklist;
   public int remainingFuel;
   public float bookPrintingProgress;
   public float schematicProgress;
   public String statusMsg;
   public SchematicannonBlockEntity.State state;
   public int blocksPlaced;
   public int blocksToPlace;
   public int replaceMode;
   public boolean skipMissing;
   public boolean replaceBlockEntities;
   public boolean firstRenderTick;
   public float defaultYaw;

   public SchematicannonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.setLazyTickRate(30);
      this.attachedInventories = new LinkedHashSet<>();
      this.flyingBlocks = new LinkedList<>();
      this.inventory = new SchematicannonInventory(this);
      this.statusMsg = "idle";
      this.state = SchematicannonBlockEntity.State.STOPPED;
      this.replaceMode = 2;
      this.checklist = new MaterialChecklist();
      this.printer = new SchematicPrinter();
   }

   public void findInventories() {
      this.hasCreativeCrate = false;
      this.attachedInventories.clear();

      for (Direction facing : Iterate.directions) {
         if (this.level.isLoaded(this.worldPosition.relative(facing))) {
            if (AllBlocks.CREATIVE_CRATE.has(this.level.getBlockState(this.worldPosition.relative(facing)))) {
               this.hasCreativeCrate = true;
            }

            BlockEntity blockEntity = this.level.getBlockEntity(this.worldPosition.relative(facing));
            if (blockEntity != null) {
               IItemHandler capability = (IItemHandler)this.level.getCapability(ItemHandler.BLOCK, blockEntity.getBlockPos(), facing.getOpposite());
               if (capability != null) {
                  this.attachedInventories.add(capability);
               }
            }
         }
      }
   }

   public void clearContent() {
      ((ItemStackHandlerAccessor)this.inventory).create$getStacks().clear();
   }

   @Override
   protected void read(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (!clientPacket) {
         this.inventory.deserializeNBT(registries, compound.getCompound("Inventory"));
      }

      this.statusMsg = compound.getString("Status");
      this.schematicProgress = compound.getFloat("Progress");
      this.bookPrintingProgress = compound.getFloat("PaperProgress");
      this.remainingFuel = compound.getInt("RemainingFuel");
      String stateString = compound.getString("State");
      this.state = stateString.isEmpty() ? SchematicannonBlockEntity.State.STOPPED : SchematicannonBlockEntity.State.valueOf(compound.getString("State"));
      this.blocksPlaced = compound.getInt("AmountPlaced");
      this.blocksToPlace = compound.getInt("AmountToPlace");
      this.missingItem = null;
      if (compound.contains("MissingItem")) {
         ItemStack.parse(registries, compound.getCompound("MissingItem")).ifPresent(i -> this.missingItem = i);
      }

      SchematicannonBlockEntity.SchematicannonOptions options = CatnipCodecUtils.decode(
            SchematicannonBlockEntity.SchematicannonOptions.CODEC, registries, compound.getCompound("Options")
         )
         .orElse(new SchematicannonBlockEntity.SchematicannonOptions(2, false, false));
      this.replaceMode = options.replaceMode;
      this.skipMissing = options.skipMissing;
      this.replaceBlockEntities = options.replaceBlockEntities;
      if (compound.contains("Printer")) {
         this.printer.fromTag(compound.getCompound("Printer"), clientPacket);
      }

      if (compound.contains("FlyingBlocks")) {
         this.readFlyingBlocks(compound, registries);
      }

      this.defaultYaw = compound.getFloat("DefaultYaw");
      super.read(compound, registries, clientPacket);
   }

   protected void readFlyingBlocks(CompoundTag compound, Provider registries) {
      ListTag tagBlocks = compound.getList("FlyingBlocks", 10);
      if (tagBlocks.isEmpty()) {
         this.flyingBlocks.clear();
      }

      boolean pastDead = false;

      for (int i = 0; i < tagBlocks.size(); i++) {
         CompoundTag c = tagBlocks.getCompound(i);
         LaunchedItem launched = LaunchedItem.fromNBT(c, registries, this.blockHolderGetter());
         BlockPos readBlockPos = launched.target;
         if (this.level != null && this.level.isClientSide) {
            while (!pastDead && !this.flyingBlocks.isEmpty() && !this.flyingBlocks.get(0).target.equals(readBlockPos)) {
               this.flyingBlocks.remove(0);
            }

            pastDead = true;
            if (i >= this.flyingBlocks.size()) {
               this.flyingBlocks.add(launched);
            }
         } else {
            this.flyingBlocks.add(launched);
         }
      }
   }

   @Override
   public void write(CompoundTag compound, Provider registries, boolean clientPacket) {
      if (!clientPacket) {
         compound.put("Inventory", this.inventory.serializeNBT(registries));
         if (this.state == SchematicannonBlockEntity.State.RUNNING) {
            compound.putBoolean("Running", true);
         }
      }

      compound.putFloat("Progress", this.schematicProgress);
      compound.putFloat("PaperProgress", this.bookPrintingProgress);
      compound.putInt("RemainingFuel", this.remainingFuel);
      compound.putString("Status", this.statusMsg);
      compound.putString("State", this.state.name());
      compound.putInt("AmountPlaced", this.blocksPlaced);
      compound.putInt("AmountToPlace", this.blocksToPlace);
      if (this.missingItem != null) {
         compound.put("MissingItem", this.missingItem.saveOptional(registries));
      }

      Tag options = (Tag)CatnipCodecUtils.encode(
            SchematicannonBlockEntity.SchematicannonOptions.CODEC,
            registries,
            new SchematicannonBlockEntity.SchematicannonOptions(this.replaceMode, this.skipMissing, this.replaceBlockEntities)
         )
         .orElseThrow();
      compound.put("Options", options);
      CompoundTag printerData = new CompoundTag();
      this.printer.write(printerData);
      compound.put("Printer", printerData);
      ListTag tagFlyingBlocks = new ListTag();

      for (LaunchedItem b : this.flyingBlocks) {
         tagFlyingBlocks.add(b.serializeNBT(registries));
      }

      compound.put("FlyingBlocks", tagFlyingBlocks);
      compound.putFloat("DefaultYaw", this.defaultYaw);
      super.write(compound, registries, clientPacket);
   }

   @Override
   public void tick() {
      super.tick();
      if (this.state != SchematicannonBlockEntity.State.STOPPED && this.neighbourCheckCooldown-- <= 0) {
         this.neighbourCheckCooldown = 100;
         this.findInventories();
      }

      this.firstRenderTick = true;
      this.previousTarget = this.printer.getCurrentTarget();
      this.tickFlyingBlocks();
      if (!this.level.isClientSide) {
         this.tickPaperPrinter();
         this.refillFuelIfPossible();
         this.skipsLeft = 1000;
         this.blockSkipped = true;

         while (this.blockSkipped && this.skipsLeft-- > 0) {
            this.tickPrinter();
         }

         this.schematicProgress = 0.0F;
         if (this.blocksToPlace > 0) {
            this.schematicProgress = (float)this.blocksPlaced / (float)this.blocksToPlace;
         }

         if (this.sendUpdate) {
            this.sendUpdate = false;
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 6);
         }
      }
   }

   public CSchematics config() {
      return AllConfigs.server().schematics;
   }

   protected void tickPrinter() {
      ItemStack blueprint = this.inventory.getStackInSlot(0);
      this.blockSkipped = false;
      if (blueprint.isEmpty() && !this.statusMsg.equals("idle") && this.inventory.getStackInSlot(1).isEmpty()) {
         this.state = SchematicannonBlockEntity.State.STOPPED;
         this.statusMsg = "idle";
         this.sendUpdate = true;
      } else if (this.state == SchematicannonBlockEntity.State.STOPPED) {
         if (this.printer.isLoaded()) {
            this.resetPrinter();
         }
      } else if (this.state != SchematicannonBlockEntity.State.PAUSED || this.positionNotLoaded || this.missingItem != null || this.remainingFuel <= 0) {
         if (!this.printer.isLoaded()) {
            this.initializePrinter(blueprint);
         } else if (this.printerCooldown > 0) {
            this.printerCooldown--;
         } else {
            if (this.remainingFuel <= 0 && !this.hasCreativeCrate) {
               this.refillFuelIfPossible();
               if (this.remainingFuel <= 0) {
                  this.state = SchematicannonBlockEntity.State.PAUSED;
                  this.statusMsg = "noGunpowder";
                  this.sendUpdate = true;
                  return;
               }
            }

            if (this.hasCreativeCrate) {
               this.remainingFuel = 0;
               if (this.missingItem != null) {
                  this.missingItem = null;
                  this.state = SchematicannonBlockEntity.State.RUNNING;
               }
            }

            if (this.missingItem == null && !this.positionNotLoaded) {
               if (!this.printer.advanceCurrentPos()) {
                  this.finishedPrinting();
                  return;
               }

               this.sendUpdate = true;
            }

            if (!this.getLevel().isLoaded(this.printer.getCurrentTarget())) {
               this.positionNotLoaded = true;
               this.statusMsg = "targetNotLoaded";
               this.state = SchematicannonBlockEntity.State.PAUSED;
            } else {
               if (this.positionNotLoaded) {
                  this.positionNotLoaded = false;
                  this.state = SchematicannonBlockEntity.State.RUNNING;
               }

               ItemRequirement requirement = this.printer.getCurrentRequirement();
               if (!requirement.isInvalid() && this.printer.shouldPlaceCurrent(this.level, this::shouldPlace)) {
                  List<ItemRequirement.StackRequirement> requiredItems = requirement.getRequiredItems();
                  if (!requirement.isEmpty()) {
                     for (ItemRequirement.StackRequirement required : requiredItems) {
                        if (!this.grabItemsFromAttachedInventories(required, true)) {
                           if (this.skipMissing) {
                              this.statusMsg = "skipping";
                              this.blockSkipped = true;
                              if (this.missingItem != null) {
                                 this.missingItem = null;
                                 this.state = SchematicannonBlockEntity.State.RUNNING;
                              }

                              return;
                           }

                           this.missingItem = required.stack;
                           this.state = SchematicannonBlockEntity.State.PAUSED;
                           this.statusMsg = "missingBlock";
                           return;
                        }
                     }

                     for (ItemRequirement.StackRequirement requiredx : requiredItems) {
                        this.grabItemsFromAttachedInventories(requiredx, false);
                     }
                  }

                  this.state = SchematicannonBlockEntity.State.RUNNING;
                  ItemStack icon = !requirement.isEmpty() && !requiredItems.isEmpty() ? requiredItems.get(0).stack : ItemStack.EMPTY;
                  this.printer.handleCurrentTarget((target, blockState, blockEntity) -> {
                     this.statusMsg = blockState.getBlock() != Blocks.AIR ? "placing" : "clearing";
                     this.launchBlockOrBelt(target, icon, blockState, blockEntity);
                  }, (target, entity) -> {
                     this.statusMsg = "placing";
                     this.launchEntity(target, icon, entity);
                  });
                  this.printerCooldown = (Integer)this.config().schematicannonDelay.get();
                  this.remainingFuel--;
                  this.sendUpdate = true;
                  this.missingItem = null;
               } else {
                  this.sendUpdate = !this.statusMsg.equals("searching");
                  this.statusMsg = "searching";
                  this.blockSkipped = true;
               }
            }
         }
      }
   }

   public int getShotsPerGunpowder() {
      return this.hasCreativeCrate ? 0 : (Integer)this.config().schematicannonShotsPerGunpowder.get();
   }

   protected void initializePrinter(ItemStack blueprint) {
      if (!blueprint.has(AllDataComponents.SCHEMATIC_ANCHOR)) {
         this.state = SchematicannonBlockEntity.State.STOPPED;
         this.statusMsg = "schematicInvalid";
         this.sendUpdate = true;
      } else if (!(Boolean)blueprint.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false)) {
         this.state = SchematicannonBlockEntity.State.STOPPED;
         this.statusMsg = "schematicNotPlaced";
         this.sendUpdate = true;
      } else {
         this.printer.loadSchematic(blueprint, this.level, true);
         if (this.printer.isErrored()) {
            this.state = SchematicannonBlockEntity.State.STOPPED;
            this.statusMsg = "schematicErrored";
            this.inventory.setStackInSlot(0, ItemStack.EMPTY);
            this.inventory.setStackInSlot(1, new ItemStack((ItemLike)AllItems.EMPTY_SCHEMATIC.get()));
            this.printer.resetSchematic();
            this.sendUpdate = true;
         } else if (this.printer.isWorldEmpty()) {
            this.state = SchematicannonBlockEntity.State.STOPPED;
            this.statusMsg = "schematicExpired";
            this.inventory.setStackInSlot(0, ItemStack.EMPTY);
            this.inventory.setStackInSlot(1, new ItemStack((ItemLike)AllItems.EMPTY_SCHEMATIC.get()));
            this.printer.resetSchematic();
            this.sendUpdate = true;
         } else if (!this.printer.getAnchor().closerThan(this.getBlockPos(), 256.0)) {
            this.state = SchematicannonBlockEntity.State.STOPPED;
            this.statusMsg = "targetOutsideRange";
            this.printer.resetSchematic();
            this.sendUpdate = true;
         } else {
            this.state = SchematicannonBlockEntity.State.PAUSED;
            this.statusMsg = "ready";
            this.updateChecklist();
            this.sendUpdate = true;
            this.blocksToPlace = this.blocksToPlace + this.blocksPlaced;
         }
      }
   }

   protected ItemStack getItemForBlock(BlockState blockState) {
      Item item = BlockItem.BY_BLOCK.getOrDefault(blockState.getBlock(), Items.AIR);
      return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
   }

   protected boolean grabItemsFromAttachedInventories(ItemRequirement.StackRequirement required, boolean simulate) {
      if (this.hasCreativeCrate) {
         return true;
      } else {
         this.attachedInventories.removeIf(Objects::isNull);
         ItemRequirement.ItemUseType usage = required.usage;
         if (usage == ItemRequirement.ItemUseType.DAMAGE) {
            for (IItemHandler cap : this.attachedInventories) {
               if (cap == null) {
                  cap = EmptyItemHandler.INSTANCE;
               }

               for (int slot = 0; slot < cap.getSlots(); slot++) {
                  ItemStack extractItem = cap.extractItem(slot, 1, true);
                  if (required.matches(extractItem) && extractItem.isDamageableItem()) {
                     if (!simulate) {
                        ItemStack stack = cap.extractItem(slot, 1, false);
                        stack.setDamageValue(stack.getDamageValue() + 1);
                        if (stack.getDamageValue() <= stack.getMaxDamage()) {
                           if (cap.getStackInSlot(slot).isEmpty()) {
                              cap.insertItem(slot, stack, false);
                           } else {
                              ItemHandlerHelper.insertItem(cap, stack, false);
                           }
                        }
                     }

                     return true;
                  }
               }
            }

            return false;
         } else {
            boolean success = false;
            int amountFound = 0;

            for (IItemHandler cap : this.attachedInventories) {
               if (cap == null) {
                  cap = EmptyItemHandler.INSTANCE;
               }

               amountFound += ItemHelper.extract(cap, required::matches, ItemHelper.ExtractionCountMode.UPTO, required.stack.getCount(), true).getCount();
               if (amountFound >= required.stack.getCount()) {
                  success = true;
                  break;
               }
            }

            if (!simulate && success) {
               amountFound = 0;

               for (IItemHandler cap : this.attachedInventories) {
                  if (cap == null) {
                     cap = EmptyItemHandler.INSTANCE;
                  }

                  amountFound += ItemHelper.extract(cap, required::matches, ItemHelper.ExtractionCountMode.UPTO, required.stack.getCount(), false).getCount();
                  if (amountFound >= required.stack.getCount()) {
                     break;
                  }
               }
            }

            return success;
         }
      }
   }

   public void finishedPrinting() {
      if (this.replaceMode == ConfigureSchematicannonPacket.Option.REPLACE_EMPTY.ordinal()) {
         this.printer.sendBlockUpdates(this.level);
      }

      this.inventory.setStackInSlot(0, ItemStack.EMPTY);
      this.inventory.setStackInSlot(1, new ItemStack((ItemLike)AllItems.EMPTY_SCHEMATIC.get(), this.inventory.getStackInSlot(1).getCount() + 1));
      this.state = SchematicannonBlockEntity.State.STOPPED;
      this.statusMsg = "finished";
      this.resetPrinter();
      AllSoundEvents.SCHEMATICANNON_FINISH.playOnServer(this.level, this.worldPosition);
      this.sendUpdate = true;
   }

   protected void resetPrinter() {
      this.printer.resetSchematic();
      this.missingItem = null;
      this.sendUpdate = true;
      this.schematicProgress = 0.0F;
      this.blocksPlaced = 0;
      this.blocksToPlace = 0;
   }

   protected boolean shouldPlace(BlockPos pos, BlockState state, BlockEntity be, BlockState toReplace, BlockState toReplaceOther, boolean isNormalCube) {
      if (pos.closerThan(this.getBlockPos(), 2.0)) {
         return false;
      } else if (this.replaceBlockEntities || !toReplace.hasBlockEntity() && (toReplaceOther == null || !toReplaceOther.hasBlockEntity())) {
         if (this.shouldIgnoreBlockState(state, be)) {
            return false;
         } else {
            boolean placingAir = state.isAir();
            if (this.replaceMode == 3) {
               return true;
            } else if (this.replaceMode == 2 && !placingAir) {
               return true;
            } else {
               return this.replaceMode == 1
                     && (
                        isNormalCube
                           || !toReplace.isRedstoneConductor(this.level, pos)
                              && (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(this.level, pos))
                     )
                     && !placingAir
                  ? true
                  : this.replaceMode == 0
                     && !toReplace.isRedstoneConductor(this.level, pos)
                     && (toReplaceOther == null || !toReplaceOther.isRedstoneConductor(this.level, pos))
                     && !placingAir;
            }
         }
      } else {
         return false;
      }
   }

   protected boolean shouldIgnoreBlockState(BlockState state, BlockEntity be) {
      if (state.getBlock() == Blocks.STRUCTURE_VOID) {
         return true;
      } else {
         ItemRequirement requirement = ItemRequirement.of(state, be);
         if (requirement.isEmpty()) {
            return false;
         } else if (requirement.isInvalid()) {
            return false;
         } else if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return true;
         } else if (state.hasProperty(BlockStateProperties.BED_PART) && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return true;
         } else if (state.getBlock() instanceof PistonHeadBlock) {
            return true;
         } else {
            return AllBlocks.BELT.has(state) ? state.getValue(BeltBlock.PART) == BeltPart.MIDDLE : false;
         }
      }
   }

   protected void tickFlyingBlocks() {
      List<LaunchedItem> toRemove = new LinkedList<>();

      for (LaunchedItem b : this.flyingBlocks) {
         if (b.update(this.level)) {
            toRemove.add(b);
         }
      }

      this.flyingBlocks.removeAll(toRemove);
   }

   protected void refillFuelIfPossible() {
      if (!this.hasCreativeCrate) {
         if (this.remainingFuel > this.getShotsPerGunpowder()) {
            this.remainingFuel = this.getShotsPerGunpowder();
            this.sendUpdate = true;
         } else if (this.remainingFuel <= 0) {
            if (!this.inventory.getStackInSlot(4).isEmpty()) {
               this.inventory.getStackInSlot(4).shrink(1);
            } else {
               boolean externalGunpowderFound = false;

               for (IItemHandler cap : this.attachedInventories) {
                  IItemHandler itemHandler = cap;
                  if (cap == null) {
                     itemHandler = EmptyItemHandler.INSTANCE;
                  }

                  if (!ItemHelper.extract(itemHandler, stack -> this.inventory.isItemValid(4, stack), 1, false).isEmpty()) {
                     externalGunpowderFound = true;
                     break;
                  }
               }

               if (!externalGunpowderFound) {
                  return;
               }
            }

            this.remainingFuel = this.remainingFuel + this.getShotsPerGunpowder();
            if (this.statusMsg.equals("noGunpowder")) {
               if (this.blocksPlaced > 0) {
                  this.state = SchematicannonBlockEntity.State.RUNNING;
               }

               this.statusMsg = "ready";
            }

            this.sendUpdate = true;
         }
      }
   }

   protected void tickPaperPrinter() {
      int BookInput = 2;
      int BookOutput = 3;
      ItemStack blueprint = this.inventory.getStackInSlot(0);
      ItemStack paper = this.inventory.extractItem(BookInput, 1, true);
      boolean outputFull = this.inventory.getStackInSlot(BookOutput).getCount() == this.inventory.getSlotLimit(BookOutput);
      if (!this.printer.isErrored()) {
         if (!this.printer.isLoaded()) {
            if (!blueprint.isEmpty()) {
               this.initializePrinter(blueprint);
            }
         } else if (!paper.isEmpty() && !outputFull) {
            if (this.bookPrintingProgress >= 1.0F) {
               this.bookPrintingProgress = 0.0F;
               if (!this.dontUpdateChecklist) {
                  this.updateChecklist();
               }

               this.dontUpdateChecklist = true;
               ItemStack extractItem = this.inventory.extractItem(BookInput, 1, false);
               ItemStack stack = AllBlocks.CLIPBOARD.isIn(extractItem) ? this.checklist.createWrittenClipboard() : this.checklist.createWrittenBook();
               stack.setCount(this.inventory.getStackInSlot(BookOutput).getCount() + 1);
               this.inventory.setStackInSlot(BookOutput, stack);
               this.sendUpdate = true;
            } else {
               this.bookPrintingProgress += 0.05F;
               this.sendUpdate = true;
            }
         } else {
            if (this.bookPrintingProgress != 0.0F) {
               this.sendUpdate = true;
            }

            this.bookPrintingProgress = 0.0F;
            this.dontUpdateChecklist = false;
         }
      }
   }

   public static BlockState stripBeltIfNotLast(BlockState blockState) {
      BeltPart part = (BeltPart)blockState.getValue(BeltBlock.PART);
      if (part == BeltPart.MIDDLE) {
         return Blocks.AIR.defaultBlockState();
      } else {
         boolean isLastSegment = false;
         Direction facing = (Direction)blockState.getValue(BeltBlock.HORIZONTAL_FACING);
         BeltSlope slope = (BeltSlope)blockState.getValue(BeltBlock.SLOPE);
         boolean positive = facing.getAxisDirection() == AxisDirection.POSITIVE;
         boolean start = part == BeltPart.START;
         boolean end = part == BeltPart.END;

         return switch (slope) {
               case DOWNWARD -> start;
               case UPWARD -> end;
               default -> positive && end || !positive && start;
            }
            ? blockState
            : (BlockState)AllBlocks.SHAFT
               .getDefaultState()
               .setValue(AbstractSimpleShaftBlock.AXIS, slope == BeltSlope.SIDEWAYS ? Axis.Y : facing.getClockWise().getAxis());
      }
   }

   protected void launchBlockOrBelt(BlockPos target, ItemStack icon, BlockState blockState, BlockEntity blockEntity) {
      if (!AllBlocks.BELT.has(blockState)) {
         CompoundTag data = BlockHelper.prepareBlockEntityData(this.level, blockState, blockEntity);
         this.launchBlock(target, icon, blockState, data);
      } else {
         blockState = stripBeltIfNotLast(blockState);
         if (blockEntity instanceof BeltBlockEntity bbe && AllBlocks.BELT.has(blockState)) {
            BeltBlockEntity.CasingType[] casings = new BeltBlockEntity.CasingType[bbe.beltLength];
            Arrays.fill(casings, BeltBlockEntity.CasingType.NONE);
            BlockPos currentPos = target;

            for (int i = 0; i < bbe.beltLength; i++) {
               BlockState currentState = bbe.getLevel().getBlockState(currentPos);
               if (!(currentState.getBlock() instanceof BeltBlock) || !(bbe.getLevel().getBlockEntity(currentPos) instanceof BeltBlockEntity beltAtSegment)) {
                  break;
               }

               casings[i] = beltAtSegment.casing;
               currentPos = BeltBlock.nextSegmentPosition(currentState, currentPos, blockState.getValue(BeltBlock.PART) != BeltPart.END);
            }

            this.launchBelt(target, blockState, bbe.beltLength, casings);
            return;
         }

         if (blockState != Blocks.AIR.defaultBlockState()) {
            this.launchBlock(target, icon, blockState, null);
         }
      }
   }

   protected void launchBelt(BlockPos target, BlockState state, int length, BeltBlockEntity.CasingType[] casings) {
      this.blocksPlaced++;
      ItemStack connector = AllItems.BELT_CONNECTOR.asStack();
      this.flyingBlocks.add(new LaunchedItem.ForBelt(this.getBlockPos(), target, connector, state, casings));
      this.playFiringSound();
   }

   protected void launchBlock(BlockPos target, ItemStack stack, BlockState state, @Nullable CompoundTag data) {
      if (!state.isAir()) {
         this.blocksPlaced++;
      }

      this.flyingBlocks.add(new LaunchedItem.ForBlockState(this.getBlockPos(), target, stack, state, data));
      this.playFiringSound();
   }

   protected void launchEntity(BlockPos target, ItemStack stack, Entity entity) {
      this.blocksPlaced++;
      this.flyingBlocks.add(new LaunchedItem.ForEntity(this.getBlockPos(), target, stack, entity));
      this.playFiringSound();
   }

   public void playFiringSound() {
      AllSoundEvents.SCHEMATICANNON_LAUNCH_BLOCK.playOnServer(this.level, this.worldPosition);
   }

   public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
      return SchematicannonMenu.create(id, inv, this);
   }

   public Component getDisplayName() {
      return CreateLang.translateDirect("gui.schematicannon.title");
   }

   public void updateChecklist() {
      this.checklist.required.clear();
      this.checklist.damageRequired.clear();
      this.checklist.blocksNotLoaded = false;
      if (this.printer.isLoaded() && !this.printer.isErrored()) {
         this.blocksToPlace = this.blocksPlaced;
         this.blocksToPlace = this.blocksToPlace + this.printer.markAllBlockRequirements(this.checklist, this.level, this::shouldPlace);
         this.printer.markAllEntityRequirements(this.checklist);
      }

      this.checklist.gathered.clear();
      this.findInventories();

      for (IItemHandler cap : this.attachedInventories) {
         if (cap != null) {
            for (int slot = 0; slot < cap.getSlots(); slot++) {
               ItemStack stackInSlot = cap.getStackInSlot(slot);
               if (!cap.extractItem(slot, 1, true).isEmpty()) {
                  this.checklist.collect(stackInSlot);
               }
            }
         }
      }

      this.sendUpdate = true;
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
   }

   @Override
   public void lazyTick() {
      super.lazyTick();
      this.findInventories();
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public AABB getRenderBoundingBox() {
      return AABB.INFINITE;
   }

   protected void applyImplicitComponents(DataComponentInput componentInput) {
      SchematicannonBlockEntity.SchematicannonOptions options = (SchematicannonBlockEntity.SchematicannonOptions)componentInput.getOrDefault(
         AllDataComponents.SCHEMATICANNON_OPTIONS, new SchematicannonBlockEntity.SchematicannonOptions(2, true, false)
      );
      this.replaceMode = options.replaceMode;
      this.skipMissing = options.skipMissing;
      this.replaceBlockEntities = options.replaceBlockEntities;
   }

   protected void collectImplicitComponents(Builder components) {
      components.set(
         AllDataComponents.SCHEMATICANNON_OPTIONS,
         new SchematicannonBlockEntity.SchematicannonOptions(this.replaceMode, this.skipMissing, this.replaceBlockEntities)
      );
   }

   public static record SchematicannonOptions(int replaceMode, boolean skipMissing, boolean replaceBlockEntities) {
      public static final Codec<SchematicannonBlockEntity.SchematicannonOptions> CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  Codec.INT.fieldOf("replace_mode").forGetter(SchematicannonBlockEntity.SchematicannonOptions::replaceMode),
                  Codec.BOOL.fieldOf("skip_missing").forGetter(SchematicannonBlockEntity.SchematicannonOptions::skipMissing),
                  Codec.BOOL.fieldOf("replace_block_entities").forGetter(SchematicannonBlockEntity.SchematicannonOptions::replaceBlockEntities)
               )
               .apply(i, SchematicannonBlockEntity.SchematicannonOptions::new)
      );
      public static final StreamCodec<ByteBuf, SchematicannonBlockEntity.SchematicannonOptions> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.INT,
         SchematicannonBlockEntity.SchematicannonOptions::replaceMode,
         ByteBufCodecs.BOOL,
         SchematicannonBlockEntity.SchematicannonOptions::skipMissing,
         ByteBufCodecs.BOOL,
         SchematicannonBlockEntity.SchematicannonOptions::replaceBlockEntities,
         SchematicannonBlockEntity.SchematicannonOptions::new
      );
   }

   public static enum State {
      STOPPED,
      PAUSED,
      RUNNING;
   }
}

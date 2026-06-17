package com.simibubi.create.content.schematics;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.Create;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.cannon.MaterialChecklist;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.IMergeableBE;
import com.simibubi.create.foundation.utility.BlockHelper;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.math.BBHelper;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;

public class SchematicPrinter {
   private boolean schematicLoaded;
   private boolean isErrored;
   private SchematicLevel blockReader;
   private BlockPos schematicAnchor;
   private BlockPos currentPos;
   private int printingEntityIndex = -1;
   private SchematicPrinter.PrintStage printStage = SchematicPrinter.PrintStage.BLOCKS;
   private List<BlockPos> deferredBlocks = new LinkedList<>();

   public void fromTag(CompoundTag compound, boolean clientPacket) {
      if (compound.contains("CurrentPos")) {
         this.currentPos = NBTHelper.readBlockPos(compound, "CurrentPos");
      }

      if (clientPacket) {
         this.schematicLoaded = false;
         if (compound.contains("Anchor")) {
            this.schematicAnchor = NBTHelper.readBlockPos(compound, "Anchor");
            this.schematicLoaded = true;
         }
      }

      this.printingEntityIndex = compound.getInt("EntityProgress");
      this.printStage = SchematicPrinter.PrintStage.valueOf(compound.getString("PrintStage"));
      compound.getList("DeferredBlocks", 10)
         .stream()
         .map(p -> NBTHelper.readBlockPos((CompoundTag)p, "Pos"))
         .collect(Collectors.toCollection(() -> this.deferredBlocks));
   }

   public void write(CompoundTag compound) {
      if (this.currentPos != null) {
         compound.put("CurrentPos", NbtUtils.writeBlockPos(this.currentPos));
      }

      if (this.schematicAnchor != null) {
         compound.put("Anchor", NbtUtils.writeBlockPos(this.schematicAnchor));
      }

      compound.putInt("EntityProgress", this.printingEntityIndex);
      compound.putString("PrintStage", this.printStage.name());
      ListTag tagDeferredBlocks = new ListTag();

      for (BlockPos p : this.deferredBlocks) {
         CompoundTag tag = new CompoundTag();
         tag.put("Pos", NbtUtils.writeBlockPos(p));
         tagDeferredBlocks.add(tag);
      }

      compound.put("DeferredBlocks", tagDeferredBlocks);
   }

   public void loadSchematic(ItemStack blueprint, Level originalWorld, boolean processNBT) {
      if (blueprint.has(AllDataComponents.SCHEMATIC_ANCHOR) && blueprint.has(AllDataComponents.SCHEMATIC_DEPLOYED)) {
         StructureTemplate activeTemplate = SchematicItem.loadSchematic(originalWorld, blueprint);
         StructurePlaceSettings settings = SchematicItem.getSettings(blueprint, processNBT);
         this.schematicAnchor = (BlockPos)blueprint.get(AllDataComponents.SCHEMATIC_ANCHOR);
         this.blockReader = new SchematicLevel(this.schematicAnchor, originalWorld);

         try {
            activeTemplate.placeInWorld(this.blockReader, this.schematicAnchor, this.schematicAnchor, settings, this.blockReader.getRandom(), 2);
         } catch (Exception var10) {
            Create.LOGGER.error("Failed to load Schematic for Printing", var10);
            this.schematicLoaded = true;
            this.isErrored = true;
            return;
         }

         BlockPos extraBounds = StructureTemplate.calculateRelativePosition(settings, new BlockPos(activeTemplate.getSize()).offset(-1, -1, -1));
         this.blockReader.setBounds(BBHelper.encapsulate(this.blockReader.getBounds(), extraBounds));
         StructureTransform transform = new StructureTransform(settings.getRotationPivot(), Axis.Y, settings.getRotation(), settings.getMirror());

         for (BlockEntity be : this.blockReader.getBlockEntities()) {
            transform.apply(be);
         }

         this.printingEntityIndex = -1;
         this.printStage = SchematicPrinter.PrintStage.BLOCKS;
         this.deferredBlocks.clear();
         BoundingBox bounds = this.blockReader.getBounds();
         this.currentPos = new BlockPos(bounds.minX() - 1, bounds.minY(), bounds.minZ());
         this.schematicLoaded = true;
      }
   }

   public void resetSchematic() {
      this.schematicLoaded = false;
      this.schematicAnchor = null;
      this.isErrored = false;
      this.currentPos = null;
      this.blockReader = null;
      this.printingEntityIndex = -1;
      this.printStage = SchematicPrinter.PrintStage.BLOCKS;
      this.deferredBlocks.clear();
   }

   public boolean isLoaded() {
      return this.schematicLoaded;
   }

   public boolean isErrored() {
      return this.isErrored;
   }

   public BlockPos getCurrentTarget() {
      return this.isLoaded() && !this.isErrored() ? this.schematicAnchor.offset(this.currentPos) : null;
   }

   public SchematicPrinter.PrintStage getPrintStage() {
      return this.printStage;
   }

   public BlockPos getAnchor() {
      return this.schematicAnchor;
   }

   public boolean isWorldEmpty() {
      return this.blockReader.getAllPositions().isEmpty();
   }

   public void handleCurrentTarget(SchematicPrinter.BlockTargetHandler blockHandler, SchematicPrinter.EntityTargetHandler entityHandler) {
      BlockPos target = this.getCurrentTarget();
      if (this.printStage == SchematicPrinter.PrintStage.ENTITIES) {
         Entity entity = (Entity)this.blockReader.getEntityList().get(this.printingEntityIndex);
         entityHandler.handle(target, entity);
      } else {
         BlockState blockState = BlockHelper.setZeroAge(this.blockReader.getBlockState(target));
         BlockEntity blockEntity = this.blockReader.getBlockEntity(target);
         blockHandler.handle(target, blockState, blockEntity);
      }
   }

   public boolean shouldPlaceCurrent(Level world) {
      return this.shouldPlaceCurrent(world, (a, b, c, d, e, f) -> true);
   }

   public boolean shouldPlaceCurrent(Level world, SchematicPrinter.PlacementPredicate predicate) {
      if (world == null) {
         return false;
      } else {
         return this.printStage == SchematicPrinter.PrintStage.ENTITIES ? true : this.shouldPlaceBlock(world, predicate, this.getCurrentTarget());
      }
   }

   public boolean shouldPlaceBlock(Level world, SchematicPrinter.PlacementPredicate predicate, BlockPos pos) {
      BlockState state = BlockHelper.setZeroAge(this.blockReader.getBlockState(pos));
      BlockEntity blockEntity = this.blockReader.getBlockEntity(pos);
      BlockState toReplace = world.getBlockState(pos);
      BlockEntity toReplaceBE = world.getBlockEntity(pos);
      BlockState toReplaceOther = null;
      if (state.hasProperty(BlockStateProperties.BED_PART)
         && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
         && state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT) {
         toReplaceOther = world.getBlockState(pos.relative((Direction)state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
      }

      if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
         toReplaceOther = world.getBlockState(pos.above());
      }

      boolean var10000;
      label51: {
         if (blockEntity != null && toReplaceBE instanceof IMergeableBE mergeBE && toReplaceBE.getType().equals(blockEntity.getType())) {
            var10000 = true;
            break label51;
         }

         var10000 = false;
      }

      boolean mergeTEs = var10000;
      if (!world.isLoaded(pos)) {
         return false;
      } else if (!world.getWorldBorder().isWithinBounds(pos)) {
         return false;
      } else if (toReplace == state && !mergeTEs) {
         return false;
      } else if (toReplace.getDestroySpeed(world, pos) != -1.0F && (toReplaceOther == null || toReplaceOther.getDestroySpeed(world, pos) != -1.0F)) {
         boolean isNormalCube = state.isRedstoneConductor(this.blockReader, this.currentPos);
         return predicate.shouldPlace(pos, state, blockEntity, toReplace, toReplaceOther, isNormalCube);
      } else {
         return false;
      }
   }

   public ItemRequirement getCurrentRequirement() {
      if (this.printStage == SchematicPrinter.PrintStage.ENTITIES) {
         return ItemRequirement.of((Entity)this.blockReader.getEntityList().get(this.printingEntityIndex));
      } else {
         BlockPos target = this.getCurrentTarget();
         BlockState blockState = BlockHelper.setZeroAge(this.blockReader.getBlockState(target));
         BlockEntity blockEntity = null;
         if (blockState.hasBlockEntity()) {
            blockEntity = ((EntityBlock)blockState.getBlock()).newBlockEntity(target, blockState);
            CompoundTag data = BlockHelper.prepareBlockEntityData(this.blockReader, blockState, this.blockReader.getBlockEntity(target));
            if (blockEntity != null && data != null) {
               blockEntity.loadWithComponents(data, this.blockReader.registryAccess());
            }
         }

         return ItemRequirement.of(blockState, blockEntity);
      }
   }

   public int markAllBlockRequirements(MaterialChecklist checklist, Level world, SchematicPrinter.PlacementPredicate predicate) {
      int blocksToPlace = 0;

      for (BlockPos pos : this.blockReader.getAllPositions()) {
         BlockPos relPos = pos.offset(this.schematicAnchor);
         BlockState required = this.blockReader.getBlockState(relPos);
         BlockEntity requiredBE = this.blockReader.getBlockEntity(relPos);
         if (!world.isLoaded(pos.offset(this.schematicAnchor))) {
            checklist.warnBlockNotLoaded();
         } else if (this.shouldPlaceBlock(world, predicate, relPos)) {
            ItemRequirement requirement = ItemRequirement.of(required, requiredBE);
            if (!requirement.isEmpty() && !requirement.isInvalid()) {
               checklist.require(requirement);
               blocksToPlace++;
            }
         }
      }

      return blocksToPlace;
   }

   public void markAllEntityRequirements(MaterialChecklist checklist) {
      for (Entity entity : this.blockReader.getEntityList()) {
         ItemRequirement requirement = ItemRequirement.of(entity);
         if (requirement.isEmpty()) {
            return;
         }

         if (requirement.isInvalid()) {
            return;
         }

         checklist.require(requirement);
      }
   }

   public boolean advanceCurrentPos() {
      List<Entity> entities = this.blockReader.getEntityList();

      do {
         if (this.printStage == SchematicPrinter.PrintStage.BLOCKS) {
            while (this.tryAdvanceCurrentPos()) {
               this.deferredBlocks.add(this.currentPos);
            }
         }

         if (this.printStage == SchematicPrinter.PrintStage.DEFERRED_BLOCKS) {
            if (this.deferredBlocks.isEmpty()) {
               this.printStage = SchematicPrinter.PrintStage.ENTITIES;
            } else {
               this.currentPos = this.deferredBlocks.remove(0);
            }
         }

         if (this.printStage == SchematicPrinter.PrintStage.ENTITIES) {
            if (this.printingEntityIndex + 1 >= entities.size()) {
               return false;
            }

            this.printingEntityIndex++;
            this.currentPos = entities.get(this.printingEntityIndex).blockPosition().subtract(this.schematicAnchor);
         }
      } while (!this.blockReader.getBounds().isInside(this.currentPos));

      return true;
   }

   public boolean tryAdvanceCurrentPos() {
      this.currentPos = this.currentPos.relative(Direction.EAST);
      BoundingBox bounds = this.blockReader.getBounds();
      BlockPos posInBounds = this.currentPos.offset(-bounds.minX(), -bounds.minY(), -bounds.minZ());
      if (posInBounds.getX() > bounds.getXSpan()) {
         this.currentPos = new BlockPos(bounds.minX(), this.currentPos.getY(), this.currentPos.getZ() + 1).west();
      }

      if (posInBounds.getZ() > bounds.getZSpan()) {
         this.currentPos = new BlockPos(this.currentPos.getX(), this.currentPos.getY() + 1, bounds.minZ()).west();
      }

      if (this.currentPos.getY() > bounds.getYSpan()) {
         this.printStage = SchematicPrinter.PrintStage.DEFERRED_BLOCKS;
         return false;
      } else {
         return shouldDeferBlock(this.blockReader.getBlockState(this.getCurrentTarget()));
      }
   }

   public static boolean shouldDeferBlock(BlockState state) {
      return AllBlocks.GANTRY_CARRIAGE.has(state) || AllBlocks.MECHANICAL_ARM.has(state) || BlockMovementChecks.isBrittle(state);
   }

   public void sendBlockUpdates(Level level) {
      BoundingBox bounds = this.blockReader.getBounds();
      BlockPos.betweenClosedStream(bounds.inflatedBy(1))
         .filter(pos -> !bounds.isInside(pos))
         .filter(pos -> level.isLoaded(pos.offset(this.schematicAnchor)) && level.getFluidState(pos.offset(this.schematicAnchor)).is(Fluids.WATER))
         .forEach(pos -> level.scheduleTick(pos.offset(this.schematicAnchor), Fluids.WATER, Fluids.WATER.getTickDelay(level)));
   }

   @FunctionalInterface
   public interface BlockTargetHandler {
      void handle(BlockPos var1, BlockState var2, BlockEntity var3);
   }

   @FunctionalInterface
   public interface EntityTargetHandler {
      void handle(BlockPos var1, Entity var2);
   }

   @FunctionalInterface
   public interface PlacementPredicate {
      boolean shouldPlace(BlockPos var1, BlockState var2, BlockEntity var3, BlockState var4, BlockState var5, boolean var6);
   }

   public static enum PrintStage {
      BLOCKS,
      DEFERRED_BLOCKS,
      ENTITIES;
   }
}

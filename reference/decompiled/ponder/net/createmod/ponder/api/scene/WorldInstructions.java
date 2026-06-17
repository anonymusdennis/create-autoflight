package net.createmod.ponder.api.scene;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

public interface WorldInstructions {
   Provider getHolderLookupProvider();

   void incrementBlockBreakingProgress(BlockPos var1);

   void showSection(Selection var1, Direction var2);

   void showSectionAndMerge(Selection var1, Direction var2, ElementLink<WorldSectionElement> var3);

   void glueBlockOnto(BlockPos var1, Direction var2, ElementLink<WorldSectionElement> var3);

   ElementLink<WorldSectionElement> showIndependentSection(Selection var1, Direction var2);

   ElementLink<WorldSectionElement> showIndependentSectionImmediately(Selection var1);

   void hideSection(Selection var1, Direction var2);

   void hideIndependentSection(ElementLink<WorldSectionElement> var1, Direction var2);

   void restoreBlocks(Selection var1);

   ElementLink<WorldSectionElement> makeSectionIndependent(Selection var1);

   void rotateSection(ElementLink<WorldSectionElement> var1, double var2, double var4, double var6, int var8);

   void configureCenterOfRotation(ElementLink<WorldSectionElement> var1, Vec3 var2);

   void configureStabilization(ElementLink<WorldSectionElement> var1, Vec3 var2);

   void moveSection(ElementLink<WorldSectionElement> var1, Vec3 var2, int var3);

   void setBlocks(Selection var1, BlockState var2, boolean var3);

   void destroyBlock(BlockPos var1);

   void setBlock(BlockPos var1, BlockState var2, boolean var3);

   void replaceBlocks(Selection var1, BlockState var2, boolean var3);

   void modifyBlock(BlockPos var1, UnaryOperator<BlockState> var2, boolean var3);

   void cycleBlockProperty(BlockPos var1, Property<?> var2);

   void modifyBlocks(Selection var1, UnaryOperator<BlockState> var2, boolean var3);

   void toggleRedstonePower(Selection var1);

   <T extends Entity> void modifyEntities(Class<T> var1, Consumer<T> var2);

   <T extends Entity> void modifyEntitiesInside(Class<T> var1, Selection var2, Consumer<T> var3);

   void modifyEntity(ElementLink<EntityElement> var1, Consumer<Entity> var2);

   ElementLink<EntityElement> createEntity(Function<Level, Entity> var1);

   ElementLink<EntityElement> createItemEntity(Vec3 var1, Vec3 var2, ItemStack var3);

   void modifyBlockEntityNBT(Selection var1, Class<? extends BlockEntity> var2, Consumer<CompoundTag> var3);

   <T extends BlockEntity> void modifyBlockEntity(BlockPos var1, Class<T> var2, Consumer<T> var3);

   void modifyBlockEntityNBT(Selection var1, Class<? extends BlockEntity> var2, Consumer<CompoundTag> var3, boolean var4);
}

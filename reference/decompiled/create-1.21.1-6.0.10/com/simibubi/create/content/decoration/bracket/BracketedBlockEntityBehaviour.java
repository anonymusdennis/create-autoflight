package com.simibubi.create.content.decoration.bracket;

import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.function.Predicate;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BracketedBlockEntityBehaviour extends BlockEntityBehaviour {
   public static final BehaviourType<BracketedBlockEntityBehaviour> TYPE = new BehaviourType<>();
   private BlockState bracket;
   private boolean reRender;
   private Predicate<BlockState> pred;

   public BracketedBlockEntityBehaviour(SmartBlockEntity be) {
      this(be, state -> true);
   }

   public BracketedBlockEntityBehaviour(SmartBlockEntity be, Predicate<BlockState> pred) {
      super(be);
      this.pred = pred;
   }

   @Override
   public BehaviourType<?> getType() {
      return TYPE;
   }

   public void applyBracket(BlockState state) {
      this.bracket = state;
      this.reRender = true;
      this.blockEntity.notifyUpdate();
      Level world = this.getWorld();
      if (!world.isClientSide) {
         this.blockEntity.getBlockState().updateNeighbourShapes(world, this.getPos(), 3);
      }
   }

   public void transformBracket(StructureTransform transform) {
      if (this.isBracketPresent()) {
         BlockState transformedBracket = transform.apply(this.bracket);
         this.applyBracket(transformedBracket);
      }
   }

   @Nullable
   public BlockState removeBracket(boolean inOnReplacedContext) {
      if (this.bracket == null) {
         return null;
      } else {
         BlockState removed = this.bracket;
         Level world = this.getWorld();
         if (!world.isClientSide) {
            world.levelEvent(2001, this.getPos(), Block.getId(this.bracket));
         }

         this.bracket = null;
         this.reRender = true;
         if (inOnReplacedContext) {
            this.blockEntity.sendData();
            return removed;
         } else {
            this.blockEntity.notifyUpdate();
            if (world.isClientSide) {
               return removed;
            } else {
               this.blockEntity.getBlockState().updateNeighbourShapes(world, this.getPos(), 3);
               return removed;
            }
         }
      }
   }

   public boolean isBracketPresent() {
      return this.bracket != null;
   }

   public boolean isBracketValid(BlockState bracketState) {
      return bracketState.getBlock() instanceof BracketBlock;
   }

   @Nullable
   public BlockState getBracket() {
      return this.bracket;
   }

   public boolean canHaveBracket() {
      return this.pred.test(this.blockEntity.getBlockState());
   }

   @Override
   public ItemRequirement getRequiredItems() {
      return !this.isBracketPresent() ? ItemRequirement.NONE : ItemRequirement.of(this.bracket, null);
   }

   @Override
   public boolean isSafeNBT() {
      return true;
   }

   @Override
   public void write(CompoundTag nbt, Provider registries, boolean clientPacket) {
      if (this.isBracketPresent() && this.isBracketValid(this.bracket)) {
         nbt.put("Bracket", NbtUtils.writeBlockState(this.bracket));
      }

      if (clientPacket && this.reRender) {
         NBTHelper.putMarker(nbt, "Redraw");
         this.reRender = false;
      }

      super.write(nbt, registries, clientPacket);
   }

   @Override
   public void read(CompoundTag nbt, Provider registries, boolean clientPacket) {
      if (nbt.contains("Bracket")) {
         this.bracket = null;
         BlockState readBlockState = NbtUtils.readBlockState(this.blockEntity.blockHolderGetter(), nbt.getCompound("Bracket"));
         if (this.isBracketValid(readBlockState)) {
            this.bracket = readBlockState;
         }
      }

      if (clientPacket && nbt.contains("Redraw")) {
         this.getWorld().sendBlockUpdated(this.getPos(), this.blockEntity.getBlockState(), this.blockEntity.getBlockState(), 16);
      }

      super.read(nbt, registries, clientPacket);
   }
}

package com.simibubi.create.content.logistics.crate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;

public class CreativeCrateBlockEntity extends CrateBlockEntity implements Clearable {
   FilteringBehaviour filtering;
   BottomlessItemHandler inv;

   public CreativeCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
      super(type, pos, state);
      this.inv = new BottomlessItemHandler(this.filtering::getFilter);
   }

   public static void registerCapabilities(RegisterCapabilitiesEvent event) {
      event.registerBlockEntity(ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.CREATIVE_CRATE.get(), (be, context) -> be.inv);
   }

   @Override
   public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
      behaviours.add(this.filtering = this.createFilter());
      this.filtering.setLabel(CreateLang.translateDirect("logistics.creative_crate.supply"));
   }

   @Override
   public void invalidate() {
      super.invalidate();
      if (this.inv != null) {
         this.invalidateCapabilities();
      }
   }

   public void clearContent() {
      this.filtering.setFilter(ItemStack.EMPTY);
   }

   public FilteringBehaviour createFilter() {
      return new FilteringBehaviour(this, new ValueBoxTransform() {
         @Override
         public void rotate(LevelAccessor level, BlockPos pos, BlockState state, PoseStack ms) {
            TransformStack.of(ms).rotateXDegrees(90.0F);
         }

         @Override
         public Vec3 getLocalOffset(LevelAccessor level, BlockPos pos, BlockState state) {
            return new Vec3(0.5, 0.84375, 0.5);
         }

         @Override
         public float getScale() {
            return super.getScale();
         }
      });
   }
}

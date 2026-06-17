package com.simibubi.create.api.event;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.Event;

public class BlockEntityBehaviourEvent extends Event {
   private final SmartBlockEntity smartBlockEntity;
   private final Map<BehaviourType<?>, BlockEntityBehaviour> behaviours;

   public BlockEntityBehaviourEvent(SmartBlockEntity blockEntity, Map<BehaviourType<?>, BlockEntityBehaviour> behaviours) {
      this.smartBlockEntity = blockEntity;
      this.behaviours = behaviours;
   }

   public <T extends SmartBlockEntity> void forType(BlockEntityType<T> type, Consumer<T> action) {
      if (this.smartBlockEntity.getType() == type) {
         action.accept((T)this.smartBlockEntity);
      }
   }

   public void attach(BlockEntityBehaviour behaviour) {
      this.behaviours.put(behaviour.getType(), behaviour);
   }

   public BlockEntityBehaviour remove(BehaviourType<?> type) {
      return this.behaviours.remove(type);
   }
}

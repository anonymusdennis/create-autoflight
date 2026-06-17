package com.simibubi.create.content.trains.bogey;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import java.util.function.Consumer;
import net.minecraft.nbt.CompoundTag;

public interface BogeyVisual {
   void update(CompoundTag var1, float var2, PoseStack var3);

   void hide();

   void updateLight(int var1);

   void collectCrumblingInstances(Consumer<Instance> var1);

   void delete();
}

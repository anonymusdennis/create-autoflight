package dev.ryanhcode.sable.neoforge.mixinhelper.compatibility.create.mechanical_arm;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint.Mode;
import java.util.Optional;
import net.createmod.catnip.nbt.NBTHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import org.apache.commons.lang3.mutable.MutableBoolean;

public record PointHolder(BlockPos pos, Mode interactionMode, MutableBoolean covered) {
   public CompoundTag serialize(BlockPos anchor) {
      CompoundTag tag = new CompoundTag();
      Tag pos = NbtUtils.writeBlockPos(this.pos.subtract(anchor));
      tag.put("pos", pos);
      NBTHelper.writeEnum(tag, "mode", this.interactionMode);
      return tag;
   }

   public static PointHolder deserialize(CompoundTag tag, BlockPos anchor) {
      Optional<BlockPos> pos = NbtUtils.readBlockPos(tag, "pos");
      return pos.<PointHolder>map(
            blockPos -> new PointHolder(blockPos.offset(anchor), (Mode)NBTHelper.readEnum(tag, "mode", Mode.class), new MutableBoolean(false))
         )
         .orElse(null);
   }
}

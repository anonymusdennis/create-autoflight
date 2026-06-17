package com.simibubi.create.api.contraption.storage.fluid;

import com.mojang.serialization.Codec;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public abstract class MountedFluidStorage implements IFluidHandler {
   public static final Codec<MountedFluidStorage> CODEC = MountedFluidStorageType.CODEC.dispatch(storage -> storage.type, type -> type.codec);
   public static final StreamCodec<RegistryFriendlyByteBuf, MountedFluidStorage> STREAM_CODEC = StreamCodec.of(
      (b, t) -> b.writeWithCodec(RegistryOps.create(NbtOps.INSTANCE, b.registryAccess()), CODEC, t),
      b -> (MountedFluidStorage)b.readWithCodecTrusted(RegistryOps.create(NbtOps.INSTANCE, b.registryAccess()), CODEC)
   );
   public final MountedFluidStorageType<? extends MountedFluidStorage> type;

   protected MountedFluidStorage(MountedFluidStorageType<?> type) {
      this.type = Objects.requireNonNull((MountedFluidStorageType<? extends MountedFluidStorage>)type);
   }

   public abstract void unmount(Level var1, BlockState var2, BlockPos var3, @Nullable BlockEntity var4);
}

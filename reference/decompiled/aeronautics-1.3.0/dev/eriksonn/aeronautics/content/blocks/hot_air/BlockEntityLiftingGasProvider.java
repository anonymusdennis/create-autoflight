package dev.eriksonn.aeronautics.content.blocks.hot_air;

import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.Balloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonBuilder;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.graph.BalloonLayerGraph;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.SavedBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.lifting_gas.LiftingGasType;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroAdvancements;
import dev.eriksonn.aeronautics.index.AeroTags;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3dc;

public interface BlockEntityLiftingGasProvider {
   static double getPredictedVolume(BlockEntityLiftingGasProvider.ClientBalloonInfo info, int ticksSinceSync) {
      double volumeInterp = info.clientBalloonFilled + info.clientBalloonChange * (double)ticksSinceSync;
      if (Math.abs(info.clientBalloonFilled - info.clientBalloonTarget - info.clientBalloonChange) > 0.01) {
         double r = (info.clientBalloonFilled - info.clientBalloonTarget) / (info.clientBalloonFilled - info.clientBalloonTarget - info.clientBalloonChange);
         volumeInterp = Math.pow(r, (double)ticksSinceSync) * (info.clientBalloonFilled - info.clientBalloonTarget) + info.clientBalloonTarget;
      }

      return volumeInterp;
   }

   static MutableComponent barComponent(int amount, int target, int total) {
      int lower = Math.min(amount, target - 1);
      int upper = Math.max(amount - target, 0);
      return Component.empty()
         .append(bars(Math.max(0, lower), ChatFormatting.DARK_AQUA))
         .append(bars(Math.max(0, target - lower - 1), ChatFormatting.DARK_GRAY))
         .append(bars(target == 0 ? 0 : 1, ChatFormatting.GOLD))
         .append(bars(upper, ChatFormatting.DARK_AQUA))
         .append(bars(Math.max(0, total - target - upper), ChatFormatting.DARK_GRAY));
   }

   private static MutableComponent bars(int count, ChatFormatting format) {
      return Component.literal(Strings.repeat('|', count)).withStyle(format);
   }

   default BlockPos getRaycastedPosition(Level level, Vec3 rayStart, Vec3 rayEnd) {
      BlockHitResult clip = level.clip(new ClipContext(rayStart, rayEnd, Block.COLLIDER, Fluid.NONE, CollisionContext.empty()));
      BlockPos hitBlockPos = clip.getBlockPos();
      return clip.getType() != Type.MISS && level.getBlockState(hitBlockPos).is(AeroTags.BlockTags.AIRTIGHT) ? hitBlockPos.relative(clip.getDirection()) : null;
   }

   Balloon getBalloon();

   void setBalloon(Balloon var1);

   default void tryJoinBalloon() {
      if (this.getBalloon() == null) {
         BlockPos castPos = this.getCastPosition();
         if (castPos != null) {
            Balloon existingBalloon = ((BalloonMap)BalloonMap.MAP.get(this.getLevel())).getBalloon(castPos);
            if (existingBalloon != null) {
               existingBalloon.addHeater(this);
               this.setBalloon(existingBalloon);
            }
         }
      }
   }

   default void tryCreateBalloon() {
      if (this.getBalloon() == null) {
         Level level = this.getLevel();
         BlockPos castPos = this.getCastPosition();
         BalloonMap balloonMap = (BalloonMap)BalloonMap.MAP.get(level);
         if (castPos != null) {
            Balloon newBalloon = BalloonBuilder.attemptBuildBalloon(this, castPos);
            if (newBalloon != null) {
               if (newBalloon instanceof ServerBalloon serverBalloon) {
                  Iterable<SavedBalloon> unloadedBalloons = balloonMap.getUnloadedBalloons();
                  Iterator<SavedBalloon> iter = unloadedBalloons.iterator();

                  while (iter.hasNext()) {
                     SavedBalloon unloaded = iter.next();
                     BalloonLayerGraph graph = newBalloon.getGraph();
                     if (graph.hasBlockAt(unloaded.controllerPos())) {
                        serverBalloon.loadFrom(unloaded);
                        balloonMap.markDirty();
                        iter.remove();
                        break;
                     }
                  }
               }

               this.setBalloon(newBalloon);
               balloonMap.addBalloon(newBalloon);
            }
         }
      }
   }

   default void removeFromBalloon() {
      Balloon balloon = this.getBalloon();
      if (balloon instanceof ServerBalloon serverBalloon) {
         balloon.removeHeater(this);
         if (this.isChunkUnloaded() && balloon.getHeaters().isEmpty()) {
            Level level = this.getLevel();
            if (!<unrepresentable>.$assertionsDisabled && level == null) {
               throw new AssertionError();
            }

            ((BalloonMap)BalloonMap.MAP.get(level)).unloadBalloon(serverBalloon);
         }

         this.setBalloon(null);
      }
   }

   default void addBalloonGoggleInformation(
      List<Component> tooltip, BlockEntityLiftingGasProvider.ClientBalloonInfo info, int ticksSinceSync, double airPressure
   ) {
      if (info != null) {
         int totalVolume = info.clientBalloonVolume;
         if (totalVolume == 0) {
            AeroLang.translate("lifting_gas.no_suitable_balloon").style(ChatFormatting.RED).forGoggles(tooltip, 2);
            return;
         }

         MutableComponent gasOutputComponent = AeroLang.translate("unit.meter_cubed", String.format("%.2f", this.getGasOutput()))
            .style(ChatFormatting.AQUA)
            .component();
         AeroLang.translate("lifting_gas.gas_output", this.getLiftingGasType().getName(), gasOutputComponent).style(ChatFormatting.GRAY).forGoggles(tooltip, 2);
         AeroLang.emptyLine(tooltip);
         AeroLang.translate("lifting_gas.balloon").forGoggles(tooltip, 1);
         int totalBar = 30;
         int targetBar = (int)Math.ceil(30.0 * info.clientBalloonTarget / (double)totalVolume);
         double volumeInterp = getPredictedVolume(info, ticksSinceSync);
         int volumeBar = Mth.clamp((int)Math.ceil(30.0 * volumeInterp / (double)totalVolume), 0, 30);
         MutableComponent base = barComponent(volumeBar, targetBar, 30);
         AeroLang.translate("lifting_gas.fill", base).style(ChatFormatting.GRAY).forGoggles(tooltip, 2);
         double lift = info.clientBalloonLift * airPressure;
         if (info.clientBalloonFilled > 0.01) {
            lift *= volumeInterp / info.clientBalloonFilled;
         }

         MutableComponent liftComponent = AeroLang.kilopixelGram(lift).style(ChatFormatting.AQUA).component();
         AeroLang.translate("lifting_gas.total_lift", liftComponent).style(ChatFormatting.GRAY).forGoggles(tooltip, 2);
         MutableComponent balloonVolumeComponent = AeroLang.translate("unit.meter_cubed", totalVolume).style(ChatFormatting.AQUA).component();
         AeroLang.translate("lifting_gas.balloon_volume", balloonVolumeComponent).style(ChatFormatting.GRAY).forGoggles(tooltip, 2);
      }
   }

   default double getAirPressure(BlockEntityLiftingGasProvider.ClientBalloonInfo balloonInfo, Level level) {
      Vector3dc globalPosition = Sable.HELPER.projectOutOfSubLevel(level, JOMLConversion.toJOML(balloonInfo.gasCenter()));
      return DimensionPhysicsData.getAirPressure(level, globalPosition);
   }

   @Nullable
   BlockPos getCastPosition();

   @Nullable
   void doRaycast();

   double getGasOutput();

   LiftingGasType getLiftingGasType();

   boolean canOutputGas();

   double getClientPredictedVolume();

   BlockPos getBlockPos();

   Level getLevel();

   boolean isChunkUnloaded();

   default void tickBalloonLogic() {
      this.doRaycast();
      if (this.getBalloon() == null) {
         this.tryJoinBalloon();
      }

      if (this.getBalloon() == null) {
         this.tryCreateBalloon();
      }

      if (this.getBalloon() instanceof ServerBalloon balloon && balloon.getTotalFilledVolume() > 1.0) {
         AeroAdvancements.HEAD_IN_THE_CLOUDS.awardToNearby(this.getBlockPos(), this.getLevel());
      }
   }

   static {
      if (<unrepresentable>.$assertionsDisabled) {
      }
   }

   public static record ClientBalloonInfo(
      int clientBalloonVolume, double clientBalloonFilled, double clientBalloonTarget, double clientBalloonLift, double clientBalloonChange, Vec3 gasCenter
   ) {
      public static void writeToNBT(CompoundTag tag, ServerBalloon balloon) {
         if (balloon != null && balloon.getCenter() != null) {
            tag.putInt("Volume", balloon.getCapacity());
            tag.putDouble("Filled", balloon.getTotalFilledVolume());
            tag.putDouble("Target", balloon.getTotalTargetVolume());
            tag.putDouble("Delta", balloon.getTotalVolumeChange());
            tag.putDouble("Lift", balloon.getTotalLift());
            tag.putDouble("CenterX", balloon.getCenter().x);
            tag.putDouble("CenterY", balloon.getCenter().y);
            tag.putDouble("CenterZ", balloon.getCenter().z);
         }
      }

      public static BlockEntityLiftingGasProvider.ClientBalloonInfo readFromNBT(CompoundTag tag) {
         return new BlockEntityLiftingGasProvider.ClientBalloonInfo(
            tag.getInt("Volume"),
            tag.getDouble("Filled"),
            tag.getDouble("Target"),
            tag.getDouble("Lift"),
            tag.getDouble("Delta"),
            new Vec3(tag.getDouble("CenterX"), tag.getDouble("CenterY"), tag.getDouble("CenterZ"))
         );
      }
   }
}

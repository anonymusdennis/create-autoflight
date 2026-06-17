package dev.simulated_team.simulated.events;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.index.SableAttributes;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeTrackingSystem;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerServerHandler;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.LodestoneTrackingMap;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffSubLevelObserver;
import dev.simulated_team.simulated.content.worldgen.SimulatedWorldPreset;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentPatch.Builder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

public class SimulatedCommonEvents {
   public static void onServerTickEnd(ServerLevel level) {
      RedstoneMagnetBlockEntity.GLOBAL_REDSTONE_MAGNET_MAP.tick(level);
      DockingConnectorBlockEntity.MAGNET_CONTROLLER.tick(level);
      LodestoneTrackingMap lodestoneMap = LodestoneTrackingMap.getOrLoad(level);
      if (lodestoneMap != null) {
         lodestoneMap.tick();
      }
   }

   public static void onWorldLoad(LevelAccessor level) {
   }

   public static void onBlockModifiedEvent(LevelAccessor level, BlockPos breakPos) {
   }

   public static void onServerStopped(MinecraftServer server) {
   }

   public static void onPlayerLoggedIn(Player player) {
      for (Entry<ResourceLocation, SimulatedWorldPreset> entry : SimWorldPresets.PRESETS.entrySet()) {
         ServerLevel level = (ServerLevel)player.level();
         if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            ResourceLocation worldPreset = ((PrimaryLevelDataExtension)level.getServer().getWorldData()).getPreset();
            if (entry.getValue().id().equals(worldPreset)) {
               entry.getValue().onPlayerJoin(level, serverPlayer);
            }
         }
      }

      PhysicsStaffServerHandler.sendAllData(player);
   }

   public static void onChunkLoad(LevelAccessor level, ChunkAccess chunk, boolean newChunk) {
      for (Entry<ResourceLocation, SimulatedWorldPreset> entry : SimWorldPresets.PRESETS.entrySet()) {
         if (level instanceof ServerLevel) {
            ServerLevel serverLevel = (ServerLevel)level;
            ResourceLocation worldPreset = ((PrimaryLevelDataExtension)serverLevel.getServer().getWorldData()).getPreset();
            if (entry.getValue().id().equals(worldPreset)) {
               entry.getValue().onChunkLoad(serverLevel, chunk, newChunk);
            }
         }
      }
   }

   public static void onPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      ServerLevel level = physicsSystem.getLevel();
      RedstoneMagnetBlockEntity.GLOBAL_REDSTONE_MAGNET_MAP.physicsTick(timeStep, level);
      DockingConnectorBlockEntity.MAGNET_CONTROLLER.physicsTick(timeStep, level);
      EndSeaPhysicsData.physicsTick(timeStep, level);
      ServerLevelRopeManager.getOrCreate(level).physicsTick(physicsSystem, timeStep);
      LaunchedPlungerServerHandler.physicsTickAllPlungers(physicsSystem, timeStep);
      PhysicsStaffServerHandler.get(level).physicsTick(physicsSystem);
   }

   @Nullable
   public static InteractionResult rightClickBlock(Level level, BlockPos pos, Player player, ItemStack useStack) {
      if (!level.getBlockState(pos).is(SimBlocks.SPRING) || !useStack.is(SimTags.Items.SPRING_ADJUSTER)) {
         return null;
      } else {
         return SpringBlock.tryAdjustSpring(level, pos, player) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
      }
   }

   public static void onPostPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      DiagramEntity.postPhysicsTick(physicsSystem.getLevel());
   }

   private static void onContainerReady(Level level, SubLevelContainer subLevelContainer) {
      if (subLevelContainer instanceof ServerSubLevelContainer serverContainer) {
         serverContainer.addObserver(new PhysicsStaffSubLevelObserver(serverContainer.getLevel()));
         SubLevelTrackingSystem trackingSystem = serverContainer.trackingSystem();
         trackingSystem.addTrackingPlugin(new ServerRopeTrackingSystem(serverContainer.getLevel()));
      }
   }

   public static void register() {
      SableEventPlatform.INSTANCE.onSubLevelContainerReady(SimulatedCommonEvents::onContainerReady);
   }

   public static void modifyDefaultComponents(BiConsumer<ItemLike, Consumer<Builder>> modify) {
      ResourceLocation basePunchStrengthId = Simulated.path("base_punch_strength");
      ResourceLocation basePunchCooldownId = Simulated.path("base_punch_cooldown");
      modify.accept(
         AllItems.EXTENDO_GRIP,
         builder -> {
            AttributeModifier strengthModifier = new AttributeModifier(basePunchStrengthId, 10.0, Operation.ADD_MULTIPLIED_TOTAL);
            AttributeModifier cooldownModifier = new AttributeModifier(basePunchCooldownId, 0.5, Operation.ADD_VALUE);
            builder.set(
               DataComponents.ATTRIBUTE_MODIFIERS,
               ItemAttributeModifiers.builder()
                  .add(SableAttributes.PUNCH_STRENGTH, strengthModifier, EquipmentSlotGroup.MAINHAND)
                  .add(SableAttributes.PUNCH_COOLDOWN, cooldownModifier, EquipmentSlotGroup.MAINHAND)
                  .build()
            );
         }
      );
      modify.accept(
         AllItems.CARDBOARD_SWORD,
         builder -> {
            AttributeModifier attributeModifier = new AttributeModifier(basePunchStrengthId, 2.0, Operation.ADD_MULTIPLIED_TOTAL);
            builder.set(
               DataComponents.ATTRIBUTE_MODIFIERS,
               ItemAttributeModifiers.builder().add(SableAttributes.PUNCH_STRENGTH, attributeModifier, EquipmentSlotGroup.MAINHAND).build()
            );
         }
      );
      SimulatedRegistrate.onAddDefaultComponents(modify);
   }
}

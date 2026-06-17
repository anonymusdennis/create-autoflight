package dev.simulated_team.simulated.content.items.plunger_launcher;

import com.simibubi.create.content.equipment.armor.BacktankUtil;
import com.simibubi.create.content.equipment.zapper.ShootableGadgetItemMethods;
import com.simibubi.create.foundation.item.CustomArmPoseItem;
import com.simibubi.create.foundation.utility.RaycastHelper;
import dev.ryanhcode.sable.Sable;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerServerHandler;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimSoundEvents;
import dev.simulated_team.simulated.mixin_interface.PlayerLaunchedPlungerExtension;
import dev.simulated_team.simulated.network.packets.PlungerLauncherShootPacket;
import dev.simulated_team.simulated.service.SimConfigService;
import dev.simulated_team.simulated.service.SimEntityService;
import foundry.veil.api.network.VeilPacketManager;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlungerLauncherItem extends Item implements CustomArmPoseItem {
   public static boolean reloadCooldown = false;

   public PlungerLauncherItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {
      return InteractionResult.PASS;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
      ItemStack heldStack = player.getItemInHand(interactionHand);
      if (SimEntityService.INSTANCE.isFake(player)) {
         return InteractionResultHolder.fail(heldStack);
      } else if (ShootableGadgetItemMethods.shouldSwap(player, heldStack, interactionHand, s -> s.getItem() instanceof PlungerLauncherItem)) {
         return InteractionResultHolder.fail(heldStack);
      } else {
         if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
               LaunchedPlungerServerHandler.removePlayerPlungers(player);
               player.displayClientMessage(SimLang.translate("plunger_launcher.clear_plungers").color(11184810).component(), true);
               return InteractionResultHolder.success(heldStack);
            }

            PlungerLauncherItem.BarrelAndCorrectionInfo info = this.getCorrectionInfo(player, interactionHand);
            Vec3 barrelPos = info.barrelPos();
            level.playSound(null, barrelPos.x, barrelPos.y, barrelPos.z, SimSoundEvents.PLUNGER_LAUNCH.event(), SoundSource.PLAYERS, 1.0F, 1.0F);
            LaunchedPlungerEntity newPlunger = (LaunchedPlungerEntity)SimEntityTypes.PLUNGER.create(level);
            newPlunger.setPos(barrelPos);
            newPlunger.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 0.5F, 0.0F);
            newPlunger.setOldPosAndRot();
            newPlunger.setDeltaMovement(info.motion());
            newPlunger.setOwner(player);
            level.addFreshEntity(newPlunger);
            PlayerLaunchedPlungerExtension duck = (PlayerLaunchedPlungerExtension)player;
            LaunchedPlungerEntity plunger = duck.simulated$getLaunchedPlunger();
            if (plunger != null && !plunger.isRemoved()) {
               duck.simulated$setLaunchedPlunger(null);
               newPlunger.setOther(plunger);
               plunger.setOther(newPlunger);
               ShootableGadgetItemMethods.applyCooldown(player, heldStack, interactionHand, b -> b.getItem() instanceof PlungerLauncherItem, 16);
               reloadCooldown = true;
            } else {
               newPlunger.setData(LaunchedPlungerEntity.IS_FIRST, true);
               duck.simulated$setLaunchedPlunger(newPlunger);
               ShootableGadgetItemMethods.applyCooldown(player, heldStack, interactionHand, b -> b.getItem() instanceof PlungerLauncherItem, 4);
               reloadCooldown = false;
            }

            player.awardStat(Stats.ITEM_USED.get(heldStack.getItem()));
            VeilPacketManager.player((ServerPlayer)player).sendPacket(new CustomPacketPayload[]{new PlungerLauncherShootPacket(interactionHand)});
            if (!BacktankUtil.canAbsorbDamage(player, maxUses())) {
               heldStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(interactionHand));
            }
         } else {
            SimulatedClient.PLUNGER_LAUNCHER_RENDER_HANDLER.dontAnimateItem(interactionHand);
         }

         return InteractionResultHolder.success(heldStack);
      }
   }

   public boolean isBarVisible(ItemStack stack) {
      return BacktankUtil.isBarVisible(stack, maxUses());
   }

   public int getBarWidth(ItemStack stack) {
      return BacktankUtil.getBarWidth(stack, maxUses());
   }

   public int getBarColor(ItemStack stack) {
      return BacktankUtil.getBarColor(stack, maxUses());
   }

   private static int maxUses() {
      return (Integer)SimConfigService.INSTANCE.server().equipment.maxPlungerLauncherShots.get();
   }

   @NotNull
   public PlungerLauncherItem.BarrelAndCorrectionInfo getCorrectionInfo(Player player, InteractionHand interactionHand) {
      Vec3 barrelPos = ShootableGadgetItemMethods.getGunBarrelVec(player, interactionHand == InteractionHand.MAIN_HAND, new Vec3(0.825F, -0.3F, 1.5));
      Level level = player.level();
      barrelPos = level.clip(new ClipContext(player.getEyePosition(), barrelPos, Block.COLLIDER, Fluid.NONE, CollisionContext.empty())).getLocation();
      barrelPos = Sable.HELPER.projectOutOfSubLevel(level, barrelPos);
      Vec3 motion = player.getLookAngle();
      BlockHitResult hit = RaycastHelper.rayTraceRange(level, player, 48.0);
      if (hit != null) {
         Vec3 projectedHit = Sable.HELPER.projectOutOfSubLevel(level, hit.getLocation());
         motion = projectedHit.subtract(barrelPos).normalize().scale(1.35F);
      }

      return new PlungerLauncherItem.BarrelAndCorrectionInfo(barrelPos, motion);
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   @Nullable
   public ArmPose getArmPose(ItemStack stack, AbstractClientPlayer player, InteractionHand hand) {
      return ArmPose.CROSSBOW_HOLD;
   }

   public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
      return false;
   }

   public int getEnchantmentValue() {
      return 1;
   }

   public static record BarrelAndCorrectionInfo(Vec3 barrelPos, Vec3 motion) {
   }
}

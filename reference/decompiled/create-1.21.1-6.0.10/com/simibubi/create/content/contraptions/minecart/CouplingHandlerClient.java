package com.simibubi.create.content.contraptions.minecart;

import com.simibubi.create.AllItems;
import net.createmod.catnip.math.VecHelper;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class CouplingHandlerClient {
   static AbstractMinecart selectedCart;
   static RandomSource r = RandomSource.create();

   public static void tick() {
      if (selectedCart != null) {
         spawnSelectionParticles(selectedCart.getBoundingBox(), false);
         LocalPlayer player = Minecraft.getInstance().player;
         ItemStack heldItemMainhand = player.getMainHandItem();
         ItemStack heldItemOffhand = player.getOffhandItem();
         if (!AllItems.MINECART_COUPLING.isIn(heldItemMainhand) && !AllItems.MINECART_COUPLING.isIn(heldItemOffhand)) {
            selectedCart = null;
         }
      }
   }

   static void onCartClicked(Player player, AbstractMinecart entity) {
      if (Minecraft.getInstance().player == player) {
         if (selectedCart != null && selectedCart != entity) {
            spawnSelectionParticles(entity.getBoundingBox(), true);
            CatnipServices.NETWORK.sendToServer(new CouplingCreationPacket(selectedCart, entity));
            selectedCart = null;
         } else {
            selectedCart = entity;
            spawnSelectionParticles(selectedCart.getBoundingBox(), true);
         }
      }
   }

   static void sneakClick() {
      selectedCart = null;
   }

   private static void spawnSelectionParticles(AABB AABB, boolean highlight) {
      ClientLevel world = Minecraft.getInstance().level;
      Vec3 center = AABB.getCenter();
      int amount = highlight ? 100 : 2;
      ParticleOptions particleData = (ParticleOptions)(highlight ? ParticleTypes.END_ROD : new DustParticleOptions(new Vector3f(1.0F, 1.0F, 1.0F), 1.0F));

      for (int i = 0; i < amount; i++) {
         Vec3 v = VecHelper.offsetRandomly(Vec3.ZERO, r, 1.0F);
         double yOffset = v.y;
         v = v.multiply(1.0, 0.0, 1.0).normalize().add(0.0, yOffset / 8.0, 0.0).add(center);
         world.addParticle(particleData, v.x, v.y, v.z, 0.0, 0.0, 0.0);
      }
   }
}

package com.simibubi.create.content.fluids.particle;

import com.simibubi.create.AllParticleTypes;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class FluidStackParticle extends TextureSheetParticle {
   private final float uo;
   private final float vo;
   private final FluidStack fluid;
   private final IClientFluidTypeExtensions clientFluid;

   public static FluidStackParticle create(
      ParticleType<FluidParticleData> type, ClientLevel world, FluidStack fluid, double x, double y, double z, double vx, double vy, double vz
   ) {
      return (FluidStackParticle)(type == AllParticleTypes.BASIN_FLUID.get()
         ? new BasinFluidParticle(world, fluid, x, y, z, vx, vy, vz)
         : new FluidStackParticle(world, fluid, x, y, z, vx, vy, vz));
   }

   public FluidStackParticle(ClientLevel world, FluidStack fluid, double x, double y, double z, double vx, double vy, double vz) {
      super(world, x, y, z, vx, vy, vz);
      this.clientFluid = IClientFluidTypeExtensions.of(fluid.getFluid());
      this.fluid = fluid;
      this.setSprite((TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(this.clientFluid.getStillTexture(fluid)));
      this.gravity = 1.0F;
      this.rCol = 0.8F;
      this.gCol = 0.8F;
      this.bCol = 0.8F;
      this.multiplyColor(this.clientFluid.getTintColor(fluid));
      this.xd = vx;
      this.yd = vy;
      this.zd = vz;
      this.quadSize /= 2.0F;
      this.uo = this.random.nextFloat() * 3.0F;
      this.vo = this.random.nextFloat() * 3.0F;
   }

   protected int getLightColor(float p_189214_1_) {
      int brightnessForRender = super.getLightColor(p_189214_1_);
      int skyLight = brightnessForRender >> 20;
      int blockLight = brightnessForRender >> 4 & 15;
      blockLight = Math.max(blockLight, this.fluid.getFluid().getFluidType().getLightLevel(this.fluid));
      return skyLight << 20 | blockLight << 4;
   }

   protected void multiplyColor(int color) {
      this.rCol *= (float)(color >> 16 & 0xFF) / 255.0F;
      this.gCol *= (float)(color >> 8 & 0xFF) / 255.0F;
      this.bCol *= (float)(color & 0xFF) / 255.0F;
   }

   protected float getU0() {
      return this.sprite.getU((this.uo + 1.0F) / 4.0F);
   }

   protected float getU1() {
      return this.sprite.getU(this.uo / 4.0F);
   }

   protected float getV0() {
      return this.sprite.getV(this.vo / 4.0F);
   }

   protected float getV1() {
      return this.sprite.getV((this.vo + 1.0F) / 4.0F);
   }

   public void tick() {
      super.tick();
      if (this.canEvaporate()) {
         if (this.onGround) {
            this.remove();
         }

         if (this.removed) {
            if (this.onGround || !(this.level.random.nextFloat() < 0.125F)) {
               Color color = new Color(this.clientFluid.getTintColor(this.fluid));
               this.level
                  .addParticle(
                     ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, color.getRedAsFloat(), color.getGreenAsFloat(), color.getBlueAsFloat()),
                     this.x,
                     this.y,
                     this.z,
                     0.0,
                     0.0,
                     0.0
                  );
            }
         }
      }
   }

   protected boolean canEvaporate() {
      return this.fluid.getFluid() instanceof PotionFluid;
   }

   @NotNull
   public ParticleRenderType getRenderType() {
      return ParticleRenderType.TERRAIN_SHEET;
   }
}

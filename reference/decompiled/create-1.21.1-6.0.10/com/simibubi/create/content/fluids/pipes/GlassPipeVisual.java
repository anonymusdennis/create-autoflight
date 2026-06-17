package com.simibubi.create.content.fluids.pipes;

import com.simibubi.create.content.fluids.FluidInstance;
import com.simibubi.create.content.fluids.FluidMesh;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.PipeConnection;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.DynamicVisual.Context;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.util.SmartRecycler;
import java.util.function.Consumer;
import java.util.function.Function;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class GlassPipeVisual extends AbstractBlockEntityVisual<StraightPipeBlockEntity> implements SimpleDynamicVisual {
   private int light;
   private final SmartRecycler<TextureAtlasSprite, FluidInstance> stream;
   private final SmartRecycler<TextureAtlasSprite, TransformedInstance> surface;

   public GlassPipeVisual(VisualizationContext ctx, StraightPipeBlockEntity blockEntity, float partialTick) {
      super(ctx, blockEntity, partialTick);
      this.stream = new SmartRecycler(
         sprite -> (FluidInstance)ctx.instancerProvider().instancer(AllInstanceTypes.FLUID, FluidMesh.stream(sprite)).createInstance()
      );
      this.surface = new SmartRecycler(
         sprite -> (TransformedInstance)ctx.instancerProvider().instancer(InstanceTypes.TRANSFORMED, FluidMesh.surface(sprite, 0.1875F)).createInstance()
      );
   }

   public void beginFrame(Context ctx) {
      this.stream.resetCount();
      this.surface.resetCount();
      FluidTransportBehaviour pipe = ((StraightPipeBlockEntity)this.blockEntity).getBehaviour(FluidTransportBehaviour.TYPE);
      if (pipe == null) {
         this.stream.discardExtra();
         this.surface.discardExtra();
      } else {
         for (Direction side : Iterate.directions) {
            PipeConnection.Flow flow = pipe.getFlow(side);
            if (flow != null) {
               FluidStack fluidStack = flow.fluid;
               if (!fluidStack.isEmpty()) {
                  LerpedFloat progressLerp = flow.progress;
                  if (progressLerp != null) {
                     float progress = progressLerp.getValue(ctx.partialTick());
                     boolean inbound = flow.inbound;
                     if (progress == 1.0F) {
                        if (inbound) {
                           PipeConnection.Flow opposite = pipe.getFlow(side.getOpposite());
                           if (opposite == null) {
                              progress -= 1.0E-6F;
                           }
                        } else {
                           FluidTransportBehaviour adjacent = BlockEntityBehaviour.get(this.level, this.pos.relative(side), FluidTransportBehaviour.TYPE);
                           if (adjacent == null) {
                              progress -= 1.0E-6F;
                           } else {
                              PipeConnection.Flow other = adjacent.getFlow(side.getOpposite());
                              if (other == null || !other.inbound && !other.complete) {
                                 progress -= 1.0E-6F;
                              }
                           }
                        }
                     }

                     Fluid fluid = fluidStack.getFluid();
                     IClientFluidTypeExtensions clientFluid = IClientFluidTypeExtensions.of(fluid);
                     FluidType fluidAttributes = fluid.getFluidType();
                     Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
                     TextureAtlasSprite flowTexture = atlas.apply(clientFluid.getFlowingTexture(fluidStack));
                     int color = clientFluid.getTintColor(fluidStack);
                     int blockLightIn = this.light >> 4 & 15;
                     int luminosity = Math.max(blockLightIn, fluidAttributes.getLightLevel(fluidStack));
                     int light = this.light & 15728640 | luminosity << 4;
                     if (inbound) {
                        side = side.getOpposite();
                     }

                     float yStart = inbound ? 0.0F : 0.5F;
                     float progressOffset = Mth.clamp(progress * 0.5F, 0.0F, 1.0F);
                     FluidInstance fluidInstance = (FluidInstance)this.stream.get(flowTexture);
                     ((TransformedInstance)((TransformedInstance)((TransformedInstance)fluidInstance.setIdentityTransform().translate(this.getVisualPosition()))
                              .center())
                           .rotateTo(Direction.UP, side))
                        .translate(0.0F, -0.5F + yStart, 0.0F);
                     fluidInstance.light(light).colorArgb(color);
                     fluidInstance.vScale = (flowTexture.getV1() - flowTexture.getV0()) * 0.5F;
                     fluidInstance.v0 = flowTexture.getV0() + yStart * fluidInstance.vScale;
                     fluidInstance.progress = progressOffset;
                     fluidInstance.setChanged();
                     if (progress != 1.0F) {
                        TextureAtlasSprite stillTexture = atlas.apply(clientFluid.getStillTexture(fluidStack));
                        ((TransformedInstance)((TransformedInstance)((TransformedInstance)((TransformedInstance)this.surface.get(stillTexture))
                                    .setIdentityTransform()
                                    .translate(this.getVisualPosition()))
                                 .center())
                              .rotateTo(Direction.UP, side))
                           .translate(0.0F, -0.5F + yStart + progressOffset, 0.0F)
                           .light(light)
                           .colorArgb(color)
                           .setChanged();
                     }
                  }
               }
            }
         }

         this.stream.discardExtra();
         this.surface.discardExtra();
      }
   }

   public void collectCrumblingInstances(Consumer<Instance> consumer) {
   }

   public void updateLight(float partialTick) {
      this.light = this.computePackedLight();
   }

   protected void _delete() {
      this.stream.delete();
      this.surface.delete();
   }
}

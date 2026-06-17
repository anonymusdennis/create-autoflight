package dev.simulated_team.simulated.client;

import com.simibubi.create.AllKeys;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.ryanhcode.sable.mixinterface.block_properties.BlockStateExtension;
import dev.ryanhcode.sable.physics.config.FloatingBlockMaterialDataHandler;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyTypes.PhysicsBlockPropertyType;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockMaterial;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.index.SimRegistries;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.mixin.accessor.BlockBehaviourAccessor;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import dev.simulated_team.simulated.util.SimColors;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockPropertiesTooltip {
   public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat();
   private static final Component NONE = Component.translatable("simulated.tooltip.mass.none").withStyle(ChatFormatting.GRAY);
   private static final Component SUPER_LIGHT = Component.translatable("simulated.tooltip.mass.super_light").withStyle(ChatFormatting.AQUA);
   private static final Component LIGHT = Component.translatable("simulated.tooltip.mass.light").withStyle(ChatFormatting.GREEN);
   private static final Component HEAVY = Component.translatable("simulated.tooltip.mass.heavy").withStyle(ChatFormatting.YELLOW);
   private static final Component SUPER_HEAVY = Component.translatable("simulated.tooltip.mass.super_heavy").withColor(SimColors.NUH_UH_RED);
   private static final Component ABSURDLY_HEAVY = Component.translatable("simulated.tooltip.mass.absurdly_heavy").withColor(SimColors.NUH_UH_RED);
   private static final Component BOUNCY = Component.translatable("simulated.tooltip.bouncy").withStyle(ChatFormatting.GREEN);
   private static final Component SLIPPERY = Component.translatable("simulated.tooltip.friction.slippery").withStyle(ChatFormatting.AQUA);
   private static final Component STICKY = Component.translatable("simulated.tooltip.friction.sticky").withStyle(ChatFormatting.DARK_GREEN);
   private static final Component FRAGILE = Component.translatable("simulated.tooltip.fragile").withColor(SimColors.NUH_UH_RED);
   private static final Component AIRTIGHT = Component.translatable("simulated.tooltip.airtight").withStyle(ChatFormatting.WHITE);
   private static final Component FLOATING = Component.translatable("simulated.tooltip.floating").withStyle(ChatFormatting.DARK_GREEN);

   public static boolean shouldShowTooltip(BlockPropertiesTooltip.Condition condition, TooltipFlag iTooltipFlag, @Nullable Player player) {
      if (Minecraft.getInstance().screen instanceof PonderUI) {
         return true;
      } else {
         return player == null ? condition.allows() : condition.test(AllKeys.isKeyDown(340), GogglesItem.isWearingGoggles(player));
      }
   }

   public static void register(SimulatedRegistrate registrate, String name, BlockPropertiesTooltip.TooltipFunction tooltipFunction, float priority) {
      registrate.propertyTooltip(name, () -> new BlockPropertiesTooltip.Entry(tooltipFunction, priority));
   }

   public static void init() {
      SimulatedRegistrate registrate = Simulated.getRegistrate();
      int priority = 0;
      register(registrate, "mass", BlockPropertiesTooltip::getMassComponent, (float)(priority++));
      register(registrate, "friction", BlockPropertiesTooltip::getFrictionComponent, (float)(priority++));
      register(registrate, "restitution", BlockPropertiesTooltip::getRestitutionComponent, (float)(priority++));
      register(registrate, "fragile", BlockPropertiesTooltip::getFragileComponent, (float)(priority++));
      register(registrate, "airtight", BlockPropertiesTooltip::getAirtightComponent, (float)(priority++));
      register(registrate, "floating", BlockPropertiesTooltip::getFloatingComponent, (float)(priority++));
   }

   public static void appendTooltip(ItemStack stack, TooltipFlag iTooltipFlag, Player player, List<Component> itemTooltip) {
      if (stack.getItem() instanceof BlockItem blockItem) {
         boolean showNumbers = true;
         BlockStateExtension properties = (BlockStateExtension)blockItem.getBlock().defaultBlockState();
         List<Component> toAdd = new ObjectArrayList();
         SimRegistries.PROPERTY_TOOLTIP.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEach(f -> {
            Component component = ((BlockPropertiesTooltip.Entry)f.getValue()).tooltipFunction.apply(properties, blockItem, true);
            if (component != null) {
               toAdd.add(component);
            }
         });
         if (!toAdd.isEmpty()) {
            for (Component property : toAdd) {
               itemTooltip.add(Component.literal(" ").append(property));
            }
         }
      }
   }

   public static Component getMassComponent(BlockStateExtension properties, BlockItem item, boolean showNumbers) {
      double mass = ((BlockBehaviourAccessor)item.getBlock()).getHasCollision()
         ? (Double)properties.sable$getProperty((PhysicsBlockPropertyType)PhysicsBlockPropertyTypes.MASS.get())
         : 0.0;
      if (mass == 1.0) {
         return null;
      } else {
         Component comp;
         if (mass <= 0.0) {
            comp = NONE;
         } else if (mass <= 0.25) {
            comp = SUPER_LIGHT;
         } else if (mass <= 0.5) {
            comp = LIGHT;
         } else if (mass < 4.0) {
            comp = HEAVY;
         } else if (mass < 50.0) {
            comp = SUPER_HEAVY;
         } else {
            comp = ABSURDLY_HEAVY;
         }

         return (Component)(showNumbers
            ? Component.empty().append(comp).append(formatValue("simulated.unit.mass", mass).withStyle(ChatFormatting.DARK_GRAY))
            : comp);
      }
   }

   @Nullable
   public static Component getRestitutionComponent(BlockStateExtension properties, BlockItem item, boolean showNumbers) {
      double restitution = (Double)properties.sable$getProperty((PhysicsBlockPropertyType)PhysicsBlockPropertyTypes.RESTITUTION.get());
      if (restitution == 0.0) {
         return null;
      } else {
         return (Component)(showNumbers
            ? Component.empty().append(BOUNCY).append(formatValue("simulated.unit.restitution", restitution * 100.0).withStyle(ChatFormatting.DARK_GRAY))
            : BOUNCY);
      }
   }

   @Nullable
   public static Component getFrictionComponent(BlockStateExtension properties, BlockItem item, boolean showNumbers) {
      double friction = (Double)properties.sable$getProperty((PhysicsBlockPropertyType)PhysicsBlockPropertyTypes.FRICTION.get());
      if (friction == 1.0) {
         return null;
      } else {
         Component comp;
         if (friction < 1.0) {
            comp = SLIPPERY;
         } else {
            comp = STICKY;
         }

         return (Component)(showNumbers
            ? Component.empty().append(comp).append(formatValue("simulated.unit.friction", friction).withStyle(ChatFormatting.DARK_GRAY))
            : comp);
      }
   }

   @Nullable
   public static Component getFragileComponent(BlockStateExtension properties, BlockItem item, boolean showNumbers) {
      boolean fragile = (Boolean)properties.sable$getProperty((PhysicsBlockPropertyType)PhysicsBlockPropertyTypes.FRAGILE.get());
      return fragile ? FRAGILE : null;
   }

   @Nullable
   public static Component getAirtightComponent(BlockStateExtension properties, BlockItem item, boolean showNumbers) {
      return item.getBlock().defaultBlockState().is(SimTags.Blocks.AIRTIGHT) ? AIRTIGHT : null;
   }

   @Nullable
   public static Component getFloatingComponent(BlockStateExtension properties, BlockItem item, boolean showNumbers) {
      ResourceLocation materialID = (ResourceLocation)properties.sable$getProperty((PhysicsBlockPropertyType)PhysicsBlockPropertyTypes.FLOATING_MATERIAL.get());
      if (materialID == null) {
         return null;
      } else {
         FloatingBlockMaterial material = (FloatingBlockMaterial)FloatingBlockMaterialDataHandler.allMaterials.get(materialID);
         if (material == null) {
            return null;
         } else {
            double materialScale = (Double)properties.sable$getProperty((PhysicsBlockPropertyType)PhysicsBlockPropertyTypes.FLOATING_SCALE.get());
            double liftStrength = material.liftStrength() * materialScale;
            if (liftStrength <= 0.0) {
               return null;
            } else {
               return (Component)(showNumbers
                  ? Component.empty().append(FLOATING).append(formatValue("simulated.unit.floating", liftStrength).withStyle(ChatFormatting.DARK_GRAY))
                  : FLOATING);
            }
         }
      }
   }

   private static MutableComponent formatValue(String key, double value) {
      String valueString = DECIMAL_FORMAT.format(value);
      return Component.literal(" (").append(Component.translatable(key, new Object[]{valueString})).append(")");
   }

   static {
      DECIMAL_FORMAT.setDecimalSeparatorAlwaysShown(false);
      DECIMAL_FORMAT.setMaximumFractionDigits(2);
      DECIMAL_FORMAT.setMinimumIntegerDigits(1);
   }

   public static enum Condition {
      ALWAYS(true, false, false),
      SHIFT(true, true, false),
      GOGGLES(true, false, true),
      SHIFT_GOGGLES(true, true, true),
      NEVER(false, false, false);

      private final boolean allow;
      private final boolean requireShift;
      private final boolean requireGoggles;

      private Condition(final boolean allow, final boolean requireShift, final boolean requireGoggles) {
         this.allow = allow;
         this.requireShift = requireShift;
         this.requireGoggles = requireGoggles;
      }

      public boolean test(boolean shift, boolean goggles) {
         return this.allow && (!this.requireShift || shift) && (!this.requireGoggles || goggles);
      }

      public boolean allows() {
         return this.allow;
      }
   }

   public static record Entry(BlockPropertiesTooltip.TooltipFunction tooltipFunction, float priority) implements Comparable<BlockPropertiesTooltip.Entry> {
      public int compareTo(@NotNull BlockPropertiesTooltip.Entry o) {
         return Float.compare(this.priority, o.priority);
      }
   }

   @FunctionalInterface
   public interface TooltipFunction {
      @Nullable
      Component apply(BlockStateExtension var1, BlockItem var2, boolean var3);
   }
}

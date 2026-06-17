package com.simibubi.create.content.logistics.box;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllEntityTypes;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.item.ItemHelper;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import net.createmod.catnip.codecs.stream.CatnipStreamCodecBuilders;
import net.createmod.catnip.data.Glob;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class PackageItem extends Item {
   public static final int SLOTS = 9;
   public PackageStyles.PackageStyle style;

   public PackageItem(Properties properties, PackageStyles.PackageStyle style) {
      super(properties);
      this.style = style;
      PackageStyles.ALL_BOXES.add(this);
      (style.rare() ? PackageStyles.RARE_BOXES : PackageStyles.STANDARD_BOXES).add(this);
   }

   public String getDescriptionId() {
      return "item.create" + (this.style.rare() ? ".rare_package" : ".package");
   }

   public static boolean isPackage(ItemStack stack) {
      return stack.getItem() instanceof PackageItem;
   }

   public boolean canFitInsideContainerItems() {
      return false;
   }

   public boolean hasCustomEntity(ItemStack stack) {
      return true;
   }

   public Entity createEntity(Level world, Entity location, ItemStack itemstack) {
      return PackageEntity.fromDroppedItem(world, location, itemstack);
   }

   public static ItemStack containing(List<ItemStack> stacks) {
      ItemStackHandler newInv = new ItemStackHandler(9);
      stacks.forEach(s -> ItemHandlerHelper.insertItemStacked(newInv, s, false));
      return containing(newInv);
   }

   public static ItemStack containing(ItemStackHandler stacks) {
      ItemStack box = PackageStyles.getRandomBox();
      box.set(AllDataComponents.PACKAGE_CONTENTS, ItemHelper.containerContentsFromHandler(stacks));
      return box;
   }

   public static void clearAddress(ItemStack box) {
      box.remove(AllDataComponents.PACKAGE_ADDRESS);
   }

   public static void addAddress(ItemStack box, String address) {
      box.set(AllDataComponents.PACKAGE_ADDRESS, address);
   }

   public static void setOrder(
      ItemStack box, int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, @Nullable PackageOrderWithCrafts orderContext
   ) {
      PackageItem.PackageOrderData order = new PackageItem.PackageOrderData(orderId, linkIndex, isFinalLink, fragmentIndex, isFinal, orderContext);
      box.set(AllDataComponents.PACKAGE_ORDER_DATA, order);
   }

   public static int getOrderId(ItemStack box) {
      return box.has(AllDataComponents.PACKAGE_ORDER_DATA) ? ((PackageItem.PackageOrderData)box.get(AllDataComponents.PACKAGE_ORDER_DATA)).orderId() : -1;
   }

   public static boolean hasOrderData(ItemStack box) {
      return box.has(AllDataComponents.PACKAGE_ORDER_DATA);
   }

   public static int getIndex(ItemStack box) {
      return box.has(AllDataComponents.PACKAGE_ORDER_DATA) ? ((PackageItem.PackageOrderData)box.get(AllDataComponents.PACKAGE_ORDER_DATA)).fragmentIndex() : -1;
   }

   public static boolean isFinal(ItemStack box) {
      return box.has(AllDataComponents.PACKAGE_ORDER_DATA) && ((PackageItem.PackageOrderData)box.get(AllDataComponents.PACKAGE_ORDER_DATA)).isFinal();
   }

   public static int getLinkIndex(ItemStack box) {
      return box.has(AllDataComponents.PACKAGE_ORDER_DATA) ? ((PackageItem.PackageOrderData)box.get(AllDataComponents.PACKAGE_ORDER_DATA)).linkIndex() : -1;
   }

   public static boolean isFinalLink(ItemStack box) {
      return box.has(AllDataComponents.PACKAGE_ORDER_DATA) && ((PackageItem.PackageOrderData)box.get(AllDataComponents.PACKAGE_ORDER_DATA)).isFinalLink();
   }

   @Nullable
   public static PackageOrderWithCrafts getOrderContext(ItemStack box) {
      if (box.has(AllDataComponents.PACKAGE_ORDER_DATA)) {
         PackageItem.PackageOrderData data = (PackageItem.PackageOrderData)box.get(AllDataComponents.PACKAGE_ORDER_DATA);
         return data.orderContext();
      } else {
         return box.has(AllDataComponents.PACKAGE_ORDER_CONTEXT) ? (PackageOrderWithCrafts)box.get(AllDataComponents.PACKAGE_ORDER_CONTEXT) : null;
      }
   }

   public static void addOrderContext(ItemStack box, PackageOrderWithCrafts orderContext) {
      box.set(AllDataComponents.PACKAGE_ORDER_CONTEXT, orderContext);
   }

   public static boolean matchAddress(ItemStack box, String address) {
      return matchAddress(getAddress(box), address);
   }

   public static boolean matchAddress(String boxAddress, String address) {
      if (address.isBlank()) {
         return boxAddress.isBlank();
      } else if (address.equals("*") || boxAddress.equals("*")) {
         return true;
      } else {
         return address.equals(boxAddress)
            ? true
            : address.matches(Glob.toRegexPattern(boxAddress, "")) || boxAddress.matches(Glob.toRegexPattern(address, ""));
      }
   }

   public static String getAddress(ItemStack box) {
      return (String)box.getOrDefault(AllDataComponents.PACKAGE_ADDRESS, "");
   }

   public static float getWidth(ItemStack box) {
      return box.getItem() instanceof PackageItem pi ? (float)pi.style.width() / 16.0F : 1.0F;
   }

   public static float getHeight(ItemStack box) {
      return box.getItem() instanceof PackageItem pi ? (float)pi.style.height() / 16.0F : 1.0F;
   }

   public static float getHookDistance(ItemStack box) {
      return box.getItem() instanceof PackageItem pi ? pi.style.riggingOffset() / 16.0F : 1.0F;
   }

   public static ItemStackHandler getContents(ItemStack box) {
      ItemStackHandler newInv = new ItemStackHandler(9);
      ItemContainerContents contents = (ItemContainerContents)box.getOrDefault(AllDataComponents.PACKAGE_CONTENTS, ItemContainerContents.EMPTY);
      ItemHelper.fillItemStackHandler(contents, newInv);
      return newInv;
   }

   public void appendHoverText(ItemStack stack, TooltipContext tooltipContext, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
      super.appendHoverText(stack, tooltipContext, tooltipComponents, tooltipFlag);
      if (stack.has(AllDataComponents.PACKAGE_ADDRESS)) {
         tooltipComponents.add(Component.literal("→ " + (String)stack.get(AllDataComponents.PACKAGE_ADDRESS)).withStyle(ChatFormatting.GOLD));
      }

      if (stack.has(AllDataComponents.PACKAGE_CONTENTS)) {
         int visibleNames = 0;
         int skippedNames = 0;
         ItemStackHandler contents = getContents(stack);

         for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack itemstack = contents.getStackInSlot(i);
            if (!itemstack.isEmpty() && !(itemstack.getItem() instanceof SpawnEggItem)) {
               if (visibleNames > 2) {
                  skippedNames++;
               } else {
                  visibleNames++;
                  tooltipComponents.add(
                     itemstack.getHoverName().copy().append(" x").append(String.valueOf(itemstack.getCount())).withStyle(ChatFormatting.GRAY)
                  );
               }
            }
         }

         if (skippedNames > 0) {
            tooltipComponents.add(Component.translatable("container.shulkerBox.more", new Object[]{skippedNames}).withStyle(ChatFormatting.ITALIC));
         }
      }
   }

   public int getUseDuration(ItemStack stack, LivingEntity entity) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack pStack) {
      return UseAnim.BOW;
   }

   public InteractionResultHolder<ItemStack> open(Level worldIn, Player playerIn, InteractionHand handIn) {
      ItemStack box = playerIn.getItemInHand(handIn);
      ItemStackHandler contents = getContents(box);
      ItemStack particle = box.copy();
      playerIn.setItemInHand(handIn, box.getCount() <= 1 ? ItemStack.EMPTY : box.copyWithCount(box.getCount() - 1));
      if (!worldIn.isClientSide()) {
         for (int i = 0; i < contents.getSlots(); i++) {
            ItemStack itemstack = contents.getStackInSlot(i);
            if (!itemstack.isEmpty()) {
               Item entitytype = itemstack.getItem();
               if (entitytype instanceof SpawnEggItem) {
                  SpawnEggItem sei = (SpawnEggItem)entitytype;
                  if (worldIn instanceof ServerLevel) {
                     ServerLevel sl = (ServerLevel)worldIn;
                     EntityType<?> entitytypex = sei.getType(itemstack);
                     Entity entity = entitytypex.spawn(
                        sl,
                        itemstack,
                        null,
                        BlockPos.containing(playerIn.position().add(playerIn.getLookAngle().multiply(1.0, 0.0, 1.0).normalize())),
                        MobSpawnType.SPAWN_EGG,
                        false,
                        false
                     );
                     if (entity != null) {
                        itemstack.shrink(1);
                     }
                  }
               }

               playerIn.getInventory().placeItemBackInInventory(itemstack.copy());
            }
         }
      }

      Vec3 position = playerIn.position();
      AllSoundEvents.PACKAGE_POP.playOnServer(worldIn, playerIn.blockPosition());
      if (worldIn.isClientSide()) {
         for (int ix = 0; ix < 10; ix++) {
            Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, worldIn.getRandom(), 0.125F);
            Vec3 pos = position.add(0.0, 0.5, 0.0).add(playerIn.getLookAngle().scale(0.5)).add(motion.scale(4.0));
            worldIn.addParticle(new ItemParticleOption(ParticleTypes.ITEM, particle), pos.x, pos.y, pos.z, motion.x, motion.y, motion.z);
         }
      }

      return new InteractionResultHolder(InteractionResult.SUCCESS, box);
   }

   public InteractionResult useOn(UseOnContext context) {
      if (context.getPlayer().isShiftKeyDown()) {
         return this.open(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
      } else {
         Vec3 point = context.getClickLocation();
         float h = (float)this.style.height() / 16.0F;
         float r = (float)this.style.width() / 2.0F / 16.0F;
         if (context.getClickedFace() == Direction.DOWN) {
            point = point.subtract(0.0, (double)(h + 0.25F), 0.0);
         } else if (context.getClickedFace().getAxis().isHorizontal()) {
            point = point.add(Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale((double)r));
         }

         AABB scanBB = new AABB(point, point).inflate((double)r, 0.0, (double)r).expandTowards(0.0, (double)h, 0.0);
         Level world = context.getLevel();
         if (!world.getEntities((EntityTypeTest)AllEntityTypes.PACKAGE.get(), scanBB, e -> true).isEmpty()) {
            return super.useOn(context);
         } else {
            PackageEntity packageEntity = new PackageEntity(world, point.x, point.y, point.z);
            ItemStack itemInHand = context.getItemInHand();
            packageEntity.setBox(itemInHand.copy());
            world.addFreshEntity(packageEntity);
            itemInHand.shrink(1);
            return InteractionResult.SUCCESS;
         }
      }
   }

   public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
      if (player.isShiftKeyDown()) {
         return this.open(world, player, hand);
      } else {
         ItemStack itemstack = player.getItemInHand(hand);
         player.startUsingItem(hand);
         return InteractionResultHolder.success(itemstack);
      }
   }

   public void releaseUsing(ItemStack stack, Level world, LivingEntity entity, int ticks) {
      if (entity instanceof Player player) {
         int i = this.getUseDuration(stack, entity) - ticks;
         if (i >= 0) {
            float f = getPackageVelocity(i);
            if (!((double)f < 0.1)) {
               if (!world.isClientSide) {
                  world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5F, 0.5F);
                  ItemStack copy = stack.copy();
                  if (!player.getAbilities().instabuild) {
                     stack.shrink(1);
                  }

                  Vec3 vec = new Vec3(entity.getX(), entity.getY() + entity.getBoundingBox().getYsize() / 2.0, entity.getZ());
                  Vec3 motion = entity.getLookAngle().scale((double)(f * 2.0F));
                  vec = vec.add(motion);
                  PackageEntity packageEntity = new PackageEntity(world, vec.x, vec.y, vec.z);
                  packageEntity.setBox(copy);
                  packageEntity.setDeltaMovement(motion);
                  packageEntity.tossedBy = new WeakReference<>(player);
                  world.addFreshEntity(packageEntity);
               }
            }
         }
      }
   }

   public static float getPackageVelocity(int p_185059_0_) {
      float f = (float)p_185059_0_ / 20.0F;
      f = (f * f + f * 2.0F) / 3.0F;
      if (f > 1.0F) {
         f = 1.0F;
      }

      return f;
   }

   public static record PackageOrderData(
      int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, @Nullable PackageOrderWithCrafts orderContext
   ) {
      public static final Codec<PackageItem.PackageOrderData> CODEC = RecordCodecBuilder.create(
         instance -> instance.group(
                  Codec.INT.fieldOf("order_id").forGetter(PackageItem.PackageOrderData::orderId),
                  Codec.INT.fieldOf("link_index").forGetter(PackageItem.PackageOrderData::linkIndex),
                  Codec.BOOL.fieldOf("is_final_link").forGetter(PackageItem.PackageOrderData::isFinalLink),
                  Codec.INT.fieldOf("fragment_index").forGetter(PackageItem.PackageOrderData::fragmentIndex),
                  Codec.BOOL.fieldOf("is_final").forGetter(PackageItem.PackageOrderData::isFinal),
                  PackageOrderWithCrafts.CODEC.optionalFieldOf("order_context").forGetter(i -> Optional.ofNullable(i.orderContext))
               )
               .apply(instance, PackageItem.PackageOrderData::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, PackageItem.PackageOrderData> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.INT,
         PackageItem.PackageOrderData::orderId,
         ByteBufCodecs.INT,
         PackageItem.PackageOrderData::linkIndex,
         ByteBufCodecs.BOOL,
         PackageItem.PackageOrderData::isFinalLink,
         ByteBufCodecs.INT,
         PackageItem.PackageOrderData::fragmentIndex,
         ByteBufCodecs.BOOL,
         PackageItem.PackageOrderData::isFinal,
         CatnipStreamCodecBuilders.nullable(PackageOrderWithCrafts.STREAM_CODEC),
         PackageItem.PackageOrderData::orderContext,
         PackageItem.PackageOrderData::new
      );

      public PackageOrderData(
         int orderId, int linkIndex, boolean isFinalLink, int fragmentIndex, boolean isFinal, Optional<PackageOrderWithCrafts> orderContext
      ) {
         this(orderId, linkIndex, isFinalLink, fragmentIndex, isFinal, orderContext.orElse(null));
      }
   }
}

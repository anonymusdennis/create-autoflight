package com.simibubi.create.content.logistics.packagerLink;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.createmod.catnip.codecs.CatnipCodecUtils;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RequestPromiseQueue {
   private Map<Item, List<RequestPromise>> promisesByItem = new IdentityHashMap<>();
   private Runnable onChanged;

   public RequestPromiseQueue(Runnable onChanged) {
      this.onChanged = onChanged;
   }

   public void add(RequestPromise promise) {
      this.promisesByItem.computeIfAbsent(promise.promisedStack.stack.getItem(), $ -> new LinkedList<>()).add(promise);
      this.onChanged.run();
   }

   public void setOnChanged(Runnable onChanged) {
      this.onChanged = onChanged;
   }

   public int getTotalPromisedAndRemoveExpired(ItemStack stack, int expiryTime) {
      int promised = 0;
      List<RequestPromise> list = this.promisesByItem.get(stack.getItem());
      if (list == null) {
         return promised;
      } else {
         Iterator<RequestPromise> iterator = list.iterator();

         while (iterator.hasNext()) {
            RequestPromise promise = iterator.next();
            if (ItemStack.isSameItemSameComponents(promise.promisedStack.stack, stack)) {
               if (expiryTime != -1 && promise.ticksExisted >= expiryTime) {
                  iterator.remove();
                  this.onChanged.run();
               } else {
                  promised += promise.promisedStack.count;
               }
            }
         }

         return promised;
      }
   }

   public void forceClear(ItemStack stack) {
      List<RequestPromise> list = this.promisesByItem.get(stack.getItem());
      if (list != null) {
         Iterator<RequestPromise> iterator = list.iterator();

         while (iterator.hasNext()) {
            RequestPromise promise = iterator.next();
            if (ItemStack.isSameItemSameComponents(promise.promisedStack.stack, stack)) {
               iterator.remove();
               this.onChanged.run();
            }
         }

         if (list.isEmpty()) {
            this.promisesByItem.remove(stack.getItem());
         }
      }
   }

   public void itemEnteredSystem(ItemStack stack, int amount) {
      List<RequestPromise> list = this.promisesByItem.get(stack.getItem());
      if (list != null) {
         Iterator<RequestPromise> iterator = list.iterator();

         while (iterator.hasNext()) {
            RequestPromise requestPromise = iterator.next();
            if (ItemStack.isSameItemSameComponents(requestPromise.promisedStack.stack, stack)) {
               int toSubtract = Math.min(amount, requestPromise.promisedStack.count);
               amount -= toSubtract;
               requestPromise.promisedStack.count -= toSubtract;
               if (requestPromise.promisedStack.count <= 0) {
                  iterator.remove();
                  this.onChanged.run();
               }

               if (amount <= 0) {
                  break;
               }
            }
         }

         if (list.isEmpty()) {
            this.promisesByItem.remove(stack.getItem());
         }
      }
   }

   public List<RequestPromise> flatten(boolean sorted) {
      List<RequestPromise> all = new ArrayList<>();
      this.promisesByItem.forEach((key, list) -> all.addAll(list));
      if (sorted) {
         all.sort(RequestPromise.ageComparator());
      }

      return all;
   }

   public CompoundTag write(Provider registries) {
      CompoundTag tag = new CompoundTag();
      tag.put("List", (Tag)CatnipCodecUtils.encode(Codec.list(RequestPromise.CODEC), registries, this.flatten(false)).orElseThrow());
      return tag;
   }

   public static RequestPromiseQueue read(CompoundTag tag, Provider registries, Runnable onChanged) {
      RequestPromiseQueue queue = new RequestPromiseQueue(onChanged);

      for (RequestPromise promise : CatnipCodecUtils.decode(Codec.list(RequestPromise.CODEC), registries, tag.get("List")).orElse(List.of())) {
         queue.add(promise);
      }

      return queue;
   }

   public void tick() {
      this.promisesByItem.forEach((key, list) -> list.forEach(RequestPromise::tick));
   }

   public boolean isEmpty() {
      return this.promisesByItem.isEmpty();
   }
}

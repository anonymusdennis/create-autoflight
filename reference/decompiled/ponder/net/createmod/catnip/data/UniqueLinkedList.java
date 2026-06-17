package net.createmod.catnip.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class UniqueLinkedList<E> extends LinkedList<E> {
   private static final long serialVersionUID = 1L;
   private final HashSet<E> contained = new HashSet<>();

   @Override
   public boolean contains(Object o) {
      return this.contained.contains(o);
   }

   @Override
   public E poll() {
      E e = super.poll();
      this.contained.remove(e);
      return e;
   }

   @Override
   public boolean add(E e) {
      return this.contained.add(e) ? super.add(e) : false;
   }

   @Override
   public void add(int index, E element) {
      if (this.contained.add(element)) {
         super.add(index, element);
      }
   }

   @Override
   public void addFirst(E e) {
      if (this.contained.add(e)) {
         super.addFirst(e);
      }
   }

   @Override
   public void addLast(E e) {
      if (this.contained.add(e)) {
         super.addLast(e);
      }
   }

   @Override
   public boolean addAll(Collection<? extends E> c) {
      List<E> filtered = new ArrayList<>();

      for (E i : c) {
         if (!this.contained.contains(i)) {
            filtered.add(i);
         }
      }

      return super.addAll(filtered);
   }

   @Override
   public boolean addAll(int index, Collection<? extends E> c) {
      List<E> filtered = new ArrayList<>();

      for (E i : c) {
         if (!this.contained.contains(i)) {
            filtered.add(i);
         }
      }

      return super.addAll(index, filtered);
   }

   @Override
   public boolean remove(Object o) {
      this.contained.remove(o);
      return super.remove(o);
   }

   @Override
   public E remove(int index) {
      E e = super.remove(index);
      this.contained.remove(e);
      return e;
   }

   @Override
   public E removeFirst() {
      E e = super.removeFirst();
      this.contained.remove(e);
      return e;
   }

   @Override
   public E removeLast() {
      E e = super.removeLast();
      this.contained.remove(e);
      return e;
   }

   @Override
   public void clear() {
      super.clear();
      this.contained.clear();
   }
}

package net.createmod.catnip.layout;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class PaginationState {
   private final boolean usesPagination;
   private int pageIndex;
   private final int elementsPerPage;
   private final int elementCount;

   public PaginationState() {
      this(false, 1, 1);
   }

   public PaginationState(boolean usesPagination, int elementsPerPage, int elementCount) {
      this(usesPagination, 0, elementsPerPage, elementCount);
   }

   public PaginationState(boolean usesPagination, int pageIndex, int elementsPerPage, int elementCount) {
      this.usesPagination = usesPagination;
      this.pageIndex = pageIndex;
      this.elementsPerPage = elementsPerPage;
      this.elementCount = elementCount;
   }

   public boolean usesPagination() {
      return this.usesPagination;
   }

   public int getPageIndex() {
      return this.pageIndex;
   }

   public int getMaxPages() {
      return !this.usesPagination ? 1 : (int)Math.ceil((double)this.elementCount / (double)this.elementsPerPage);
   }

   public int getElementsPerPage() {
      return this.elementsPerPage;
   }

   public int getElementCount() {
      return this.elementCount;
   }

   public int getStartIndex() {
      return this.pageIndex * this.elementsPerPage;
   }

   public int getCurrentPageElementCount() {
      return !this.usesPagination ? this.elementCount : Math.min(this.elementsPerPage, this.elementCount - this.pageIndex * this.elementsPerPage);
   }

   public void iterateForCurrentPage(BiConsumer<Integer, Integer> consumer) {
      for (int i = 0; i < this.getCurrentPageElementCount(); i++) {
         consumer.accept(i, i + this.getStartIndex());
      }
   }

   public boolean hasPreviousPage() {
      return !this.usesPagination ? false : this.pageIndex > 0;
   }

   public boolean hasNextPage() {
      return !this.usesPagination ? false : (this.pageIndex + 1) * this.elementsPerPage < this.elementCount;
   }

   public void nextPage() {
      if (this.hasNextPage()) {
         this.pageIndex++;
      }
   }

   public void previousPage() {
      if (this.hasPreviousPage()) {
         this.pageIndex--;
      }
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (obj != null && obj.getClass() == this.getClass()) {
         PaginationState that = (PaginationState)obj;
         return this.usesPagination == that.usesPagination
            && this.pageIndex == that.pageIndex
            && this.elementsPerPage == that.elementsPerPage
            && this.elementCount == that.elementCount;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.usesPagination, this.pageIndex, this.elementsPerPage, this.elementCount);
   }

   @Override
   public String toString() {
      return "PaginationState[usesPagination="
         + this.usesPagination
         + ", pageIndex="
         + this.pageIndex
         + ", elementsPerPage="
         + this.elementsPerPage
         + ", elementCount="
         + this.elementCount
         + "]";
   }
}

package dev.ryanhcode.sable.sublevel.storage;

import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import dev.ryanhcode.sable.sublevel.storage.serialization.SubLevelData;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class HoldingSubLevel {
   @NotNull
   private final SubLevelData data;
   private GlobalSavedSubLevelPointer pointer;

   public HoldingSubLevel(@NotNull SubLevelData data, GlobalSavedSubLevelPointer pointer) {
      this.data = data;
      this.pointer = pointer;
   }

   @NotNull
   public SubLevelData data() {
      return this.data;
   }

   public GlobalSavedSubLevelPointer pointer() {
      return this.pointer;
   }

   public void setPointer(GlobalSavedSubLevelPointer pointer) {
      this.pointer = pointer;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      } else if (obj != null && obj.getClass() == this.getClass()) {
         HoldingSubLevel that = (HoldingSubLevel)obj;
         return Objects.equals(this.data, that.data) && Objects.equals(this.pointer, that.pointer);
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.data, this.pointer);
   }

   @Override
   public String toString() {
      return "HoldingSubLevel[data=" + this.data + ", pointer=" + this.pointer + "]";
   }
}

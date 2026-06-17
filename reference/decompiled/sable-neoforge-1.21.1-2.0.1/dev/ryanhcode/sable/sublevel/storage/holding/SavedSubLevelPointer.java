package dev.ryanhcode.sable.sublevel.storage.holding;

public record SavedSubLevelPointer(short storageIndex, short subLevelIndex) {
   public int packed() {
      return this.storageIndex << 16 | this.subLevelIndex & 65535;
   }

   public static SavedSubLevelPointer unpack(int packed) {
      short storageIndex = (short)(packed >> 16);
      short subLevelIndex = (short)(packed & 65535);
      return new SavedSubLevelPointer(storageIndex, subLevelIndex);
   }

   @Override
   public String toString() {
      return "local->[storageIndex=" + this.storageIndex + ", subLevelIndex=" + this.subLevelIndex + "]";
   }
}

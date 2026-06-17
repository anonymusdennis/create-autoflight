package dev.ryanhcode.sable.sublevel.storage.region;

import dev.ryanhcode.sable.Sable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.BitSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.jetbrains.annotations.Nullable;

public class SubLevelStorageFile implements AutoCloseable {
   public static final String FILE_EXTENSION = ".slvls";
   public static final String SINGLE_FILE_EXTENSION = ".slvl";
   private static final ByteBuffer PADDING_BUFFER = ByteBuffer.allocateDirect(1);
   public static boolean COMPRESS_DATA = true;
   public static int EXTERNAL_MASK = 16;
   protected final BitSet usedSectors = new BitSet();
   protected final BitSet usedIndices = new BitSet();
   private final int beginningSectorSize = 4096;
   private final int sectorSize;
   private final Path path;
   private final Path externalFileDir;
   private final FileChannel file;
   private final ByteBuffer header;
   private final IntBuffer sectorSpans;

   public SubLevelStorageFile(Path path, Path externalFileDir, int sectorSize) throws IOException {
      this.path = path;
      this.externalFileDir = externalFileDir;
      this.sectorSize = sectorSize;
      this.file = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.DSYNC);
      int totalIndexCount = 4096 / 4;
      this.header = ByteBuffer.allocateDirect(4096);
      this.sectorSpans = this.header.asIntBuffer();
      this.sectorSpans.limit(totalIndexCount);
      this.header.position(0);
      this.usedSectors.set(0, 4096 / this.sectorSize, true);
      long existingSize = Files.size(path);
      int headerBytesRead = this.file.read(this.header, 0L);
      if (headerBytesRead != -1) {
         if (headerBytesRead != 4096) {
            Sable.LOGGER.error("Sub-level storage file {} has truncated header: {}", path, headerBytesRead);
         }

         for (int spanIndex = 0; spanIndex < totalIndexCount; spanIndex++) {
            int span = this.sectorSpans.get(spanIndex);
            this.usedIndices.set(spanIndex, span != 0);
            if (span != 0) {
               int spanStart = this.getSpanStart(span);
               int spanLength = this.getSpanLength(span);
               if ((long)spanStart * (long)sectorSize > existingSize) {
                  Sable.LOGGER
                     .warn(
                        "SubLevelStorageFile: Start of span at index {} in file {} is out of bounds (span start: {}, span length: {}, file size: {})",
                        new Object[]{spanIndex, path, spanStart, spanLength, existingSize}
                     );
               }

               if (spanStart >= 0 && spanLength > 0) {
                  for (int i = spanStart; i < spanStart + spanLength; i++) {
                     if (this.usedSectors.get(i)) {
                        Sable.LOGGER.warn("SubLevelStorageFile: Overlapping span at index {} in file {}", spanIndex, path);
                     }
                  }

                  this.usedSectors.set(spanStart, spanStart + spanLength, true);
               } else {
                  Sable.LOGGER.warn("SubLevelStorageFile: Invalid span at index {} in file {}", spanIndex, path);
               }
            }
         }
      }
   }

   public SubLevelStorageFile(Path path, Path externalFileDir) throws IOException {
      this(path, externalFileDir, 4096);
   }

   private static boolean isExternalStreamChunk(byte b) {
      return (b & EXTERNAL_MASK) != 0;
   }

   public int findFreeIndex() {
      return this.usedIndices.nextClearBit(0);
   }

   public int getTotalIndexCapacity() {
      return 4096 / 4;
   }

   private int sizeToSectors(int sizeBytes) {
      return (sizeBytes + this.sectorSize - 1) / this.sectorSize;
   }

   private Path getExternalFilePath(int index) {
      String string = index + ".slvl";
      return this.externalFileDir.resolve(string);
   }

   private InputStream createExternalSubLevelInputStream(int index) throws IOException {
      Path path = this.getExternalFilePath(index);
      if (Files.isRegularFile(path)) {
         return Files.newInputStream(path);
      } else {
         throw new IOException("External sub-level path " + path + " is not file");
      }
   }

   @Nullable
   public DataInputStream getSubLevelDataInputStream(int index) throws IOException {
      int span = this.sectorSpans.get(index);
      int spanStart = this.getSpanStart(span);
      int spanLength = this.getSpanLength(span);
      if (spanStart == 0) {
         return null;
      } else if (spanLength > 0 && spanStart + spanLength <= this.usedSectors.length()) {
         int bufferSize = spanLength * this.sectorSize;
         ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
         this.file.read(byteBuffer, (long)spanStart * (long)this.sectorSize);
         byteBuffer.flip();
         if (byteBuffer.remaining() < 5) {
            Sable.LOGGER.error("SubLevelStorageFile: Not enough data to read sector data header at index {} in file {}", index, this.path);
            return null;
         } else {
            int subLevelRemainingBytes = byteBuffer.getInt();
            byte dataType = byteBuffer.get();
            if (subLevelRemainingBytes == 0) {
               Sable.LOGGER
                  .warn("SubLevelStorageFile: Invalid sector data size at index {} in file {}: {}", new Object[]{index, this.path, subLevelRemainingBytes});
               return null;
            } else {
               int actualRemainingBytes = subLevelRemainingBytes - 1;
               if (isExternalStreamChunk(dataType)) {
                  if (actualRemainingBytes != 0) {
                     Sable.LOGGER.warn("Sub-level has both internal and external streams");
                  }

                  return new DataInputStream(this.createExternalSubLevelInputStream(index));
               } else if (actualRemainingBytes > byteBuffer.remaining()) {
                  Sable.LOGGER
                     .error("Sub-level {} stream is truncated: expected {} but read {}", new Object[]{index, actualRemainingBytes, byteBuffer.remaining()});
                  return null;
               } else if (actualRemainingBytes < 0) {
                  Sable.LOGGER.error("Declared size {} of sub-level {} is negative", subLevelRemainingBytes, index);
                  return null;
               } else {
                  return new DataInputStream(new ByteArrayInputStream(byteBuffer.array(), byteBuffer.position(), actualRemainingBytes));
               }
            }
         }
      } else {
         Sable.LOGGER.error("SubLevelStorageFile: Invalid span at index {} in file {}", index, this.path);
         return null;
      }
   }

   private void writeHeader() throws IOException {
      this.header.position(0);
      this.file.write(this.header, 0L);
   }

   private ByteBuffer createExternalStub() {
      ByteBuffer byteBuffer = ByteBuffer.allocate(5);
      byteBuffer.putInt(1);
      byteBuffer.put((byte)EXTERNAL_MASK);
      byteBuffer.flip();
      return byteBuffer;
   }

   protected void write(int index, ByteBuffer byteBuffer) throws IOException {
      int oldSpan = this.sectorSpans.get(index);
      int oldSectorStart = this.getSpanStart(oldSpan);
      int oldSpanLength = this.getSpanLength(oldSpan);
      int remaining = byteBuffer.remaining();
      int sectorsNeeded = this.sizeToSectors(remaining);
      boolean savingToExternalFile = false;
      Path temporaryExternalFile = null;
      if (sectorsNeeded > 255) {
         savingToExternalFile = true;
         sectorsNeeded = 1;
      }

      int sectorWriteStart = this.allocateSpace(sectorsNeeded);
      if (savingToExternalFile) {
         temporaryExternalFile = this.writeToExternalFile(byteBuffer);
         ByteBuffer stub = this.createExternalStub();
         this.file.write(stub, (long)sectorWriteStart * (long)this.sectorSize);
      } else {
         this.file.write(byteBuffer, (long)sectorWriteStart * (long)this.sectorSize);
      }

      this.sectorSpans.put(index, this.packSpan(sectorWriteStart, sectorsNeeded));
      this.usedIndices.set(index, true);
      this.writeHeader();
      if (savingToExternalFile) {
         Files.move(temporaryExternalFile, this.getExternalFilePath(index), StandardCopyOption.REPLACE_EXISTING);
      } else {
         Files.deleteIfExists(this.getExternalFilePath(index));
      }

      if (oldSectorStart != 0) {
         this.usedSectors.clear(oldSectorStart, oldSectorStart + oldSpanLength);
      }
   }

   private Path writeToExternalFile(ByteBuffer byteBuffer) throws IOException {
      Path tempFile = Files.createTempFile(this.externalFileDir, "tmp", null);

      try (FileChannel fileChannel = FileChannel.open(tempFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
         byteBuffer.position(5);
         fileChannel.write(byteBuffer);
      }

      return tempFile;
   }

   public int allocateSpace(int spaceNeeded) {
      int j = 0;

      while (true) {
         int start = this.usedSectors.nextClearBit(j);
         int nextSetBit = this.usedSectors.nextSetBit(start);
         if (nextSetBit == -1 || nextSetBit - start >= spaceNeeded) {
            this.usedSectors.set(start, start + spaceNeeded);
            return start;
         }

         j = nextSetBit;
      }
   }

   public void write(int index, @Nullable CompoundTag compoundTag) throws IOException {
      if (compoundTag == null) {
         this.clear(index);
      } else {
         try (DataOutputStream dataOutputStream = this.getSubLevelDataOutputStream(index)) {
            if (COMPRESS_DATA) {
               NbtIo.writeCompressed(compoundTag, dataOutputStream);
            } else {
               NbtIo.write(compoundTag, dataOutputStream);
            }
         }
      }
   }

   public CompoundTag read(int index) throws IOException {
      DataInputStream dataInputStream = this.getSubLevelDataInputStream(index);
      if (dataInputStream == null) {
         return null;
      } else {
         DataInputStream var3 = dataInputStream;

         CompoundTag var8;
         label47: {
            try {
               if (COMPRESS_DATA) {
                  var8 = NbtIo.readCompressed(dataInputStream, NbtAccounter.unlimitedHeap());
                  break label47;
               }

               var8 = NbtIo.read(dataInputStream);
            } catch (Throwable var7) {
               if (dataInputStream != null) {
                  try {
                     var3.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (dataInputStream != null) {
               dataInputStream.close();
            }

            return var8;
         }

         if (dataInputStream != null) {
            dataInputStream.close();
         }

         return var8;
      }
   }

   private void clear(int index) throws IOException {
      int span = this.sectorSpans.get(index);
      if (span != 0) {
         this.sectorSpans.put(index, 0);
         this.usedIndices.clear(index);
         int spanStart = this.getSpanStart(span);
         this.usedSectors.clear(spanStart, spanStart + this.getSpanLength(span));
         this.writeHeader();
      }
   }

   public DataOutputStream getSubLevelDataOutputStream(int index) {
      return new DataOutputStream(new SubLevelStorageFile.SectorSpanDataBuffer(index));
   }

   public Path getPath() {
      return this.path;
   }

   public int getSpanStart(int span) {
      return span >> 8 & 16777215;
   }

   public int getSpanLength(int span) {
      return span & 0xFF;
   }

   public int packSpan(int start, int length) {
      if (start >= 0 && length > 0 && length <= 255) {
         return start << 8 | length;
      } else {
         throw new IllegalArgumentException("Invalid span: start=" + start + ", length=" + length);
      }
   }

   @Override
   public void close() throws IOException {
      try {
         this.padOrTruncateToFullSector();
      } finally {
         try {
            this.file.force(true);
         } finally {
            this.file.close();
         }
      }
   }

   public void flush() throws IOException {
      this.file.force(true);
   }

   private void padOrTruncateToFullSector() throws IOException {
      int bytesNeededForFile = this.usedSectors.length() * this.sectorSize;
      int currentFileSize = (int)this.file.size();
      if (currentFileSize > bytesNeededForFile) {
         this.file.truncate((long)bytesNeededForFile);
      } else if (currentFileSize < bytesNeededForFile) {
         ByteBuffer byteBuffer = PADDING_BUFFER.duplicate();
         byteBuffer.position(0);
         this.file.write(byteBuffer, (long)(bytesNeededForFile - 1));
      }
   }

   class SectorSpanDataBuffer extends ByteArrayOutputStream {
      private final int subLevelIndex;

      public SectorSpanDataBuffer(final int subLevelIndex) {
         super(SubLevelStorageFile.this.sectorSize);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(0);
         super.write(0);
         this.subLevelIndex = subLevelIndex;
      }

      @Override
      public void close() throws IOException {
         ByteBuffer byteBuffer = ByteBuffer.wrap(this.buf, 0, this.count);
         int start = this.count - 4;
         byteBuffer.putInt(0, start);
         SubLevelStorageFile.this.write(this.subLevelIndex, byteBuffer);
      }
   }
}

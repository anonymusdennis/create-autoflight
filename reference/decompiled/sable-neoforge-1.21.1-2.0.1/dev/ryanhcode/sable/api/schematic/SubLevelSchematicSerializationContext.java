package dev.ryanhcode.sable.api.schematic;

import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.netty.util.concurrent.FastThreadLocal;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

public class SubLevelSchematicSerializationContext {
   private static final FastThreadLocal<SubLevelSchematicSerializationContext> THREAD_LOCAL = new FastThreadLocal();
   private final Map<UUID, SubLevelSchematicSerializationContext.SchematicMapping> mappings = new Object2ObjectOpenHashMap();
   private Function<BlockPos, BlockPos> placeTransform;
   private Function<BlockPos, BlockPos> setupTransform;
   private final SubLevelSchematicSerializationContext.Type type;
   private final BoundingBox3i boundingBox;

   public SubLevelSchematicSerializationContext(SubLevelSchematicSerializationContext.Type type, BoundingBox3i boundingBox) {
      this.type = type;
      this.boundingBox = boundingBox;
   }

   public SubLevelSchematicSerializationContext.Type getType() {
      return this.type;
   }

   public BoundingBox3i getBoundingBox() {
      return this.boundingBox;
   }

   public static SubLevelSchematicSerializationContext getCurrentContext() {
      return (SubLevelSchematicSerializationContext)THREAD_LOCAL.get();
   }

   @Internal
   public static void setCurrentContext(@Nullable SubLevelSchematicSerializationContext context) {
      THREAD_LOCAL.set(context);
   }

   public Function<BlockPos, BlockPos> getPlaceTransform() {
      return this.placeTransform;
   }

   public Function<BlockPos, BlockPos> getSetupTransform() {
      return this.setupTransform;
   }

   @Internal
   public void setPlaceTransform(Function<BlockPos, BlockPos> transform) {
      this.placeTransform = transform;
   }

   @Internal
   public void setSetupTransform(Function<BlockPos, BlockPos> transform) {
      this.setupTransform = transform;
   }

   @Nullable
   public SubLevelSchematicSerializationContext.SchematicMapping getMapping(SubLevel subLevel) {
      return this.mappings.get(subLevel.getUniqueId());
   }

   @Nullable
   public SubLevelSchematicSerializationContext.SchematicMapping getMapping(UUID uuid) {
      return this.mappings.get(uuid);
   }

   @Internal
   public Map<UUID, SubLevelSchematicSerializationContext.SchematicMapping> getMappings() {
      return this.mappings;
   }

   public static record SchematicMapping(Vector3dc newCorner, Quaterniondc newOrientation, UUID newUUID, Function<BlockPos, BlockPos> transform) {
   }

   public static enum Type {
      PLACE,
      SAVE;
   }
}

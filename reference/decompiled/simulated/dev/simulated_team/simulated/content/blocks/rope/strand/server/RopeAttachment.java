package dev.simulated_team.simulated.content.blocks.rope.strand.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

public record RopeAttachment(RopeAttachmentPoint point, @Nullable UUID subLevelID, BlockPos blockAttachment) {
   private static final Codec<RopeAttachmentPoint> ATTACHMENT_POINT_CODEC = Codec.STRING.xmap(RopeAttachmentPoint::valueOf, Enum::name);
   public static final Codec<RopeAttachment> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               ATTACHMENT_POINT_CODEC.fieldOf("point").forGetter(RopeAttachment::point),
               Codec.STRING
                  .optionalFieldOf("subLevelID")
                  .xmap(opt -> opt.map(UUID::fromString), uuid -> uuid.map(UUID::toString))
                  .forGetter(x -> Optional.ofNullable(x.subLevelID())),
               BlockPos.CODEC.fieldOf("blockAttachment").forGetter(RopeAttachment::blockAttachment)
            )
            .apply(instance, RopeAttachment::new)
   );

   private RopeAttachment(RopeAttachmentPoint point, Optional<UUID> subLevelID, BlockPos blockAttachment) {
      this(point, subLevelID.orElse(null), blockAttachment);
   }
}

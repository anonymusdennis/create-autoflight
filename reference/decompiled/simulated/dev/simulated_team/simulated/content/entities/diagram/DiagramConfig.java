package dev.simulated_team.simulated.content.entities.diagram;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.simulated_team.simulated.util.SimCodecUtil;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public class DiagramConfig {
   public static final Codec<DiagramConfig> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
               Codec.list(ResourceLocation.CODEC).fieldOf("enabled_force_groups").forGetter(DiagramConfig::enabledForceGroups),
               Codec.BOOL.fieldOf("display_center_of_mass").forGetter(DiagramConfig::displayCenterOfMass),
               Codec.BOOL.fieldOf("merge_forces").forGetter(DiagramConfig::mergeForces),
               Codec.DOUBLE.fieldOf("yaw").forGetter(DiagramConfig::yaw),
               Codec.DOUBLE.fieldOf("pitch").forGetter(DiagramConfig::pitch),
               DiagramConfig.NoteConfigs.NOTE_CONFIG_CODEC.fieldOf("note").forGetter(DiagramConfig::getNoteConfigs)
            )
            .apply(instance, DiagramConfig::new)
   );
   public static final StreamCodec<ByteBuf, DiagramConfig> STREAM_CODEC = StreamCodec.composite(
      ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
      DiagramConfig::enabledForceGroups,
      ByteBufCodecs.BOOL,
      DiagramConfig::displayCenterOfMass,
      ByteBufCodecs.BOOL,
      DiagramConfig::mergeForces,
      ByteBufCodecs.DOUBLE,
      DiagramConfig::yaw,
      ByteBufCodecs.DOUBLE,
      DiagramConfig::pitch,
      DiagramConfig.NoteConfigs.NOTE_CONFIG_STREAM_CODEC,
      DiagramConfig::getNoteConfigs,
      DiagramConfig::new
   );
   private final List<ResourceLocation> enabledForceGroups;
   private boolean displayCenterOfMass;
   private boolean mergeForces;
   private double yaw;
   private double pitch;
   private final DiagramConfig.NoteConfigs noteConfig;

   public static DiagramConfig makeDefault(DiagramEntity entity) {
      ObjectList<ResourceLocation> enabledForceGroups = new ObjectArrayList();

      for (ResourceLocation groupId : ForceGroups.REGISTRY.keySet()) {
         if (((ForceGroup)ForceGroups.REGISTRY.get(groupId)).defaultDisplayed()) {
            enabledForceGroups.add(groupId);
         }
      }

      DiagramConfig.NoteConfigs noteConfig = new DiagramConfig.NoteConfigs(new BoundingBox3d(), (double)(-entity.getYRot()), (double)entity.getXRot(), false);
      return new DiagramConfig(enabledForceGroups, false, false, (double)(-entity.getYRot()), (double)entity.getXRot(), noteConfig);
   }

   public DiagramConfig(
      List<ResourceLocation> enabledForceGroups,
      boolean displayCenterOfMass,
      boolean mergeForces,
      double yaw,
      double pitch,
      DiagramConfig.NoteConfigs noteConfig
   ) {
      this.enabledForceGroups = enabledForceGroups;
      this.displayCenterOfMass = displayCenterOfMass;
      this.mergeForces = mergeForces;
      this.yaw = yaw;
      this.pitch = pitch;
      this.noteConfig = noteConfig;
   }

   public List<ResourceLocation> enabledForceGroups() {
      return this.enabledForceGroups;
   }

   public boolean displayCenterOfMass() {
      return this.displayCenterOfMass;
   }

   public boolean mergeForces() {
      return this.mergeForces;
   }

   public double yaw() {
      return this.yaw;
   }

   public double pitch() {
      return this.pitch;
   }

   public void setDisplayCenterOfMass(boolean displayCenterOfMass) {
      this.displayCenterOfMass = displayCenterOfMass;
   }

   public void setMergeForces(boolean mergeForces) {
      this.mergeForces = mergeForces;
   }

   public void setYaw(double yaw) {
      this.yaw = yaw;
   }

   public void setPitch(double pitch) {
      this.pitch = pitch;
   }

   public DiagramConfig.NoteConfigs getNoteConfigs() {
      return this.noteConfig;
   }

   public static final class NoteConfigs {
      public static final Codec<DiagramConfig.NoteConfigs> NOTE_CONFIG_CODEC = RecordCodecBuilder.create(
         i -> i.group(
                  BoundingBox3d.CODEC.fieldOf("note_scope").forGetter(DiagramConfig.NoteConfigs::getNoteScope),
                  Codec.DOUBLE.fieldOf("note_yaw").forGetter(DiagramConfig.NoteConfigs::getNoteYaw),
                  Codec.DOUBLE.fieldOf("note_pitch").forGetter(DiagramConfig.NoteConfigs::getNotePitch),
                  Codec.BOOL.fieldOf("note_active").forGetter(DiagramConfig.NoteConfigs::isActive)
               )
               .apply(i, DiagramConfig.NoteConfigs::new)
      );
      public static final StreamCodec<ByteBuf, DiagramConfig.NoteConfigs> NOTE_CONFIG_STREAM_CODEC = StreamCodec.composite(
         SimCodecUtil.BOUNDING_BOX_3D_STREAM_CODEC,
         DiagramConfig.NoteConfigs::getNoteScope,
         ByteBufCodecs.DOUBLE,
         DiagramConfig.NoteConfigs::getNoteYaw,
         ByteBufCodecs.DOUBLE,
         DiagramConfig.NoteConfigs::getNotePitch,
         ByteBufCodecs.BOOL,
         DiagramConfig.NoteConfigs::isActive,
         DiagramConfig.NoteConfigs::new
      );
      private final BoundingBox3d noteScope;
      private double noteYaw;
      private double notePitch;
      private boolean active;

      public NoteConfigs(BoundingBox3d noteScope, double noteYaw, double notePitch, boolean active) {
         this.noteScope = noteScope;
         this.noteYaw = noteYaw;
         this.notePitch = notePitch;
         this.active = active;
      }

      public BoundingBox3d getNoteScope() {
         return this.noteScope;
      }

      public double getNotePitch() {
         return this.notePitch;
      }

      public double getNoteYaw() {
         return this.noteYaw;
      }

      public boolean isActive() {
         return this.active;
      }

      public void setActive(boolean active) {
         this.active = active;
      }

      public void setNotePitch(double notePitch) {
         this.notePitch = notePitch;
      }

      public void setNoteYaw(double noteYaw) {
         this.noteYaw = noteYaw;
      }
   }
}

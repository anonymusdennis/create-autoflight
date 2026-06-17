package dev.simulated_team.simulated.content.blocks.rope;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ClientRopeStrand;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.RopeAttachment;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeStrand;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface RopeStrandHolderBlockEntity extends BlockEntitySubLevelActor {
   RopeStrandHolderBehavior getBehavior();

   Vec3 getAttachmentPoint(BlockPos var1, BlockState var2);

   default Vec3 getVisualAttachmentPoint(BlockPos pos, BlockState state) {
      return this.getAttachmentPoint(pos, state);
   }

   @Nullable
   default Iterable<SubLevel> sable$getConnectionDependencies() {
      RopeStrandHolderBehavior behavior = this.getBehavior();
      SmartBlockEntity be = behavior.blockEntity;
      Level level = be.getLevel();
      SubLevelContainer container = SubLevelContainer.getContainer(level);
      if (!<unrepresentable>.$assertionsDisabled && container == null) {
         throw new AssertionError();
      } else {
         ServerRopeStrand serverStrand = behavior.getAttachedStrand();
         if (serverStrand != null) {
            ObjectList<SubLevel> dependencies = new ObjectArrayList();

            for (RopeAttachment attachment : serverStrand.getAttachments()) {
               UUID id = attachment.subLevelID();
               if (id != null) {
                  SubLevel subLevel = container.getSubLevel(id);
                  if (subLevel != null) {
                     dependencies.add(subLevel);
                  }
               }
            }

            return dependencies;
         } else {
            ClientRopeStrand clientStrand = behavior.getClientStrand();
            if (clientStrand != null) {
               ObjectList<SubLevel> dependencies = new ObjectArrayList();
               SubLevel subLevelA = Sable.HELPER.getContaining(level, clientStrand.startAttachment);
               SubLevel subLevelB = Sable.HELPER.getContaining(level, clientStrand.endAttachment);
               if (subLevelA != null) {
                  dependencies.add(subLevelA);
               }

               if (subLevelB != null) {
                  dependencies.add(subLevelB);
               }

               return dependencies;
            } else {
               return null;
            }
         }
      }
   }

   static {
      if (<unrepresentable>.$assertionsDisabled) {
      }
   }
}

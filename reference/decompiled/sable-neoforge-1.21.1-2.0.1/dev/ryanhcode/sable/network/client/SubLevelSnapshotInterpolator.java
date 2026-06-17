package dev.ryanhcode.sable.network.client;

import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Comparator;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class SubLevelSnapshotInterpolator {
   public final ObjectArrayList<SubLevelSnapshotInterpolator.Snapshot> buffer = new ObjectArrayList();
   private final Pose3d runningSnapshot = new Pose3d();
   private boolean stopped;

   public SubLevelSnapshotInterpolator(Pose3d pose) {
      this.runningSnapshot.set(pose);
   }

   public void getSampleAt(double gameTick, Pose3d dest) {
      int beforeIndex = -1;
      SubLevelSnapshotInterpolator.Snapshot before = null;
      SubLevelSnapshotInterpolator.Snapshot after = null;

      for (int i = 0; i < this.buffer.size(); i++) {
         SubLevelSnapshotInterpolator.Snapshot snapshot = (SubLevelSnapshotInterpolator.Snapshot)this.buffer.get(i);
         if ((double)snapshot.gameTick == gameTick) {
            dest.set(snapshot.pose);
            return;
         }

         if ((double)snapshot.gameTick < gameTick) {
            beforeIndex = i;
            before = snapshot;
         } else if ((double)snapshot.gameTick > gameTick) {
            after = snapshot;
            break;
         }
      }

      if (before != null && after != null) {
         double factor = (gameTick - (double)before.gameTick) / (double)(after.gameTick - before.gameTick);
         before.pose.lerp(after.pose, factor, dest);
      } else if (before != null) {
         dest.set(before.pose);
         int beforeBeforeIndex = beforeIndex - 1;
         if (beforeBeforeIndex >= 0 && !this.stopped) {
            SubLevelSnapshotInterpolator.Snapshot beforeBefore = (SubLevelSnapshotInterpolator.Snapshot)this.buffer.get(beforeBeforeIndex);
            double deadReckoningTicks = Mth.clamp(gameTick - (double)before.gameTick, 0.0, 1.0);
            double fraction = deadReckoningTicks / (double)(before.gameTick - beforeBefore.gameTick);
            dest.set(beforeBefore.pose).lerp(before.pose, 1.0 + fraction);
         }
      } else if (after != null) {
         dest.set(after.pose);
      }
   }

   public void receiveSnapshot(int gameTick, Pose3dc data) {
      synchronized (this.buffer) {
         if (this.buffer.isEmpty() || ((SubLevelSnapshotInterpolator.Snapshot)this.buffer.getLast()).gameTick != gameTick) {
            this.buffer.add(new SubLevelSnapshotInterpolator.Snapshot(gameTick, data));
         }
      }

      this.stopped = false;
   }

   public void setFirstPoses(Pose3dc poseA, Pose3dc poseB) {
      this.runningSnapshot.rotationPoint().set(poseA.rotationPoint());
      this.runningSnapshot.position().set(poseB.position());
   }

   public Pose3dc getInterpolatedPose() {
      return this.runningSnapshot;
   }

   public void receiveStop() {
      this.stopped = true;
   }

   public void splitFrom(SubLevelSnapshotInterpolator other, @NotNull Pose3dc pose) {
      ObjectListIterator var3 = other.buffer.iterator();

      while (var3.hasNext()) {
         SubLevelSnapshotInterpolator.Snapshot otherSnapshot = (SubLevelSnapshotInterpolator.Snapshot)var3.next();
         if (otherSnapshot.gameTick < ((SubLevelSnapshotInterpolator.Snapshot)this.buffer.getFirst()).gameTick) {
            Pose3dc containingPose = otherSnapshot.pose;
            Pose3d madeUpPastPose = new Pose3d(pose);
            madeUpPastPose.orientation().set(containingPose.orientation());
            containingPose.transformPosition(madeUpPastPose.position());
            this.buffer.add(new SubLevelSnapshotInterpolator.Snapshot(otherSnapshot.gameTick, madeUpPastPose));
         }
      }

      this.buffer.sort(Comparator.comparingDouble(a -> (double)a.gameTick));
   }

   public void tick(double backTick) {
      int bufferStartTime = (int)(backTick - 6.0);

      while (!this.buffer.isEmpty() && ((SubLevelSnapshotInterpolator.Snapshot)this.buffer.getFirst()).gameTick < bufferStartTime) {
         this.buffer.removeFirst();
      }

      if (!this.buffer.isEmpty()) {
         this.getSampleAt(backTick, this.runningSnapshot);
      }
   }

   public static record Snapshot(int gameTick, Pose3dc pose) {
   }
}

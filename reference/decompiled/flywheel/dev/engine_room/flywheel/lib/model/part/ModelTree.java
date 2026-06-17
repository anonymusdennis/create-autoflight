package dev.engine_room.flywheel.lib.model.part;

import dev.engine_room.flywheel.api.model.Model;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import net.minecraft.client.model.geom.PartPose;
import org.jetbrains.annotations.Nullable;

public final class ModelTree {
   @Nullable
   private final Model model;
   private final PartPose initialPose;
   private final ModelTree[] children;
   private final String[] childNames;

   public ModelTree(@Nullable Model model, PartPose initialPose, Map<String, ModelTree> children) {
      this.model = model;
      this.initialPose = initialPose;
      String[] childNames = children.keySet().toArray(String[]::new);
      Arrays.sort((Object[])childNames);
      ModelTree[] childArray = new ModelTree[childNames.length];

      for (int i = 0; i < childNames.length; i++) {
         childArray[i] = children.get(childNames[i]);
      }

      this.children = childArray;
      this.childNames = childNames;
   }

   @Nullable
   public Model model() {
      return this.model;
   }

   public PartPose initialPose() {
      return this.initialPose;
   }

   public int childCount() {
      return this.children.length;
   }

   public ModelTree child(int index) {
      return this.children[index];
   }

   public String childName(int index) {
      return this.childNames[index];
   }

   public int childIndex(String name) {
      return Arrays.binarySearch(this.childNames, name);
   }

   public boolean hasChild(String name) {
      return this.childIndex(name) >= 0;
   }

   @Nullable
   public ModelTree child(String name) {
      int index = this.childIndex(name);
      return index < 0 ? null : this.child(index);
   }

   public ModelTree childOrThrow(String name) {
      ModelTree child = this.child(name);
      if (child == null) {
         throw new NoSuchElementException("Can't find part " + name);
      } else {
         return child;
      }
   }
}

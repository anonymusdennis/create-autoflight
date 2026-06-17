package dev.ryanhcode.sable.command.argument;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class SubLevelSelector {
   private final SubLevelSelectorType type;
   private final List<Pair<SubLevelSelectorModifierType, SubLevelSelectorModifierType.Modifier>> modifiers;

   public SubLevelSelector(SubLevelSelectorType type, List<Pair<SubLevelSelectorModifierType, SubLevelSelectorModifierType.Modifier>> modifiers) {
      this.type = type;
      this.modifiers = modifiers;
   }

   public SubLevelSelectorType getSelectorType() {
      return this.type;
   }

   public Collection<ServerSubLevel> getSubLevels(CommandSourceStack source) throws CommandSyntaxException {
      if (this.type == null) {
         return List.of();
      } else {
         ServerLevel level = source.getLevel();
         ServerSubLevelContainer container = SableCommandHelper.requireSubLevelContainer(source);
         Iterable<ServerSubLevel> containerBodies = container.getAllSubLevels();
         Collection<ServerSubLevel> bodies = new ObjectArrayList();

         for (ServerSubLevel subLevel : containerBodies) {
            bodies.add(subLevel);
         }

         if (bodies.isEmpty()) {
            return Collections.emptySet();
         } else {
            ActiveSableCompanion helper = Sable.HELPER;

            Collection<ServerSubLevel> collectedSubLevels = (Collection<ServerSubLevel>)(switch (this.type) {
               case ALL -> new HashSet(bodies);
               case NEAREST -> {
                  double closest = Double.MAX_VALUE;
                  ServerSubLevel closestSubLevel = null;

                  for (ServerSubLevel body : bodies) {
                     Vec3 sourcePosition = helper.projectOutOfSubLevel(source.getLevel(), source.getPosition());
                     double distance = body.logicalPose().position().distance(sourcePosition.x, sourcePosition.y, sourcePosition.z);
                     if (distance < closest) {
                        closest = distance;
                        closestSubLevel = body;
                     }
                  }

                  yield Collections.singleton(closestSubLevel);
               }
               case RANDOM -> {
                  List<ServerSubLevel> list = new ArrayList<>(bodies);
                  yield Collections.singleton(list.get(level.random.nextInt(list.size())));
               }
               case INSIDE -> {
                  ServerSubLevel subLevel = (ServerSubLevel)helper.getContaining(level, source.getPosition());
                  yield subLevel != null ? Collections.singleton(subLevel) : Collections.emptySet();
               }
               case TRACKING -> {
                  if (source.getEntity() == null) {
                     yield Collections.emptySet();
                  } else {
                     ServerSubLevel subLevel = (ServerSubLevel)Sable.HELPER.getTrackingSubLevel(source.getEntity());
                     yield subLevel != null ? Collections.singleton(subLevel) : Collections.emptySet();
                  }
               }
               case VIEWED -> {
                  if (source.getEntity() != null) {
                     if (source.getEntity().pick(100.0, 1.0F, true) instanceof BlockHitResult blockHitResult) {
                        ServerSubLevel containing = (ServerSubLevel)helper.getContaining(level, blockHitResult.getBlockPos());
                        yield containing != null ? Collections.singleton(containing) : Collections.emptySet();
                     } else {
                        yield Collections.emptySet();
                     }
                  } else {
                     yield Collections.emptySet();
                  }
               }
               case LATEST -> {
                  List<ServerSubLevel> subLevels = container.getAllSubLevels();
                  yield subLevels.isEmpty() ? Collections.emptySet() : Collections.singleton(subLevels.getLast());
               }
            });
            List<ServerSubLevel> modifiedSubLevels = new ObjectArrayList(collectedSubLevels);
            Vector3d position = new Vector3d(source.getPosition().x, source.getPosition().y, source.getPosition().z);
            this.modifiers.sort(Comparator.comparingInt(a -> ((SubLevelSelectorModifierType)a.first()).getFilterPriority().ordinal()));

            for (Pair<SubLevelSelectorModifierType, SubLevelSelectorModifierType.Modifier> modifier : this.modifiers) {
               modifiedSubLevels = ((SubLevelSelectorModifierType.Modifier)modifier.right()).apply(modifiedSubLevels, position);
            }

            return modifiedSubLevels;
         }
      }
   }
}

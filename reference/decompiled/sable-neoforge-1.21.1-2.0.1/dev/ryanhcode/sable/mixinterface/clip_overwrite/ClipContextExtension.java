package dev.ryanhcode.sable.mixinterface.clip_overwrite;

import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.function.Predicate;
import org.jetbrains.annotations.Nullable;

public interface ClipContextExtension {
   @Nullable
   SubLevel sable$getIgnoredSubLevel();

   @Nullable
   Predicate<SubLevel> sable$getSubLevelIgnoring();

   void sable$setIgnoredSubLevel(@Nullable SubLevel var1);

   void sable$setSubLevelIgnoring(@Nullable Predicate<SubLevel> var1);

   void sable$setIgnoreMainLevel(boolean var1);

   boolean sable$isIgnoreMainLevel();

   void sable$setDoNotProject(boolean var1);

   boolean sable$doNotProject();
}

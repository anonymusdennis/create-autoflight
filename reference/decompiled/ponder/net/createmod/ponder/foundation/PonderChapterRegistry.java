package net.createmod.ponder.foundation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.createmod.catnip.data.Pair;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.minecraft.resources.ResourceLocation;

public class PonderChapterRegistry {
   private final Map<ResourceLocation, Pair<PonderChapter, List<StoryBoardEntry>>> chapters = new HashMap<>();

   PonderChapter addChapter(@Nonnull PonderChapter chapter) {
      synchronized (this.chapters) {
         this.chapters.put(chapter.getId(), Pair.of(chapter, new ArrayList<>()));
         return chapter;
      }
   }

   @Nullable
   PonderChapter getChapter(ResourceLocation id) {
      Pair<PonderChapter, List<StoryBoardEntry>> pair = this.chapters.get(id);
      return pair == null ? null : pair.getFirst();
   }

   public void addStoriesToChapter(@Nonnull PonderChapter chapter, StoryBoardEntry... entries) {
      List<StoryBoardEntry> entryList = this.chapters.get(chapter.getId()).getSecond();
      synchronized (entryList) {
         Collections.addAll(entryList, entries);
      }
   }

   public List<PonderChapter> getAllChapters() {
      return this.chapters.values().stream().map(Pair::getFirst).collect(Collectors.toList());
   }

   public List<StoryBoardEntry> getStories(PonderChapter chapter) {
      Pair<PonderChapter, List<StoryBoardEntry>> chapterPair = this.chapters.get(chapter.getId());
      return chapterPair == null ? List.of() : chapterPair.getSecond();
   }
}

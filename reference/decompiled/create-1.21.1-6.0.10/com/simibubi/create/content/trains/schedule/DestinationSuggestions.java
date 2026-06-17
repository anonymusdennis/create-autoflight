package com.simibubi.create.content.trains.schedule;

import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import java.util.ArrayList;
import java.util.List;
import net.createmod.catnip.data.IntAttached;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CommandSuggestions.SuggestionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

public class DestinationSuggestions extends CommandSuggestions {
   private EditBox textBox;
   private List<IntAttached<String>> viableStations;
   private String previous = "<>";
   private Font font;
   private boolean active;
   List<Suggestion> currentSuggestions;
   private int yOffset;

   public DestinationSuggestions(
      Minecraft pMinecraft, Screen pScreen, EditBox pInput, Font pFont, List<IntAttached<String>> viableStations, boolean anchorToBottom, int yOffset
   ) {
      super(pMinecraft, pScreen, pInput, pFont, true, true, 0, 7, anchorToBottom, -298831824);
      this.textBox = pInput;
      this.font = pFont;
      this.viableStations = viableStations;
      this.yOffset = yOffset;
      this.currentSuggestions = new ArrayList<>();
      this.active = false;
   }

   public void tick() {
      if (this.suggestions == null) {
         this.textBox.setSuggestion("");
      }

      if (this.active != this.textBox.isFocused()) {
         this.active = this.textBox.isFocused();
         this.updateCommandInfo();
      }
   }

   public void updateCommandInfo() {
      if (this.textBox.getValue().length() >= this.textBox.getCursorPosition()) {
         String trimmed = this.textBox.getValue().substring(0, this.textBox.getCursorPosition());
         if (!this.textBox.getHighlighted().isBlank()) {
            trimmed = trimmed.replace(this.textBox.getHighlighted(), "");
         }

         String value = trimmed;
         if (!value.equals(this.previous)) {
            if (!this.active) {
               this.suggestions = null;
            } else {
               this.previous = value;
               this.currentSuggestions = this.viableStations
                  .stream()
                  .filter(ia -> !((String)ia.getValue()).equals(value) && ((String)ia.getValue()).toLowerCase().startsWith(value.toLowerCase()))
                  .sorted((ia1, ia2) -> Integer.compare((Integer)ia1.getFirst(), (Integer)ia2.getFirst()))
                  .<String>map(IntAttached::getValue)
                  .map(s -> new Suggestion(new StringRange(0, 1000), s))
                  .toList();
               this.showSuggestions(false);
            }
         }
      }
   }

   public void showSuggestions(boolean pNarrateFirstSuggestion) {
      if (this.currentSuggestions.isEmpty()) {
         this.suggestions = null;
      } else {
         int width = 0;

         for (Suggestion suggestion : this.currentSuggestions) {
            width = Math.max(width, this.font.width(suggestion.getText()));
         }

         int x = Mth.clamp(this.textBox.getScreenX(0), 0, this.textBox.getScreenX(0) + this.textBox.getInnerWidth() - width);
         this.suggestions = new SuggestionsList(this, x, 72 + this.yOffset, width, this.currentSuggestions, false);
      }
   }

   public boolean isEmpty() {
      return this.viableStations.isEmpty();
   }
}

package com.simibubi.create.content.trains.station;

import com.simibubi.create.foundation.mixin.accessor.FontAccessor;
import java.util.List;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;

public class NoShadowFontWrapper extends Font {
   private Font wrapped;

   public NoShadowFontWrapper(Font wrapped) {
      super(((FontAccessor)wrapped).create$getFonts(), false);
      this.wrapped = wrapped;
   }

   public int drawInBatch(
      Component pText,
      float pX,
      float pY,
      int pColor,
      boolean pDropShadow,
      Matrix4f pMatrix,
      MultiBufferSource pBuffer,
      DisplayMode pDisplayMode,
      int pBackgroundColor,
      int pPackedLightCoords
   ) {
      return this.wrapped.drawInBatch(pText, pX, pY, pColor, false, pMatrix, pBuffer, pDisplayMode, pBackgroundColor, pPackedLightCoords);
   }

   public int drawInBatch(
      FormattedCharSequence pText,
      float pX,
      float pY,
      int pColor,
      boolean pDropShadow,
      Matrix4f pMatrix,
      MultiBufferSource pBuffer,
      DisplayMode pDisplayMode,
      int pBackgroundColor,
      int pPackedLightCoords
   ) {
      return this.wrapped.drawInBatch(pText, pX, pY, pColor, false, pMatrix, pBuffer, pDisplayMode, pBackgroundColor, pPackedLightCoords);
   }

   public int drawInBatch(
      String pText,
      float pX,
      float pY,
      int pColor,
      boolean pDropShadow,
      Matrix4f pMatrix,
      MultiBufferSource pBuffer,
      DisplayMode pDisplayMode,
      int pBackgroundColor,
      int pPackedLightCoords
   ) {
      return this.wrapped.drawInBatch(pText, pX, pY, pColor, false, pMatrix, pBuffer, pDisplayMode, pBackgroundColor, pPackedLightCoords);
   }

   public int drawInBatch(
      String pText,
      float pX,
      float pY,
      int pColor,
      boolean pDropShadow,
      Matrix4f pMatrix,
      MultiBufferSource pBuffer,
      DisplayMode pDisplayMode,
      int pBackgroundColor,
      int pPackedLightCoords,
      boolean pBidirectional
   ) {
      return this.wrapped.drawInBatch(pText, pX, pY, pColor, false, pMatrix, pBuffer, pDisplayMode, pBackgroundColor, pPackedLightCoords, pBidirectional);
   }

   public FormattedText ellipsize(FormattedText text, int maxWidth) {
      return this.wrapped.ellipsize(text, maxWidth);
   }

   public int wordWrapHeight(FormattedText pText, int pMaxWidth) {
      return this.wrapped.wordWrapHeight(pText, pMaxWidth);
   }

   public String bidirectionalShaping(String pText) {
      return this.wrapped.bidirectionalShaping(pText);
   }

   public void drawInBatch8xOutline(
      FormattedCharSequence pText, float pX, float pY, int pColor, int pBackgroundColor, Matrix4f pMatrix, MultiBufferSource pBuffer, int pPackedLightCoords
   ) {
      this.wrapped.drawInBatch8xOutline(pText, pX, pY, pColor, pBackgroundColor, pMatrix, pBuffer, pPackedLightCoords);
   }

   public int width(String pText) {
      return this.wrapped.width(pText);
   }

   public int width(FormattedText pText) {
      return this.wrapped.width(pText);
   }

   public int width(FormattedCharSequence pText) {
      return this.wrapped.width(pText);
   }

   public String plainSubstrByWidth(String p_92838_, int p_92839_, boolean p_92840_) {
      return this.wrapped.plainSubstrByWidth(p_92838_, p_92839_, p_92840_);
   }

   public String plainSubstrByWidth(String pText, int pMaxWidth) {
      return this.wrapped.plainSubstrByWidth(pText, pMaxWidth);
   }

   public FormattedText substrByWidth(FormattedText pText, int pMaxWidth) {
      return this.wrapped.substrByWidth(pText, pMaxWidth);
   }

   public int wordWrapHeight(String pStr, int pMaxWidth) {
      return this.wrapped.wordWrapHeight(pStr, pMaxWidth);
   }

   public List<FormattedCharSequence> split(FormattedText pText, int pMaxWidth) {
      return this.wrapped.split(pText, pMaxWidth);
   }

   public boolean isBidirectional() {
      return this.wrapped.isBidirectional();
   }

   public StringSplitter getSplitter() {
      return this.wrapped.getSplitter();
   }
}

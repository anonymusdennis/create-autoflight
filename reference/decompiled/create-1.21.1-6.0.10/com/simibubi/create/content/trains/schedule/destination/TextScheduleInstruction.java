package com.simibubi.create.content.trains.schedule.destination;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.utility.CreateLang;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class TextScheduleInstruction extends ScheduleInstruction {
   protected String getLabelText() {
      return this.textData("Text");
   }

   @Override
   public List<Component> getTitleAs(String type) {
      return ImmutableList.of(
         CreateLang.translateDirect("schedule." + type + "." + this.getId().getPath() + ".summary").withStyle(ChatFormatting.GOLD),
         CreateLang.translateDirect("generic.in_quotes", Component.literal(this.getLabelText()))
      );
   }

   @OnlyIn(Dist.CLIENT)
   @Override
   public void initConfigurationWidgets(ModularGuiLineBuilder builder) {
      builder.addTextInput(0, 121, (e, t) -> this.modifyEditBox(e), "Text");
   }

   @OnlyIn(Dist.CLIENT)
   protected void modifyEditBox(EditBox box) {
   }
}

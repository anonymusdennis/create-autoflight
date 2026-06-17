package net.createmod.catnip.config.ui;

import com.electronwill.nightconfig.core.AbstractConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.createmod.catnip.config.ui.entries.BooleanEntry;
import net.createmod.catnip.config.ui.entries.EnumEntry;
import net.createmod.catnip.config.ui.entries.NumberEntry;
import net.createmod.catnip.config.ui.entries.StringEntry;
import net.createmod.catnip.config.ui.entries.SubMenuEntry;
import net.createmod.catnip.config.ui.entries.ValueEntry;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.data.Pair;
import net.createmod.catnip.gui.ConfirmationScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.DelegatedStencilElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.gui.widget.BoxWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.catnip.net.packets.ServerboundConfigPacket;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.theme.Color;
import net.createmod.ponder.enums.PonderGuiTextures;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.neoforged.fml.config.ModConfig.Type;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ValueSpec;

public class SubMenuConfigScreen extends ConfigScreen {
   public final Type type;
   protected ModConfigSpec spec;
   protected UnmodifiableConfig configGroup;
   protected ConfigScreenList list;
   @Nullable
   protected BoxWidget resetAll;
   @Nullable
   protected BoxWidget saveChanges;
   @Nullable
   protected BoxWidget discardChanges;
   @Nullable
   protected BoxWidget goBack;
   @Nullable
   protected BoxWidget serverLocked;
   @Nullable
   protected HintableTextFieldWidget search;
   protected int listWidth;
   protected String title;
   protected Set<String> highlights = new HashSet<>();

   public static SubMenuConfigScreen find(ConfigHelper.ConfigPath path) {
      ModConfigSpec spec = ConfigHelper.findModConfigSpecFor(path.getType(), path.getModID());
      UnmodifiableConfig values = spec.getValues();
      BaseConfigScreen base = new BaseConfigScreen(null, path.getModID());
      SubMenuConfigScreen screen = new SubMenuConfigScreen(base, "root", path.getType(), spec, values);
      List<String> remainingPath = Lists.newArrayList(path.getPath());

      label25:
      while (!remainingPath.isEmpty()) {
         String next = remainingPath.remove(0);

         for (Entry<String, Object> entry : values.valueMap().entrySet()) {
            String key = entry.getKey();
            Object obj = entry.getValue();
            if (key.equalsIgnoreCase(next)) {
               if (obj instanceof AbstractConfig) {
                  values = (UnmodifiableConfig)obj;
                  screen = new SubMenuConfigScreen(screen, toHumanReadable(key), path.getType(), spec, values);
                  continue label25;
               }

               screen.highlights.add(path.getPath()[path.getPath().length - 1]);
            }
         }
         break;
      }

      ConfigScreen.modID = path.getModID();
      return screen;
   }

   public SubMenuConfigScreen(@Nullable Screen parent, String title, Type type, ModConfigSpec configSpec, UnmodifiableConfig configGroup) {
      super(parent);
      this.type = type;
      this.spec = configSpec;
      this.title = title;
      this.configGroup = configGroup;
   }

   public SubMenuConfigScreen(Screen parent, Type type, ModConfigSpec configSpec) {
      super(parent);
      this.type = type;
      this.spec = configSpec;
      this.title = "root";
      this.configGroup = configSpec.getValues();
   }

   protected void clearChanges() {
      ConfigHelper.changes.clear();

      for (ConfigScreenList.Entry e : this.list.children()) {
         if (e instanceof ValueEntry<?> valueEntry) {
            valueEntry.onValueChange();
         }
      }
   }

   protected void saveChanges() {
      UnmodifiableConfig values = this.spec.getValues();
      ConfigHelper.changes.forEach((path, change) -> {
         ConfigValue<Object> configValue = (ConfigValue<Object>)values.get(path);
         configValue.set(change.value);
         configValue.save();
         if (this.type == Type.SERVER) {
            assert ConfigScreen.modID != null;

            CatnipServices.NETWORK.sendToServer(new ServerboundConfigPacket<>(ConfigScreen.modID, path, change.value));
         }

         String command = change.annotations.get("Execute");
         if (this.minecraft.player != null && command != null && command.startsWith("/")) {
            this.minecraft.player.connection.sendCommand(command.substring(1));
         }
      });
      this.clearChanges();
   }

   protected void resetConfig(UnmodifiableConfig values) {
      values.valueMap().forEach((key, obj) -> {
         if (obj instanceof AbstractConfig) {
            this.resetConfig((UnmodifiableConfig)obj);
         } else if (obj instanceof ConfigValue<Object> configValue) {
            ValueSpec valueSpec = (ValueSpec)this.spec.getSpec().getRaw(configValue.getPath());
            List<String> comments = new ArrayList<>();
            if (valueSpec.getComment() != null) {
               comments.addAll(Arrays.asList(valueSpec.getComment().split("\n")));
            }

            Pair<String, Map<String, String>> metadata = ConfigHelper.readMetadataFromComment(comments);
            ConfigHelper.setValue(String.join(".", configValue.getPath()), configValue, valueSpec.getDefault(), metadata.getSecond());
         }
      });

      for (ConfigScreenList.Entry e : this.list.children()) {
         if (e instanceof ValueEntry<?> valueEntry) {
            valueEntry.onValueChange();
         }
      }
   }

   @Override
   protected void init() {
      super.init();
      this.listWidth = Math.min(this.width - 80, 300);
      int yCenter = this.height / 2;
      int listL = this.width / 2 - this.listWidth / 2;
      int listR = this.width / 2 + this.listWidth / 2;
      this.resetAll = new BoxWidget(listR + 10, yCenter - 25, 20, 20)
         .<ElementWidget>withPadding(2.0F, 2.0F)
         .withCallback(
            (x, y) -> new ConfirmationScreen()
                  .centered()
                  .withText(Component.translatable("catnip.ui.resetting_changes_message", new Object[]{this.type.toString()}))
                  .withAction(success -> {
                     if (success) {
                        this.resetConfig(this.spec.getValues());
                     }
                  })
                  .open(this)
         );
      this.resetAll.showingElement(PonderGuiTextures.ICON_CONFIG_RESET.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(this.resetAll)));
      this.resetAll.getToolTip().add(Component.translatable("catnip.ui.reset_all_button"));
      this.resetAll.getToolTip().addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.reset_all_button_tooltip"), FontHelper.Palette.ALL_GRAY));
      this.saveChanges = new BoxWidget(listL - 30, yCenter - 25, 20, 20)
         .<ElementWidget>withPadding(2.0F, 2.0F)
         .withCallback(
            (x, y) -> {
               if (!ConfigHelper.changes.isEmpty()) {
                  ConfirmationScreen confirm = new ConfirmationScreen()
                     .centered()
                     .withText(
                        Component.translatable(
                           "catnip.ui.saving_changes_message",
                           new Object[]{
                              ConfigHelper.changes.size(),
                              Component.translatable(ConfigHelper.changes.size() != 1 ? "catnip.ui.changed_values_plural" : "catnip.ui.changed_values_singular")
                           }
                        )
                     )
                     .withAction(success -> {
                        if (success) {
                           this.saveChanges();
                        }
                     });
                  this.addAnnotationsToConfirm(confirm).open(this);
               }
            }
         );
      this.saveChanges.showingElement(PonderGuiTextures.ICON_CONFIG_SAVE.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(this.saveChanges)));
      this.saveChanges.getToolTip().add(Component.translatable("catnip.ui.save_changes_button"));
      this.saveChanges
         .getToolTip()
         .addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.save_changes_button_tooltip"), FontHelper.Palette.ALL_GRAY));
      this.discardChanges = new BoxWidget(listL - 30, yCenter + 5, 20, 20)
         .<ElementWidget>withPadding(2.0F, 2.0F)
         .withCallback(
            (x, y) -> {
               if (!ConfigHelper.changes.isEmpty()) {
                  new ConfirmationScreen()
                     .centered()
                     .withText(
                        Component.translatable(
                           "catnip.ui.discarding_changes_message",
                           new Object[]{
                              ConfigHelper.changes.size(),
                              Component.translatable(ConfigHelper.changes.size() != 1 ? "catnip.ui.value_changes_plural" : "catnip.ui.value_changes_singular")
                           }
                        )
                     )
                     .withAction(success -> {
                        if (success) {
                           this.clearChanges();
                        }
                     })
                     .open(this);
               }
            }
         );
      this.discardChanges
         .showingElement(PonderGuiTextures.ICON_CONFIG_DISCARD.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(this.discardChanges)));
      this.discardChanges.getToolTip().add(Component.translatable("catnip.ui.discard_changes_button"));
      this.discardChanges
         .getToolTip()
         .addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.discard_changes_button_tooltip"), FontHelper.Palette.ALL_GRAY));
      this.goBack = new BoxWidget(listL - 30, yCenter + 65, 20, 20).<ElementWidget>withPadding(2.0F, 2.0F).withCallback(this::attemptBackstep);
      this.goBack.showingElement(PonderGuiTextures.ICON_CONFIG_BACK.asStencil().withElementRenderer(BoxWidget.gradientFactory.apply(this.goBack)));
      this.goBack.getToolTip().add(Component.translatable("catnip.ui.go_back_button"));
      this.addRenderableWidget(this.resetAll);
      this.addRenderableWidget(this.saveChanges);
      this.addRenderableWidget(this.discardChanges);
      this.addRenderableWidget(this.goBack);
      this.list = new ConfigScreenList(this.minecraft, this.listWidth, this.height - 80, 35, 40);
      this.list.setX(this.width / 2 - this.list.getWidth() / 2);
      this.addRenderableWidget(this.list);
      this.search = new ConfigTextField(this.font, this.width / 2 - this.listWidth / 2, this.height - 35, this.listWidth, 20);
      this.search.setResponder(this::updateFilter);
      this.search.setHint(Component.translatable("catnip.ui.search_hint"));
      this.search.moveCursorToStart(false);
      this.addRenderableWidget(this.search);
      this.configGroup.valueMap().forEach((key, obj) -> {
         String humanKey = toHumanReadable(key);
         if (obj instanceof AbstractConfig) {
            SubMenuEntry entry = new SubMenuEntry(this, humanKey, this.spec, (UnmodifiableConfig)obj);
            entry.path = key;
            this.list.children().add(entry);
            if (this.configGroup.valueMap().size() == 1) {
               ScreenOpener.open(new SubMenuConfigScreen(this.parent, humanKey, this.type, this.spec, (UnmodifiableConfig)obj));
            }
         } else if (obj instanceof ConfigValue<?> configValue) {
            ValueSpec valueSpec = (ValueSpec)this.spec.getSpec().getRaw(configValue.getPath());
            Object value = configValue.get();
            ConfigScreenList.Entry entry = null;
            if (value instanceof Boolean) {
               entry = new BooleanEntry(humanKey, (ConfigValue<Boolean>)configValue, valueSpec);
            } else if (value instanceof Enum) {
               entry = new EnumEntry(humanKey, (ConfigValue<Enum<?>>)configValue, valueSpec);
            } else if (value instanceof Number) {
               entry = NumberEntry.create(value, humanKey, configValue, valueSpec);
            } else if (value instanceof String) {
               entry = new StringEntry(humanKey, (ConfigValue<String>)configValue, valueSpec);
            }

            if (entry == null) {
               entry = new ConfigScreenList.LabeledEntry("Impl missing - " + configValue.get().getClass().getSimpleName() + "  " + humanKey + " : " + value);
            }

            if (this.highlights.contains(key)) {
               entry.annotations.put("highlight", ":)");
            }

            this.list.children().add(entry);
         }
      });
      this.list.children().sort((e, e2) -> {
         int group = (e2 instanceof SubMenuEntry ? 1 : 0) - (e instanceof SubMenuEntry ? 1 : 0);
         if (group == 0 && e instanceof ConfigScreenList.LabeledEntry le && e2 instanceof ConfigScreenList.LabeledEntry le2) {
            return le.label.getComponent().getString().compareTo(le2.label.getComponent().getString());
         }

         return group;
      });
      this.list.search(this.highlights.stream().findFirst().orElse(""));
      if (this.type == Type.SERVER) {
         if (!this.minecraft.hasSingleplayerServer()) {
            boolean canEdit = this.minecraft != null && this.minecraft.player != null && this.minecraft.player.hasPermissions(2);
            Couple<Color> red = AbstractSimiWidget.COLOR_FAIL;
            Couple<Color> green = AbstractSimiWidget.COLOR_SUCCESS;
            DelegatedStencilElement stencil = new DelegatedStencilElement();
            this.serverLocked = new BoxWidget(listR + 10, yCenter + 5, 20, 20).<ElementWidget>withPadding(2.0F, 2.0F).showingElement(stencil);
            if (!canEdit) {
               this.list.children().forEach(e -> e.setEditable(false));
               this.resetAll.active = false;
               stencil.withStencilRenderer((ms, w, h, alpha) -> PonderGuiTextures.ICON_CONFIG_LOCKED.render(ms, 0, 0));
               stencil.withElementRenderer((ms, w, h, alpha) -> UIRenderHelper.angledGradient(ms, 90.0F, 8, 0, 16.0F, 16.0F, red));
               this.serverLocked.withBorderColors(red);
               this.serverLocked.getToolTip().add(Component.translatable("catnip.ui.server_config_locked").withStyle(ChatFormatting.BOLD));
               this.serverLocked
                  .getToolTip()
                  .addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.server_config_locked_tooltip"), FontHelper.Palette.ALL_GRAY));
            } else {
               stencil.withStencilRenderer((ms, w, h, alpha) -> PonderGuiTextures.ICON_CONFIG_UNLOCKED.render(ms, 0, 0));
               stencil.withElementRenderer((ms, w, h, alpha) -> UIRenderHelper.angledGradient(ms, 90.0F, 8, 0, 16.0F, 16.0F, green));
               this.serverLocked.withBorderColors(green);
               this.serverLocked.getToolTip().add(Component.translatable("catnip.ui.server_config_unlocked").withStyle(ChatFormatting.BOLD));
               this.serverLocked
                  .getToolTip()
                  .addAll(FontHelper.cutTextComponent(Component.translatable("catnip.ui.server_config_unlocked_tooltip"), FontHelper.Palette.ALL_GRAY));
            }

            this.addRenderableWidget(this.serverLocked);
         }
      }
   }

   @Override
   protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindow(graphics, mouseX, mouseY, partialTicks);
      int x = this.width / 2;
      graphics.drawCenteredString(
         this.minecraft.font,
         ConfigScreen.modID + " > " + this.type.toString().toLowerCase(Locale.ROOT) + " > " + this.title,
         x,
         15,
         UIRenderHelper.COLOR_TEXT.getFirst().getRGB()
      );
   }

   @Override
   protected void renderWindowForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
      super.renderWindowForeground(graphics, mouseX, mouseY, partialTicks);
   }

   public void resize(@Nonnull Minecraft client, int width, int height) {
      double scroll = this.list.getScrollAmount();
      this.init(client, width, height);
      this.list.setScrollAmount(scroll);
   }

   @Nullable
   @Override
   public GuiEventListener getFocused() {
      return (GuiEventListener)(ConfigScreenList.currentText != null ? ConfigScreenList.currentText : super.getFocused());
   }

   @Override
   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (super.keyPressed(keyCode, scanCode, modifiers)) {
         return true;
      } else {
         if (this.search != null && Screen.hasControlDown() && keyCode == 70) {
            this.search.setFocused(true);
         }

         if (keyCode == 259) {
            this.attemptBackstep();
         }

         return false;
      }
   }

   private void updateFilter(String search) {
      if (this.list.search(search)) {
         this.search.setTextColor(UIRenderHelper.COLOR_TEXT.getFirst().getRGB());
      } else {
         this.search.setTextColor(AbstractSimiWidget.COLOR_SUCCESS.getFirst().getRGB());
      }
   }

   private void attemptBackstep() {
      if (!ConfigHelper.changes.isEmpty() && this.parent instanceof BaseConfigScreen) {
         this.showLeavingPrompt(success -> {
            if (success != ConfirmationScreen.Response.Cancel) {
               if (success == ConfirmationScreen.Response.Confirm) {
                  this.saveChanges();
               }

               ConfigHelper.changes.clear();
               ScreenOpener.open(this.parent);
            }
         });
      } else {
         ScreenOpener.open(this.parent);
      }
   }

   public void onClose() {
      if (ConfigHelper.changes.isEmpty()) {
         super.onClose();
      } else {
         this.showLeavingPrompt(success -> {
            if (success != ConfirmationScreen.Response.Cancel) {
               if (success == ConfirmationScreen.Response.Confirm) {
                  this.saveChanges();
               }

               ConfigHelper.changes.clear();
               super.onClose();
            }
         });
      }
   }

   public void showLeavingPrompt(Consumer<ConfirmationScreen.Response> action) {
      ConfirmationScreen screen = new ConfirmationScreen()
         .centered()
         .withThreeActions(action)
         .addText(
            Component.translatable(
               "catnip.ui.leaving_with_changes_message",
               new Object[]{
                  ConfigHelper.changes.size(),
                  Component.translatable(ConfigHelper.changes.size() != 1 ? "catnip.ui.value_changes_plural" : "catnip.ui.value_changes_singular")
               }
            )
         );
      this.addAnnotationsToConfirm(screen).open(this);
   }

   private ConfirmationScreen addAnnotationsToConfirm(ConfirmationScreen screen) {
      AtomicBoolean relog = new AtomicBoolean(false);
      AtomicBoolean restart = new AtomicBoolean(false);
      ConfigHelper.changes.values().forEach(change -> {
         if (change.annotations.containsKey(ConfigAnnotations.RequiresRelog.TRUE.getName())) {
            relog.set(true);
         }

         if (change.annotations.containsKey(ConfigAnnotations.RequiresRestart.CLIENT.getName())) {
            restart.set(true);
         }
      });
      if (relog.get()) {
         screen.addText(FormattedText.of(" "));
         screen.addText(Component.translatable("catnip.ui.relog_required_message"));
      }

      if (restart.get()) {
         screen.addText(FormattedText.of(" "));
         screen.addText(Component.translatable("catnip.ui.restart_required_message"));
      }

      return screen;
   }
}

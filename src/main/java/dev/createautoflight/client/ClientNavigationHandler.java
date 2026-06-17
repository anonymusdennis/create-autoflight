package dev.createautoflight.client;

import dev.createautoflight.content.navigation.NavigationBlockEntity;
import net.createmod.catnip.gui.ScreenOpener;

public final class ClientNavigationHandler {
    private ClientNavigationHandler() {}

    public static void open(NavigationBlockEntity blockEntity) {
        if (blockEntity.isRemoved() || blockEntity.getLevel() == null) {
            return;
        }
        ScreenOpener.open(new NavigationScreen(blockEntity.getBlockPos()));
    }
}

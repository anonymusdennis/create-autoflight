package dev.createautoflight.client;

import dev.createautoflight.content.thrust.DynamicThrustControllerBlockEntity;
import net.createmod.catnip.gui.ScreenOpener;

public final class ClientThrustControllerHandler {
    private ClientThrustControllerHandler() {}

    public static void open(DynamicThrustControllerBlockEntity blockEntity) {
        ScreenOpener.open(new ThrustControllerScreen(blockEntity));
    }
}

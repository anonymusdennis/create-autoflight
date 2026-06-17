package dev.createautoflight.client;

import dev.createautoflight.content.gyroscope.GyroscopeBlockEntity;
import net.createmod.catnip.gui.ScreenOpener;

public final class ClientGyroscopeHandler {
    private ClientGyroscopeHandler() {}

    public static void open(GyroscopeBlockEntity blockEntity) {
        ScreenOpener.open(new GyroscopeScreen(blockEntity));
    }
}

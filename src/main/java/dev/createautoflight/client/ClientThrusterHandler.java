package dev.createautoflight.client;

import dev.createautoflight.content.thruster.ThrusterBlockEntity;
import net.createmod.catnip.gui.ScreenOpener;

public final class ClientThrusterHandler {
    private ClientThrusterHandler() {}

    public static void open(ThrusterBlockEntity blockEntity) {
        ScreenOpener.open(new ThrusterScreen(blockEntity));
    }
}

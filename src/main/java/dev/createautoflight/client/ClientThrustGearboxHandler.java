package dev.createautoflight.client;

import dev.createautoflight.content.thrust.ThrustVectoringGearboxBlockEntity;
import net.createmod.catnip.gui.ScreenOpener;

public final class ClientThrustGearboxHandler {
    private ClientThrustGearboxHandler() {}

    public static void open(ThrustVectoringGearboxBlockEntity blockEntity) {
        ScreenOpener.open(new ThrustGearboxScreen(blockEntity));
    }
}

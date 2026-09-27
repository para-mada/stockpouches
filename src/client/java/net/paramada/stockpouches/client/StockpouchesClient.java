package net.paramada.stockpouches.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.paramada.stockpouches.component.PouchTooltip;

public class StockpouchesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientTooltipComponentCallback.EVENT.register(component -> component instanceof PouchTooltip tooltip
                ? new ClientPouchTooltip(tooltip)
                : null);
    }
}

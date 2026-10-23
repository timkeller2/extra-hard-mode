package dev.extrahardmode.client;

import dev.extrahardmode.feature.ReconnectRules;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Waits a moment, then joins the server that just dropped. */
public final class ReconnectScreen extends Screen {
    private static final String TITLE_KEY = "tougher.reconnect.title";
    private static final String TITLE = "Reconnecting";
    private static final String BODY_KEY = "tougher.reconnect.body";
    private static final String BODY =
            "Attempt %s of %s. The connection dropped. Joining %s again in a moment. Cancel to stay disconnected.";

    private final int attempt;
    private final String serverName;
    private int ticksLeft = ReconnectRules.DELAY_TICKS;
    private boolean started;

    public ReconnectScreen(int attempt, String serverName) {
        super(Component.translatableWithFallback(TITLE_KEY, TITLE));
        this.attempt = attempt;
        this.serverName = serverName;
    }

    @Override
    protected void init() {
        LinearLayout layout = LinearLayout.vertical().spacing(8);
        layout.defaultCellSetting().alignHorizontallyCenter();
        layout.addChild(new StringWidget(getTitle(), this.font));
        layout.addChild(new MultiLineTextWidget(
                        Component.translatableWithFallback(
                                BODY_KEY, BODY, this.attempt, ReconnectRules.MAX_ATTEMPTS, this.serverName),
                        this.font)
                .setMaxWidth(Math.max(40, this.width - 50))
                .setCentered(true));
        layout.addChild(Button.builder(CommonComponents.GUI_CANCEL, button -> BriefReconnect.cancel(this.minecraft))
                .width(200)
                .build());
        layout.arrangeElements();
        layout.visitWidgets(widget -> this.addRenderableWidget(widget));
        FrameLayout.centerInRectangle(layout, getRectangle());
    }

    @Override
    public void tick() {
        if (this.started) {
            return;
        }
        if (this.ticksLeft > 0) {
            this.ticksLeft--;
        }
        if (this.ticksLeft == 0) {
            this.started = true;
            BriefReconnect.beginConnect(this.minecraft);
        }
    }

    @Override
    public void onClose() {
        BriefReconnect.cancel(this.minecraft);
    }
}

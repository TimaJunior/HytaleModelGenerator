package dev.denismasterherobrine.talesofthevoid.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import javax.annotation.Nonnull;

/**
 * TalesOfTheVoid Dashboard UI
 *
 * A simple interactive dashboard page showing plugin information
 * with refresh and close buttons.
 */
public class TalesOfTheVoidDashboardUI extends InteractiveCustomUIPage<TalesOfTheVoidDashboardUI.UIEventData> {

    // Path relative to Common/UI/Custom/
    public static final String LAYOUT = "Dashboard/Dashboard.ui";

    private final PlayerRef playerRef;
    private int refreshCount = 0;

    public TalesOfTheVoidDashboardUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, UIEventData.CODEC);
        this.playerRef = playerRef;
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> reference,
            @Nonnull UICommandBuilder command,
            @Nonnull UIEventBuilder event,
            @Nonnull Store<EntityStore> store
    ) {
        // Load base layout
        command.append(LAYOUT);

        // Bind refresh button
        event.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RefreshButton",
            new EventData().append("Action", "refresh"),
            false
        );

        // Bind close button
        event.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            new EventData().append("Action", "close"),
            false
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> reference,
            @Nonnull Store<EntityStore> store,
            @Nonnull UIEventData data
    ) {
        if (data.action == null) return;

        switch (data.action) {
            case "refresh":
                refreshCount++;
                UICommandBuilder command = new UICommandBuilder();
                command.set("#StatusText.Text", "Refreshed " + refreshCount + " time(s)!");
                this.sendUpdate(command, false);

                NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw("Tales Of The Void"),
                    Message.raw("Dashboard refreshed!"),
                    NotificationStyle.Success
                );
                break;

            case "close":
                this.close();
                break;
        }
    }

    /**
     * Event data class with codec for handling UI events.
     */
    public static class UIEventData {
        public static final BuilderCodec<UIEventData> CODEC = BuilderCodec.builder(
                UIEventData.class, UIEventData::new
        )
        .append(new KeyedCodec<>("Action", Codec.STRING), (e, v) -> e.action = v, e -> e.action)
        .add()
        .build();

        private String action;

        public UIEventData() {}
    }
}
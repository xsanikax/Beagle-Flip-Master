package com.beagleflipper.controller;

import com.beagleflipper.ui.graph.PriceGraphController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class PriceGraphOpener {

    private final BeagleFlipperConfig config;
    private final Client client;
    private final ItemManager itemManager;
    private final PriceGraphController priceGraphController;


    public void injectCopilotPriceGraphMenuEntry(MenuEntryAdded event) {
        if(!config.priceGraphMenuOptionEnabled()) {
            return;
        }
        if (event.getOption().equals("View offer")) {
            long slotWidgetId = event.getActionParam1();
            client.getMenu()
                    .createMenuEntry(-1)
                    .setOption("Copilot price graph")
                    .onClick((MenuEntry e) -> {
                        GrandExchangeOffer[] offers = client.getGrandExchangeOffers();
                        for (int i = 0; i < offers.length; i++) {
                            Widget slotWidget = client.getWidget(465, 7 + i);
                            if (slotWidget != null && slotWidget.getId() == slotWidgetId) {
                                int itemId = offers[i].getItemId();
                                priceGraphController.setUserItemGraphData(null);
                                priceGraphController.loadAndAndShowPriceGraph(itemId);
                                log.debug("matched widget to slot {}, item {}", i, offers[i].getItemId());
                            }
                        }
                    });
        }
    }
}



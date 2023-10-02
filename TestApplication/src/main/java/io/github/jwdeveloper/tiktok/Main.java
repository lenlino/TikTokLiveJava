/*
 * Copyright (c) 2023-2023 jwdeveloper jacekwoln@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.jwdeveloper.tiktok;

import io.github.jwdeveloper.tiktok.events.messages.*;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import io.github.jwdeveloper.tiktok.util.ConsoleColors;

import java.io.IOException;
import java.time.Duration;

public class Main {

    public static String TEST_TIKTOK_USER = "bangbetmenygy";

    public static void main(String[] args) throws IOException
    {
        LiveClient client = TikTokLive.newClient(TEST_TIKTOK_USER)
                .configure(clientSettings ->
                {
                    clientSettings.setRetryConnectionTimeout(Duration.ofSeconds(5));
                    clientSettings.setRetryOnConnectionFailure(true);
                    clientSettings.setHandleExistingEvents(true);
                })
                .onConnected(Main::onConnected)
                .onDisconnected(Main::onDisconnected)
                .onRoomViewerData(Main::onViewerData)
                .onJoin(Main::onJoin)
                .onComment(Main::onComment)
                .onFollow(Main::onFollow)
                .onShare(Main::onShare)
                .onSubscribe(Main::onSubscribe)
                .onLike(Main::onLike)
                .onGift(Main::onGift)
                .onEmote(Main::onEmote)
                .onError((_client, error) ->
                {
                    error.getException().printStackTrace();
                })
                .buildAndRun();
        System.in.read();
    }

    private static void onConnected(LiveClient tikTokLive, TikTokConnectedEvent e) {
        print(ConsoleColors.GREEN, "[Connected]");
    }

    private static void onGift(LiveClient tikTokLive, TikTokGiftEvent e)
    {
        switch (e.getGift()) {
            case ROSE -> print( "\uD83D\uDC95",ConsoleColors.YELLOW,"x", e.getComboCount(), " roses!", "\uD83D\uDC95");
            default -> print(ConsoleColors.YELLOW,"Thanks for gift: ", e.getGift().getName(),"X",e.getComboCount());
        }
        if(e.getGift().hasDiamondCostRange(1000,10000))
        {
            print("Thank you for expensive Gift!");
        }

    }
    private static void onDisconnected(LiveClient tikTokLive, TikTokDisconnectedEvent e) {
        print(ConsoleColors.GREEN, "[Disconnected]");
    }
    private static void onViewerData(LiveClient tikTokLive, TikTokRoomViewerDataEvent e) {
        print("Viewer count is:", e.getViewerCount());
    }

    private static void onJoin(LiveClient tikTokLive, TikTokJoinEvent e) {
        print(ConsoleColors.GREEN, "Join -> ", ConsoleColors.WHITE_BRIGHT, e.getUser().getNickName());
    }

    private static void onComment(LiveClient tikTokLive, TikTokCommentEvent e) {
        print(ConsoleColors.WHITE, e.getUser().getUniqueId(), ":", ConsoleColors.WHITE_BRIGHT, e.getText());
    }

    private static void onFollow(LiveClient tikTokLive, TikTokFollowEvent e) {
        print(e.getNewFollower().getUniqueId(), "followed!");
    }

    private static void onShare(LiveClient tikTokLive, TikTokShareEvent e) {
        print(e.getUser().getUniqueId(), "shared!");
    }

    private static void onSubscribe(LiveClient tikTokLive, TikTokSubscribeEvent e) {
        print(e.getNewSubscriber().getUniqueId(), "subscribed!");
    }

    private static void onLike(LiveClient tikTokLive, TikTokLikeEvent e) {

        print(e.getSender().getUniqueId(), "liked!");
    }
    private static void onEmote(LiveClient tikTokLive, TikTokEmoteEvent e) {
        print(e.getUser().getUniqueId(), "sent", e.getEmoteId());
    }

    private static void print(Object... messages) {
        var sb = new StringBuilder();
        for (var message : messages) {
            sb.append(message).append(" ");
        }
        System.out.println(sb);
    }
}
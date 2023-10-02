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
package io.github.jwdeveloper.tiktok.websocket;


import io.github.jwdeveloper.tiktok.ClientSettings;
import io.github.jwdeveloper.tiktok.Constants;
import io.github.jwdeveloper.tiktok.TikTokLiveClient;
import io.github.jwdeveloper.tiktok.exceptions.TikTokLiveException;
import io.github.jwdeveloper.tiktok.handlers.TikTokEventObserver;
import io.github.jwdeveloper.tiktok.handlers.TikTokMessageHandlerRegistration;
import io.github.jwdeveloper.tiktok.http.HttpUtils;
import io.github.jwdeveloper.tiktok.http.TikTokCookieJar;
import io.github.jwdeveloper.tiktok.messages.WebcastResponse;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.logging.Logger;

public class TikTokWebSocketClient {
    private final Logger logger;
    private final ClientSettings clientSettings;
    private final TikTokCookieJar tikTokCookieJar;
    private final TikTokMessageHandlerRegistration webResponseHandler;
    private final TikTokEventObserver tikTokEventHandler;
    private WebSocketClient webSocketClient;
    private TikTokWebSocketPingingTask pingingTask;
    private boolean isConnected;

    public TikTokWebSocketClient(Logger logger,
                                 TikTokCookieJar tikTokCookieJar,
                                 ClientSettings clientSettings,
                                 TikTokMessageHandlerRegistration webResponseHandler,
                                 TikTokEventObserver tikTokEventHandler) {
        this.logger = logger;
        this.tikTokCookieJar = tikTokCookieJar;
        this.clientSettings = clientSettings;
        this.webResponseHandler = webResponseHandler;
        this.tikTokEventHandler = tikTokEventHandler;
        isConnected = false;
    }

    public void start(WebcastResponse webcastResponse, TikTokLiveClient tikTokLiveClient) {
        if (isConnected) {
            stop();
        }

        if (webcastResponse.getSocketUrl().isEmpty() ||
            webcastResponse.getSocketParamsList().isEmpty()) {
            throw new TikTokLiveException("Could not find Room");
        }
        try {
            var url = getWebSocketUrl(webcastResponse);
            if (clientSettings.isHandleExistingEvents()) {
                logger.info("Handling existing messages");
                webResponseHandler.handle(tikTokLiveClient, webcastResponse);
            }
            webSocketClient = startWebSocket(url, tikTokLiveClient);
            webSocketClient.connect();

            pingingTask = new TikTokWebSocketPingingTask();
            pingingTask.run(webSocketClient);
            isConnected = true;
        } catch (Exception e) {
            isConnected = false;
            throw new TikTokLiveException("Failed to connect to the websocket", e);
        }
    }

    private WebSocketClient startWebSocket(String url, TikTokLiveClient liveClient) {
        var cookie = tikTokCookieJar.parseCookies();
        var map = new HashMap<String, String>();
        map.put("Cookie", cookie);
        return new TikTokWebSocketListener(URI.create(url),
                map,
                3000,
                webResponseHandler,
                tikTokEventHandler,
                liveClient);
    }

    private String getWebSocketUrl(WebcastResponse webcastResponse) {
        var params = webcastResponse.getSocketParamsList().get(0);
        var name = params.getName();
        var value = params.getValue();
        var headers = Constants.DefaultRequestHeaders();


        var clone = new TreeMap<>(clientSettings.getClientParameters());
        clone.putAll(headers);
        clone.put(name, value);
        var url = webcastResponse.getSocketUrl();
        return HttpUtils.parseParametersEncode(url, clone);
    }

    public void stop()
    {
        if (isConnected && webSocketClient != null) {
            webSocketClient.closeConnection(0, "");
            pingingTask.stop();
        }
        webSocketClient = null;
        pingingTask = null;
        isConnected = false;
    }
}

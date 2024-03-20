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
package io.github.jwdeveloper.tiktok.extension.collector.impl;

import com.mongodb.client.MongoCollection;
import io.github.jwdeveloper.tiktok.annotations.TikTokEventObserver;
import io.github.jwdeveloper.tiktok.data.events.TikTokErrorEvent;
import io.github.jwdeveloper.tiktok.data.events.common.TikTokEvent;
import io.github.jwdeveloper.tiktok.data.events.control.TikTokConnectingEvent;
import io.github.jwdeveloper.tiktok.data.events.websocket.TikTokWebsocketResponseEvent;
import io.github.jwdeveloper.tiktok.exceptions.TikTokLiveMessageException;
import io.github.jwdeveloper.tiktok.extension.collector.api.LiveDataCollector;
import io.github.jwdeveloper.tiktok.extension.collector.api.data.CollectorListenerSettings;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import io.github.jwdeveloper.tiktok.messages.webcast.WebcastResponse;
import io.github.jwdeveloper.tiktok.utils.JsonUtil;
import org.bson.Document;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;
import java.util.UUID;

public class TikTokLiveDataCollectorListener implements LiveDataCollector {

    private final MongoCollection<Document> collection;
    private final CollectorListenerSettings settings;
    private String sessionId;
    private String userName;

    public TikTokLiveDataCollectorListener(MongoCollection<Document> collection, CollectorListenerSettings settings) {
        this.collection = collection;
        this.settings = settings;
    }


    @TikTokEventObserver
    private void onResponse(LiveClient liveClient, TikTokWebsocketResponseEvent event) {
        includeResponse(liveClient, event.getResponse());
        event.getResponse().getMessagesList().forEach(message ->
        {
            includeMessage(liveClient, message);
        });
    }

    @TikTokEventObserver
    private void onEvent(LiveClient liveClient, TikTokEvent event) {
        if (event instanceof TikTokConnectingEvent) {
            sessionId = UUID.randomUUID().toString();
            userName = liveClient.getRoomInfo().getHostName();
        }

        if (event instanceof TikTokErrorEvent) {
            return;
        }

        includeEvent(event);
    }

    @TikTokEventObserver
    private void onError(LiveClient liveClient, TikTokErrorEvent event) {
        event.getException().printStackTrace();
        includeError(event);
    }


    private void includeResponse(LiveClient liveClient, WebcastResponse message) {
        var messageContent = Base64.getEncoder().encodeToString(message.toByteArray());
        insertDocument(createDocument("response", "webcast", messageContent));
    }

    private void includeMessage(LiveClient liveClient, WebcastResponse.Message message) {
        var method = message.getMethod();
        var messageContent = Base64.getEncoder().encodeToString(message.getPayload().toByteArray());

        insertDocument(createDocument("message", method, messageContent));
    }

    private void includeEvent(TikTokEvent event) {
        var json = JsonUtil.toJson(event);
        var content = Base64.getEncoder().encodeToString(json.getBytes());
        var name = event.getClass().getSimpleName();
        insertDocument(createDocument("event", name, content));
    }

    private void includeError(TikTokErrorEvent event) {
        var exception = event.getException();
        var exceptionName = event.getException().getClass().getSimpleName();

        var sw = new StringWriter();
        var pw = new PrintWriter(sw);
        event.getException().printStackTrace(pw);
        var content = sw.toString();

        var doc = createDocument("error", exceptionName, content);
        if (exception instanceof TikTokLiveMessageException ex) {
            doc.append("message", ex.messageToBase64())
                    .append("response", ex.webcastResponseToBase64());
        }
        insertDocument(doc);
    }


    private void insertDocument(Document document) {
        if (!settings.getFilter().apply(document)) {
            return;
        }
        collection.insertOne(document);
    }


    private Document createDocument(String dataType, String dataTypeName, String content) {
        var doc = new Document();
        doc.append("session", sessionId);
        for (var entry : settings.getExtraFields().entrySet()) {
            doc.append(entry.getKey(), entry.getValue());
        }
        doc.append("tiktokUser", userName);
        doc.append("dataType", dataType);
        doc.append("dataTypeName", dataTypeName);
        doc.append("content", content);
        return doc;
    }
}

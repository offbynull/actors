/*
 * Copyright (c) 2017, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.actors.core.gateway.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import com.offbynull.actors.core.shuttle.Message;
import java.lang.reflect.Type;
import org.apache.commons.lang3.Validate;

final class SystemToHttpBundleJsonSerializer implements JsonSerializer<SystemToHttpBundle> {

    private final String prefix;
    
    private final Type httpToSystemMessageType;

    public SystemToHttpBundleJsonSerializer(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
        
        httpToSystemMessageType = new TypeToken<Message>() { }.getType();
    }

    @Override
    public JsonElement serialize(SystemToHttpBundle src, Type typeOfSrc, JsonSerializationContext context) {
        Validate.notNull(src);
        Validate.notNull(typeOfSrc);
        Validate.notNull(context);
        
        String httpAddressId = src.getHttpAddressId();
        int systemToHttpOffset = src.getSystemToHttpOffset();
        int httpToSystemOffset = src.getHttpToSystemOffset();
        src.getMessages().forEach(msg -> {
            Validate.isTrue(msg.getDestinationAddress().size() >= 2);
            String dstPrefix = msg.getDestinationAddress().getElement(0);
            String dstId = msg.getDestinationAddress().getElement(1);
            Validate.isTrue(dstPrefix.equals(prefix));
            Validate.isTrue(dstId.equals(httpAddressId));
        });
        
        JsonArray messagesJsonArray = new JsonArray();
        src.getMessages().stream()
                .map(msg -> context.serialize(msg, httpToSystemMessageType))
                .forEachOrdered(msg -> messagesJsonArray.add(msg));

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("httpAddressId", new JsonPrimitive(httpAddressId));
        jsonObject.add("systemToHttpOffset", new JsonPrimitive(systemToHttpOffset));
        jsonObject.add("httpToSystemOffset", new JsonPrimitive(httpToSystemOffset));
        jsonObject.add("messages", messagesJsonArray);

        return jsonObject;
    }
}

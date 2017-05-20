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
package com.offbynull.actors.core.gateway.servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.offbynull.actors.core.shuttle.Message;
import java.lang.reflect.Type;
import org.apache.commons.lang3.Validate;

final class MessageJsonSerializer implements JsonSerializer<Message> {

    private static final String SOURCE_PROPERTY = "source";
    private static final String DESTINATION_PROPERTY = "destination";
    private static final String TYPE_PROPERTY = "type";
    private static final String CONTENT_PROPERTY = "content";

    private final String prefix;

    public MessageJsonSerializer(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
    }

    @Override
    public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
        String dstPrefix = src.getDestinationAddress().getElement(0);
        Validate.isTrue(dstPrefix.equals(prefix));

        String from = src.getSourceAddress().toString();
        String to = src.getDestinationAddress().toString();
        Object obj = src.getMessage();
        String type = obj.getClass().getName();
        
        JsonElement serializedContent = context.serialize(obj);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add(SOURCE_PROPERTY, new JsonPrimitive(from));
        jsonObject.add(DESTINATION_PROPERTY, new JsonPrimitive(to));
        jsonObject.add(TYPE_PROPERTY, new JsonPrimitive(type));
        jsonObject.add(CONTENT_PROPERTY, serializedContent);

        return jsonObject;
    }
}

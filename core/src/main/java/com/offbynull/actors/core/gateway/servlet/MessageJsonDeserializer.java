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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.offbynull.actors.core.shuttle.Address;
import com.offbynull.actors.core.shuttle.Message;
import java.lang.reflect.Type;
import org.apache.commons.lang3.Validate;

final class MessageJsonDeserializer implements JsonDeserializer<Message> {

    private static final String SOURCE_PROPERTY = "source";
    private static final String DESTINATION_PROPERTY = "destination";
    private static final String TYPE_PROPERTY = "type";
    private static final String CONTENT_PROPERTY = "content";
    
    private final String prefix;

    public MessageJsonDeserializer(String prefix) {
        Validate.notNull(prefix);
        this.prefix = prefix;
    }

    @Override
    public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            JsonObject jsonObject = json.getAsJsonObject();

            String from = jsonObject.get(SOURCE_PROPERTY).getAsString();
            Address fromAddress = Address.fromString(from);

            String dstPrefix = fromAddress.getElement(0);
            Validate.isTrue(dstPrefix.equals(prefix));

            String to = jsonObject.get(DESTINATION_PROPERTY).getAsString();
            Address toAddress = Address.fromString(to);

            JsonElement jsonType = jsonObject.get(TYPE_PROPERTY);
            String type = jsonType.getAsString();
            Class<?> cls;

            try {
                cls = Class.forName(type, false, getClass().getClassLoader());
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalArgumentException(cnfe);
            }

            JsonElement jsonContent = jsonObject.get(CONTENT_PROPERTY);
            Object obj = context.deserialize(jsonContent, cls);

            return new Message(fromAddress, toAddress, obj);
        } catch (JsonParseException jpe) {
            throw new IllegalArgumentException(jpe);
        }
    }
}

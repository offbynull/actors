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
package com.offbynull.actors.gateways.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import com.offbynull.actors.shuttle.Address;
import com.offbynull.actors.shuttle.Message;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;

final class JsonConverter {
    
    private static final String SOURCE_PROPERTY = "source";
    private static final String DESTINATION_PROPERTY = "destination";
    private static final String TYPE_PROPERTY = "type";
    private static final String DATA_PROPERTY = "data";

    private static final String ID_PROPERTY = "id";
    private static final String MESSAGES_PROPERTY = "messages";
    
    private final Gson gson;

    JsonConverter() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonDeserializer());
        gsonBuilder.registerTypeAdapter(Message.class, new MessageJsonSerializer());
        gsonBuilder.registerTypeAdapter(RequestBlock.class, new RequestBlockDeserializer());
        gsonBuilder.registerTypeAdapter(ResponseBlock.class, new ResponseBlockSerializer());
        gson = gsonBuilder.create();
    }
    
    public String toJson(ResponseBlock response) {
        Validate.notNull(response);

        return gson.toJson(response, ResponseBlock.class);
    }
    
    public RequestBlock fromJson(String request) {
        Validate.notNull(request);

        try {
            return gson.fromJson(request, RequestBlock.class);
        } catch (NullPointerException | IllegalArgumentException | JsonSyntaxException e) {
            // these types are things that either gson or the serializer/deserializer we registered can throw -- it means the input was bad
            throw new IllegalArgumentException(e);
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    private static final class MessageJsonDeserializer implements JsonDeserializer<Message> {

        @Override
        public Message deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            String from = jsonObject.get(SOURCE_PROPERTY).getAsString();
            Address fromAddress = Address.fromString(from);

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

            JsonElement jsonData = jsonObject.get(DATA_PROPERTY);

            Object obj = context.deserialize(jsonData, cls);

            return new Message(fromAddress, toAddress, obj);
        }
    }

    
    
    private static final class MessageJsonSerializer implements JsonSerializer<Message> {

        @Override
        public JsonElement serialize(Message src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject output = new JsonObject();
            JsonElement payload = context.serialize(src.getMessage());

            String from = src.getSourceAddress().toString();
            String to = src.getDestinationAddress().toString();
            Object obj = src.getMessage();
            String type = obj.getClass().getName();

            output.add(SOURCE_PROPERTY, new JsonPrimitive(from));
            output.add(DESTINATION_PROPERTY, new JsonPrimitive(to));
            output.add(TYPE_PROPERTY, new JsonPrimitive(type));
            output.add(DATA_PROPERTY, payload);

            return output;
        }
    }
    
    
    
    private static final class ResponseBlockSerializer implements JsonSerializer<ResponseBlock> {

        @Override
        public JsonElement serialize(ResponseBlock src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            
            JsonArray messages = new JsonArray();

            for (Message message : src.getMessages()) {
                JsonElement jsonMessage = context.serialize(message);
                messages.add(jsonMessage);
            }
            
            jsonObject.add(MESSAGES_PROPERTY, messages);

            return jsonObject;
        }

    }
    
    
    
    private static final class RequestBlockDeserializer implements JsonDeserializer<RequestBlock> {

        @Override
        public RequestBlock deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();

            String id = jsonObject.get(ID_PROPERTY).getAsString();

            JsonArray jsonMessages = jsonObject.get(MESSAGES_PROPERTY).getAsJsonArray();
            List<Message> messages = new ArrayList<>(jsonMessages.size());
            for (JsonElement jsonMessage : jsonMessages) {
                Message message = context.deserialize(jsonMessage, Message.class);
                messages.add(message);
            }

            return new RequestBlock(id, messages);
        }
    }
}

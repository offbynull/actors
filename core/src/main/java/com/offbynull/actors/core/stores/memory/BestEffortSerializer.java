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
package com.offbynull.actors.core.stores.memory;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import static java.util.stream.Collectors.toList;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.objenesis.ObjenesisStd;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// No serializers seem to work -- they all have some type of problem.
//
// This code delegates to Java's serializer, but tries to work around the case where an object in the graph doesn't implement Serializable.
// Note that this won't work for lambdas UNLESS THEY'RE SPECIFICALLY MARKED AS SERIALIZABLE because lambdas don't use the standard Java
// serialization mechanism -- extra code is generated for lambdas to take care of serialization/deserialization.
final class BestEffortSerializer {
    
    private static final Logger LOG = LoggerFactory.getLogger(BestEffortSerializer.class);

    private static final byte NORMAL_OBJECT_START = 1;
    private static final byte NORMAL_OBJECT_STOP = -1;
    private static final byte DECONSTRUCTED_ARRAY_START = 2;
    private static final byte DECONSTRUCTED_ARRAY_STOP = -2;
    private static final byte DECONSTRUCTED_OBJECT_START = 3;
    private static final byte DECONSTRUCTED_OBJECT_STOP = -3;
    
    private static final byte PARENT_REFERENCE_MARKER = 100;
    private static final byte NEW_REFERENCE_MARKER = 101;
    
    byte[] serialize(Object obj) {
        try {
            CustomObjectOutputStream coos = new CustomObjectOutputStream();
            recurseWriteObject(obj, coos, new TraversalPath());

            return coos.toByteArray();
        } catch (IllegalAccessException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    <T> T deserialize(byte[] data) {
        try {
            CustomObjectInputStream ois = new CustomObjectInputStream(data);
            return (T) recurseReadObject(getClass().getClassLoader(), ois, new TraversalPath());
        } catch (ClassCastException | ClassNotFoundException | IllegalAccessException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    
    
    
    
    
    
    
    
    
    private static void recurseWriteObject(Object obj, CustomObjectOutputStream coos, TraversalPath traversalPath)
            throws IOException, IllegalAccessException {
        int idx = traversalPath.indexOf(obj);
        if (idx != -1) { // is it a reference to a obj we've already written?
            coos.writeByte(PARENT_REFERENCE_MARKER);
            coos.writeInt(idx);
            return;
        }

        coos.writeByte(NEW_REFERENCE_MARKER); // no, it's a new obj
        if (isSerializable(obj)) { // is it serializable? if so do it
            coos.writeByte(NORMAL_OBJECT_START);
            coos.writeObject(obj);
            coos.writeByte(NORMAL_OBJECT_STOP);
            return;
        }
        
        LOG.warn("{} marked with Serializable but the object graph contains a reference to a non-serializable object, falling back to best"
                + " effort serialization for this object", obj.getClass().getSimpleName());
        
        // if isn't serializable, so make a 'best effort' to write it out as-is
        if (obj.getClass().isArray()) {  // best effort to write out obj array
            coos.writeByte(DECONSTRUCTED_ARRAY_START);
            traversalPath.addLast(obj);
            
            Class<?> cls = obj.getClass();
            coos.writeUTF(cls.getName());
            
            int len = Array.getLength(obj);
            coos.writeInt(len);
            
            for (int i = 0; i < len; i++) {
                Object value = Array.get(obj, i);
                recurseWriteObject(value, coos, traversalPath);
            }

            traversalPath.removeLast();
            coos.writeByte(DECONSTRUCTED_ARRAY_STOP);
        } else {  // best effort to write out obj
            coos.writeByte(DECONSTRUCTED_OBJECT_START);
            traversalPath.addLast(obj);

            Class<?> cls = obj.getClass();
            coos.writeUTF(cls.getName());

            List<Field> writableFields = FieldUtils.getAllFieldsList(cls).stream()
                    .filter(f -> (f.getModifiers() & Modifier.STATIC) == 0) // no static, but yes transient and volatile
                    .collect(toList());
            
            coos.writeInt(writableFields.size());
            for (Field f : writableFields) {
                String name = f.getName();
                Object value = FieldUtils.readField(f, obj, true);
                
                coos.writeUTF(name);
                recurseWriteObject(value, coos, traversalPath);
            }
            
            traversalPath.removeLast();
            coos.writeByte(DECONSTRUCTED_OBJECT_STOP);
        }
    }
    
    private static boolean isSerializable(Object obj) throws IllegalAccessException {
        if (obj == null) {
            return true;
        }

        Class<?> cls = obj.getClass();
        if (ClassUtils.isPrimitiveOrWrapper(cls)
                || cls == String.class
                || cls == BigDecimal.class
                || cls == BigInteger.class) {
            return true;
        }

        if (!(obj instanceof Serializable)) {
            return false;
        }

        // Try to serialize it into a null buffer -- if it doesn't work, it means we need to use our fallback method of serialization
        try (NullObjectOutputStream innerCoos = new NullObjectOutputStream()) {
            innerCoos.writeObject(obj);
            return true;
        } catch (NotSerializableException nse) { // can't serialize it
            return false;
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe); // should never happen
        }
    }





    private static Object recurseReadObject(ClassLoader classLoader, ObjectInputStream ois, TraversalPath traversalPath)
            throws IOException, IllegalAccessException, ClassNotFoundException {
        
        int objectMarker = ois.readByte();
        switch (objectMarker) {
            case NEW_REFERENCE_MARKER:
                break;
            case PARENT_REFERENCE_MARKER:
                int idx = ois.readInt();
                if (idx >= traversalPath.size() || idx < 0) {
                    throw new IOException("Traversal reference goes out of bounds: " + idx);
                }
                Object ret = traversalPath.get(idx);
                return ret;
            default:
                throw new IOException("Unexpected reference marker: " + objectMarker);
        }

        byte startMarker = ois.readByte();
        switch (startMarker) {
            case NORMAL_OBJECT_START: {
                Object ret = ois.readObject();
                
                readAndCheckEndMarker(ois, NORMAL_OBJECT_STOP);
                
                return ret;
            }
            case DECONSTRUCTED_ARRAY_START: {
                String clsName = ois.readUTF();
                int len = ois.readInt();
                
                Class<?> cls = classLoader.loadClass(clsName);
                Object obj = Array.newInstance(cls.getComponentType(), len);
                
                traversalPath.addLast(obj);
                
                for (int i = 0; i < len; i++) {
                    Object arrVal = recurseReadObject(classLoader, ois, traversalPath);
                    Array.set(obj, i, arrVal);
                }
                
                readAndCheckEndMarker(ois, DECONSTRUCTED_ARRAY_STOP);

                traversalPath.removeLast();
                return obj;
            }
            case DECONSTRUCTED_OBJECT_START: {
                String clsName = ois.readUTF();
                
                Class<?> cls = classLoader.loadClass(clsName);
                Object obj = new ObjenesisStd().getInstantiatorOf(cls).newInstance();
                
                traversalPath.addLast(obj);
                
                int fieldCount = ois.readInt();
                for (int i = 0; i < fieldCount; i++) {
                    String fieldName = ois.readUTF();
                    
                    Object value = recurseReadObject(classLoader, ois, traversalPath);
                    
                    Field field = FieldUtils.getField(cls, fieldName, true);
                    FieldUtils.writeField(field, obj, value, true);
                }
                
                readAndCheckEndMarker(ois, DECONSTRUCTED_OBJECT_STOP);

                traversalPath.removeLast();
                return obj;
            }
            default:
                throw new IllegalArgumentException("Unexpected start marker: " + startMarker);
        }
    }
    
    private static void readAndCheckEndMarker(ObjectInputStream ois, int expectedMarker) throws IOException {
        int endMarker = ois.readByte();
        if (endMarker != expectedMarker) {
            throw new IOException("Unexpected end marker : " + endMarker);
        }
    }
    
    
    
    
    

    
    
    
    
    
    
    
    private static final class TraversalPath {
        private final LinkedList<Object> data = new LinkedList<>();
        
        public void addLast(Object obj) {
            data.add(obj);
        }

        public void removeLast() {
            data.remove();
        }
        
        public int indexOf(Object obj) {
            int idx = 0;
            for (Object item : data) {
                if (item == obj) { // USE == INSTEAD OF equals() -- we want to make to look for the same object, not objects that are equal
                    return idx;
                }
                idx++;
            }
            return -1;
        }

        public int size() {
            return data.size();
        }

        public Object get(int index) {
            return data.get(index);
        }
        
    }
}

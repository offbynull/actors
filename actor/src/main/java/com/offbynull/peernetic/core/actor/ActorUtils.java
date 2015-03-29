package com.offbynull.peernetic.core.actor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import org.apache.commons.lang3.Validate;

public final class ActorUtils {

    public static final String SEPARATOR = ":";

    private ActorUtils() {
        // do nothing
    }

    public static String getPrefix(String address) {
        return splitAddress(address).getPrefix();
    }

    public static String getIdElement(String address, int idx) {
        return splitAddress(address).getIdElement(idx);
    }

    public static int getIdElementSize(String address) {
        return splitAddress(address).getIdElementSize();
    }

    public static String getAddress(String prefix, String id) {
        Validate.notNull(prefix);
        Validate.notNull(id);
        return getAddress(prefix, Collections.singletonList(id));
    }
    
    public static String getAddress(String prefix, List<String> idElements) {
        Validate.notNull(prefix);
        Validate.notNull(idElements);
        Validate.notEmpty(prefix);
        Validate.noNullElements(idElements);
        
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        joiner.add(prefix);
        idElements.forEach(x -> joiner.add(x));
        
        return joiner.toString();
    }

    public static String getAddress(ActorAddress actorAddress) {
        Validate.notNull(actorAddress);
        return getAddress(actorAddress.prefix, actorAddress.idElements);
    }

    public static ActorAddress splitAddress(String address) {
        // get prefix, prefix must always end with a separator, so even if there are no id elements it'd be something like "prefix:"
        int separatorIdx = address.indexOf(SEPARATOR);
        Validate.isTrue(separatorIdx != -1);
        String prefix = address.substring(0, separatorIdx);
        
        // add stuff after prefix
        int startIdx = separatorIdx;
        int endIdx;
        List<String> idElements = new LinkedList<>();
        while ((endIdx = address.indexOf(SEPARATOR, separatorIdx + 1)) != -1) {
            String idElement = address.substring(startIdx + 1, endIdx);
            idElements.add(idElement);
        }
        
        // add tail element, if it exists
        if (startIdx < address.length()) {
            String idElement = address.substring(startIdx + 1);
            idElements.add(idElement);
        }

        return new ActorAddress(prefix, idElements);
    }

    public static final class ActorAddress {

        private final String prefix;
        private final List<String> idElements;

        public ActorAddress(String prefix, List<String> idElements) {
            Validate.notNull(prefix);
            Validate.notNull(idElements);
            Validate.notEmpty(prefix);
            Validate.noNullElements(idElements);
            this.prefix = prefix;
            this.idElements = new ArrayList<>(idElements);
        }

        public String getPrefix() {
            return prefix;
        }

        public String getIdElement(int idx) {
            return idElements.get(idx); //ioobe if negative or 
        }
        
        public int getIdElementSize() {
            return idElements.size();
        }
    }
}

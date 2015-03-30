package com.offbynull.peernetic.core.common;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.apache.commons.lang3.Validate;

public final class Address {
    final String prefix;
    final List<String> idElements;

    public Address(String prefix, List<String> idElements) {
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

    public String getId() {
        StringJoiner joiner = new StringJoiner(AddressUtils.SEPARATOR);
        idElements.forEach((x) -> joiner.add(x));
        return joiner.toString();
    }
    
}

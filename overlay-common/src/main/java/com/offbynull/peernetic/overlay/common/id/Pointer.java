/*
 * Copyright (c) 2013-2014, Kasra Faghihi, All rights reserved.
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
package com.offbynull.peernetic.overlay.common.id;

import java.util.Objects;
import org.apache.commons.lang3.Validate;

/**
 * A {@link Id} paired with an address.
 * @author Kasra Faghihi
 * @param <A> address type
 */
public final class Pointer<A> {
    private Id id;
    private A address;

    /**
     * Constructs a {@link Pointer} object.
     * @param id id
     * @param address address
     * @throws NullPointerException if any arguments are {@code null}
     */
    public Pointer(Id id, A address) {
        Validate.notNull(id);
        Validate.notNull(address);
        
        this.id = id;
        this.address = address;
    }

    /**
     * Get ID.
     * @return id
     */
    public Id getId() {
        return id;
    }

    /**
     * Get address.
     * @return address
     */
    public A getAddress() {
        return address;
    }

    /**
     * Like {@link #equals(java.lang.Object) }, but ensures that if the IDs are the same, the address have to be the same as well. If the
     * IDs are the same but the addresses are different, then an exception will be thrown.
     * @param other pointer being tested against
     * @return {@code true} if equal, {@code false} otherwise
     * @throws IllegalArgumentException if the IDs are the same but the
     * addresses are different
     */
    public boolean equalsEnsureAddress(Pointer other) {
        Id otherLimitedId = other.getId();
        
        if (id.equals(otherLimitedId)) {
            if (!this.equals(other)) {
                throw new IllegalArgumentException();
            }
            
            return true;
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        hash = 37 * hash + Objects.hashCode(this.address);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pointer other = (Pointer) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.address, other.address)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Pointer{" + "id=" + id + ", address=" + address + '}';
    }
}

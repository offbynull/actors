package com.offbynull.peernetic.chord;

import java.util.Objects;

public final class Pointer {
    private Id id;
    private Address address;

    public Pointer(Id id, Address address) {
        if (id == null || address == null) {
            throw new NullPointerException();
        }
        
        this.id = id;
        this.address = address;
    }

    public Id getId() {
        return id;
    }

    public Address getAddress() {
        return address;
    }

    /**
     * Like {@link #equals(java.lang.Object) }, but ensures that if the ids are
     * the same, the address have to be the same as well. If the ids are the
     * same but the addresses are different, then an exception will be thrown.
     * @param other pointer being tested against
     * @return {@code true} if equal, {@code false} otherwise
     * @throws IllegalArgumentException if the ids are the same but the
     * addresses are different
     */
    public boolean equalsEnsureAddress(Pointer other) {
        Id otherId = other.getId();
        
        if (id.equals(otherId)) {
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

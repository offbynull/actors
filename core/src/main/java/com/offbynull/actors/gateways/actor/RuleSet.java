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
package com.offbynull.actors.gateways.actor;

import com.offbynull.actors.address.Address;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.apache.commons.collections4.set.UnmodifiableSet;
import org.apache.commons.lang3.Validate;

/**
 * Access control rules. Controls what type of messages can come from which sources.
 * @author Kasra Faghihi
 */
public final class RuleSet implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private AccessType defaultAccessType;
    private final Map<Address, AddressRule> rules;
    
    RuleSet() {
        defaultAccessType = AccessType.REJECT;
        rules = new HashMap<>();
    }

    // deep copy
    RuleSet(RuleSet other) {
        defaultAccessType = other.defaultAccessType;
        rules = new HashMap<>(other.rules);
    }
    
    /**
     * Allow incoming messages from any source of any type. All previous rules are cleared.
     */
    public void allowAll() {
        defaultAccessType = AccessType.ALLOW;
        rules.clear();
    }

    /**
     * Block incoming messages from any source of any type. All previous rules are cleared.
     */
    public void rejectAll() {
        defaultAccessType = AccessType.REJECT;
        rules.clear();
    }

    /**
     * Allow incoming messages from some specific source with some specific type.
     * @param address source address to allow
     * @param includeChildren if {@code true}, children of {@code address} will also be allowed through
     * @param types message types to allow through (if empty, all message types are allowed through)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void allow(Address address, boolean includeChildren, Class<?>... types) {
        Validate.notNull(address);
        Validate.notNull(types);
        Validate.noNullElements(types);
        rules.put(address, new AddressRule(includeChildren, AccessType.ALLOW, Arrays.asList(types)));
    }

    /**
     * Equivalent to calling {@code allow(Address.fromString(source), children, types)}.
     * @param source source address to allow messages from
     * @param children if {@code true}, children of {@code source} will also be allowed
     * @param types types of messages to allow (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void allow(String source, boolean children, Class<?>... types) {
        allow(Address.fromString(source), children, types);
    }

    /**
     * Block incoming messages from some specific source with some specific type.
     * @param address source address to block
     * @param includeChildren if {@code true}, children of {@code address} will also be blocked
     * @param types message types to block (if empty, all message types are blocked)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void reject(Address address, boolean includeChildren, Class<?>... types) {
        Validate.notNull(address);
        Validate.notNull(types);
        Validate.noNullElements(types);
        rules.put(address, new AddressRule(includeChildren, AccessType.REJECT, Arrays.asList(types)));
    }
    
    /**
     * Equivalent to calling {@code reject(Address.fromString(source), children, types)}.
     * @param source source address to block messages from
     * @param children if {@code true}, children of {@code source} will also be blocked
     * @param types types of messages to block (child types aren't recognized)
     * @throws NullPointerException if any argument is {@code null} or contains {@code null}
     */
    public void reject(String source, boolean children, Class<?>... types) {
        reject(Address.fromString(source), children, types);
    }

    /**
     * Evaluate whether some message from an some address should be blocked or allowed through.
     * @param address source address
     * @param type message type
     * @return allow or block
     */
    public AccessType evaluate(Address address, Class<?> type) {
        Validate.notNull(address);
        Validate.notNull(type);
        
        // Find greatest prefix;
        for (int i = 0; i < address.size(); i++) {
            Address foundAddressPrefix = address.removeSuffix(i);
            AddressRule rule = rules.get(foundAddressPrefix);
            
            // If you found a rule prefix, and you're not evaluating a child address of the rule OR you are evaluating a child address of
            // the rule but the rule applies to child address as well, then return the rule's access type
            if (rule != null) {
                boolean evaluatingChildAddress = foundAddressPrefix.size() < address.size();
                if (!evaluatingChildAddress || (evaluatingChildAddress && rule.includeChildren)) {
                    // The address matches the address in the rule, but we still have to check the type being evaluated to see if it matches
                    // Note that an empty type set means that any type is let through
                    if (rule.getTypes().isEmpty() || rule.getTypes().contains(type)) {
                        return rule.getAccessType();
                    }
                }
            }
            
            // Otherwise keep going up until you find the next rule to evaluate
        }
        
        // No rule found, return the default access type
        return defaultAccessType;
    }
    

    private static final class AddressRule implements Serializable {

        private static final long serialVersionUID = 1L;
        
        private final boolean includeChildren;
        private final AccessType accessType;
        private final UnmodifiableSet<Class<?>> types;

        AddressRule(boolean includeChildren, AccessType accessType, Collection<Class<?>> types) {
            Validate.notNull(accessType);
            Validate.notNull(types);
            Validate.noNullElements(types);
            
            this.includeChildren = includeChildren;
            this.accessType = accessType;
            this.types = (UnmodifiableSet<Class<?>>) UnmodifiableSet.unmodifiableSet(new LinkedHashSet<>(types));
        }

        public boolean isIncludeChildren() {
            return includeChildren;
        }

        public AccessType getAccessType() {
            return accessType;
        }

        public UnmodifiableSet<Class<?>> getTypes() {
            return types;
        }
        
    }
    
    /**
     * Access type.
     */
    public enum AccessType {
        /**
         * Access should be allowed.
         */
        ALLOW,
        /**
         * Access should be blocked.
         */
        REJECT
    }
}

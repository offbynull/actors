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
package com.offbynull.actors.core.actor;

import com.offbynull.actors.core.shuttle.Address;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.Validate;

final class RuleSet {
    
    private AccessType defaultAccessType;
    private final Map<Address, AddressRule> rules;
    
    public RuleSet() {
        defaultAccessType = AccessType.REJECT;
        rules = new HashMap<>();
    }
    
    public void allowAll() {
        defaultAccessType = AccessType.ALLOW;
        rules.clear();
    }

    public void rejectAll() {
        defaultAccessType = AccessType.REJECT;
        rules.clear();
    }

    public void allow(Address address, boolean includeChildren, Class<?> ... types) {
        Validate.notNull(address);
        Validate.notNull(types);
        Validate.noNullElements(types);
        rules.put(address, new AddressRule(includeChildren, AccessType.ALLOW, Arrays.asList(types)));
    }

    public void reject(Address address, boolean includeChildren, Class<?> ... types) {
        Validate.notNull(address);
        Validate.notNull(types);
        Validate.noNullElements(types);
        rules.put(address, new AddressRule(includeChildren, AccessType.REJECT, Arrays.asList(types)));
    }
    
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
    

    private static final class AddressRule {
        private final boolean includeChildren;
        private final AccessType accessType;
        private final Set<Class<?>> types;

        public AddressRule(boolean includeChildren, AccessType accessType, Collection<Class<?>> types) {
            Validate.notNull(accessType);
            Validate.notNull(types);
            Validate.noNullElements(types);
            
            this.includeChildren = includeChildren;
            this.accessType = accessType;
            this.types = Collections.unmodifiableSet(new LinkedHashSet<>(types));
        }

        public boolean isIncludeChildren() {
            return includeChildren;
        }

        public AccessType getAccessType() {
            return accessType;
        }

        public Set<Class<?>> getTypes() {
            return types;
        }
        
    }
    
    public enum AccessType {
        ALLOW,
        REJECT
    }
}

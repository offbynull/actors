package com.offbynull.actors.gateways.actor;

import com.offbynull.actors.gateways.actor.RuleSet.AccessType;
import com.offbynull.actors.address.Address;
import org.junit.Test;
import static org.junit.Assert.*;

public class RuleSetTest {
    
    private RuleSet fixture = new RuleSet();

    @Test
    public void mustRejectByDefault() {
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
    }

    @Test
    public void mustAlwaysReject() {
        fixture.rejectAll();
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr2:addr1"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    @Test
    public void mustRejectExceptForAllowedAddress() {
        fixture.rejectAll();
        fixture.allow(Address.fromString("addr1:addr2"), false);
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    @Test
    public void mustRejectExceptForAllowedAddressAndItsChildren() {
        fixture.rejectAll();
        fixture.allow(Address.fromString("addr1:addr2"), true);
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    @Test
    public void mustRejectExceptForAllowedAddressAndItsChildrenButNotOneSpecificChild() {
        fixture.rejectAll();
        fixture.allow(Address.fromString("addr1:addr2"), true);
        fixture.reject(Address.fromString("addr1:addr2:addr3:addr4"), false);
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4:addr5"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    @Test
    public void mustRejectExceptForAllowedAddressAndItsChildrenButNotOneSpecificChildAndItsChildren() {
        fixture.rejectAll();
        fixture.allow(Address.fromString("addr1:addr2"), true);
        fixture.reject(Address.fromString("addr1:addr2:addr3:addr4"), true);
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4:addr5"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    
    
    
    
    
    @Test
    public void mustAlwaysAllow() {
        fixture.allowAll();
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr2:addr1"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }
    
    @Test
    public void mustAllowExceptForRejectedAddress() {
        fixture.allowAll();
        fixture.reject(Address.fromString("addr1:addr2"), false);
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    @Test
    public void mustAllowExceptForRejectedAddressAndItsChildren() {
        fixture.allowAll();
        fixture.reject(Address.fromString("addr1:addr2"), true);
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    @Test
    public void mustAllowExceptForRjectedAddressAndItsChildrenButNotOneSpecificChild() {
        fixture.allowAll();
        fixture.reject(Address.fromString("addr1:addr2"), true);
        fixture.allow(Address.fromString("addr1:addr2:addr3:addr4"), false);
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4:addr5"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    @Test
    public void mustAllowExceptForRejectedAddressAndItsChildrenButNotOneSpecificChildAndItsChildren() {
        fixture.allowAll();
        fixture.reject(Address.fromString("addr1:addr2"), true);
        fixture.allow(Address.fromString("addr1:addr2:addr3:addr4"), true);
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2:addr3:addr4:addr5"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString(""), Object.class)
        );
    }

    
    
    
    
    
    @Test
    public void mustBeAbleToSwitchDefaultAccess() {
        fixture.allowAll();
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        fixture.rejectAll();
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        fixture.allowAll();
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        fixture.rejectAll();
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
    }

    @Test
    public void mustBeAbleToSwitchAccessOfAnAddress() {
        fixture.rejectAll();
        fixture.allow(Address.fromString("addr1:addr2"), false);
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        fixture.reject(Address.fromString("addr1:addr2"), false);
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        fixture.allow(Address.fromString("addr1:addr2"), false);
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
        fixture.reject(Address.fromString("addr1:addr2"), false);
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );
    }

    
    
    
    
    
    @Test
    public void mustAllowOnlyCertainTypes() {
        fixture.rejectAll();
        fixture.allow(Address.fromString("addr1:addr2"), false, String.class);
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1"), String.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), String.class)
        );        
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Integer.class)
        );        
    }

    @Test
    public void mustAllowOnlyExactTypesNotInheritedTypes() {
        fixture.rejectAll();
        fixture.allow(Address.fromString("addr1:addr2"), false, Object.class);
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );        
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), String.class)
        );        
    }

    
    
    
    
    
    @Test
    public void mustRejectOnlyCertainTypes() {
        fixture.allowAll();
        fixture.reject(Address.fromString("addr1:addr2"), false, String.class);
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1"), String.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), String.class)
        );        
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), Integer.class)
        );        
    }
    
    @Test
    public void mustRejectExactTypesNotInheritedTypes() {
        fixture.allowAll();
        fixture.reject(Address.fromString("addr1:addr2"), false, Object.class);
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1"), Object.class)
        );
        assertEquals(
                AccessType.REJECT,
                fixture.evaluate(Address.fromString("addr1:addr2"), Object.class)
        );        
        assertEquals(
                AccessType.ALLOW,
                fixture.evaluate(Address.fromString("addr1:addr2"), String.class)
        );        
    }
}

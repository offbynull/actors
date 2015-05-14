package com.offbynull.peernetic.core.actor.helpers;

import com.offbynull.coroutines.user.Continuation;
import com.offbynull.peernetic.core.actor.Context;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME;
import static com.offbynull.peernetic.core.actor.helpers.SubcoroutineRouter.AddBehaviour.ADD_PRIME_NO_FINISH;
import com.offbynull.peernetic.core.shuttle.Address;
import org.apache.commons.lang3.mutable.MutableInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SubcoroutineRouterTest {

    private static final Address DST_ADDRESS_PREFIX = Address.fromString("local:actor");
    private static final Address ROUTER_ID = Address.fromString("router");
    private static final Address CHILD_ID = Address.fromString("router:child");

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Context context;
    private SubcoroutineRouter fixture;

    @Before
    public void setUp() {
        context = mock(Context.class);
        fixture = new SubcoroutineRouter(ROUTER_ID, context);
    }

    @Test
    public void mustSilentlyIgnoreForwardsToUnknownChildren() throws Exception {
        when(context.getSelf()).thenReturn(DST_ADDRESS_PREFIX);
        when(context.getDestination()).thenReturn(DST_ADDRESS_PREFIX.appendSuffix(CHILD_ID));
        when(context.getIncomingMessage()).thenReturn(new Object());
        boolean forwarded = fixture.forward();

        assertFalse(forwarded);
    }

    @Test
    public void mustForwardToChild() throws Exception {
        MutableInt mutableInt = new MutableInt();
        Subcoroutine<Void> subcoroutine = new Subcoroutine<Void>() {

            @Override
            public Address getId() {
                return CHILD_ID;
            }

            @Override
            public Void run(Continuation cnt) throws Exception {
                mutableInt.increment();
                return null;
            }
        };

        fixture.getController().add(subcoroutine, ADD);

        when(context.getSelf()).thenReturn(DST_ADDRESS_PREFIX);
        when(context.getDestination()).thenReturn(DST_ADDRESS_PREFIX.appendSuffix(CHILD_ID));
        when(context.getIncomingMessage()).thenReturn(new Object());
        boolean forwarded = fixture.forward();

        assertTrue(forwarded);
        assertEquals(1, mutableInt.intValue());
    }

    @Test
    public void mustAddNewChildAndPrime() throws Exception {
        MutableInt mutableInt = new MutableInt();
        Subcoroutine<Void> subcoroutine = new Subcoroutine<Void>() {

            @Override
            public Address getId() {
                return CHILD_ID;
            }

            @Override
            public Void run(Continuation cnt) throws Exception {
                mutableInt.increment();
                return null;
            }
        };

        when(context.getSelf()).thenReturn(DST_ADDRESS_PREFIX);
        when(context.getDestination()).thenReturn(DST_ADDRESS_PREFIX.appendSuffix(CHILD_ID));
        when(context.getIncomingMessage()).thenReturn(new Object());
        fixture.getController().add(subcoroutine, ADD_PRIME);

        assertEquals(1, mutableInt.intValue());
    }

    @Test
    public void mustSilentlyIgnoreForwardsToRemovedChild() throws Exception {
        MutableInt mutableInt = new MutableInt();
        Subcoroutine<Void> subcoroutine = new Subcoroutine<Void>() {

            @Override
            public Address getId() {
                return CHILD_ID;
            }

            @Override
            public Void run(Continuation cnt) throws Exception {
                mutableInt.increment();
                cnt.suspend();
                return null;
            }
        };

        fixture.getController().add(subcoroutine, ADD);

        when(context.getSelf()).thenReturn(DST_ADDRESS_PREFIX);
        when(context.getDestination()).thenReturn(DST_ADDRESS_PREFIX.appendSuffix(CHILD_ID));
        when(context.getIncomingMessage()).thenReturn(new Object());
        boolean forwarded = fixture.forward();

        assertTrue(forwarded);
        assertEquals(1, mutableInt.intValue());
        
        
        fixture.getController().remove(CHILD_ID);
        
        forwarded = fixture.forward();
        assertFalse(forwarded);
        assertEquals(1, mutableInt.intValue());
    }

    @Test
    public void mustAddNewChildAndPrimeWhenChildDoesNotFinishOnPrime() throws Exception {
        MutableInt mutableInt = new MutableInt();
        Subcoroutine<Void> subcoroutine = new Subcoroutine<Void>() {

            @Override
            public Address getId() {
                return CHILD_ID;
            }

            @Override
            public Void run(Continuation cnt) throws Exception {
                mutableInt.increment();
                cnt.suspend();
                return null;
            }
        };

        when(context.getSelf()).thenReturn(DST_ADDRESS_PREFIX);
        when(context.getDestination()).thenReturn(DST_ADDRESS_PREFIX.appendSuffix(CHILD_ID));
        when(context.getIncomingMessage()).thenReturn(new Object());
        fixture.getController().add(subcoroutine, ADD_PRIME_NO_FINISH);
        
        assertEquals(1, mutableInt.intValue());
    }

    @Test
    public void mustFailAddNewChildAndPrimeWhenChildFinishesOnPrime() throws Exception {
        Subcoroutine<Void> subcoroutine = new Subcoroutine<Void>() {

            @Override
            public Address getId() {
                return CHILD_ID;
            }

            @Override
            public Void run(Continuation cnt) throws Exception {
                return null;
            }
        };

        when(context.getSelf()).thenReturn(DST_ADDRESS_PREFIX);
        when(context.getDestination()).thenReturn(DST_ADDRESS_PREFIX.appendSuffix(CHILD_ID));
        when(context.getIncomingMessage()).thenReturn(new Object());
        
        exception.expect(IllegalStateException.class);
        
        fixture.getController().add(subcoroutine, ADD_PRIME_NO_FINISH);
    }

    @Test
    public void mustFailOnConflictingAdd() throws Exception {
        Subcoroutine<Void> subcoroutine = new Subcoroutine<Void>() {

            @Override
            public Address getId() {
                return CHILD_ID;
            }

            @Override
            public Void run(Continuation cnt) throws Exception {
                return null;
            }
        };
        
        fixture.getController().add(subcoroutine, ADD);
        
        exception.expect(IllegalArgumentException.class);
        fixture.getController().add(subcoroutine, ADD);
    }

    @Test
    public void mustFailOnIncorrectAdd() throws Exception {
        Subcoroutine<Void> subcoroutine = new Subcoroutine<Void>() {

            @Override
            public Address getId() {
                return Address.of("badid");
            }

            @Override
            public Void run(Continuation cnt) throws Exception {
                return null;
            }
        };
        
        exception.expect(IllegalArgumentException.class);
        fixture.getController().add(subcoroutine, ADD);
    }

    @Test
    public void mustFailOnMissingRemove() throws Exception {
        exception.expect(IllegalArgumentException.class);
        fixture.getController().remove(CHILD_ID);
    }

    @Test
    public void mustFailOnIncorrectRemove() throws Exception {
        exception.expect(IllegalArgumentException.class);
        fixture.getController().remove(ROUTER_ID.appendSuffix("fake"));
    }
}

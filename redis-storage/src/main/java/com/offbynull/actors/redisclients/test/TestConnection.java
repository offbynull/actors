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
package com.offbynull.actors.redisclients.test;

import com.offbynull.actors.redisclient.Connection;
import com.offbynull.actors.redisclient.ConnectionException;
import com.offbynull.actors.redisclient.Connector;
import com.offbynull.actors.redisclient.SortedSetItem;
import com.offbynull.actors.redisclient.Transaction;
import com.offbynull.actors.redisclient.TransactionQueue;
import com.offbynull.actors.redisclient.TransactionResult;
import com.offbynull.actors.redisclient.Watch;
import java.io.IOException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import static java.util.stream.Collectors.toList;
import org.apache.commons.lang3.Validate;

/**
 * Test connection. This class is thread-safe.
 * <p>
 * If the {@link Connector} that generated this {@link Connection} is closed, this {@link Connection} is also closed.
 * @author Kasra Faghihi
 */
public final class TestConnection implements Connection {
    private final Map<String, Item> database;
    private final AtomicBoolean factoryClosed;
    private final AtomicBoolean clientClosed;

    /**
     * Constructs a {@link TestConnection} object. This instance doesn't share its internal database with any other {@link TestConnection}s.
     * @throws NullPointerException if any argument is {@code null}
     */
    public TestConnection() {
        this(new HashMap<>(), new AtomicBoolean());
    }

    TestConnection(Map<String, Item> database, AtomicBoolean factoryClosed) {
        Validate.notNull(database);
        Validate.notNull(factoryClosed);
        this.database = database;
        this.factoryClosed = factoryClosed;
        this.clientClosed = new AtomicBoolean();
    }

    @Override
    public boolean exists(String key) {
        Validate.notNull(key);
        Validate.validState(!factoryClosed.get(), "Closed");
        Validate.validState(!clientClosed.get(), "Closed");
        synchronized (database) {
            return database.containsKey(key);
        }
    }

    @Override
    public <T> T get(String key, Function<byte[], T> converter) {
        Validate.notNull(key);
        Validate.notNull(converter);
        Validate.validState(!factoryClosed.get(), "Closed");
        Validate.validState(!clientClosed.get(), "Closed");
        synchronized (database) {
            byte[] val = getItem(key);
            return converter.apply(copy(val));
        }
    }

    @Override
    public long llen(String key) {
        Validate.notNull(key);
        Validate.validState(!factoryClosed.get(), "Closed");
        Validate.validState(!clientClosed.get(), "Closed");
        synchronized (database) {
            InternalList ret = getItem(key);
            return ret == null ? 0 : ret.size();
        }
    }

    @Override
    public void zadd(String key, double score, byte[] val) {
        Validate.notNull(key);
        Validate.notNull(val);
        Validate.validState(!factoryClosed.get(), "Closed");
        Validate.validState(!clientClosed.get(), "Closed");
        synchronized (database) {
            InternalSortedSet set = getItem(key);
            if (set == null) {
                set = setItem(key, new InternalSortedSet());
            }
            set.put(score, copy(val));
        }
    }

    @Override
    public void pexire(String key, long duration) {
        Validate.notNull(key);
        Validate.validState(!factoryClosed.get(), "Closed");
        Validate.validState(!clientClosed.get(), "Closed");
        synchronized (database) {
            Item item = database.get(key);
            if (item != null) {
                item.killTime = System.currentTimeMillis() + duration;
            }
        }
    }

    @Override
    public void pexireAt(String key, long timestamp) {
        Validate.notNull(key);
        Validate.validState(!factoryClosed.get(), "Closed");
        Validate.validState(!clientClosed.get(), "Closed");
        synchronized (database) {
            Item item = database.get(key);
            if (item != null) {
                item.killTime = timestamp;
            }
        }
    }

    @Override
    public <T> Collection<SortedSetItem> zrangeWithScores(String key, long start, long end, Function<byte[], T> converter) {
        Validate.notNull(key);
        Validate.notNull(converter);
        Validate.isTrue(start >= 0L);
        Validate.isTrue(end >= 0L);
        Validate.validState(!factoryClosed.get(), "Closed");
        Validate.validState(!clientClosed.get(), "Closed");
        synchronized (database) {
            InternalSortedSet set = getItem(key);
            if (set == null) {
                return new ArrayList<>();
            }
            return set.getByRank(start, end).stream()
                    .map(i -> new SortedSetItem(i.getScore(), converter.apply(copy(i.getData()))))
                    .collect(toList());
        }
    }

    @Override
    public TransactionResult transaction(Transaction transaction, Watch... watches) throws ConnectionException {
        Validate.notNull(transaction);
        Validate.notNull(watches);
        Validate.noNullElements(watches);
        Validate.validState(!factoryClosed.get(), "Closed");
        Validate.validState(!clientClosed.get(), "Closed");
        synchronized (database) {
            top1:
            while (true) {
                for (Watch watch : watches) {
                    if (!watch.getBlock().execute()) {
                        if (!watch.getRetry()) {
                            return null;
                        }
                        continue top1;
                    }
                }
                
                break;
            }

            // this is where the actual watches would be placed

            top2:
            while (true) {
                for (Watch watch : watches) {
                    if (!watch.getBlock().execute()) {
                        if (!watch.getRetry()) {
                            return null;
                        }
                        continue top2;
                    }
                }
                
                break;
            }

            // this is where the multi would happen
            List<Supplier<Object>> queueOps = new ArrayList<>();
            TransactionQueue queue = new TransactionQueue() {
                @Override
                public <T> void zrange(String key, long start, long end, Function<byte[], T> converter) {
                    Validate.notNull(key);
                    Validate.notNull(converter);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        InternalSortedSet set = getItem(key);
                        if (set == null) {
                            return new ArrayList<>();
                        }
                        return set.getByRank(start, end).stream()
                                .map(i -> converter.apply(copy(i.getData())))
                                .collect(toList());
                    });
                }

                @Override
                public void zremrangeByRank(String key, long start, long end) {
                    Validate.notNull(key);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        InternalSortedSet set = getItem(key);
                        if (set != null) {
                            set.removeByRank(start, end);
                            if (set.isEmpty()) {
                                database.remove(key);
                            }
                        }
                        return null;
                    });
                }

                @Override
                public void lpush(String key, byte[] val) {
                    Validate.notNull(key);
                    Validate.notNull(val);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        InternalList list = getItem(key);
                        if (list == null) {
                            list = setItem(key, new InternalList());
                        }
                        list.lpush(copy(val));
                        return (long) list.size();
                    });
                }

                @Override
                public void rpush(String key, byte[] val) {
                    Validate.notNull(key);
                    Validate.notNull(val);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        InternalList list = getItem(key);
                        if (list == null) {
                            list = setItem(key, new InternalList());
                        }
                        list.rpush(copy(val));
                        return (long) list.size();
                    });
                }

                @Override
                public <T> void rpop(String key, Function<byte[], T> converter) {
                    Validate.notNull(key);
                    Validate.notNull(converter);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        InternalList list = getItem(key);
                        if (list == null) {
                            return converter.apply(null);
                        }
                        byte[] val = list.rpop();
                        if (list.isEmpty()) {
                            database.remove(key);
                        }
                        return converter.apply(copy(val));
                    });
                }

                @Override
                public <T> void lpop(String key, Function<byte[], T> converter) {
                    Validate.notNull(key);
                    Validate.notNull(converter);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        InternalList list = getItem(key);
                        if (list == null) {
                            return converter.apply(null);
                        }
                        byte[] val = list.lpop();
                        if (list.isEmpty()) {
                            database.remove(key);
                        }
                        return converter.apply(copy(val));
                    });
                }

                @Override
                public void llen(String key) {
                    Validate.notNull(key);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        InternalList ret = getItem(key);
                        return ret == null ? 0L : (long) ret.size();
                    });
                }

                @Override
                public <T> void lrange(String key, int start, int end, Function<byte[], T> converter) throws ConnectionException {
                    Validate.notNull(key);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        InternalList ret = getItem(key);
                        return ret == null ? new LinkedList<>() : ret.lrange(start, end).stream()
                                .map(i -> converter.apply(copy(i)))
                                .collect(toList());
                    });
                }

                @Override
                public void set(String key, byte[] val) {
                    Validate.notNull(key);
                    Validate.notNull(val);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        setItem(key, copy(val));
                        return null;
                    });
                }

                @Override
                public <T> void get(String key, Function<byte[], T> converter) {
                    Validate.notNull(key);
                    Validate.notNull(converter);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        byte[] val = getItem(key);
                        return converter.apply(copy(val));
                    });
                }

                @Override
                public void exists(String key) {
                    Validate.notNull(key);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        boolean val = database.containsKey(key);
                        return val;
                    });
                }

                @Override
                public void incr(String key) {
                    Validate.notNull(key);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        byte[] valRaw = getItem(key);
                        long newVal;
                        if (valRaw != null) {
                            String val = new String(valRaw, UTF_8);
                            newVal = Long.parseLong(val) + 1;
                        } else {
                            newVal = 1L;
                        }
                        valRaw = String.valueOf(newVal).getBytes(UTF_8);
                        setItem(key, valRaw);

                        return newVal;
                    });
                }

                @Override
                public void del(String key) {
                    Validate.notNull(key);
                    Validate.validState(!factoryClosed.get(), "Closed");
                    Validate.validState(!clientClosed.get(), "Closed");
                    queueOps.add(() -> {
                        database.remove(key);

                        return null;
                    });
                }
            };
            transaction.getBlock().execute(queue);

            List<Object> result = queueOps.stream().map(s -> s.get()).collect(toList());
            return new TransactionResult(result);
        }
    }

    @Override
    public void close() throws IOException {
        clientClosed.set(true);
    }
    
    
    private static byte[] copy(byte[] data) {
        return data == null ? null : Arrays.copyOf(data, data.length);
    }
    
    
    
    private <T> T getItem(String key) {
        Item item = database.get(key);
        if (item == null) {
            return null;
        }
        if (item.killTime <= System.currentTimeMillis()) {
            database.remove(key);
            return null;
        }
        return (T) item.item;
    }

    private <T> T setItem(String key, T value) {
        Item item = new Item();
        item.item = value;
        item.killTime = Long.MAX_VALUE;

        database.put(key, item);
        
        return value;
    }

    static final class Item {
        private Object item;
        private long killTime = System.currentTimeMillis();
    }
}

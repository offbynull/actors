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
package com.offbynull.actors.stores.redis.connectors.jedis;

import java.io.IOException;
import org.apache.commons.lang3.Validate;
import redis.clients.jedis.Jedis;
import com.offbynull.actors.stores.redis.connector.SortedSetItem;
import com.offbynull.actors.stores.redis.connector.Watch;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import static java.util.stream.Collectors.toList;
import com.offbynull.actors.stores.redis.connector.Connection;
import com.offbynull.actors.stores.redis.connector.ConnectionException;
import com.offbynull.actors.stores.redis.connector.Connector;
import com.offbynull.actors.stores.redis.connector.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;
import com.offbynull.actors.stores.redis.connector.TransactionQueue;
import com.offbynull.actors.stores.redis.connector.TransactionResult;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import static java.util.stream.Collectors.toSet;

/**
 * Jedis connection. This class isn't thread-safe.
 * <p>
 * If the {@link Connector} that generated this {@link Connection} is closed, this {@link Connection} is also closed.
 * @author Kasra Faghihi
 */
public final class JedisConnection implements Connection {
    // IMPORTANT NOTE:
    //
    // When you call JedisPool.close(), it calls Jedis.close() on all the objects in the pool. Calling Jedis.close() closes the socket, but
    // if you try to keep working with that Jedis object it'll re-open the socket. As such, it isn't enough for us to just call
    // Jedis.close() here.
    // 
    // We need to manually track closing. That means that...
    //   1. when this object is closed, we need to call Jedis.close().
    //   2. when this object is closed, we need to set a flag to indicate the object can't be used anymore (throws illegalstateexc).
    //   3. when the factory for this object is closed, the Jedis instance for this object is closed as well. But, another thread may
    //      executing a method in the object while Jedis.close() is called -- This may end up re-opening the Jedis object because if it
    //      gets used after being closed it re-opens the internal socket used to connect to the Redis servert. We need to make sure that
    //      when the method returns, if the factory was closed, the Jedis object goes back to being closed. If we don't do this, we may end
    //      up with a leaked socket.
    
    
    private final Jedis jedis; // set to null when closed
    private final AtomicBoolean factoryClosed;
    private boolean clientClosed; // the closed flag for this object, remember this object is not THREADSAFE

    /**
     * Constructs a {@link JedisConnection} object.
     * @param jedis jedis object
     * @throws NullPointerException if any argument is {@code null}
     */
    public JedisConnection(Jedis jedis) {
        this(jedis, new AtomicBoolean());
    }

    /**
     * Constructs a {@link JedisConnection} object from a {@link JedisConnector}.
     * @param jedis jedis object
     * @param factoryClosed factory's closed state
     * @throws NullPointerException if any argument is {@code null}
     */
    JedisConnection(Jedis jedis, AtomicBoolean factoryClosed) {
        Validate.notNull(jedis);
        Validate.notNull(factoryClosed);
        this.jedis = jedis;
        this.factoryClosed = factoryClosed;
    }

    @Override
    public void zadd(String key, double score, byte[] val) throws ConnectionException {
        Validate.notNull(key);
        Validate.notNull(val);
        Validate.validState(!clientClosed, "Closed");
        Validate.validState(!factoryClosed.get(), "Closed");
        try {
            jedis.zadd(key.getBytes(UTF_8), score, val);
        } catch (JedisConnectionException jce) {
            throw new ConnectionException(true, jce);
        } catch (RuntimeException re) {
            throw new ConnectionException(false, re);
        } finally {
            ifClosedEnsureJedisClosedAsWell();
        }
    }

    @Override
    public <T> Collection<SortedSetItem> zrangeWithScores(String key, long start, long end, Function<byte[], T> converter)
            throws ConnectionException {
        Validate.notNull(key);
        Validate.notNull(converter);
        Validate.isTrue(start >= 0L);
        Validate.isTrue(end >= 0L);
        Validate.validState(!clientClosed, "Closed");
        Validate.validState(!factoryClosed.get(), "Closed");
        try {
            Collection<SortedSetItem> val =
                    jedis.zrangeWithScores(key.getBytes(UTF_8), start, end).stream()
                    .map(t -> new SortedSetItem(t.getScore(), t.getBinaryElement()))
                    .collect(toList());

            return val;
        } catch (JedisConnectionException jce) {
            throw new ConnectionException(true, jce);
        } catch (RuntimeException re) {
            throw new ConnectionException(false, re);
        } finally {
            ifClosedEnsureJedisClosedAsWell();
        }
    }
    


    @Override
    public long llen(String key) throws ConnectionException {
        Validate.notNull(key);
        Validate.validState(!clientClosed, "Closed");
        Validate.validState(!factoryClosed.get(), "Closed");
        try {
            long len = jedis.llen(key.getBytes(UTF_8));

            return len;
        } catch (JedisConnectionException jce) {
            throw new ConnectionException(true, jce);
        } catch (RuntimeException re) {
            throw new ConnectionException(false, re);
        } finally {
            ifClosedEnsureJedisClosedAsWell();
        }
    }

    @Override
    public <T> T get(String key, Function<byte[], T> converter) throws ConnectionException {
        Validate.notNull(key);
        Validate.notNull(converter);
        Validate.validState(!clientClosed, "Closed");
        Validate.validState(!factoryClosed.get(), "Closed");
        try {
            T val = (T) converter.apply(jedis.get(key.getBytes(UTF_8)));
            
            return val;
        } catch (JedisConnectionException jce) {
            throw new ConnectionException(true, jce);
        } catch (RuntimeException re) {
            throw new ConnectionException(false, re);
        } finally {
            ifClosedEnsureJedisClosedAsWell();
        }
    }

    @Override
    public boolean exists(String key) throws ConnectionException {
        Validate.notNull(key);
        Validate.validState(!clientClosed, "Closed");
        Validate.validState(!factoryClosed.get(), "Closed");
        try {
            boolean ret = jedis.exists(key.getBytes(UTF_8));

            return ret;
        } catch (JedisConnectionException jce) {
            throw new ConnectionException(true, jce);
        } catch (RuntimeException re) {
            throw new ConnectionException(false, re);
        } finally {
            ifClosedEnsureJedisClosedAsWell();
        }
    }

    @Override
    public TransactionResult transaction(Transaction transaction, Watch... watches) throws ConnectionException {
        Validate.notNull(transaction);
        Validate.notNull(watches);
        Validate.noNullElements(watches);
        Validate.validState(!clientClosed, "Closed");
        Validate.validState(!factoryClosed.get(), "Closed");
        try {
            top:
            while (true) {
                for (Watch watch : watches) {
                    if (!watch.getBlock().execute()) {
                        if (!watch.getRetry()) {
                            return null;
                        }
                        continue top;
                    }
                }

                for (Watch watch : watches) {
                    jedis.watch(watch.getKey());
                }

                for (Watch watch : watches) {
                    if (!watch.getBlock().execute()) {
                        jedis.unwatch();
                        if (!watch.getRetry()) {
                            return null;
                        }
                        continue top;
                    }
                }



                try (redis.clients.jedis.Transaction jedisTransaction = jedis.multi()) {
                    JedisTransactionQueue transactionQueue = new JedisTransactionQueue(jedisTransaction);
                    
                    transaction.getBlock().execute(transactionQueue);
                    
                    List<Object> ret = jedisTransaction.exec();

                    if (ret != null && ret.isEmpty() && !transactionQueue.converters.isEmpty()) {
                        // this is a bug with jedis -- https://github.com/xetorthio/jedis/issues/1592
                        // it returns an empty list of results for some failed transactions, but it should be returning null for failed
                        // transactions... if the number of returned results is 0 but we expect more than 0 results, it means that the
                        // transaction failed
                        ret = null;
                    }

                    if (ret != null) {
                        LinkedList<Object> convertedRet = new LinkedList<>();
                        for (Object retItem : ret) {
                            Object convertedRetItem = transactionQueue.converters.removeFirst().apply(retItem);
                            convertedRet.addLast(convertedRetItem);
                        }
                        return new TransactionResult(convertedRet);
                    }

                    if (/*ret == null && */!transaction.isRetry()) { // ret is known to be null if we reach this point
                        return null;
                    }
                } catch (IOException ioe) {
                    // the jedis client will never throw this -- the developers left the default 'throws IOException' into close which is
                    // what is causing the need for this
                    throw new IllegalStateException(ioe); // should never happen
                }
            } 
        } catch (JedisConnectionException jce) {
            throw new ConnectionException(true, jce);
        } catch (RuntimeException re) {
            throw new ConnectionException(false, re);
        } finally {
            ifClosedEnsureJedisClosedAsWell();
        }
    }

    @Override
    public void close() throws IOException {
        jedis.close();
    }
    
    
    
    
    
    
    private final class JedisTransactionQueue implements TransactionQueue {
        private final redis.clients.jedis.Transaction t;
        private final LinkedList<Function<Object, Object>> converters;

        JedisTransactionQueue(redis.clients.jedis.Transaction internalTransaction) {
            Validate.notNull(internalTransaction);
            this.t = internalTransaction;
            this.converters = new LinkedList<>();
        }

        @Override
        public <T> void zrange(String key, long start, long end, Function<byte[], T> converter) throws ConnectionException {
            Validate.notNull(key);
            Validate.notNull(converter);
            Validate.isTrue(start >= 0L);
            Validate.isTrue(end >= 0L);
            try {
                t.zrange(key.getBytes(UTF_8), start, end);
                converters.add(in -> {
                    return ((Set<byte[]>) in).stream()
                            .map(val -> (Object) converter.apply(val))
                            .collect(toSet());
                });
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }

        @Override
        public void zremrangeByRank(String key, long start, long end) throws ConnectionException {
            Validate.notNull(key);
            Validate.isTrue(start >= 0L);
            Validate.isTrue(end >= 0L);
            Validate.validState(!clientClosed, "Closed");
            Validate.validState(!factoryClosed.get(), "Closed");
            try {
                t.zremrangeByRank(key.getBytes(UTF_8), start, end);
                converters.add(in -> null);
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }

        @Override
        public void lpush(String key, byte[] val) throws ConnectionException {
            Validate.notNull(key);
            Validate.notNull(val);
            Validate.validState(!clientClosed, "Closed");
            Validate.validState(!factoryClosed.get(), "Closed");
            try {
                t.lpush(key.getBytes(UTF_8), val);
                converters.add(in -> null);
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }

        @Override
        public <T> void rpop(String key, Function<byte[], T> converter) throws ConnectionException {
            Validate.notNull(key);
            Validate.notNull(converter);
            Validate.validState(!clientClosed, "Closed");
            Validate.validState(!factoryClosed.get(), "Closed");
            try {
                t.rpop(key.getBytes(UTF_8));
                converters.add(in -> (Object) converter.apply((byte[]) in));
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }

        @Override
        public void set(String key, byte[] val) throws ConnectionException {
            Validate.notNull(key);
            Validate.notNull(val);
            Validate.validState(!clientClosed, "Closed");
            Validate.validState(!factoryClosed.get(), "Closed");
            try {
                t.set(key.getBytes(UTF_8), val);
                converters.add(in -> null);
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }

        @Override
        public <T> void get(String key, Function<byte[], T> converter) throws ConnectionException  {
            Validate.notNull(key);
            Validate.notNull(converter);
            Validate.validState(!clientClosed, "Closed");
            Validate.validState(!factoryClosed.get(), "Closed");
            try {
                t.get(key.getBytes(UTF_8));
                converters.add(in -> (Object) converter.apply((byte[]) in));
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }

        @Override
        public void exists(String key) throws ConnectionException  {
            Validate.notNull(key);
            Validate.validState(!clientClosed, "Closed");
            Validate.validState(!factoryClosed.get(), "Closed");
            try {
                t.exists(key.getBytes(UTF_8));
                converters.add(in -> in);
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }

        @Override
        public void incr(String key) throws ConnectionException  {
            Validate.notNull(key);
            Validate.validState(!clientClosed, "Closed");
            Validate.validState(!factoryClosed.get(), "Closed");
            try {
                t.incr(key.getBytes(UTF_8));
                converters.add(in -> in);
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }
        
        @Override
        public void del(String key) throws ConnectionException {
            Validate.notNull(key);
            Validate.validState(!clientClosed, "Closed");
            Validate.validState(!factoryClosed.get(), "Closed");
            try {
                t.del(key.getBytes(UTF_8));
                converters.add(in -> null);
            } catch (JedisConnectionException jce) {
                throw new ConnectionException(true, jce);
            } catch (RuntimeException re) {
                throw new ConnectionException(false, re);
            } finally {
                ifClosedEnsureJedisClosedAsWell();
            }
        }
    }
    
    
    // See comment at top of class for why this is here
    private void ifClosedEnsureJedisClosedAsWell() {
        if (factoryClosed.get() || clientClosed) {
            jedis.close();
            throw new IllegalStateException("Closed");
        }
    }
}

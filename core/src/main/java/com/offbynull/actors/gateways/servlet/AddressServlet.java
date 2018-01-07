/*
 * Copyright (c) 2018, Kasra Faghihi, All rights reserved.
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
package com.offbynull.actors.gateways.servlet;

import com.offbynull.actors.address.Address;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AddressServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(AddressServlet.class);

    private final String prefix;
    private final SecureRandom secureRandom;
    private final CountDownLatch shutdownLatch;

    AddressServlet(String prefix, CountDownLatch shutdownLatch) {
        Validate.notNull(prefix);
        Validate.notNull(shutdownLatch);
        
        try {
            this.secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException nsae) {
            throw new IllegalStateException(nsae); // should never happen
        }
        this.prefix = prefix;
        this.shutdownLatch = shutdownLatch;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (shutdownLatch.getCount() == 0L) {
            resp.setStatus(SC_SERVICE_UNAVAILABLE);
            return;
        }

        try {
            String id = secureRandom.ints(64, 0x20, 0x80) // printable ascii characters
                    .mapToObj(r -> String.valueOf((char) r))
                    .collect(Collectors.joining());
            Address address = Address.of(prefix, id);
            
            try (Writer writer = resp.getWriter()) {
                IOUtils.write(address.toString(), writer);
            }
        } catch (RuntimeException | IOException e) {
            resp.setStatus(SC_INTERNAL_SERVER_ERROR);
            LOG.error("Servlet failed: {}", e);
        }
    }

}

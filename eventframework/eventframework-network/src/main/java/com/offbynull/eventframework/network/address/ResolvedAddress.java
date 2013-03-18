package com.offbynull.eventframework.network.address;

public final class ResolvedAddress {
    private String hostname;
    private String ip;

    public ResolvedAddress(String hostname, String ip) {
        // hostname can be null
        if (ip == null) {
            throw new NullPointerException();
        }
        
        this.hostname = hostname;
        this.ip = ip;
    }

    public String getHostname() {
        return hostname;
    }

    public String getIp() {
        return ip;
    }
    
}

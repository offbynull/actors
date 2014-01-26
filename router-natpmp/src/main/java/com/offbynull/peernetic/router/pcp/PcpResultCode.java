package com.offbynull.peernetic.router.pcp;

import org.apache.commons.lang3.Validate;

// Error messages taken directly out of http://tools.ietf.org/html/rfc6887
enum PcpResultCode {
    SUCCESS("Success."),
    UNSUPP_VERSION("The version number at the start of the PCP Request header is not recognized by this PCP server.  This is a long"
            + " lifetime error.  This document describes PCP version 2."),
    NOT_AUTHORIZED("The requested operation is disabled for this PCP client, or the PCP client requested an operation that cannot be"
            + " fulfilled by the PCP server's security policy.  This is a long lifetime error."),
    MALFORMED_REQUEST("The request could not be successfully parsed. This is a long lifetime error."),
    UNSUPP_OPCODE("Unsupported Opcode.  This is a long lifetime error."),
    UNSUPP_OPTION("Unsupported option.  This error only occurs if the option is in the mandatory-to-process range.  This is a long"
            + " lifetime error."),
    MALFORMED_OPTION("Malformed option (e.g., appears too many times, invalid length).  This is a long lifetime error."),
    NETWORK_FAILURE("The PCP server or the device it controls is experiencing a network failure of some sort (e.g., has not yet obtained"
            + " an external IP address).  This is a short lifetime error."),
    NO_RESOURCES("Request is well-formed and valid, but the server has insufficient resources to complete the requested operation at this"
            + " time.  For example, the NAT device cannot create more mappings at this time, is short of CPU cycles or memory, or is unable"
            + " to handle the request due to some other temporary condition.  The same request may succeed in the future.  This is a"
            + " system-wide error, different from USER_EX_QUOTA.  This can be used as a catch-all error, should no other error message be"
            + " suitable.  This is a short lifetime error."),
    UNSUPP_PROTOCOL("Unsupported transport protocol, e.g., SCTP in a NAT that handles only UDP and TCP.  This is a long lifetime error."),
    USER_EX_QUOTA("This attempt to create a new mapping would exceed this subscriber's port quota.  This is a short lifetime error."),
    CANNOT_PROVIDE_EXTERNAL("The suggested external port and/or external address cannot be provided.  This error MUST only be returned"
            + " for:\n"
            + " *  MAP requests that included the PREFER_FAILURE option\n"
            + "    (normal MAP requests will return an available external port)\n"
            + " *  MAP requests for the SCTP protocol (PREFER_FAILURE is implied)\n"
            + " *  PEER requests"),
    ADDRESS_MISMATCH("The source IP address of the request packet does not match the contents of the PCP Client's IP Address field, due"
            + " to an unexpected NAT on the path between the PCP client and the PCP-controlled NAT or firewall.  This is a long lifetime"
            + " error."),
    EXCESSIVE_REMOTE_PEERS("The PCP server was not able to create the filters in this request.  This result code MUST only be returned"
            + " if the MAP request contained the FILTER option.  See Section 13.3 for details of the FILTER Option.  This is a long"
            + " lifetime error.");
    
    private final String message;

    PcpResultCode(String message) {
        Validate.notNull(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
    
}

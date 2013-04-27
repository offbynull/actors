package com.offbynull.peernetic.chord.messages.util;

import com.offbynull.peernetic.chord.messages.StatusResponse;
import com.offbynull.peernetic.chord.messages.shared.NodeAddress;
import com.offbynull.peernetic.chord.messages.shared.NodeId;
import com.offbynull.peernetic.chord.messages.shared.NodePointer;
import com.offbynull.peernetic.p2ptools.identification.Address;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedId;
import com.offbynull.peernetic.p2ptools.identification.BitLimitedPointer;
import com.offbynull.peernetic.p2ptools.overlay.chord.FingerTable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public final class MessageUtils {
    private MessageUtils() {
        
    }

    public static BitLimitedPointer convertTo(NodePointer src) {
        return convertTo(src, false);
    }
    
    public static BitLimitedPointer convertTo(NodePointer src, boolean validate) {
        if (validate) {
            validate(src);
        }
        
        BitLimitedId id = convertTo(src.getId(), false);
        Address address = convertTo(src.getAddress(), false);
        
        return new BitLimitedPointer(id, address);
    }

    public static NodePointer createFrom(BitLimitedPointer pointer) {
        return createFrom(pointer, false);
    }
    
    public static NodePointer createFrom(BitLimitedPointer pointer, boolean validate) {
        NodePointer ret = new NodePointer();
        ret.setAddress(createFrom(pointer.getAddress(), false));
        ret.setId(createFrom(pointer.getId(), false));
        
        if (validate) {
            validate(ret);
        }
        
        return ret;
    }
    
    public static BitLimitedId convertTo(NodeId src) {
        return convertTo(src, false);
    }
    
    public static BitLimitedId convertTo(NodeId src, boolean validate) {
        if (validate) {
            validate(src);
        }
        
        return new BitLimitedId(src.getBitCount(), src.getData());
    }
    
    public static NodeId createFrom(BitLimitedId src) {
        return createFrom(src, false);
    }
    
    public static NodeId createFrom(BitLimitedId src, boolean validate) {
        NodeId ret = new NodeId();
        ret.setBitCount(src.getBitCount());
        ret.setData(src.asByteArray());
        
        if (validate) {
            validate(ret);
        }
        
        return ret;
    }

    public static NodeAddress createFrom(Address src) {
        return createFrom(src, false);
    }
    
    public static NodeAddress createFrom(Address src, boolean validate) {
        NodeAddress ret = new NodeAddress();
        ret.setIp(src.getIp());
        ret.setPort(src.getPort());
        
        if (validate) {
            validate(ret);
        }
        
        return ret;
    }

    public static Address convertTo(NodeAddress src) {
        return convertTo(src, false);
    }
    
    public static Address convertTo(NodeAddress src, boolean validate) {
        if (validate) {
            validate(src);
        }
        
        return new Address(src.getIp(), src.getPort());
    }

    public static StatusResponse createFrom(BitLimitedId id, BitLimitedPointer predecessor,
            FingerTable fingers) {
        return createFrom(id, predecessor, fingers, false);
    }
    
    public static StatusResponse createFrom(BitLimitedId id, BitLimitedPointer predecessor,
            FingerTable fingers, boolean validate) {
        return createFrom(id, predecessor, fingers.dump(), validate);
    }

    public static StatusResponse createFrom(BitLimitedId id, BitLimitedPointer predecessor,
            List<BitLimitedPointer> pointers) {
        return createFrom(id, predecessor, pointers, false);
    }
    
    public static StatusResponse createFrom(BitLimitedId id, BitLimitedPointer predecessor,
            List<BitLimitedPointer> pointers, boolean validate) {
        StatusResponse ret = new StatusResponse();
        ret.setId(createFrom(id, false));
        Set<NodePointer> nodePointers = new HashSet<>();
        for (BitLimitedPointer pointer : pointers) {
            NodePointer nodePointer = createFrom(pointer, false);
            nodePointers.add(nodePointer);
        }
        ret.setPointers(nodePointers);
        ret.setPredecessor(predecessor == null ? null
                : createFrom(predecessor, false));
        
        if (validate) {
            validate(ret);
        }
        
        return ret;
    }
    
    public static NodeId buildNodeId(int bitCount, byte[] data) {
        return buildNodeId(bitCount, data, false);
    }
    
    public static NodeId buildNodeId(int bitCount, byte[] data,
            boolean validate) {
        NodeId ret = new NodeId();
        
        ret.setBitCount(bitCount);
        ret.setData(data);
        
        if (validate) {
            validate(ret);
        }

        return ret;
    }

    public static NodeAddress buildNodeAddress(byte[] ip, int port) {
        return buildNodeAddress(ip, port, false);
    }
    
    public static NodeAddress buildNodeAddress(byte[] ip, int port,
            boolean validate) {
        NodeAddress ret = new NodeAddress();
        
        ret.setPort(port);
        ret.setIp(ip);
        
        if (validate) {
            validate(ret);
        }

        return ret;
    }
    
    public static NodePointer buildNodePointer(byte[] ip, int port,
            int bitCount, byte[] data) {
        return buildNodePointer(ip, port, bitCount, data, false);
    }
    
    public static NodePointer buildNodePointer(byte[] ip, int port,
            int bitCount, byte[] data, boolean validate) {
        NodeId id = new NodeId();
        id.setBitCount(bitCount);
        id.setData(data);
        
        NodeAddress address = new NodeAddress();
        address.setPort(port);
        address.setIp(ip);
        
        return buildNodePointer(address, id, validate);
    }

    public static NodePointer buildNodePointer(NodeAddress nodeAddress,
            NodeId nodeId) {
        return buildNodePointer(nodeAddress, nodeId, false);
    }
    
    public static NodePointer buildNodePointer(NodeAddress nodeAddress,
            NodeId nodeId, boolean validate) {
        NodePointer ret = new NodePointer();
        
        ret.setAddress(nodeAddress);
        ret.setId(nodeId);
        
        if (validate) {
            validate(ret);
        }

        return ret;
    }
    
    private static void validate(Object obj) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<? extends ConstraintViolation<?>> errors = validator.validate(obj);

        if (!errors.isEmpty()) {
            throw new ConstraintViolationException(errors);
        }
    }
}

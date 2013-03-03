package com.offbynull.chord.util;

import com.offbynull.chord.Address;
import com.offbynull.chord.FingerTable;
import com.offbynull.chord.Id;
import com.offbynull.chord.Pointer;
import com.offbynull.chord.messages.StatusResponse;
import com.offbynull.chord.messages.shared.NodeAddress;
import com.offbynull.chord.messages.shared.NodeId;
import com.offbynull.chord.messages.shared.NodePointer;
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
    
    public static Pointer convertTo(NodePointer src, boolean validate) {
        if (validate) {
            validate(src);
        }
        
        Id id = convertTo(src.getId(), false);
        Address address = convertTo(src.getAddress(), false);
        
        return new Pointer(id, address);
    }

    public static NodePointer createFrom(Pointer pointer, boolean validate) {
        NodePointer ret = new NodePointer();
        ret.setAddress(createFrom(pointer.getAddress(), false));
        ret.setId(createFrom(pointer.getId(), false));
        
        if (validate) {
            validate(ret);
        }
        
        return ret;
    }
    
    public static Id convertTo(NodeId src, boolean validate) {
        if (validate) {
            validate(src);
        }
        
        return new Id(src.getBitCount(), src.getData());
    }
    
    public static NodeId createFrom(Id src, boolean validate) {
        NodeId ret = new NodeId();
        ret.setBitCount(src.getBitCount());
        ret.setData(src.asByteArray());
        
        if (validate) {
            validate(ret);
        }
        
        return ret;
    }

    public static NodeAddress createFrom(Address src, boolean validate) {
        NodeAddress ret = new NodeAddress();
        ret.setHost(src.getHost());
        ret.setPort(src.getPort());
        
        if (validate) {
            validate(ret);
        }
        
        return ret;
    }

    public static Address convertTo(NodeAddress src, boolean validate) {
        if (validate) {
            validate(src);
        }
        
        return new Address(src.getHost(), src.getPort());
    }

    public static StatusResponse createFrom(Id id, FingerTable fingers,
            boolean validate) {
        return createFrom(id, fingers.dump(), validate);
    }

    public static StatusResponse createFrom(Id id, List<Pointer> pointers,
            boolean validate) {
        StatusResponse ret = new StatusResponse();
        ret.setId(createFrom(id, false));
        Set<NodePointer> nodePointers = new HashSet<>();
        for (Pointer pointer : pointers) {
            NodePointer nodePointer = createFrom(pointer, false);
            nodePointers.add(nodePointer);
        }
        ret.setPointers(nodePointers);
        
        if (validate) {
            validate(ret);
        }
        
        return ret;
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

    public static NodeAddress buildNodeAddress(String host, int port,
            boolean validate) {
        NodeAddress ret = new NodeAddress();
        
        ret.setPort(port);
        ret.setHost(host);
        
        if (validate) {
            validate(ret);
        }

        return ret;
    }
    
    public static NodePointer buildNodePointer(String host, int port,
            int bitCount, byte[] data, boolean validate) {
        NodeId id = new NodeId();
        id.setBitCount(bitCount);
        id.setData(data);
        
        NodeAddress address = new NodeAddress();
        address.setPort(port);
        address.setHost(host);
        
        return buildNodePointer(address, id, validate);
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

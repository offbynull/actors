package com.offbynull.peernetic.router.upnpigd;

import com.offbynull.peernetic.router.PortType;
import com.offbynull.peernetic.router.common.ResponseException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.DOMException;

public final class UpnpIgdController implements Closeable {

    private InetAddress selfAddress;
    private UpnpIgdService service;

    public UpnpIgdController(InetAddress selfAddress , UpnpIgdService service) {
        Validate.notNull(selfAddress);
        Validate.notNull(service);
        this.selfAddress = selfAddress;
        this.service = service;
    }

    public PortMappingInfo getMappingDetails(int externalPort, PortType portType) {
        Validate.inclusiveBetween(0, 65535, externalPort); // 0 = wildcard, any unassigned port? may not be supported according to docs
        Validate.notNull(portType);
        
        Range<Long> externalPortRange = service.getExternalPortRange();
        if (externalPortRange != null) {
            Validate.inclusiveBetween(externalPortRange.getMinimum(), externalPortRange.getMaximum(), (long) externalPort);
        }
        
        Map<String, String> respParams = performRequest("GetSpecificPortMappingEntry",
                ImmutablePair.of("NewRemoteHost", ""),
                ImmutablePair.of("NewExternalPort", "" + externalPort),
                ImmutablePair.of("NewProtocol", portType.name()));
        
        try {
            int internalPort = NumberUtils.createInteger(respParams.get("NewInternalPort"));
            InetAddress internalClient = InetAddress.getByName(respParams.get("NewInternalClient"));
            long remainingDuration = NumberUtils.createInteger(respParams.get("NewLeaseDuration"));
            
            return new PortMappingInfo(internalPort, internalClient, remainingDuration);
        } catch (UnknownHostException | NumberFormatException | NullPointerException e) {
            throw new ResponseException(e);
        }
    }
    
    public static final class PortMappingInfo {
        private int internalPort;
        private InetAddress internalClient;
        private long remainingDuration;

        private PortMappingInfo(int internalPort, InetAddress internalClient, long remainingDuration) {
            this.internalPort = internalPort;
            this.internalClient = internalClient;
            this.remainingDuration = remainingDuration;
        }

        public int getInternalPort() {
            return internalPort;
        }

        public InetAddress getInternalClient() {
            return internalClient;
        }

        public long getRemainingDuration() {
            return remainingDuration;
        }

        @Override
        public String toString() {
            return "PortMappingInfo{" + "internalPort=" + internalPort + ", internalClient=" + internalClient + ", remainingDuration="
                    + remainingDuration + '}';
        }
        
    }
    
    public PortMappingInfo addPortMapping(int externalPort, int internalPort, PortType portType, long duration) {
        Validate.inclusiveBetween(0, 65535, externalPort); // 0 = wildcard, any unassigned port? may not be supported according to docs
        Validate.inclusiveBetween(1, 65535, internalPort);
        Validate.notNull(portType);
        Validate.inclusiveBetween(0L, Long.MAX_VALUE, duration);
        
        Range<Long> externalPortRange = service.getExternalPortRange();
        if (externalPortRange != null) {
            Validate.inclusiveBetween(externalPortRange.getMinimum(), externalPortRange.getMaximum(), (long) externalPort);
        }

        Range<Long> leaseDurationRange = service.getLeaseDurationRange();
        if (leaseDurationRange != null) {
            if (duration == 0L) {
                Validate.isTrue(leaseDurationRange.getMinimum() == 0, "Infinite duration not allowed");
            }
            duration = Math.max(leaseDurationRange.getMinimum(), duration);
            duration = Math.min(leaseDurationRange.getMaximum(), duration);
        }
        
        performRequest("AddPortMapping",
                ImmutablePair.of("NewRemoteHost", ""),
                ImmutablePair.of("NewExternalPort", "" + externalPort),
                ImmutablePair.of("NewProtocol", portType.name()),
                ImmutablePair.of("NewInternalPort", "" + internalPort),
                ImmutablePair.of("NewInternalClient", selfAddress.getHostAddress()),
                ImmutablePair.of("NewEnabled", "1"),
                ImmutablePair.of("NewPortMappingDescription", ""),
                ImmutablePair.of("NewLeaseDuration", "" + duration));
        
        
        return getMappingDetails(externalPort, portType);
    }

    public void deletePortMapping(int externalPort, PortType portType) {
        Validate.inclusiveBetween(1, 65535, externalPort);
        
        performRequest("DeletePortMapping",
                ImmutablePair.of("NewRemoteHost", ""),
                ImmutablePair.of("NewExternalPort", "" + externalPort),
                ImmutablePair.of("NewProtocol", portType.name()));
    }

    public InetAddress getExternalIp() {
        Map<String, String> responseParams = performRequest("GetExternalIPAddress");
        
        try {
            return InetAddress.getByName(responseParams.get("NewExternalIPAddress"));
        } catch (UnknownHostException | NullPointerException e) {
            throw new ResponseException(e);
        }
    }
    
    private Map<String, String> performRequest(String action, ImmutablePair<String, String> ... params) {
        byte[] outgoingData = createRequestXml(action, params);
        
        HttpURLConnection conn = null;
                
        try {
            URL url = service.getService().getControlUrl().toURL();
            conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

            conn.setRequestMethod("POST");
            conn.setReadTimeout(3000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setRequestProperty("SOAPAction", service.getService().getServiceType() + "#" + action);
            conn.setRequestProperty("Connection", "Close");
        } catch (IOException ex) {
            throw new IllegalStateException(ex); // should never happen
        }
        
        
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(outgoingData);
        } catch (IOException ex) {
            IOUtils.close(conn);
            throw new ResponseException(ex);
        }

        byte[] incomingData;
        int respCode;
        try {
            respCode = conn.getResponseCode();
        } catch (IOException ioe) {
            IOUtils.close(conn);
            throw new ResponseException(ioe);
        }
        
        if (respCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            try (InputStream is = conn.getErrorStream()) {
                incomingData = IOUtils.toByteArray(is);
            } catch (IOException ex) {
                IOUtils.close(conn);
                throw new ResponseException(ex);
            }
        } else {
            try (InputStream is = conn.getInputStream()) {
                incomingData = IOUtils.toByteArray(is);
            } catch (IOException ex) {
                IOUtils.close(conn);
                throw new ResponseException(ex);
            }
        }
        
        return parseResponseXml(action + "Response", incomingData);
    }

    private Map<String, String> parseResponseXml(String expectedTagName, byte[] data) {
        try {
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapMessage = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(data));
            
            if (soapMessage.getSOAPBody().hasFault()) {
                StringWriter writer = new StringWriter();
                try {
                    Transformer transformer = TransformerFactory.newInstance().newTransformer();
                    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    transformer.transform(new DOMSource(soapMessage.getSOAPPart()), new StreamResult(writer));
                } catch (IllegalArgumentException | TransformerException | TransformerFactoryConfigurationError e) {
                    writer.append("Failed to dump fault: " + e);
                }
                
                throw new ResponseException(writer.toString());
            }
            
            Iterator<SOAPBodyElement> responseBlockIt = soapMessage.getSOAPBody().getChildElements(
                    new QName(service.getService().getServiceType(), expectedTagName));
            if (!responseBlockIt.hasNext()) {
                throw new ResponseException(expectedTagName + " tag missing");
            }
            
            Map<String, String> ret = new HashMap<>();
            
            SOAPBodyElement responseNode = responseBlockIt.next();
            Iterator<SOAPBodyElement> responseChildrenIt = responseNode.getChildElements();
            while (responseChildrenIt.hasNext()) {
                SOAPBodyElement param = responseChildrenIt.next();
                String name = StringUtils.trim(param.getLocalName().trim());
                String value = StringUtils.trim(param.getValue().trim());
                
                ret.put(name, value);
            }
            
            return ret;
        } catch (IllegalArgumentException | IOException | SOAPException | DOMException e) {
            throw new IllegalStateException(e); // should never happen
        }
    }
    
    private byte[] createRequestXml(String action, ImmutablePair<String, String> ... params) {
        try {
            MessageFactory factory = MessageFactory.newInstance();
            SOAPMessage soapMessage = factory.createMessage();
            
            SOAPBodyElement actionElement = soapMessage.getSOAPBody().addBodyElement(new QName(null, action, "m"));
            actionElement.addNamespaceDeclaration("m", service.getService().getServiceType());
            
            for (Pair<String, String> param : params) {
                SOAPElement paramElement = actionElement.addChildElement(QName.valueOf(param.getKey()));
                paramElement.setValue(param.getValue());
            }

            soapMessage.getSOAPPart().setXmlStandalone(true);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.transform(new DOMSource(soapMessage.getSOAPPart()), new StreamResult(baos));

            return baos.toByteArray();
        } catch (IllegalArgumentException | SOAPException | TransformerException | DOMException e) {
            throw new IllegalStateException(e); // should never happen
        }
    }

    @Override
    public void close() throws IOException {
    }
}

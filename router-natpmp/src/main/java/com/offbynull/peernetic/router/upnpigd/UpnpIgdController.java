package com.offbynull.peernetic.router.upnpigd;

import com.offbynull.peernetic.router.common.NoResponseException;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.DOMException;

public final class UpnpIgdController implements Closeable {

    private UpnpIgdService service;

    public UpnpIgdController(UpnpIgdService service) {
        Validate.notNull(service);
        this.service = service;
    }

    public void addPortMapping() {

    }

    public void deletePortMapping() {

    }

    public InetAddress getExternalIp() {
        byte[] outputData = createRequestXml("GetExternalIPAddress");
        
        HttpURLConnection conn = null;
                
        try {
            URL url = service.getService().getControlUrl().toURL();
            conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);

            conn.setRequestMethod("POST");
            conn.setReadTimeout(3000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "text/xml");
            conn.setRequestProperty("SOAPAction", service.getService().getServiceType() + "#GetExternalIPAddress");
            conn.setRequestProperty("Connection", "Close");
        } catch (IOException ex) {
            throw new IllegalStateException(ex); // should never happen
        }
        
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(outputData);
        } catch (IOException ex) {
            IOUtils.close(conn);
            throw new NoResponseException(ex);
        }

        byte[] incomingData;
        try (InputStream is = conn.getInputStream()) {
            incomingData = IOUtils.toByteArray(is);
        } catch (IOException ex) {
            IOUtils.close(conn);
            throw new NoResponseException(ex);
        }
        
        System.out.println(new String(incomingData));
        
        return null;
    }

    public byte[] createRequestXml(String action, Pair<String, String> ... params) {
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

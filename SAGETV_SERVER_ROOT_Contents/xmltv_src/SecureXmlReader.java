package xmltv;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DeclHandler;

/** Creates a SAX reader that cannot resolve external XML resources. */
final class SecureXmlReader {
    private SecureXmlReader() {
    }

    static XMLReader create(ContentHandler contentHandler, ErrorHandler errorHandler)
            throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setXIncludeAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        // XMLTV generators commonly emit <!DOCTYPE tv SYSTEM "xmltv.dtd">.
        // Permit the declaration but never load its external subset or expand
        // external entities.
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        reader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
        reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        reader.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        reader.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        reader.setProperty("http://xml.org/sax/properties/declaration-handler",
                new DeclHandler() {
                    public void elementDecl(String name, String model) {
                    }

                    public void attributeDecl(String elementName, String attributeName,
                            String type, String mode, String value) {
                    }

                    public void internalEntityDecl(String name, String value)
                            throws SAXException {
                        throw new SAXException("Inline XML entities are prohibited");
                    }

                    public void externalEntityDecl(String name, String publicId,
                            String systemId) throws SAXException {
                        throw new SAXException("External XML entities are prohibited");
                    }
                });
        reader.setEntityResolver(new EntityResolver() {
            public InputSource resolveEntity(String publicId, String systemId)
                    throws SAXException {
                throw new SAXException("External XML entities are prohibited");
            }
        });
        reader.setContentHandler(contentHandler);
        reader.setErrorHandler(errorHandler);
        return reader;
    }
}

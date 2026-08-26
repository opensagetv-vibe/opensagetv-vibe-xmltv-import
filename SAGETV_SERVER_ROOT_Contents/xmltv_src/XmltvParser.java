package xmltv;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/** Owns secure validation and parsing of an acquired XMLTV file. */
final class XmltvParser {
    private XmltvParser() {
    }

    static void parse(File file, boolean filterInvalidCharacters, boolean validateFirst,
            ContentHandler contentHandler, ErrorHandler errorHandler) throws Exception {
        if (validateFirst) validate(file, filterInvalidCharacters);
        XMLReader reader = SecureXmlReader.create(contentHandler, errorHandler);
        parseWith(reader, file, filterInvalidCharacters);
    }

    static void validate(File file, boolean filterInvalidCharacters) throws Exception {
        DefaultHandler validator = new DefaultHandler() {
            public void error(SAXParseException exception) throws SAXException {
                throw exception;
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        };
        XMLReader reader = SecureXmlReader.create(validator, validator);
        parseWith(reader, file, filterInvalidCharacters);
    }

    private static void parseWith(XMLReader reader, File file, boolean filterInvalidCharacters)
            throws Exception {
        try (InputStream fileInput = new FileInputStream(file);
             InputStream parseInput = filterInvalidCharacters
                     ? new XMLInputStreamFilter(fileInput) : fileInput) {
            reader.parse(new InputSource(parseInput));
        }
    }
}

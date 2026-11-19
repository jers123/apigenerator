package org.jers.apigenerator.infrastructure.version;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee la lista de versiones de un archivo {@code maven-metadata.xml}.
 *
 * <p>El XML viene de Internet, así que se bloquean DOCTYPE y entidades
 * externas para evitar ataques XXE.</p>
 */
final class MavenMetadataParser {

    /** Constructor privado: clase de utilidades sin estado. */
    private MavenMetadataParser() {
    }

    /**
     * Extrae todas las versiones listadas en {@code <versioning><versions>}.
     *
     * @param xml contenido del archivo
     * @return versiones en el orden del archivo, sin filtrar
     * @throws IllegalArgumentException si el contenido no es un XML válido
     *                                  o declara un DOCTYPE
     */
    static List<String> versions(final byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));

            NodeList versionsNodes = document.getElementsByTagName("versions");
            if (versionsNodes.getLength() == 0) {
                return List.of();
            }
            NodeList nodes = ((Element) versionsNodes.item(0)).getElementsByTagName("version");
            List<String> versions = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                versions.add(nodes.item(i).getTextContent().strip());
            }
            return versions;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IllegalArgumentException("maven-metadata.xml inválido: " + e.getMessage(), e);
        }
    }
}
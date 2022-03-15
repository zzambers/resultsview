package resultsview.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.HashMap;

public class SAXTreeHandlerNode {

    private final String name;
    private final HashMap<String, SAXTreeHandlerNode> children = new HashMap();

    public SAXTreeHandlerNode(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public void addChild(SAXTreeHandlerNode node) {
        children.put(node.getName(), node);
    }
    
    public void addChild(String name, SAXTreeHandlerNode node) {
        children.put(name, node);
    }

    public SAXTreeHandlerNode getChild(String name) {
        return children.get(name);
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
    }

    public void characters(char ch[], int start, int length) throws SAXException {
    }

}

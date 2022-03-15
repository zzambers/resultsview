package resultsview.xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import java.util.ArrayList;

public class SAXTreeHandler extends DefaultHandler {

    private SAXTreeHandlerNode rootNode;
    private final ArrayList<StackElement> stack = new ArrayList<StackElement>();

    public void setRootNode(SAXTreeHandlerNode rootNode) {
        this.rootNode = rootNode;
        StackElement rootElement = new StackElement(null, rootNode);
        stack.add(rootElement);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        StackElement prevElement = stack.get(stack.size() - 1);
        SAXTreeHandlerNode prevNode = prevElement.handlerNode;

        SAXTreeHandlerNode curNode = null;
        if (prevNode != null) {
            curNode = prevNode.getChild(qName);
        }

        StackElement curElement = new StackElement(qName, curNode);
        stack.add(curElement);

        if (curNode != null) {
            curNode.startElement(uri, localName, qName, attributes);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        StackElement curElement = stack.get(stack.size() - 1);
        SAXTreeHandlerNode curNode = curElement.handlerNode;

        if (curNode != null) {
            curNode.endElement(uri, localName, qName);
        }

        stack.remove(stack.size() - 1);
    }

    @Override
    public void characters(char ch[], int start, int length) throws SAXException {
        StackElement curElement = stack.get(stack.size() - 1);
        SAXTreeHandlerNode curNode = curElement.handlerNode;

        if (curNode != null) {
            curNode.characters(ch, start, length);
        }
    }

    private static class StackElement {

        String name;
        SAXTreeHandlerNode handlerNode;

        public StackElement(String name, SAXTreeHandlerNode node) {
            this.name = name;
            this.handlerNode = node;
        }
    }

}

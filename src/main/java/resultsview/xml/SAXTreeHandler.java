/*
 * The MIT License
 *
 * Copyright 2022 zzambers.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

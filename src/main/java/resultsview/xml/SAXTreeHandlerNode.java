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

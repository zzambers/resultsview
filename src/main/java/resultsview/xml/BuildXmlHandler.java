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

public class BuildXmlHandler extends SAXTreeHandler {

    private String pkgName;
    private String pkgVersion;
    private String pkgRelease;

    private String result;
    private String startTime;
    private String duration;
    private String builtOn;
    private String timestamp;

    public BuildXmlHandler() {
        init();
    }

    private void init() {
        SAXTreeHandlerNode rootNode = new SAXTreeHandlerNode(null);
        SAXTreeHandlerNode buildNode = new SAXTreeHandlerNode("build");
        SAXTreeHandlerNode matrixBuildNode = new SAXTreeHandlerNode("matrix-build");
        SAXTreeHandlerNode actionsNode = new SAXTreeHandlerNode("actions");
        SAXTreeHandlerNode kojiNode = new SAXTreeHandlerNode("hudson.plugins.scm.koji.KojiRevisionState");
        SAXTreeHandlerNode buildNode2 = new SAXTreeHandlerNode("build");
        SAXTreeHandlerNode nameNode = new ElementContentNode("name") {

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                pkgName = getContent();
            }

        };
        SAXTreeHandlerNode versionNode = new ElementContentNode("version") {

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                pkgVersion = getContent();
            }
        };
        SAXTreeHandlerNode releaseNode = new ElementContentNode("release") {

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                pkgRelease = getContent();
            }
        };

        SAXTreeHandlerNode resultNode = new ElementContentNode("result") {

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                result = getContent();
            }
        };

        SAXTreeHandlerNode startTimeNode = new ElementContentNode("startTime") {

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                startTime = getContent();
            }
        };

        SAXTreeHandlerNode durationNode = new ElementContentNode("duration") {

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                duration = getContent();
            }

        };

        SAXTreeHandlerNode buildOnNode = new ElementContentNode("builtOn") {

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                builtOn = getContent();
            }

        };

        SAXTreeHandlerNode timestampNode = new ElementContentNode("timestamp") {

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                timestamp = getContent();
            }

        };
        rootNode.addChild(buildNode.getName(), buildNode);
        rootNode.addChild(matrixBuildNode.getName(), matrixBuildNode);
        buildNode.addChild(actionsNode.getName(), actionsNode);
        actionsNode.addChild(kojiNode.getName(), kojiNode);
        kojiNode.addChild(buildNode2.getName(), buildNode2);
        buildNode2.addChild(nameNode.getName(), nameNode);
        buildNode2.addChild(versionNode.getName(), versionNode);
        buildNode2.addChild(releaseNode.getName(), releaseNode);

        buildNode.addChild(resultNode);
        buildNode.addChild(timestampNode);
        buildNode.addChild(startTimeNode);
        buildNode.addChild(durationNode);
        buildNode.addChild(buildOnNode);

        matrixBuildNode.addChild(resultNode);
        matrixBuildNode.addChild(timestampNode);
        matrixBuildNode.addChild(startTimeNode);
        matrixBuildNode.addChild(durationNode);
        matrixBuildNode.addChild(buildOnNode);

        setRootNode(rootNode);
    }

    public String getPkgName() {
        return pkgName;
    }

    public String getPkgVersion() {
        return pkgVersion;
    }

    public String getPkgRelease() {
        return pkgRelease;
    }

    public String getResult() {
        return result;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getDuration() {
        return duration;
    }

    public String getBuiltOn() {
        return builtOn;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public static class ElementContentNode extends SAXTreeHandlerNode {

        private StringBuilder builder;

        public ElementContentNode(String name) {
            super(name);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            builder = new StringBuilder();
        }

        @Override
        public void characters(char ch[], int start, int length) {
            builder.append(ch, start, length);
        }

        public String getContent() {
            return builder.toString().trim();
        }

    }

}

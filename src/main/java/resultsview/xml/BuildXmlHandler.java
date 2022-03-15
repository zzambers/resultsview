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

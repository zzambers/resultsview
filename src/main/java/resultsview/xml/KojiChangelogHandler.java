package resultsview.xml;

public class KojiChangelogHandler extends SAXTreeHandler {

    private StringBuilder pkgName;
    private StringBuilder pkgVersion;
    private StringBuilder pkgRelease;

    public KojiChangelogHandler() {
        init();
    }

    private void init() {
        SAXTreeHandlerNode rootNode = new SAXTreeHandlerNode(null);
        SAXTreeHandlerNode buildNode = new SAXTreeHandlerNode("build");
        SAXTreeHandlerNode nameNode = new SAXTreeHandlerNode("name") {
            @Override
            public void characters(char ch[], int start, int length) {
                pkgName.append(ch, start, length);
            }
        };
        SAXTreeHandlerNode versionNode = new SAXTreeHandlerNode("version") {
            @Override
            public void characters(char ch[], int start, int length) {
                pkgVersion.append(ch, start, length);
            }
        };
        SAXTreeHandlerNode releaseNode = new SAXTreeHandlerNode("release") {
            @Override
            public void characters(char ch[], int start, int length) {
                pkgRelease.append(ch, start, length);
            }
        };
        rootNode.addChild(buildNode.getName(), buildNode);
        buildNode.addChild(nameNode.getName(), nameNode);
        buildNode.addChild(versionNode.getName(), versionNode);
        buildNode.addChild(releaseNode.getName(), releaseNode);
        setRootNode(rootNode);

        pkgName = new StringBuilder();
        pkgVersion = new StringBuilder();
        pkgRelease = new StringBuilder();
    }

    public String getPkgName() {
        return pkgName.toString();
    }

    public String getPkgVersion() {
        return pkgVersion.toString();
    }

    public String getPkgRelease() {
        return pkgRelease.toString();
    }

}

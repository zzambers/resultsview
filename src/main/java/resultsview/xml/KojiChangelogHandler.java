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

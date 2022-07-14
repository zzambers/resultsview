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

public class JobConfigHandler extends SAXTreeHandler {

    private boolean usesKojiPlugin = false;
    private StringBuilder kojiTopUrl;
    private StringBuilder kojiDownloadUrl;
    private StringBuilder kojiPkgName;
    private StringBuilder kojiPkgArch;
    private StringBuilder kojiPkgTag;
    private StringBuilder kojiExcludeNvr;
    private StringBuilder kojiWhitelistNvr;

    public JobConfigHandler() {
        init();
    }

    private void init() {
        SAXTreeHandlerNode rootNode = new SAXTreeHandlerNode(null);
        SAXTreeHandlerNode projectNode = new SAXTreeHandlerNode("project");
        SAXTreeHandlerNode scmNode = new SAXTreeHandlerNode("scm") {

            boolean kojiscm = false;
            boolean dummy = initSubnodes();

            public boolean initSubnodes() {
                SAXTreeHandlerNode topUrlNode = new SAXTreeHandlerNode("kojiTopUrl") {
                    @Override
                    public void characters(char ch[], int start, int length) {
                        if (kojiscm) {
                            kojiTopUrl.append(ch, start, length);

                        }
                    }
                };
                SAXTreeHandlerNode downloadUrlNode = new SAXTreeHandlerNode("kojiDownloadUrl") {
                    @Override
                    public void characters(char ch[], int start, int length) {
                        if (kojiscm) {
                            kojiDownloadUrl.append(ch, start, length);

                        }
                    }
                };
                SAXTreeHandlerNode pkgNameNode = new SAXTreeHandlerNode("packageName") {
                    @Override
                    public void characters(char ch[], int start, int length) {
                        if (kojiscm) {
                            kojiPkgName.append(ch, start, length);

                        }
                    }
                };
                SAXTreeHandlerNode archNode = new SAXTreeHandlerNode("arch") {
                    @Override
                    public void characters(char ch[], int start, int length) {
                        if (kojiscm) {
                            kojiPkgArch.append(ch, start, length);
                        }
                    }
                };
                SAXTreeHandlerNode tagNode = new SAXTreeHandlerNode("tag") {
                    @Override
                    public void characters(char ch[], int start, int length) {
                        if (kojiscm) {
                            kojiPkgTag.append(ch, start, length);
                        }
                    }
                };
                SAXTreeHandlerNode excludeNvrNode = new SAXTreeHandlerNode("excludeNvr") {
                    @Override
                    public void characters(char ch[], int start, int length) {
                        if (kojiscm) {
                            kojiExcludeNvr.append(ch, start, length);
                        }
                    }
                };
                SAXTreeHandlerNode whitelistNvrNode = new SAXTreeHandlerNode("whitelistNvr") {
                    @Override
                    public void characters(char ch[], int start, int length) {
                        if (kojiscm) {
                            kojiWhitelistNvr.append(ch, start, length);
                        }
                    }
                };
                addChild(topUrlNode.getName(), topUrlNode);
                addChild(downloadUrlNode.getName(), downloadUrlNode);
                addChild(pkgNameNode.getName(), pkgNameNode);
                addChild(archNode.getName(), archNode);
                addChild(tagNode.getName(), tagNode);
                addChild(excludeNvrNode.getName(), excludeNvrNode);
                addChild(whitelistNvrNode.getName(), whitelistNvrNode);
                return true;
            }

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                kojiscm = attributes.getValue("class").equals("hudson.plugins.scm.koji.KojiSCM");
                if (kojiscm) {
                    usesKojiPlugin = true;
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                kojiscm = false;
            }
        };
        rootNode.addChild(projectNode.getName(), projectNode);
        projectNode.addChild(scmNode.getName(), scmNode);
        setRootNode(rootNode);

        kojiTopUrl = new StringBuilder();
        kojiDownloadUrl = new StringBuilder();
        kojiPkgName = new StringBuilder();
        kojiPkgArch = new StringBuilder();
        kojiPkgTag = new StringBuilder();
        kojiExcludeNvr = new StringBuilder();
        kojiWhitelistNvr = new StringBuilder();
    }

    public boolean usesKojiPlugin() {
        return usesKojiPlugin;
    }

    public String getKojiTopUrl() {
        return kojiTopUrl.toString();
    }

    public String getKojiDownloadUrl() {
        return kojiDownloadUrl.toString();
    }

    public String getKojiPkgName() {
        return kojiPkgName.toString();
    }

    public String getKojiPkgArch() {
        return kojiPkgArch.toString();
    }

    public String getKojiPkgTag() {
        return kojiPkgTag.toString();
    }

    public String getKojiExcludeNvr() {
        return kojiExcludeNvr.toString();
    }

    public String getKojiWhitelistNvr() {
        return kojiWhitelistNvr.toString();
    }
}

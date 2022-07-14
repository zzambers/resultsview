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

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TckResultsHandler extends SAXTreeHandler {

    private final List<TestResult> results = new ArrayList();

    public TckResultsHandler() {
        init();
    }

    private void init() {
        SAXTreeHandlerNode rootNode = new SAXTreeHandlerNode(null);
        SAXTreeHandlerNode reportNode = new SAXTreeHandlerNode("Report");
        SAXTreeHandlerNode testResultsNode = new SAXTreeHandlerNode("TestResults");
        SAXTreeHandlerNode testResultNode = new SAXTreeHandlerNode("TestResult") {

            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                String url = attributes.getValue("url");
                String name = attributes.getValue("status");
                results.add(new TestResult(url, name));
            }

        };

        rootNode.addChild(reportNode.getName(), reportNode);
        reportNode.addChild(testResultsNode.getName(), testResultsNode);
        testResultsNode.addChild(testResultNode.getName(), testResultNode);

        setRootNode(rootNode);
    }

    private static class TestResult {

        private static final String PASSED = "PASSED";
        private static final String FAILED = "FAILED";

        String url;
        String status;

        public TestResult(String url, String status) {
            this.url = url;

            if (status.equals(PASSED)) {
                this.status = PASSED;
            } else if (status.equals(FAILED)) {
                this.status = FAILED;
            } else {
                this.status = status;
            }
        }
    }

    public int getTestResultsCount() {
        return results.size();
    }

    public String getResultUrl(int i) {
        return results.get(i).url;
    }

    public String getResultStatus(int i) {
        return results.get(i).status;
    }

}

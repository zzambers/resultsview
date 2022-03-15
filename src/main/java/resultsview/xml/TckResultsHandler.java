package resultsview.xml;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author zzambers
 */
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

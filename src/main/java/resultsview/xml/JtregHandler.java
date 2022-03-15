/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resultsview.xml;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 *
 * @author zzambers
 */
public class JtregHandler extends SAXTreeHandler {

    /*
    https://github.com/junit-team/junit5/blob/master/platform-tests/src/test/resources/jenkins-junit.xsd
     */
    ArrayList<TestSuite> testSuites = new ArrayList();
    TestSuite currentTestSuite = null;
    TestCase currentTestCase = null;

    public JtregHandler() {
        init();
    }

    private void init() {
        SAXTreeHandlerNode rootNode = new SAXTreeHandlerNode(null);

        SAXTreeHandlerNode testsuitesNode = new SAXTreeHandlerNode("testsuites");
        SAXTreeHandlerNode testsuiteNode = new SAXTreeHandlerNode("testsuite") {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                System.out.println("testsuite");
                currentTestSuite = new TestSuite();
                testSuites.add(currentTestSuite);
                currentTestSuite.name = attributes.getValue("name");
                currentTestSuite.tests = attributes.getValue("tests");
                currentTestSuite.failures = attributes.getValue("falures");
                currentTestSuite.errors = attributes.getValue("errors");
                currentTestSuite.disabled = attributes.getValue("disabled");
                currentTestSuite.skipped = attributes.getValue("skipped");
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                currentTestSuite = null;
            }
        };
        SAXTreeHandlerNode testcaseNode = new SAXTreeHandlerNode("testcase") {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                System.out.println("testcase");
                currentTestCase = new TestCase();
                currentTestCase.name = attributes.getValue("name");
                currentTestCase.className = attributes.getValue("classname");
                currentTestCase.numId = attributes.getValue("numid");
                currentTestSuite.testCases.add(currentTestCase);
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
                currentTestCase = null;
            }
        };
        SAXTreeHandlerNode skippedNode = new SAXTreeHandlerNode("skipped") {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                currentTestCase.skipped = true;
            }
        };

        SAXTreeHandlerNode errorNode = new SAXTreeHandlerNode("error") {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                currentTestCase.error = true;
            }
        };

        SAXTreeHandlerNode failureNode = new SAXTreeHandlerNode("failure") {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                currentTestCase.failure = true;
            }
        };

        rootNode.addChild(testsuiteNode.getName(), testsuiteNode);
        rootNode.addChild(testsuitesNode.getName(), testsuitesNode);
        testsuitesNode.addChild(testsuiteNode.getName(), testsuiteNode);
        testsuiteNode.addChild(testcaseNode.getName(), testcaseNode);
        testcaseNode.addChild(failureNode.getName(), failureNode);
        testcaseNode.addChild(errorNode.getName(), errorNode);
        testcaseNode.addChild(skippedNode.getName(), skippedNode);
        setRootNode(rootNode);
    }

    public static class TestSuite {

        String name = null;
        String tests = null;
        String failures = null;
        String errors = null;
        String disabled = null;
        String skipped = null;

        ArrayList<TestCase> testCases = new ArrayList();

        public ArrayList<TestCase> getTestCases() {
            return testCases;
        }
        
        public void addTestCase(TestCase tc) {
            testCases.add(tc);
        }
        

        public String getName() {
            return name;
        }

        public String getTests() {
            return tests;
        }

        public String getFailures() {
            return failures;
        }

        public String getErrors() {
            return errors;
        }

        public String getDisabled() {
            return disabled;
        }

        public String getSkipped() {
            return skipped;
        }

    }

    public static class TestCase {

        String numId;
        String name;
        String className;
        boolean error;
        boolean failure;
        boolean skipped;

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }

        public boolean isError() {
            return error;
        }

        public boolean isFailure() {
            return failure;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public String getNumId() {
            return numId;
        }

    }

    public List<TestSuite> getTestsuites() {
        return testSuites;
    }

}

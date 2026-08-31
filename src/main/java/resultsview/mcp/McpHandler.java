/*
 * The MIT License
 *
 * Copyright 2026 zzambers.
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
package resultsview.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import resultsview.storage.Pkg;
import resultsview.storage.Run;
import resultsview.storage.Job;
import resultsview.storage.Storage;

public class McpHandler {

    private final Storage storage;
    private final Path jobsRoot;
    private final String jenkinsUrl;

    static final ObjectMapper mapper = new ObjectMapper();

    private static final int DEFAULT_PAGE_SIZE = 100;

    public McpHandler(Storage storage, Path jobsRoot, String jenkinsUrl) {
        this.storage = storage;
        this.jobsRoot = jobsRoot;
        this.jenkinsUrl = jenkinsUrl;
    }

    /* mcp initialize request */
    void mcpInit(ObjectNode result) {
        result.put("protocolVersion", "2024-11-05");
        result.set("serverInfo", mapper.createObjectNode()
            .put("name", "results-view")
            .put("version", "0.1.0"));
        result.set("capabilities", mapper.createObjectNode()
            .set("tools", mapper.createObjectNode()));
    }

    void mcpToolsList(ObjectNode result) {
        ArrayNode tools = mapper.createArrayNode();
        pingToolDescr(tools);
        jobsToolDescr(tools);
        pkgsToolDescr(tools);
        jobRunsToolDescr(tools);
        pkgRunsToolDescr(tools);
        testsSummaryToolDescr(tools);
        jenkinsUrlToolDescr(tools);
        runLogToolDescr(tools);
        testLogToolDescr(tools);
        result.set("tools", tools);
    }

    void mcpToolsCall(JsonNode request, ObjectNode result) throws IOException {
        String name = request.path("params").path("name").asText();
        ArrayNode content = mapper.createArrayNode();
        switch (name) {
            case "ping":
                pingToolImpl(request, content);
                break;
            case "jobs":
                jobsToolImpl(request, content);
                break;
            case "pkgs":
                pkgsToolImpl(request, content);
                break;
            case "job-runs":
                jobRunsToolImpl(request, content);
                break;
            case "pkg-runs":
                pkgRunsToolImpl(request, content);
                break;
            case "tests-summary":
                testsSummaryToolImpl(request, content);
                break;
            case "jenkins-url":
                jenkinsUrlToolImpl(request, content);
                break;
            case "run-log":
                runLogToolImpl(request, content);
                break;
            case "test-log":
                testLogToolImpl(request, content);
                break;
            default:
                throw new IllegalArgumentException("Invalid tool name: " + name);
        }
        result.set("content", content);
    }

    /* TOOLS */
    // spec: https://modelcontextprotocol.io/specification/2024-11-05/server/tools

    /* ping */
    void pingToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "ping")
            .put("description", "Pings MCP server (for testing)")
            .set("inputSchema", mapper.createObjectNode()
                .put("type", "object"));
        tools.add(tool);
    }
    void pingToolImpl(JsonNode request, ArrayNode resultContent) {
        resultContent.add(mapper.createObjectNode()
            .put("type", "text")
            .put("text", "pong"));
    }

    /* jobs */
    void jobsToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "jobs")
            .put("description", "Gets list of test jobs, as MD table (can be long)")
            .set("inputSchema", mapper.createObjectNode()
                .put("type", "object")
                .set("properties", ((ObjectNode) ((ObjectNode) mapper.createObjectNode()
                    .set("pattern", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "Limits jobs to ones, whose name matches regex pattern (java style)")
                    ))
                    .set("page-size", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Sets (max) number of items per page (default " + DEFAULT_PAGE_SIZE + ")")
                    ))
                    .set("page", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Page index to show (1-bassed indexing; default 1)")
                    )
                ));
        tools.add(tool);
    }

    void jobsToolImpl(JsonNode request, ArrayNode resultContent) {
        List<Job> jobs = new ArrayList<Job>(storage.getJobs());
        String patternStr = getRequestArgument(request, "pattern");
        final Pattern p = createPattern(patternStr);
        if (p != null) {
            jobs.removeIf((job) -> !p.matcher(job.getName()).find());
        }
        int pageSize = getRequestArgumentInt(request, "page-size", DEFAULT_PAGE_SIZE);
        int page = getRequestArgumentInt(request, "page", 1);
        int itemsTotal = jobs.size();
        if (itemsTotal == 0) {
            addTextContent(resultContent, "No (matching) jobs found");
            return;
        }
        int pagesTotal = (itemsTotal + pageSize - 1) / pageSize; // rounded up
        if (page < 1 || page > pagesTotal) {
            addTextContent(resultContent, "Page is out of range (1-" + pagesTotal + "): " + page);
            return;
        }
        int pageStart = (page - 1) * pageSize;
        int pageEnd = Math.min(pageStart + pageSize, itemsTotal);
        jobs = jobs.subList(pageStart, pageEnd);
        StringBuilder sb = new StringBuilder();
        sb.append("| JOB NAME | LAST STATUS | LAST DATE |\n");
        sb.append("| --- | --- | --- |\n");
        for (Job job : jobs) {
            String jobName = job.getName();
            Run finishedRun = storage.getLatestFinishedRun(job);
            sb.append("| ");
            sb.append(jobName);
            sb.append(" | ");
            if (finishedRun != null) {
                sb.append(getStatusString(finishedRun.getStatus()));
            }
            sb.append(" | ");
            if (finishedRun != null) {
                sb.append(getFormatedDate(finishedRun.getStartTime()));
            }
            sb.append(" |\n");
        }
        appendPageInfo(sb, page, pagesTotal, itemsTotal);
        addTextContent(resultContent, sb.toString());
    }

    /* pkgs */
    void pkgsToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "pkgs")
            .put("description", "Gets list of tested packages (builds), as MD table")
            .set("inputSchema", mapper.createObjectNode()
                .put("type", "object")
                .set("properties", ((ObjectNode) ((ObjectNode) mapper.createObjectNode()
                    .set("pattern", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "Limits listed packages to ones, whose name matches regex pattern (java style)")
                    ))
                    .set("page-size", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Sets (max) number of items per page (default " + DEFAULT_PAGE_SIZE + ")")
                    ))
                    .set("page", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Page index to show (1-bassed indexing; default 1)")
                    )
                ));
        tools.add(tool);
    }

    void pkgsToolImpl(JsonNode request, ArrayNode resultContent) {
        List<Pkg> pkgs = new ArrayList<Pkg>(storage.getPkgs());
        Collections.sort(pkgs);
        String patternStr = getRequestArgument(request, "pattern");
        final Pattern p = createPattern(patternStr);
        if (p != null) {
            pkgs.removeIf((pkg) -> !p.matcher(pkg.getStrId()).find());
        }
        int pageSize = getRequestArgumentInt(request, "page-size", DEFAULT_PAGE_SIZE);
        int page = getRequestArgumentInt(request, "page", 1);
        int itemsTotal = pkgs.size();
        if (itemsTotal == 0) {
            addTextContent(resultContent, "No (matching) pkgs found");
            return;
        }
        int pagesTotal = (itemsTotal + pageSize - 1) / pageSize; // rounded up
        if (page < 1 || page > pagesTotal) {
            addTextContent(resultContent, "Page is out of range (1-" + pagesTotal + "): " + page);
            return;
        }
        int pageStart = (page - 1) * pageSize;
        int pageEnd = Math.min(pageStart + pageSize, itemsTotal);
        pkgs = pkgs.subList(pageStart, pageEnd);
        StringBuilder sb = new StringBuilder();
        sb.append("| PKG | RUNS COUNT |\n");
        sb.append("| --- | --- |\n");
        for (Pkg pkg : pkgs) {
            String pkgName = pkg.getStrId();
            sb.append("| ");
            sb.append(pkg.getStrId());
            sb.append(" | ");
            sb.append(storage.getPkgRunsCount(pkg));
            sb.append(" |\n");
        }
        appendPageInfo(sb, page, pagesTotal, itemsTotal);
        addTextContent(resultContent, sb.toString());
    }

    /* job-runs */
    void jobRunsToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "job-runs")
            .put("description", "Gets list of testsuite runs for specific job, as MD table")
            .set("inputSchema", ((ObjectNode) mapper.createObjectNode()
                .put("type", "object")
                .set("properties", ((ObjectNode) ((ObjectNode) mapper.createObjectNode()
                    .set("job", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "name of job for which to list testsuite runs")
                    ))
                    .set("page-size", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Sets (max) number of items per page (default " + DEFAULT_PAGE_SIZE + ")")
                    ))
                    .set("page", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Page index to show (1-bassed indexing; default 1)")
                    )
                ))
                .set("required", mapper.createArrayNode()
                    .add("job")
                )
            );
        tools.add(tool);
    }

    void jobRunsToolImpl(JsonNode request, ArrayNode resultContent) {
        String jobName = getRequestArgument(request, "job");
        if (jobName == null) {
            throw new IllegalArgumentException("Missing argument: job");
        }
        Job job = storage.getJob(jobName);
        if (job == null) {
            addTextContent(resultContent, "Job not found: " + jobName);
            return;
        }
        List<Run> runs = new ArrayList<Run>(storage.getJobRuns(job));
        Collections.sort(runs);
        Collections.reverse(runs); // order from last to first
        int pageSize = getRequestArgumentInt(request, "page-size", DEFAULT_PAGE_SIZE);
        int page = getRequestArgumentInt(request, "page", 1);
        int itemsTotal = runs.size();
        if (itemsTotal == 0) {
            addTextContent(resultContent, "No (matching) run found");
            return;
        }
        int pagesTotal = (itemsTotal + pageSize - 1) / pageSize; // rounded up
        if (page < 1 || page > pagesTotal) {
            addTextContent(resultContent, "Page is out of range (1-" + pagesTotal + "): " + page);
            return;
        }
        int pageStart = (page - 1) * pageSize;
        int pageEnd = Math.min(pageStart + pageSize, itemsTotal);
        runs = runs.subList(pageStart, pageEnd);
        StringBuilder sb = new StringBuilder();
        sb.append("| RUN_ID | STATUS | DATE | PKG |\n");
        sb.append("| --- | --- | --- | --- |\n");
        for (Run run : runs) {
            String status = getStatusString(run.getStatus());
            sb.append("| ");
            sb.append(run.getName());
            sb.append(" | ");
            sb.append(status);
            sb.append(" | ");
            sb.append(getFormatedDate(run.getStartTime()));
            sb.append(" | ");
            Pkg pkg = run.getPkg();
            sb.append(pkg != null ? pkg.getStrId() : "");
            sb.append(" |\n");
        }
        appendPageInfo(sb, page, pagesTotal, itemsTotal);
        addTextContent(resultContent, sb.toString());
    }

    /* pkg-runs */
    void pkgRunsToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "pkg-runs")
            .put("description", "Gets list of testsuite runs for specific package (build), as MD table")
            .set("inputSchema", ((ObjectNode) mapper.createObjectNode()
                .put("type", "object")
                .set("properties", ((ObjectNode) ((ObjectNode) ((ObjectNode) ((ObjectNode) mapper.createObjectNode()
                    .set("pkg", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "name of package for which to list testsuite runs")
                    ))
                    .set("job-pattern", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "Limits runs to those, whose job name matches regex pattern (java style)")
                    ))
                    .set("unsuccessful-only", mapper.createObjectNode()
                        .put("type", "boolean")
                        .put("description", "Only lists runs whose result is not SUCCESS, true or false (default false)")
                    ))
                    .set("page-size", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Sets (max) number of items per page (default " + DEFAULT_PAGE_SIZE + ")")
                    ))
                    .set("page", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Page index to show (1-bassed indexing; default 1)")
                    )
                ))
                .set("required", mapper.createArrayNode()
                    .add("pkg")
                )
            );
        tools.add(tool);
    }

    void pkgRunsToolImpl(JsonNode request, ArrayNode resultContent) {
        String pkgName = getRequestArgument(request, "pkg");
        if (pkgName == null) {
            throw new IllegalArgumentException("Missing argument: pkg");
        }
        Pkg pkg = storage.getPkg(pkgName);
        if (pkg == null) {
            addTextContent(resultContent, "Pkg not found: " + pkgName);
            return;
        }
        List<Run> runs = new ArrayList<Run>(storage.getPkgRuns(pkg));
        Collections.sort(runs);
        String patternStr = getRequestArgument(request, "job-pattern");
        final Pattern p = createPattern(patternStr);
        if (p != null) {
            runs.removeIf((run) -> !p.matcher(run.getJob().getName()).find());
        }
        boolean unsuccessful = getRequestArgumentBoolean(request, "unsuccessful-only", false);
        if (unsuccessful) {
            runs.removeIf((run) -> run.getStatus() == Run.SUCCESS);
        }
        int pageSize = getRequestArgumentInt(request, "page-size", DEFAULT_PAGE_SIZE);
        int page = getRequestArgumentInt(request, "page", 1);
        int itemsTotal = runs.size();
        if (itemsTotal == 0) {
            addTextContent(resultContent, "No (matching) run found");
            return;
        }
        int pagesTotal = (itemsTotal + pageSize - 1) / pageSize; // rounded up
        if (page < 1 || page > pagesTotal) {
            addTextContent(resultContent, "Page is out of range (1-" + pagesTotal + "): " + page);
            return;
        }
        int pageStart = (page - 1) * pageSize;
        int pageEnd = Math.min(pageStart + pageSize, itemsTotal);
        runs = runs.subList(pageStart, pageEnd);
        StringBuilder sb = new StringBuilder();
        sb.append("| RUN | STATUS | DATE |\n");
        sb.append("| --- | --- | --- |\n");
        for (Run run : runs) {
            String jobName = run.getJob().getName();
            int status = run.getStatus();
            sb.append("| ");
            sb.append(jobName);
            sb.append("/");
            sb.append(run.getName());
            sb.append(" | ");
            sb.append(getStatusString(status));
            sb.append(" | ");
            sb.append(getFormatedDate(run.getStartTime()));
            sb.append(" |\n");
        }
        appendPageInfo(sb, page, pagesTotal, itemsTotal);
        addTextContent(resultContent, sb.toString());
    }

    /* tests-summary */
    void testsSummaryToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "tests-summary")
            .put("description", "Gets testsuites results summary for run")
            .set("inputSchema", ((ObjectNode) mapper.createObjectNode()
                .put("type", "object")
                .set("properties", ((ObjectNode) ((ObjectNode) ((ObjectNode) mapper.createObjectNode()
                    .set("run", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "name of run for which get tests summary for (run name has form: JOB_NAME/RUN_ID)")
                    ))
                    .set("group", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "Only shows summary for specified test group (by default summary for all groups is shown)")
                    ))
                    .set("page-size", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Sets (max) number of items per page - used for test problems table(s) (default " + DEFAULT_PAGE_SIZE + ")")
                    ))
                    .set("page", mapper.createObjectNode()
                        .put("type", "integer")
                        .put("description", "Page index to show - used for test problems table(s) (1-bassed indexing; default 1)")
                    )
                ))
                .set("required", mapper.createArrayNode()
                    .add("run")
                )
            );
        tools.add(tool);
    }

    void testsSummaryToolImpl(JsonNode request, ArrayNode resultContent) throws IOException {
        String runName = getRequestArgument(request, "run");
        if (runName == null) {
            throw new IllegalArgumentException("Missing argument: run");
        }
        String[] runNameComponents = runName.split("/");
        if (runNameComponents.length != 2) {
            addTextContent(resultContent, "Invalid run arg format (should be of form JOB_NAME/RUN_ID): " + runName);
            return;
        }
        String jobName = runNameComponents[0];
        String runId = runNameComponents[1];
        Run run = storage.getRun(jobName, runId);
        if (run == null) {
            addTextContent(resultContent, "Run not found: " + runName);
            return;
        }
        Path resultsFile = reportJsonFile(jobName, runId);
        if (!Files.exists(resultsFile)){
            addTextContent(resultContent, "No test results found for: " + runName);
            return;
        }
        String groupArg = getRequestArgument(request, "group");
        JsonNode resultsNode = mapper.readTree(resultsFile.toFile());
        StringBuilder sb = new StringBuilder();
        sb.append("pkg: " + run.getPkg().getStrId() + "\n");
        sb.append("# overview\n");
        sb.append("| TEST_GROUP | PASSED | FAILED | ERROR | NOT_RAN | TOTAL |\n");
        sb.append("| --- | --- | --- | --- | --- | --- |\n");
        for (JsonNode testGroupNode : resultsNode) {
            String groupName = testGroupNode.path("name").asText();
            if (groupArg != null && !groupArg.equals(groupName)) {
                continue;
            }
            JsonNode report = testGroupNode.path("report");
            String passed = report.path("testsPassed").asText();
            String notRun = report.path("testsNotRun").asText();
            String failed = report.path("testsFailed").asText();
            String error = report.path("testsError").asText();
            String total = report.path("testsTotal").asText();
            sb.append("| " + groupName + " | " + passed + " | " + failed + " | " + error + " | " + notRun + " | " + total + "|\n");
        }
        sb.append("# test problems\n");
        for (JsonNode testGroupNode : resultsNode) {
            String groupName = testGroupNode.path("name").asText();
            if (groupArg != null && !groupArg.equals(groupName)) {
                continue;
            }
            JsonNode report = testGroupNode.path("report");
            List<JsonNode> testList = new ArrayList();
            for (JsonNode problemNode : report.path("testProblems")) {
                testList.add(problemNode);
            }
            testList.sort((node1, node2) -> node1.path("name").asText().compareTo(node2.path("name").asText()));
            int pageSize = getRequestArgumentInt(request, "page-size", DEFAULT_PAGE_SIZE);
            int page = getRequestArgumentInt(request, "page", 1);
            int itemsTotal = testList.size();
            if (itemsTotal == 0) {
                continue;
            }
            int pagesTotal = (itemsTotal + pageSize - 1) / pageSize; // rounded up
            if (page < 1 || page > pagesTotal) {
                addTextContent(resultContent, "Page is out of range (1-" + pagesTotal + "): " + page);
                continue;
            }
            int pageStart = (page - 1) * pageSize;
            int pageEnd = Math.min(pageStart + pageSize, itemsTotal);
            testList = testList.subList(pageStart, pageEnd);
            sb.append("## " + groupName + "\n");
            sb.append("| NAME | STATUS | STATUS LINE |\n");
            sb.append("| --- | --- | --- |\n");
            for (JsonNode problemNode : testList) {
                String testName =  problemNode.path("name").asText();
                String testStatus = problemNode.path("status").asText();
                String testStatusLine = problemNode.path("statusLine").asText();
                sb.append("| " + testName + " | " + testStatus + " | " + testStatusLine + " |\n");
            }
            appendPageInfo(sb, page, pagesTotal, itemsTotal);
        }
        resultContent.add(mapper.createObjectNode()
            .put("type", "text")
            .put("text", sb.toString()));
    }

    /* jenkins-url */
    void jenkinsUrlToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "jenkins-url")
            .put("description", "Gets url of jenkins server for generating links and vice-versa, jenkins urls have following form: JENKINS_URL/job/JOB_NAME/RUN_ID")
            .set("inputSchema", mapper.createObjectNode()
                .put("type", "object")
            );
        tools.add(tool);
    }

    void jenkinsUrlToolImpl(JsonNode request, ArrayNode resultContent)  {
        resultContent.add(mapper.createObjectNode()
            .put("type", "text")
            .put("text", jenkinsUrl));
    }

    /* run-log */
    void runLogToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "run-log")
            .put("description", "Gets console log for given run (can be long)")
            .set("inputSchema", ((ObjectNode) mapper.createObjectNode()
                .put("type", "object")
                .set("properties", ((ObjectNode) ((ObjectNode) ((ObjectNode) mapper.createObjectNode()
                    .set("run", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "name of run for which to get log (run name has form: JOB_NAME/RUN_ID)")
                    ))
                    .set("ranges", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "only shows line range(s), decribed as comma sepparated list (1-based, inclusive) e.g.: 1-10,20-30")
                    ))
                    .set("pattern", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "only shows lines matching regex patter (java style), lines will be prefixed by linenumber (1-based indexing)")
                    ))
                    .set("pattern-ignores-case", mapper.createObjectNode()
                        .put("type", "boolean")
                        .put("description", "configures pattern to ignore case (true or false; default is false)")
                    )

                ))
                .set("required", mapper.createArrayNode()
                    .add("run")
                )
            );
        tools.add(tool);
    }

    void runLogToolImpl(JsonNode request, ArrayNode resultContent) throws IOException {
        String runName = getRequestArgument(request, "run");
        if (runName == null) {
            throw new IllegalArgumentException("Missing argument: run");
        }
        String[] runNameComponents = runName.split("/");
        if (runNameComponents.length != 2) {
            addTextContent(resultContent, "Invalid run arg format (should be of form JOB_NAME/RUN_ID): " + runName);
            return;
        }
        String jobName = runNameComponents[0];
        String runId = runNameComponents[1];
        Run run = storage.getRun(jobName, runId);
        if (run == null) {
            addTextContent(resultContent, "Run not found: " + runName);
            return;
        }
        Path logFile = jobsRoot.resolve(jobName).resolve("builds").resolve(runId).resolve("log");
        if (!Files.exists(logFile)){
            addTextContent(resultContent, "Cannot find log for: " + runName);
            return;
        }
        List<String> lines;
        try (BufferedReader br = Files.newBufferedReader(logFile)) {
            lines = filterLines(request, br);
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line);
            sb.append("\n");
        }
        resultContent.add(mapper.createObjectNode()
            .put("type", "text")
            .put("text", sb.toString()));
    }

    /* test-log */
    void testLogToolDescr(ArrayNode tools) {
        JsonNode tool = mapper.createObjectNode()
            .put("name", "test-log")
            .put("description", "Gets console log for given test")
            .set("inputSchema", ((ObjectNode) mapper.createObjectNode()
                .put("type", "object")
                .set("properties", ((ObjectNode) ((ObjectNode) ((ObjectNode) ((ObjectNode) ((ObjectNode) mapper.createObjectNode()
                    .set("run", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "name of run (run name has form: JOB_NAME/RUN_ID)")
                    ))
                    .set("group", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "group of tests to search in (if ommited, search all, return first match)")
                    ))
                    .set("test", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "name of test to get log for")
                    ))
                    .set("ranges", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "only shows line range(s), decribed as comma sepparated list (1-based, inclusive) e.g.: 1-10,20-30")
                    ))
                    .set("pattern", mapper.createObjectNode()
                        .put("type", "string")
                        .put("description", "only shows lines matching regex patter (java style), lines will be prefixed by linenumber (1-based indexing)")
                    ))
                    .set("pattern-ignores-case", mapper.createObjectNode()
                        .put("type", "boolean")
                        .put("description", "configures pattern to ignore case (true or false; default is false)")
                    )
                ))
                .set("required", mapper.createArrayNode()
                    .add("run")
                    .add("test")
                )
            );
        tools.add(tool);
    }

    void testLogToolImpl(JsonNode request, ArrayNode resultContent) throws IOException {
        String runName = getRequestArgument(request, "run");
        if (runName == null) {
            throw new IllegalArgumentException("Missing argument: run");
        }
        String testNameArg = getRequestArgument(request, "test");
        if (testNameArg == null) {
            throw new IllegalArgumentException("Missing argument: test");
        }
        String[] runNameComponents = runName.split("/");
        if (runNameComponents.length != 2) {
            addTextContent(resultContent, "Invalid run arg format (should be of form JOB_NAME/RUN_ID): " + runName);
            return;
        }
        String jobName = runNameComponents[0];
        String runId = runNameComponents[1];
        Run run = storage.getRun(jobName, runId);
        if (run == null) {
            addTextContent(resultContent, "Run not found: " + runName);
            return;
        }
        Path resultsFile = reportJsonFile(jobName, runId);
        if (!Files.exists(resultsFile)){
            addTextContent(resultContent, "No test results found for: " + runName);
            return;
        }
        JsonNode resultsNode = mapper.readTree(resultsFile.toFile());
        String groupArg = getRequestArgument(request, "group");
        for (JsonNode testGroupNode : resultsNode) {
            String groupName = testGroupNode.path("name").asText();
            if (groupArg != null && !groupArg.equals(groupName)) {
                continue;
            }
            JsonNode testGroupReportNode = testGroupNode.path("report");
            for (JsonNode problemNode : testGroupReportNode.path("testProblems")) {
                String testName =  problemNode.path("name").asText();
                if (!testNameArg.equals(testName)) {
                    continue;
                }
                StringBuilder outputBuilder = new StringBuilder();
                for (JsonNode output : problemNode.path("outputs")) {
                        String outputName = output.path("name").asText();
                        outputBuilder.append("### " + outputName + "\n");
                        String outputValue = output.path("value").asText();
                        outputBuilder.append(outputValue + "\n");
                }
                List<String> lines;
                try (BufferedReader br = new BufferedReader(new StringReader(outputBuilder.toString()))) {
                    lines = filterLines(request, br);
                }
                StringBuilder logSb = new StringBuilder();
                for (String line : lines) {
                    logSb.append(line);
                    logSb.append("\n");
                }
                resultContent.add(mapper.createObjectNode()
                    .put("type", "text")
                    .put("text", logSb.toString()));
                return;
            }
        }
        resultContent.add(mapper.createObjectNode()
            .put("type", "text")
            .put("text", "test/log not found!"));
    }

    /* UTILITY FUNCTIONS */

    static String getStatusString(int status) {
        switch (status) {
            case Run.RUNNING:
                return "RUNNING";
            case Run.SUCCESS:
                return "SUCCESS";
            case Run.UNSTABLE:
                return "UNSTABLE";
            case Run.FAILURE:
                return "FAILURE";
            case Run.ABORTED:
                return "ABORTED";
            case Run.NOT_BUILT:
                return "NOT_BUILT";
            case Run.FINISHED:
                return "FINISHED";
            case Run.UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    private static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private static String getFormatedDate(long date) {
        return dateFormater.format(new Date(date));
    }

    private static String getRequestArgument(JsonNode request, String name) {
        JsonNode node = request.path("params").path("arguments").path(name);
        return node.isMissingNode() ? null : node.asText();
    }

    private static boolean getRequestArgumentBoolean(JsonNode request, String name, boolean defaultVal) {
        JsonNode node = request.path("params").path("arguments").path(name);
        return node.isMissingNode() ? defaultVal :
            "true".equals(node.asText().toLowerCase()) ? true : false;
    }

    private static int getRequestArgumentInt(JsonNode request, String name, int defaultVal) {
        JsonNode node = request.path("params").path("arguments").path(name);
        return node.isMissingNode() ? defaultVal : Integer.valueOf(node.asText());
    }

    private static void addTextContent(ArrayNode resultContent, String s) {
        resultContent.add(mapper.createObjectNode()
            .put("type", "text")
            .put("text", s));
    }

    boolean isInRanges(List<int[]> ranges, int i) {
        if (ranges == null) {
            return true; // no filter
        }
        for (int[] range : ranges) {
            if (i >= range[0] && i <= range[1]) {
                return true;
            }
        }
        return false;
    }

    static Pattern ansiColorsPattern = Pattern.compile("\\033\\[[0-9;]+m");

    List<String> filterLines(JsonNode request, BufferedReader reader) throws IOException {
        String rangesStr = getRequestArgument(request, "ranges");
        String patternStr = getRequestArgument(request, "pattern");
        List<String> lines = new ArrayList<String>();
        List<int[]> ranges = null;
        int rangeMax = -1;
        if (rangesStr != null) {
            String[] rangesStrComponents = rangesStr.split(",");
            if (rangesStrComponents.length > 0) {
                ranges = new ArrayList<int[]>();
                for (String rangeStr : rangesStrComponents) {
                    String[] indexesStr = rangeStr.split("-");
                    if (indexesStr.length != 2) {
                        throw new IllegalArgumentException("Illegal range: " + rangeStr);
                    }
                    int[] range = new int[2];
                    int min = Integer.valueOf(indexesStr[0].trim());
                    int max = Integer.valueOf(indexesStr[1].trim());
                    range[0] = min;
                    range[1] = max;
                    ranges.add(range);
                    rangeMax = Math.max(max, rangeMax);
                }
            }
        }
        Pattern pattern = null;
        if (patternStr != null) {
            boolean ignoreCase = getRequestArgumentBoolean(request, "pattern-ignores-case", false);
            pattern = ignoreCase ? Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE) : Pattern.compile(patternStr);
        }
        int lineNum = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNum++; // one based indexing
            if (ranges != null) {
                if (rangeMax > 0 && lineNum > rangeMax) {
                    break; // rest of file is outside of range, skip processing
                }
                if (!isInRanges(ranges, lineNum)) {
                    continue;
                }
            }
            if (line.indexOf('\033') >= 0) {
                line = ansiColorsPattern.matcher(line).replaceAll(""); // remove ansi colors
            }
            if (pattern != null && !pattern.matcher(line).find()) {
                continue;
            }
            line = pattern == null ? line : String.valueOf(lineNum) + ": " + line;
            lines.add(line);
        }
        return lines;
    }

    public Path reportJsonFile(String jobName, String runId) {
        Path resultsFile = jobsRoot.resolve(jobName).resolve("builds").resolve(runId).resolve("jtreg-report.json");
        if (!Files.exists(resultsFile)) {
            resultsFile = jobsRoot.resolve(jobName).resolve("builds").resolve(runId).resolve("jck-report.json");
        }
        return resultsFile;
    }

    private static Pattern createPattern(String patternStr) {
        if (patternStr == null || patternStr.isEmpty()) {
            return null;
        }
        return Pattern.compile(patternStr);
    }

    private static void appendPageInfo(StringBuilder sb, int page, int pagesTotal, int itemsTotal) {
        if (pagesTotal > 1) {
            sb.append("\npage " + page + "/" + pagesTotal + " of total " + itemsTotal + " items" + (page == 1 ? " (use page argument to access rest)" : "") + "\n");
        }
    }


    /* ENTRY POINT */

    public void processRequest(final HttpServletRequest request, HttpServletResponse response) throws IOException {
        BufferedReader in = request.getReader();
        JsonNode requestNode = mapper.readTree(in);
        JsonNode id = requestNode.get("id");
        if (id == null) {
            return; // just notification -> ignore
        }
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String method = requestNode.path("method").asText();
        ObjectNode responseNode = mapper.createObjectNode()
            .put("jsonrpc", "2.0")
            .set("id", id);
        ObjectNode result = mapper.createObjectNode();
        try {
            switch (method) {
                case "initialize":
                    mcpInit(result);
                    break;
                case "tools/list":
                    mcpToolsList(result);
                    break;
                case "tools/call":
                    mcpToolsCall(requestNode, result);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }
            responseNode.set("result", result);
        } catch (Exception e) {
            responseNode.set("error", mapper.createObjectNode()
                .put("code", -32602)
                .put("message", e.toString()));
        }
        out.println(mapper.writeValueAsString(responseNode));
        out.flush();
    }


}

/*
 * The MIT License
 *
 * Copyright 2020 zzambers.
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
package resultsview.plugins.jenkins;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import resultsview.common.VersionUtil;
import resultsview.storage.PluginConf;
import resultsview.storage.Job;
import resultsview.storage.Run;
import resultsview.storage.StorageInterface;
import resultsview.xml.BuildXmlHandler;

/**
 *
 * @author zzambers
 */
public class JenkinsDiscovery {

    Path jobsRoot;
    StorageInterface storage;

    PluginConf depl;
    Map<Long, Job> knownJobsMapById = new HashMap();
    Map<String, Job> knownJobsMap = new HashMap();
    Map<Long, Run> latestKnownRunMap = new HashMap();
    Set<Run> runningSet = new HashSet();

    public long lastPollTime = Long.MIN_VALUE;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    public JenkinsDiscovery(Path jobsRoot, StorageInterface storage, PluginConf depl) {
        this.jobsRoot = jobsRoot;
        this.storage = storage;
        this.depl = depl;
    }

    public List<Path> listDirVerSorted(Path dir) throws IOException {
        List<Path> list = new ArrayList();
        try (DirectoryStream<Path> buildDirs = Files.newDirectoryStream(dir)) {
            for (Path entry : buildDirs) {
                list.add(entry);
            }
        }
        Comparator<Path> cmp = new Comparator<Path>() {
            @Override
            public int compare(Path t, Path t1) {
                return VersionUtil.versionCompare(t.getFileName().toString(), t1.getFileName().toString());
            }
        };
        Collections.sort(list, cmp);
        return list;
    }

    public String readNextBuildNumber(Path nextBuildFile) throws IOException {
        List<String> lines = Files.readAllLines(nextBuildFile);
        if (lines != null && !lines.isEmpty()) {
            String line = lines.get(0);
            if (NUMBER_PATTERN.matcher(line).matches()) {
                return line;
            }
        }
        return null;
    }

    public void poll() throws Exception {
        long time = System.currentTimeMillis();
        pollNewJobs();
        lastPollTime = time;
    }

    void pollNewJobs() throws Exception {
        long modifTime = Files.getLastModifiedTime(jobsRoot).toMillis();
        if (modifTime >= lastPollTime) {
            try (DirectoryStream<Path> jobPaths = Files.newDirectoryStream(jobsRoot)) {
                for (Path jobPath : jobPaths) {
                    String jobName = jobPath.getFileName().toString();
                    Job job = knownJobsMap.get(jobName);
                    if (job == null
                            && Files.isDirectory(jobPath)
                            && Files.exists(jobPath.resolve("config.xml"))) {
                        job = new Job(depl.getId(), jobName, jobName);
                        storage.storeJob(job);
                        knownJobsMap.put(jobName, job);
                        knownJobsMapById.put(job.getId(), job);
                    }
                }
            }
        }
        for (Job job : knownJobsMap.values()) {
            pollNewRuns(job);
        }
    }

    void pollNewRuns(Job job) throws IOException {
        Path jobDir = jobsRoot.resolve(job.getStrId());
        if (!Files.isDirectory(jobDir)) {
            return;
        }
        Run latestKnownRun = latestKnownRunMap.get(job.getId());
        Path buildsDir = jobDir.resolve("builds");
        //Path nextBuildFile = jobDir.resolve("nextBuildNumber");
        long modifTime = Files.getLastModifiedTime(buildsDir).toMillis();
        if (modifTime >= lastPollTime || latestKnownRun == null) {
            String latestKnownRunName
                    = latestKnownRun == null ? null : latestKnownRun.getName();
            /*
            String nextBuildName = readNextBuildNumber(nextBuildFile);
            if (nextBuildName != null) {
                String prevBuildName = Long.toString(Long.parseLong(nextBuildName) - 1);
                if (prevBuildName.equals(latestKnownRunName)) {
                    return;
                }
            }
             */

            for (Path buildDir : listDirVerSorted(buildsDir)) {
                if (Files.isDirectory(buildDir)) {
                    String buildId = buildDir.getFileName().toString();
                    if (NUMBER_PATTERN.matcher(buildId).matches()) {
                        Run run = null;
                        if (latestKnownRunName == null || VersionUtil.versionCompare(latestKnownRunName, buildId) < 0) {
                            Path buildXml = buildDir.resolve("build.xml");
                            int status = getStatus(buildXml);
                            run = new Run(job.getId(), buildId, buildId);
                            run.setStatus(status);
                            storage.storeRun(run);
                            latestKnownRunMap.put(job.getId(), run);
                            if (status == Run.RUNNING) {
                                runningSet.add(run);
                            }
                        }
                    }
                }
            }
            
        }
    }

    public int getStatus(Path buildXml) {
        if (Files.exists(buildXml)) {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser;
                saxParser = factory.newSAXParser();
                BuildXmlHandler handler = new BuildXmlHandler();
                saxParser.parse(buildXml.toFile(), handler);
                String result = handler.getResult();
                if (result != null) {
                    switch (result.toUpperCase()) {
                        case "SUCCESS":
                            return Run.PASSED;
                        case "UNSTABLE":
                            return Run.FAILED;
                        case "FAILURE":
                            return Run.ERROR;
                        case "ABORTED":
                            return Run.CANCELED;
                    }
                }
            } catch (Exception e) {
                /* Ignore parser errors */
            }
        }
        return Run.RUNNING;
    }

    void pollRunning() throws IOException {
        for (Run run : runningSet) {
            Job owningJob = knownJobsMapById.get(run.getJobId());
            pollRunning2(run, owningJob);
        }
    }

    void pollRunning2(Run run, Job owningJob) throws IOException {
        Path jobDir = jobsRoot.resolve(owningJob.getStrId());
        if (!Files.isDirectory(jobDir)) {
            return;
        }
        Path buildsDir = jobDir.resolve("builds");
        Path buildDir = buildsDir.resolve(run.getStrId());
        Path buildXml = buildDir.resolve("build.xml");
        if (Files.exists(buildXml)) {
            long modifTime = Files.getLastModifiedTime(buildXml).toMillis();
            if (modifTime >= lastPollTime) {
                int status = getStatus(buildXml);
                if (status != Run.RUNNING) {
                    run.setStatus(status);
                    runningSet.remove(run);
                }
            }
        }
    }

    void updateRun(Run run) {

    }

    void storeJob(Job newJob) {

    }

    void storeRun(Run newRun) {

    }

}

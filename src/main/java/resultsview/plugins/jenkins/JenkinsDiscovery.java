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
import resultsview.storage.Job;
import resultsview.storage.Run;
import resultsview.storage.Pkg;
import resultsview.storage.StorageInterface;
import resultsview.xml.BuildXmlHandler;

/**
 *
 * @author zzambers
 */
public class JenkinsDiscovery {

    Path jobsRoot;
    StorageInterface storage;
    public Pattern jobPattern = null;

    Map<String, Job> knownJobsMap = new HashMap();
    Map<Job, Run> latestKnownRunMap = new HashMap();
    Map<String, String> runNameMap = new HashMap();
    public Set<Run> runningSet = new HashSet();

    public long lastPollTime = Long.MIN_VALUE;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    public JenkinsDiscovery(Path jobsRoot, StorageInterface storage) {
        this.jobsRoot = jobsRoot;
        this.storage = storage;
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
        pollRunning();
        pollNewJobs();
        lastPollTime = time;
    }

    void pollNewJobs() throws Exception {
        long modifTime = Files.getLastModifiedTime(jobsRoot).toMillis();
        if (modifTime >= lastPollTime) {
            Set<String> foundJobNames = new HashSet();
            try (DirectoryStream<Path> jobPaths = Files.newDirectoryStream(jobsRoot)) {
                for (Path jobPath : jobPaths) {
                    String jobName = jobPath.getFileName().toString();
                    if (jobPattern != null && !jobPattern.matcher(jobName).matches()) {
                        continue;
                    }
                    foundJobNames.add(jobName);
                    Job job = getJob(jobName);
                    if (job == null
                            && Files.isDirectory(jobPath)
                            && Files.exists(jobPath.resolve("config.xml"))) {
                        job = new Job(jobName);
                        storage.storeJob(job);
                        // knownJobsMap.put(jobName, job);
                    }
                }
            }
            Set<String> removedJobs = new HashSet();
            for (Job job : getJobs()) {
                String jobName = job.getName();
                if (!foundJobNames.contains(jobName)) {
                    removedJobs.add(jobName);
                }
            }
            for (String removedName : removedJobs) {
                removeJob(removedName);
            }
        } else {
            System.out.println("Skipped poll: modifTime: " + modifTime + " lastPollTime: " + lastPollTime);
        }
        for (Job job : getJobs()) {
            pollNewRuns(job);
        }
    }

    void pollNewRuns(Job job) throws IOException {
        Path jobDir = jobsRoot.resolve(job.getName());
        if (!Files.isDirectory(jobDir)) {
            return;
        }
        Run latestKnownRun = latestKnownRunMap.get(job);
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
                            runNameMap.putIfAbsent(buildId, buildId);
                            buildId = runNameMap.get(buildId);
                            run = new Run(job, buildId);
                            processRun(run);
                            storage.storeRun(run);
                            latestKnownRunMap.put(job, run);
                            int status = run.getStatus();
                            if (status == Run.RUNNING) {
                                runningSet.add(run);
                            }
                        }
                    }
                }
            }

        }
    }

    public BuildXmlHandler parseBuildXml(Path buildXml) {
        BuildXmlHandler handler = null;
        if (Files.exists(buildXml)) {
            try {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser;
                saxParser = factory.newSAXParser();
                handler = new BuildXmlHandler();
                saxParser.parse(buildXml.toFile(), handler);
            } catch (Exception e) {
                return null;
                /* Ignore parser errors */
            }
        }
        return handler;
    }

    public int getStatus(BuildXmlHandler handler) {
        if (handler != null) {
            String resultStr = handler.getResult();
            int result = Run.RUNNING;
            if (resultStr != null) {
                switch (resultStr.toUpperCase()) {
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
        }
        return Run.RUNNING;
    }

    public void pollRunning() throws IOException {
        Set<Run> finishedSet = new HashSet();
        for (Run run : runningSet) {
            processRun(run);
            int status = run.getStatus();
            if (status != Run.RUNNING) {
                finishedSet.add(run);
            }
        }
        for (Run run : finishedSet) {
            runningSet.remove(run);
        }
    }

    void processRun(Run run) throws IOException {
        Job owningJob = run.getJob();
        Path jobDir = jobsRoot.resolve(owningJob.getName());
        if (!Files.isDirectory(jobDir)) {
            return;
        }
        Path buildsDir = jobDir.resolve("builds");
        Path buildDir = buildsDir.resolve(run.getName());
        Path buildXml = buildDir.resolve("build.xml");
        if (Files.exists(buildXml)) {
            long modifTime = Files.getLastModifiedTime(buildXml).toMillis();
            if (modifTime >= lastPollTime) {
                int status = Run.RUNNING;
                BuildXmlHandler handler = parseBuildXml(buildXml);
                if (handler != null) {
                    status = getStatus(handler);
                    String pkgName = handler.getPkgName();
                    String pkgVersion = handler.getPkgVersion();
                    String pkgRelease = handler.getPkgRelease();
                    if (pkgName != null && pkgVersion != null && pkgRelease != null) {
                        String nvr = pkgName + "-" + pkgVersion + "-" + pkgRelease;
                        //run.build = nvr;
                        Pkg pkg = storage.getPkg(nvr);
                        if (pkg == null) {
                            pkg = new Pkg(nvr);
                            storage.storePkg(pkg);
                        }
                        storage.addPkgRun(pkg, run);
                    }
                }
                run.setStatus(status);
            }
        }
    }

    Job getJob(String name) {
        return storage.getJob(name);
        //return knownJobsMap.get(name);
    }

    Iterable<Job> getJobs() {
        return storage.getJobs();
        //return knownJobsMap.values();
    }

    Run getLatestKnownRun(Job job) {
        return latestKnownRunMap.get(job);
    }

    void removeJob(String name) {
        //Job removedJob = knownJobsMap.remove(name);
        Job removedJob = storage.getJob(name);
        for (Run run : storage.getJobRuns(removedJob)) {
            runningSet.remove(run);
        }
        latestKnownRunMap.remove(removedJob);
        storage.removeJob(name);
    }

}

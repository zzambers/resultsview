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
package resultsview.poll;

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

public class JenkinsPoller {

    Path jobsRoot;
    StorageInterface storage;
    public Pattern jobPattern = null;

    Map<String, String> runNameMap = new HashMap<>();

    public long rootModifTime = Long.MIN_VALUE;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    public JenkinsPoller(Path jobsRoot, StorageInterface storage) {
        this.jobsRoot = jobsRoot;
        this.storage = storage;
    }

    public List<Path> listDirVerSorted(Path dir) throws IOException {
        List<Path> list = new ArrayList<>();
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
        pollRunning();
        pollNewJobs();
    }

    void pollNewJobs() throws Exception {
        long modifTime = Files.getLastModifiedTime(jobsRoot).toMillis();
        if (modifTime > rootModifTime) {
            rootModifTime = modifTime;
            Set<String> foundJobNames = new HashSet<>();
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
                    }
                }
            }
            for (Job job : getJobs()) {
                String jobName = job.getName();
                if (!foundJobNames.contains(jobName)) {
                    removeJob(jobName);
                }
            }
        } else {
            // System.out.println("Skipped poll: modifTime: " + modifTime + " rootModifTime: " + rootModifTime);
        }
        for (Job job : getJobs()) {
            pollNewRuns(job);
        }
    }

    void pollNewRuns(Job job) throws IOException {
        Path jobDir = jobsRoot.resolve(job.getName());
        Path buildsDir = jobDir.resolve("builds");
        if (!Files.exists(buildsDir)) {
            return;
        }
        Run latestKnownRun = storage.getJobLatestRun(job);
        //Path nextBuildFile = jobDir.resolve("nextBuildNumber");
        long modifTime = Files.getLastModifiedTime(buildsDir).toMillis();
        if (modifTime > job.modifTime || latestKnownRun == null) {
            job.modifTime = modifTime;
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
                String buildId = buildDir.getFileName().toString();
                if (NUMBER_PATTERN.matcher(buildId).matches()) {
                    if (latestKnownRunName == null || VersionUtil.versionCompare(latestKnownRunName, buildId) < 0) {
                        if (Files.isDirectory(buildDir)) {
                            runNameMap.putIfAbsent(buildId, buildId);
                            buildId = runNameMap.get(buildId);
                            Run run = new Run(job, buildId);
                            processRun(run);
                            storage.storeRun(run);
                            storage.setJobLatestRun(job, run);
                            int status = run.getStatus();
                            if (status == Run.RUNNING) {
                                storage.addUnfinishedRun(run);
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
                        return Run.SUCCESS;
                    case "UNSTABLE":
                        return Run.UNSTABLE;
                    case "FAILURE":
                        return Run.FAILURE;
                    case "ABORTED":
                        return Run.ABORTED;
                    case "NOT_BUILT":
                        return Run.NOT_BUILT;
                }
            }
        }
        return Run.RUNNING;
    }

    public void pollRunning() throws IOException {
        for (Run run : storage.getUnfinishedRuns()) {
            processRun(run);
            int status = run.getStatus();
            if (status != Run.RUNNING) {
                storage.removeUnfinishedRun(run);
            }
        }
    }

    void processRun(Run run) throws IOException {
        Job owningJob = run.getJob();
        Path jobDir = jobsRoot.resolve(owningJob.getName());
        Path buildsDir = jobDir.resolve("builds");
        Path buildDir = buildsDir.resolve(run.getName());
        Path buildXml = buildDir.resolve("build.xml");
        if (Files.exists(buildXml)) {
            long modifTime = Files.getLastModifiedTime(buildXml).toMillis();
            if (modifTime > run.modifTime) {
                run.modifTime = modifTime;
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
        } else if (run.getStatus() == Run.UNKNOWN) {
            run.setStatus(Run.RUNNING);
        }
    }

    Job getJob(String name) {
        return storage.getJob(name);
    }

    Iterable<Job> getJobs() {
        return storage.getJobs();
    }

    void removeJob(String name) {
        Job removedJob = storage.getJob(name);
        if (removedJob == null) {
            return;
        }
        storage.removeJob(name);
    }

}

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
package resultsview.common.test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import resultsview.plugins.jenkins.JenkinsDiscovery;
import resultsview.storage.PluginConf;
import resultsview.storage.Job;
import resultsview.storage.Run;
import resultsview.storage.StorageInterface;

/**
 *
 * @author zzambers
 */
public class TestJenkinPlugin {

    Path tmpDir;
    Path jenkinsJobs;

    public static void recursiveDelete(final Path file) throws IOException {
        FileVisitor<Path> fv = new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        };
        Files.walkFileTree(file, fv);
    }

    void writeConfigXml(Path dir) throws IOException {
        Path configxml = dir.resolve("config.xml");
        List<String> lines = new ArrayList();
        lines.add("<?xml version=\"1.1\" encoding=\"UTF-8\"?><project>");
        lines.add("    <actions/>");
        lines.add("    <description/>");
        lines.add("    <keepDependencies>false</keepDependencies>");
        lines.add("    <properties/>");
        lines.add("    <builders>");
        lines.add("        <hudson.tasks.Shell>");
        lines.add("            <command>");
        lines.add("</command>");
        lines.add("        </hudson.tasks.Shell>");
        lines.add("    </builders>");
        lines.add("<publishers/>");
        lines.add("<buildWrappers/>");
        lines.add("</project>");
        Files.write(configxml, lines, Charset.forName("UTF-8"));
        lines.clear();
    }

    void createJob(Path root, String jobName) throws IOException {
        Path job = root.resolve(jobName);
        Files.createDirectories(job);
        Path builds = job.resolve("builds");
        Files.createDirectories(builds);

        List<String> lines = new ArrayList();
        Path nextBuildFile = job.resolve("nextBuildNumber");
        lines.add("1");
        Files.write(nextBuildFile, lines, Charset.defaultCharset());
        lines.clear();

        writeConfigXml(job);
    }

    void createBuild(Path root, String jobName, int buildId) throws IOException {
        Path job = root.resolve(jobName);
        Path builds = job.resolve("builds");

        List<String> lines = new ArrayList();
        Path nextBuildFile = job.resolve("nextBuildNumber");
        Files.delete(nextBuildFile);
        lines.add(Integer.toString(buildId + 1));
        Files.write(nextBuildFile, lines, Charset.defaultCharset());
        lines.clear();

        Path buildDir = builds.resolve(Integer.toString(buildId));
        Files.createDirectories(buildDir);
    }

    void prepareFakeJenkins(Path root) throws IOException {
        Files.createDirectories(root);

        createJob(root, "job1");

        createJob(root, "job2");
        createBuild(root, "job2", 1);

        createJob(root, "job3");
        createBuild(root, "job3", 1);
        createBuild(root, "job3", 2);
    }

    @Before
    public void before() throws IOException {
        tmpDir = Files.createTempDirectory("resultview");
        jenkinsJobs = tmpDir.resolve("jenkinsJobs");
        prepareFakeJenkins(jenkinsJobs);
    }

    @After
    public void after() throws IOException {
        recursiveDelete(tmpDir);
    }

    @Test
    public void basicPollTest() throws Exception {
        TestStorage storage = new TestStorage();
        PluginConf depl = new PluginConf("jenkins");
        depl.setId(1L);
        JenkinsDiscovery jenkins = new JenkinsDiscovery(jenkinsJobs, storage, depl);
        jenkins.poll();
        Assert.assertTrue("More than zero new jobs", storage.jobs.size() > 0);
        Assert.assertTrue("More than zero new runs", storage.runs.size() > 0);
        storage.jobs.clear();
        storage.runs.clear();
        jenkins.poll();
        Assert.assertEquals("Zero new jobs", 0, storage.jobs.size());
        Assert.assertEquals("Zero new runs", 0, storage.runs.size());
        storage.jobs.clear();
        storage.runs.clear();
        jenkins.lastPollTime = Long.MIN_VALUE;
        jenkins.poll();
        Assert.assertEquals("Zero new jobs", 0, storage.jobs.size());
        Assert.assertEquals("Zero new runs", 0, storage.runs.size());
    }

    static class TestStorage implements StorageInterface {

        long jobctr = 1;
        long runctr = 1;

        List<Job> jobs = new ArrayList();
        List<Run> runs = new ArrayList();

        @Override
        public void storeJob(Job job) {
            job.setId(jobctr++);
            //System.out.println("job " + job.getStrId() + " " + job.getId());
            jobs.add(job);
        }

        @Override
        public void storeRun(Run run) {
            //System.out.println("run " + run.getJobId() + " " + run.getStrId());
            run.setId(runctr++);
            runs.add(run);
        }

    }

}

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
import java.util.Collection;
import java.util.Set;
import java.util.List;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import resultsview.poll.JenkinsPoller;
import resultsview.storage.Job;
import resultsview.storage.Run;
import resultsview.storage.Pkg;
import resultsview.storage.Storage;
import java.util.regex.Pattern;
import resultsview.storage.ConcurrentStorage;

public class TestJenkinPoller {

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
        List<String> lines = new ArrayList<>();
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

    void writeBuildXml(Path dir, String nvr, String result) throws IOException {
        Path buildxml = dir.resolve("build.xml");
        List<String> lines = new ArrayList<>();
        lines.add("<?xml version='1.1' encoding='UTF-8'?>");
        lines.add("<build>");
        lines.add("  <actions>");
        if (nvr != null) {
            String tmp = nvr;
            int idx = tmp.lastIndexOf("-");
            String r = tmp.substring(idx + 1);
            tmp = tmp.substring(0, idx);
            idx = tmp.lastIndexOf("-");
            String v = tmp.substring(idx + 1);
            String n = tmp.substring(0, idx);

            lines.add("    <hudson.plugins.scm.koji.KojiRevisionState plugin=\"jenkins-scm-koji-plugin@2.0-SNAPSHOT\">");
            lines.add("      <build>");
            lines.add("        <name>" + n + "</name>");
            lines.add("        <version>" + v + "</version>");
            lines.add("        <release>" + r +"</release>");
            lines.add("        <nvr>" + nvr + "</nvr>");
            lines.add("      </build>");
            lines.add("    </hudson.plugins.scm.koji.KojiRevisionState>");
        }
        lines.add("  </actions>");
        if (result != null) {
            lines.add("  <result>" + result + "</result>");
        }
        lines.add("</build>");
        Files.write(buildxml, lines, Charset.forName("UTF-8"));
        lines.clear();
    }


    void createJob(Path root, String jobName) throws IOException {
        Path job = root.resolve(jobName);
        Files.createDirectories(job);
        Path builds = job.resolve("builds");
        Files.createDirectories(builds);

        List<String> lines = new ArrayList<>();
        Path nextBuildFile = job.resolve("nextBuildNumber");
        lines.add("1");
        Files.write(nextBuildFile, lines, Charset.defaultCharset());
        lines.clear();

        writeConfigXml(job);
    }

    void createBuildBasic(Path root, String jobName, int buildId) throws IOException {
        createBuild(root, jobName, buildId, null, null);
    }

    void createBuild(Path root, String jobName, int buildId, String nvr, String result) throws IOException {
        Path job = root.resolve(jobName);
        Path builds = job.resolve("builds");

        List<String> lines = new ArrayList<>();
        Path nextBuildFile = job.resolve("nextBuildNumber");
        Files.delete(nextBuildFile);
        lines.add(Integer.toString(buildId + 1));
        Files.write(nextBuildFile, lines, Charset.defaultCharset());
        lines.clear();

        Path buildDir = builds.resolve(Integer.toString(buildId));
        Files.createDirectories(buildDir);

        writeBuildXml(buildDir, nvr, result);
    }

    void prepareFakeJenkins(Path root) throws IOException {
        Files.createDirectories(root);
        createJob(root, "job1");

        createJob(root, "job2");
        createBuildBasic(root, "job2", 1);

        createJob(root, "job3");
        createBuild(root, "job3", 1, "pkg-1-1", "SUCCESS");
        createBuild(root, "job3", 2, "pkg-1-2", "FAILURE");

        createJob(root, "job4");
        createBuild(root, "job4", 1, "pkg-1-2", "SUCCESS");
        createBuild(root, "job4", 2, "pkg-1-3", "FAILURE");
        createBuild(root, "job4", 3, null, null);
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
    public void checkInitialScan() throws Exception  {
        Storage storage = new ConcurrentStorage();
        JenkinsPoller jenkins = new JenkinsPoller(jenkinsJobs, storage);
        jenkins.poll();

        Collection<Job> jobs = storage.getJobs();
        Assert.assertEquals("Correct number of jobs", 4, jobs.size());
        Job job1 = storage.getJob("job1");
        Assert.assertNotNull("job1 exists", job1);
        Job job2 = storage.getJob("job2");
        Assert.assertNotNull("job2 exists", job2);
        Job job3 = storage.getJob("job3");
        Assert.assertNotNull("job3 exists", job3);
        Job job4 = storage.getJob("job4");
        Assert.assertNotNull("job4 exists", job4);

        Collection<Run> runs1 = storage.getJobRuns(job1);
        Assert.assertEquals("Correct number of runs", 0, runs1.size());
        Collection<Run> runs2 = storage.getJobRuns(job2);
        Assert.assertEquals("Correct number of runs", 1, runs2.size());
        Collection<Run> runs3 = storage.getJobRuns(job3);
        Assert.assertEquals("Correct number of runs", 2, runs3.size());
        Collection<Run> runs4 = storage.getJobRuns(job4);
        Assert.assertEquals("Correct number of runs", 3, runs4.size());

        Collection<Pkg> pkgs = storage.getPkgs();
        Assert.assertEquals("Correct number of pkgs", 3, pkgs.size());
        Pkg pkg1 = storage.getPkg("pkg-1-1");
        Pkg pkg2 = storage.getPkg("pkg-1-2");
        Pkg pkg3 = storage.getPkg("pkg-1-3");

        Collection<Run> runsPkg1 = storage.getPkgRuns(pkg1);
        Assert.assertEquals("Correct number of runs for pkg1", 1, runsPkg1.size());
        Collection<Run> runsPkg2 = storage.getPkgRuns(pkg2);
        Assert.assertEquals("Correct number of runs for pkg2", 2, runsPkg2.size());
        Collection<Run> runsPkg3 = storage.getPkgRuns(pkg3);
        Assert.assertEquals("Correct number of runs for pkg3", 1, runsPkg3.size());
    }

    @Test
    public void checkNoChanges() throws Exception {
        TestStorage storage = new TestStorage();
        JenkinsPoller jenkins = new JenkinsPoller(jenkinsJobs, storage);
        jenkins.poll();
        Assert.assertTrue("More than zero new jobs", storage.jobctr > 0);
        Assert.assertTrue("More than zero new runs", storage.runctr > 0);
        Assert.assertTrue("More than zero new pkgs", storage.pkgctr > 0);
        Assert.assertTrue("More than zero new pkgsRuns", storage.pkgRunCtr > 0);
        Assert.assertEquals("Zero removed jobs", 0, storage.rmJobCtr);
        Set<Run> runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 2, runningSet.size());
        Thread.sleep(10);


        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.poll();
        runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 2, runningSet.size());
        Assert.assertEquals("Zero new jobs", 0, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("Zero new pkgsRuns", 0, storage.pkgRunCtr);
        Assert.assertEquals("Zero removed jobs", 0, storage.rmJobCtr);
        Thread.sleep(10);

        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.rootModifTime = Long.MIN_VALUE;
        jenkins.poll();
        runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 2, runningSet.size());
        Assert.assertEquals("Zero new jobs", 0, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("Zero new pkgsRuns", 0, storage.pkgRunCtr);
        Assert.assertEquals("Zero removed jobs", 0, storage.rmJobCtr);
    }


    @Test
    public void checkJobsFinished() throws Exception {
        TestStorage storage = new TestStorage();
        JenkinsPoller jenkins = new JenkinsPoller(jenkinsJobs, storage);
        jenkins.poll();
        Assert.assertTrue("More than zero new jobs", storage.jobctr > 0);
        Assert.assertTrue("More than zero new runs", storage.runctr > 0);
        Assert.assertTrue("More than zero new pkgs", storage.pkgctr > 0);
        Assert.assertTrue("More than zero new pkgsRuns", storage.pkgRunCtr > 0);
        Assert.assertEquals("Zero removed jobs", 0, storage.rmJobCtr);
        Set<Run> runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 2, runningSet.size());
        Thread.sleep(10);

        createBuild(jenkinsJobs, "job4", 3, "pkg-1-3", "SUCCESS");
        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.poll();
        Assert.assertEquals("Correct number of runs in running set", 1, runningSet.size());
        Assert.assertEquals("Zero new jobs", 0, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("One new pkgsRuns", 1, storage.pkgRunCtr);
        Assert.assertEquals("Zero removed jobs", 0, storage.rmJobCtr);
        Pkg pkg3 = storage.getPkg("pkg-1-3");
        Collection<Run> runsPkg3 = storage.getPkgRuns(pkg3);
        Assert.assertEquals("Correct number of runs for pkg3", 2, runsPkg3.size());
        Thread.sleep(10);

        createBuild(jenkinsJobs, "job2", 1, "pkg-1-3", "SUCCESS");
        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.poll();
        Assert.assertEquals("Correct number of runs in running set", 0, runningSet.size());
        Assert.assertEquals("Zero new jobs", 0, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("One new pkgsRuns", 1, storage.pkgRunCtr);
        Assert.assertEquals("Zero removed jobs", 0, storage.rmJobCtr);
        pkg3 = storage.getPkg("pkg-1-3");
        runsPkg3 = storage.getPkgRuns(pkg3);
        Assert.assertEquals("Correct number of runs for pkg3", 3, runsPkg3.size());
    }

    @Test
    public void checkJobsRemoved() throws Exception {
        TestStorage storage = new TestStorage();
        JenkinsPoller jenkins = new JenkinsPoller(jenkinsJobs, storage);
        jenkins.poll();
        Assert.assertTrue("More than zero new jobs", storage.jobctr > 0);
        Assert.assertTrue("More than zero new runs", storage.runctr > 0);
        Assert.assertTrue("More than zero new pkgs", storage.pkgctr > 0);
        Assert.assertTrue("More than zero new pkgsRuns", storage.pkgRunCtr > 0);
        Assert.assertEquals("Zero removed jobs", 0, storage.rmJobCtr);
        Set<Run> runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 2, runningSet.size());
        Collection<Job> jobs = storage.getJobs();
        Assert.assertEquals("Correct number of jobs", 4, jobs.size());
        Pkg pkg1 = storage.getPkg("pkg-1-1");
        Pkg pkg2 = storage.getPkg("pkg-1-2");
        Pkg pkg3 = storage.getPkg("pkg-1-3");
        Thread.sleep(10);

        recursiveDelete(jenkinsJobs.resolve("job1"));
        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.poll();
        Assert.assertEquals("Zero new jobs", 0, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("One new pkgsRuns", 0, storage.pkgRunCtr);
        Assert.assertEquals("Removed job", 1, storage.rmJobCtr);
        jobs = storage.getJobs();
        Assert.assertEquals("Correct number of jobs", 3, jobs.size());
        runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 2, runningSet.size());
        Collection<Run> runsPkg1 = storage.getPkgRuns(pkg1);
        Assert.assertEquals("Correct number of runs for pkg1", 1, runsPkg1.size());
        Collection<Run> runsPkg2 = storage.getPkgRuns(pkg2);
        Assert.assertEquals("Correct number of runs for pkg2", 2, runsPkg2.size());
        Collection<Run> runsPkg3 = storage.getPkgRuns(pkg3);
        Assert.assertEquals("Correct number of runs for pkg3", 1, runsPkg3.size());
        Thread.sleep(10);

        recursiveDelete(jenkinsJobs.resolve("job2"));
        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.poll();
        Assert.assertEquals("Zero new jobs", 0, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("One new pkgsRuns", 0, storage.pkgRunCtr);
        Assert.assertEquals("Removed job", 1, storage.rmJobCtr);
        jobs = storage.getJobs();
        Assert.assertEquals("Correct number of jobs", 2, jobs.size());
        runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 1, runningSet.size());
        runsPkg1 = storage.getPkgRuns(pkg1);
        Assert.assertEquals("Correct number of runs for pkg1", 1, runsPkg1.size());
        runsPkg2 = storage.getPkgRuns(pkg2);
        Assert.assertEquals("Correct number of runs for pkg2", 2, runsPkg2.size());
        runsPkg3 = storage.getPkgRuns(pkg3);
        Assert.assertEquals("Correct number of runs for pkg3", 1, runsPkg3.size());
        Thread.sleep(10);

        recursiveDelete(jenkinsJobs.resolve("job3"));
        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.poll();
        Assert.assertEquals("Zero new jobs", 0, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("One new pkgsRuns", 0, storage.pkgRunCtr);
        Assert.assertEquals("Removed job", 1, storage.rmJobCtr);
        jobs = storage.getJobs();
        Assert.assertEquals("Correct number of jobs", 1, jobs.size());
        runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 1, runningSet.size());
        runsPkg1 = storage.getPkgRuns(pkg1);
        Assert.assertEquals("Correct number of runs for pkg1", 0, runsPkg1.size());
        runsPkg2 = storage.getPkgRuns(pkg2);
        Assert.assertEquals("Correct number of runs for pkg2", 1, runsPkg2.size());
        runsPkg3 = storage.getPkgRuns(pkg3);
        Assert.assertEquals("Correct number of runs for pkg3", 1, runsPkg3.size());
        Thread.sleep(10);

        recursiveDelete(jenkinsJobs.resolve("job4"));
        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.poll();
        Assert.assertEquals("Zero new jobs", 0, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("One new pkgsRuns", 0, storage.pkgRunCtr);
        Assert.assertEquals("Removed job", 1, storage.rmJobCtr);
        jobs = storage.getJobs();
        Assert.assertEquals("Correct number of jobs", 0, jobs.size());
        runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 0, runningSet.size());
        runsPkg1 = storage.getPkgRuns(pkg1);
        Assert.assertEquals("Correct number of runs for pkg1", 0, runsPkg1.size());
        runsPkg2 = storage.getPkgRuns(pkg2);
        Assert.assertEquals("Correct number of runs for pkg2", 0, runsPkg2.size());
        runsPkg3 = storage.getPkgRuns(pkg3);
        Assert.assertEquals("Correct number of runs for pkg3", 0, runsPkg3.size());
    }

    @Test
    public void checkJobsAdded() throws Exception {
        TestStorage storage = new TestStorage();
        JenkinsPoller jenkins = new JenkinsPoller(jenkinsJobs, storage);
        jenkins.poll();
        Assert.assertTrue("More than zero new jobs", storage.jobctr > 0);
        Assert.assertTrue("More than zero new runs", storage.runctr > 0);
        Assert.assertTrue("More than zero new pkgs", storage.pkgctr > 0);
        Assert.assertTrue("More than zero new pkgsRuns", storage.pkgRunCtr > 0);
        Assert.assertEquals("Zero removed jobs", 0, storage.rmJobCtr);
        Set<Run> runningSet = jenkins.runningSet;
        Assert.assertEquals("Correct number of runs in running set", 2, runningSet.size());
        Collection<Job> jobs = storage.getJobs();
        Assert.assertEquals("Correct number of jobs", 4, jobs.size());
        Thread.sleep(10);

        createJob(jenkinsJobs, "job5");
        storage.jobctr = 0;
        storage.runctr = 0;
        storage.pkgctr = 0;
        storage.pkgRunCtr = 0;
        storage.rmJobCtr = 0;
        jenkins.poll();
        Assert.assertEquals("new job", 1, storage.jobctr);
        Assert.assertEquals("Zero new runs", 0, storage.runctr);
        Assert.assertEquals("Zero new pkgs", 0, storage.pkgctr);
        Assert.assertEquals("One new pkgsRuns", 0, storage.pkgRunCtr);
        Assert.assertEquals("Removed job", 0, storage.rmJobCtr);
    }

    //@Test
    public void poolTest() throws Exception {
        PrintStorage storage = new PrintStorage();
        Path hydraJobs = tmpDir.resolve("/mnt/hydra-mnt/raid/jobs");

        JenkinsPoller jenkins = new JenkinsPoller(hydraJobs, storage);
        jenkins.jobPattern = Pattern.compile("rhqe-jp8-ojdk8~rpms-el8z.ppc64le.*");
        System.out.println("poll 1");
        jenkins.poll();
        System.out.println("poll 2");
        jenkins.poll();
        System.out.println("done");
    }




    static class TestStorage extends ConcurrentStorage {

        int jobctr = 0;
        int runctr = 0;
        int pkgctr = 0;
        int pkgRunCtr = 0;
        int rmJobCtr = 0;

        @Override
        public void storeJob(Job job) {
            jobctr++;
            super.storeJob(job);
        }

        @Override
        public void storeRun(Run run) {
            runctr++;
            super.storeRun(run);
        }

        @Override
        public void storePkg(Pkg pkg) {
            pkgctr++;
            super.storePkg(pkg);
        }

        @Override
        public void addPkgRun(Pkg pkg, Run run) {
            pkgRunCtr++;
            super.addPkgRun(pkg, run);
        }

        public void removeJob(String name) {
            rmJobCtr++;
            super.removeJob(name);
        }


    }


    static class PrintStorage extends Storage {

        @Override
        public void storeJob(Job job) {
            System.out.println("storeJob: "+ job.getName());
            super.storeJob(job);
        }

        @Override
        public void storeRun(Run run) {
            System.out.println("storeRun: "+ run.getName() + " " + run.getStatus());
            super.storeRun(run);
        }

        @Override
        public void storePkg(Pkg pkg) {
            System.out.println("storePkg: "+ pkg.getStrId());
            super.storePkg(pkg);
        }

        @Override
        public void addPkgRun(Pkg pkg, Run run) {
            System.out.println("addPkgRun: " + pkg.getStrId() + " " + run.getName());
            super.addPkgRun(pkg, run);
        }

    }

}

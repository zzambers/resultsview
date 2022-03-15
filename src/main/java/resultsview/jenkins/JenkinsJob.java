/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resultsview.jenkins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import resultsview.common.VersionUtil;

/**
 *
 * @author zzambers
 */
public class JenkinsJob implements Comparable<JenkinsJob>{

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    private final JenkinsJobsRoot jobs;
    private final String name;

    public JenkinsJob(JenkinsJobsRoot root, String name) {
        this.jobs = root;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public File getJobDir() {
        return new File(jobs.getJobsRootDir(), name);
    }

    public File getBuildsDir() {
        return new File(getJobDir(), "builds");
    }

    public File configXmlFile() {
        return new File(getJobDir(), "config.xml");
    }

    public JenkinsBuild getBuild(String buildNumber) {
        return new JenkinsBuild(this, buildNumber);
    }

    public List<JenkinsBuild> listBuilds() {
        List<JenkinsBuild> builds = new ArrayList();
        File buildsDir = getBuildsDir();
        if (buildsDir != null && buildsDir.isDirectory()) {
            for (String fileName : buildsDir.list()) {
                if (NUMBER_PATTERN.matcher(fileName).matches()) {
                    JenkinsBuild build = getBuild(fileName);
                    if (build != null) {
                        builds.add(build);
                    }
                }
            }
        }
        Collections.sort(builds);
        return builds;
    }

    private String getNextBuildNumber() throws IOException {
        File jobDir = getJobDir();
        File nextBuildFile = new File(jobDir, "nextBuildNumber");
        List<String> lines = Files.readAllLines(nextBuildFile.toPath());
        if (lines != null && !lines.isEmpty() && NUMBER_PATTERN.matcher(lines.get(0)).matches()) {
            return lines.get(0);
        }
        return null;
    }

    public JenkinsBuild getLastBuild() throws IOException {
        String nextBuildNumber = getNextBuildNumber();
        if (nextBuildNumber != null) {
            long lastBuildNumber = Long.parseLong(nextBuildNumber) - 1;
            if (lastBuildNumber > 0) {
                return getBuild(nextBuildNumber);
            }
        }
        return null;
    }

    @Override
    public int compareTo(JenkinsJob j) {
        return VersionUtil.versionCompare(this.getName(), j.getName());
    }

}

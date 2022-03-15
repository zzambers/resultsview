/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resultsview.jenkins;

import java.io.File;
import resultsview.common.VersionUtil;

/**
 *
 * @author zzambers
 */
public class JenkinsBuild implements Comparable<JenkinsBuild> {

    private final JenkinsJob job;
    private final String id;

    public JenkinsBuild(JenkinsJob job, String id) {
        this.job = job;
        this.id = id;
    }

    public JenkinsJob getJob() {
        return this.job;
    }

    public String getId() {
        return id;
    }

    public File getBuildDir() {
        return new File(job.getBuildsDir(), id);
    }

    public File getArchiveDir() {
        return new File(getBuildDir(), "archive");
    }

    public File getBuildXmlFile() {
        return new File(getBuildDir(), "build.xml");
    }

    @Override
    public int compareTo(JenkinsBuild b2) {
        return VersionUtil.versionCompare(this.getId(), b2.getId());
    }

}

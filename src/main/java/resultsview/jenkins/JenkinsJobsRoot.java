/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package resultsview.jenkins;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author zzambers
 */
public class JenkinsJobsRoot {

    private final File rootDir;

    public JenkinsJobsRoot(String rootDir) {
        this.rootDir = new File(rootDir);
    }

    public JenkinsJob getJob(String jobName) {
        File jobDir = new File(rootDir, jobName);
        if (jobDir.isDirectory()) {
            File jobConfig = new File(jobDir, "config.xml");
            if (jobConfig.exists()) {
                return new JenkinsJob(this, jobName);
            }
        }
        return null;
    }

    public List<JenkinsJob> listJobs() throws Exception {
        List<JenkinsJob> jobs = new ArrayList();
        String[] jobDirs = rootDir.list();
        for (String jobDirName : jobDirs) {
            JenkinsJob job = getJob(jobDirName);
            if (job != null) {
                jobs.add(job);
            }
        }
        Collections.sort(jobs);
        return jobs;
    }

    public File getJobsRootDir() {
        return rootDir;
    }

}

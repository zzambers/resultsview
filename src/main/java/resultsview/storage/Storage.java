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
package resultsview.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.Collections;

public class Storage implements StorageInterface {

    Map<String, Job> jobs = new HashMap<>();
    Map<Job, Set<Run>> jobsRuns = new HashMap<>();

    Map<String, Pkg> pkgs = new HashMap<>();
    Map<Pkg, Set<Run>> pkgsRuns = new HashMap<>();
    //Map<Job, Map<String, Run>> jobsRuns = new HashMap();

    @Override
    public Job getJob(String name) {
        return jobs.get(name);
    }

    @Override
    public Pkg getPkg(String name) {
        return pkgs.get(name);
    }

    @Override
    public Collection<Job> getJobs() {
        return new ArrayList<>(jobs.values());
    }

    @Override
    public Collection<Pkg> getPkgs() {
        return new ArrayList<>(pkgs.values());
    }

    @Override
    public Collection<Run> getJobRuns(Job job) {
        HashSet<Run> runs = (HashSet<Run>) jobsRuns.get(job);
        return runs != null ? new HashSet<Run>(runs) : Collections.<Run>emptySet();
    }

    @Override
    public Collection<Run> getPkgRuns(Pkg pkg) {
        HashSet<Run> runs = (HashSet<Run>) pkgsRuns.get(pkg);
        return runs != null ? new HashSet<Run>(runs) : Collections.<Run>emptySet();
    }

    @Override
    public void removeJob(String name) {
        Job removedJob = jobs.remove(name);
        Set<Run> removedRuns = jobsRuns.remove(removedJob);
        if (removedRuns != null) {
            for (Run run : removedRuns) {
                for (Pkg pkg : pkgs.values()) {
                    Set<Run> pkgRuns = pkgsRuns.get(pkg);
                    if (pkgRuns != null) {
                        pkgRuns.remove(run);
                    }
                }
            }
        }
    }

    @Override
    public void storeJob(Job job) {
        String name = job.getName();
        jobs.put(name, job);
    }

    @Override
    public void storePkg(Pkg pkg) {
        String name = pkg.getStrId();
        pkgs.put(name, pkg);
    }

    @Override
    public void storeRun(Run run) {
        Job job = run.getJob();
        Set<Run> runs = jobsRuns.get(job);
        if (runs == null) {
            runs = new HashSet<>();
            jobsRuns.put(job, runs);
        }
        if (!runs.contains(run)) {
            runs.add(run);
        }
    }

    @Override
    public void addPkgRun(Pkg pkg, Run run) {
        Set<Run> runs = pkgsRuns.get(pkg);
        if (runs == null) {
            runs = new HashSet<>();
            pkgsRuns.put(pkg, runs);
        }
        if (!runs.contains(run)) {
            runs.add(run);
        }
    }

}

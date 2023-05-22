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

import java.util.Collection;

public interface StorageInterface {

    public Job getJob(String name);

    public Pkg getPkg(String name);

    public Collection<Job> getJobs();

    public Collection<Pkg> getPkgs();

    public Collection<Run> getJobRuns(Job job);

    public Collection<Run> getPkgRuns(Pkg pkg);

    public int getPkgRunsCount(Pkg pkg);

    public void removeJob(String name);

    public void storeJob(Job job);

    public void storePkg(Pkg pkg);

    public void storeRun(Run run);

    public void addPkgRun(Pkg pkg, Run run);

    public Run getJobLatestRun(Job job);

    public void setJobLatestRun(Job job, Run run);

    public void addUnfinishedRun(Run run);

    public void removeUnfinishedRun(Run run);

    public Collection<Run> getUnfinishedRuns();
}

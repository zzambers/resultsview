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

import resultsview.common.VersionUtil;

public class Run  implements Comparable<Run> {

    final Job job;
    final String name;
    int status;
    public long modifTime = Long.MIN_VALUE;

    public static final int UNKNOWN = 0;
    public static final int RUNNING = 1;
    public static final int FINISHED = 2;
    public static final int SUCCESS = 3;
    public static final int UNSTABLE = 4;
    public static final int FAILURE = 5;
    public static final int ABORTED = 6;
    public static final int NOT_BUILT = 7;

    public Run(Job job, String name) {
        this.job = job;
        this.name = name;
    }

    public Job getJob() {
        return job;
    }

    public String getName() {
        return name;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isFinished() {
        switch(status) {
            case FINISHED:
            case SUCCESS:
            case UNSTABLE:
            case FAILURE:
            case ABORTED:
            case NOT_BUILT:
                return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (getClass() != o.getClass()) {
            return false;
        }
        Run o1 = (Run) o;
        if (!job.equals(o1.job)) {
            return false;
        }
        if (!name.equals(o1.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 7 * job.hashCode() + name.hashCode();
    }

    @Override
    public int compareTo(Run t) {
        int res = job.compareTo(t.job);
        return res != 0 ? res : VersionUtil.versionCompare(this.name, t.name);
    }

}

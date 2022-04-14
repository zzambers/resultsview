package resultsview.storage;

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
/**
 *
 * @author zzambers
 */
public class Run  implements Comparable<Run> {

    /* sql fields */
    final Job job;
    final String name;
    int status;

    public static final int UNKNOWN = 0;
    public static final int RUNNING = 1;
    public static final int FINISHED = 2;
    public static final int PASSED = 3;
    public static final int FAILED = 4;
    public static final int ERROR = 5;
    public static final int CANCELED = 6;

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
        return res != 0 ? res : name.compareTo(t.name);
    }

}

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
package resultsview.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import resultsview.plugins.jenkins.JenkinsDiscovery;
import resultsview.storage.Pkg;
import resultsview.storage.Run;
import resultsview.storage.Storage;

/**
 *
 * @author zzambers
 */
public class Runs extends HttpServlet {

    private final Worker worker = new Worker();
    private final Storage storage = new Storage();
    private final JenkinsDiscovery jenkinsPoller = new JenkinsDiscovery(Paths.get("/mnt/hydra-mnt/raid/jobs"), storage);
    private final Timer timer = new Timer();
    private volatile boolean initialPollDone = false;

    @Override
    public void init() {
        worker.start();
        // jenkinsPoller.jobPattern = Pattern.compile("rhqe-jp8-ojdk8~rpms-el8z.ppc64le.*");
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    worker.scheduleTaskAndWait(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                jenkinsPoller.poll();
                                initialPollDone = true;
                            } catch (Exception ex) {
                                Logger.getLogger(Runs.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    });
                } catch (Exception ex) {
                    Logger.getLogger(Runs.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, 0, 60_000);
    }

    @Override
    public void destroy() {
        timer.cancel();
        worker.shutdown();
    }

    private void printRunTable(PrintWriter out, String pkgName, String regex) {
        if (pkgName == null || pkgName.isEmpty()) {
            return;
        }  
        Pkg pkg = storage.getPkg(pkgName);
        if (pkg == null) {
            out.println("Package not found: " + pkgName);
            return;
        }
        List<Run> runs = new ArrayList<Run>(storage.getPkgRuns(pkg));
        Collections.sort(runs);
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Result</th>");
        out.println("<th>Name</th>");
        out.println("</tr>");
        Pattern p = (regex == null || regex.isEmpty()) ? null : Pattern.compile(regex);
        for (Run run : runs) {
            String jobName = run.getJob().getName();
            String runName = run.getName();
            String fullName = jobName + "/" + runName;
            if (p != null && !p.matcher(jobName).matches() && !p.matcher(fullName).matches()) {
                continue;
            }
            int status = run.getStatus();
            out.println("<tr>");
            out.println("<td>");
            switch (status) {
                case Run.RUNNING:
                    out.println("RUNNING");
                    break;
                case Run.PASSED:
                    out.println("PASSED");
                    break;
                case Run.FAILED:
                    out.println("FAILED");
                    break;
                case Run.ERROR:
                    out.println("ERROR");
                    break;
                case Run.CANCELED:
                    out.println("CANCELED");
                    break;
                case Run.FINISHED:
                    out.println("FINISHED");
                    break;
                case Run.UNKNOWN:
                    out.println("UNKNOẄN");
                    break;
            }
            out.println("</td>");
            out.println("<td>");
            out.println("<a href=\"http://localhost:8080/job/" + fullName + "\">");
            out.println(fullName);
            out.println("</a>");
            out.println("</td>");
            out.println("</tr>");
        }
        out.println("</table>");
    }

    class Worker extends Thread {

        final Object lock = new Object();
        final LinkedList<Task> tasks = new LinkedList();
        private boolean running = true;

        @Override
        public void run() {
            try {
                mainLoop();
            } catch (InterruptedException ex) {
                Logger.getLogger(Runs.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void mainLoop() throws InterruptedException {
            for (;;) {
                Task next;
                synchronized (lock) {
                    while (running && tasks.isEmpty()) {
                        lock.wait();
                    }
                    if (!running) {
                        break;
                    }
                    next = tasks.removeFirst();
                }
                next.run();
            }
        }

        public void shutdown() {
            synchronized (lock) {
                running = false;
                for (Task task : tasks) {
                    task.cancel();
                }
                lock.notifyAll();
            }
        }

        public void scheduleTask(Runnable r, boolean wait) throws InterruptedException {
            Task task = new Task(r);
            synchronized (lock) {
                if (!running) {
                    throw new IllegalStateException("Worker shut down");
                }
                tasks.add(task);
                lock.notifyAll();
            }
            if (wait) {
                task.waitFor();
            }
        }

        public void scheduleTaskAndWait(Runnable r) throws InterruptedException {
            scheduleTask(r, true);
        }

    }

    static class Task {

        private final Runnable r;
        private static final Object lock = new Object();
        boolean finished = false;
        boolean cancelled = false;
        Throwable t = null;

        public Task(Runnable r) {
            this.r = r;
        }

        public void run() {
            try {
                r.run();
            } catch (Throwable t) {
                this.t = t;
            }
            synchronized (lock) {
                finished = true;
                lock.notifyAll();
            }
        }

        public void cancel() {
            synchronized (lock) {
                finished = true;
                cancelled = true;
                lock.notifyAll();
            }
        }

        public void waitFor() throws InterruptedException {
            synchronized (lock) {
                while (!finished) {
                    lock.wait();
                }
                if (cancelled) {
                    throw new InterruptedException("Cancelled");
                }
                if (t != null) {
                    throw new RuntimeException(t);
                }
            }
        }
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(final HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (final PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Runs</title>");
            out.println("</head>");
            out.println("<body>");
            final String pkgName = request.getParameter("pkg");
            final String pattern = request.getParameter("pattern");
            final String pkgNameVal = pkgName == null ? "" : pkgName;
            final String patternVal = pattern == null ? "" : pattern;
            out.println("<form>");
            out.println("<label for=\"pattern\">Pkg:</label>");
            out.println("<input type=\"text\" id=\"pkg\" name=\"pkg\" size=\"50\" value=\"" + pkgNameVal + "\"/>");
            out.println("<label for=\"pattern\">Pattern:</label>");
            out.println("<input type=\"text\" id=\"pattern\" name=\"pattern\" value=\"" + patternVal + "\"/>");
            out.println("<input type=\"submit\" value=\"Submit\"/>");
            out.println("<br/>");
            out.println("</form>");

            if (initialPollDone) {
                try {
                    worker.scheduleTaskAndWait(new Runnable() {
                        @Override
                        public void run() {
                            printRunTable(out, pkgNameVal, patternVal);
                        }
                    });
                } catch (InterruptedException ex) {
                    /* ignored */
                }

            } else {
                out.println("Initial poll in progress...");
            }
            out.println("</body>");
            out.println("</html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}

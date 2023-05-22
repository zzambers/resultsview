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
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import resultsview.poll.JenkinsPoller;
import resultsview.storage.ConcurrentStorage;
import resultsview.storage.Pkg;
import resultsview.storage.Run;
import resultsview.storage.Job;
import resultsview.storage.Storage;

public class ResultsView extends HttpServlet {

    private Properties props;
    private String jenkinsUrl;

    private Storage storage;
    private JenkinsPoller jenkinsPoller;
    private Timer timer;
    private volatile boolean initialPollDone = false;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        InputStream is = getServletContext().getResourceAsStream("/WEB-INF/config.properties");
        if (is == null) {
            throw new ServletException("Config file not found in: /WEB-INF/config.properties");
        }
        props = new Properties();
        try {
            props.load(is);
        } catch (Exception e) {
            throw new ServletException(e);
        }
        jenkinsUrl = props.getProperty("jenkins.url");

        String jobsDir = props.getProperty("jenkins.job.dir");
        String jobPattern = props.getProperty("jenkins.job.pattern");
        if (jobsDir == null) {
            throw new ServletException("Required property not configured: jenkins.job.dir");
        }

        storage = new ConcurrentStorage();
        jenkinsPoller = new JenkinsPoller(Paths.get(jobsDir), storage);
        if (jobPattern != null) {
            jenkinsPoller.jobPattern = Pattern.compile(jobPattern);
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    jenkinsPoller.poll();
                    initialPollDone = true;
                } catch (Exception ex) {
                    Logger.getLogger(ResultsView.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, 0, 60_000);
    }

    @Override
    public void destroy() {
        timer.cancel();
        timer = null;
        jenkinsPoller = null;
        storage = null;
        initialPollDone = false;
    }

    private String replace(String s, String p, String r) {
        if (p.length() == 1 && s.indexOf(p.charAt(0)) < 0) {
            return s;
        }
        return s.replace(p, r);
    }

    private String htmlEscape(String s) {
        s = replace(s, "&", "&amp;");
        s = replace(s, "<", "&lt;");
        s = replace(s, ">", "&gt;");
        s = replace(s, "\"", "&quot;");
        s = replace(s, "\'", "&apos;");
        return s;
    }

    private String urlEscape(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printStatus(PrintWriter out, int status) {
        switch (status) {
            case Run.RUNNING:
                out.println("<span class=\"rtxt-running\">");
                out.println("RUNNING");
                out.println("</span>");
                break;
            case Run.SUCCESS:
                out.println("<span class=\"rtxt-success\">");
                out.println("SUCCESS");
                out.println("</span>");
                break;
            case Run.UNSTABLE:
                out.println("<span class=\"rtxt-unstable\">");
                out.println("UNSTABLE");
                out.println("</span>");
                break;
            case Run.FAILURE:
                out.println("<span class=\"rtxt-failure\">");
                out.println("FAILURE");
                out.println("</span>");
                break;
            case Run.ABORTED:
                out.println("<span class=\"rtxt-aborted\">");
                out.println("ABORTED");
                out.println("</span>");
                break;
            case Run.NOT_BUILT:
                out.println("<span class=\"rtxt-aborted\">");
                out.println("NOT_BUILT");
                out.println("</span>");
                break;
            case Run.FINISHED:
                out.println("FINISHED");
                break;
            case Run.UNKNOWN:
                out.println("UNKNOWN");
                break;
        }
    }

    private void printRunTable(PrintWriter out, String pkgName, String regex) {
        if (pkgName == null || pkgName.isEmpty()) {
            return;
        }
        Pkg pkg = storage.getPkg(pkgName);
        if (pkg == null) {
            out.println("Package not found: " + htmlEscape(pkgName));
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
            if (p != null && !p.matcher(jobName).find() && !p.matcher(fullName).find()) {
                continue;
            }
            int status = run.getStatus();
            out.println("<tr>");
            out.println("<td>");
            printStatus(out, status);
            out.println("</td>");
            out.println("<td>");
            out.println("<a href=\"" + jenkinsUrl + "/job/" + urlEscape(jobName) + "/" + urlEscape(runName) + "\">");
            out.println(htmlEscape(fullName));
            out.println("</a>");
            out.println("</td>");
            out.println("</tr>");
        }
        out.println("</table>");
    }

    private void printRuns(HttpServletRequest request, PrintWriter out) {
            final String pkgName = request.getParameter("pkg");
            final String pattern = request.getParameter("pattern");
            final String pkgNameVal = pkgName == null ? "" : pkgName.trim();
            final String patternVal = pattern == null ? "" : pattern;
            out.println("<form class=\"filter-form\">");
            out.println("<label for=\"pattern\">Pkg:</label>");
            out.println("<input type=\"text\" id=\"pkg\" name=\"pkg\" size=\"50\" value=\"" + htmlEscape(pkgNameVal) + "\"/>");
            out.println("<label for=\"pattern\">Pattern:</label>");
            out.println("<input type=\"text\" id=\"pattern\" name=\"pattern\" value=\"" + htmlEscape(patternVal) + "\"/>");
            out.println("<input type=\"submit\" value=\"Submit\"/>");
            out.println("<br/>");
            out.println("</form>");
            printRunTable(out, pkgNameVal, patternVal);
    }

    private void printJobs(HttpServletRequest request, PrintWriter out) {
        final String pattern = request.getParameter("pattern");
        final String patternVal = pattern == null ? "" : pattern;
        out.println("<form class=\"filter-form\">");
        out.println("<label for=\"pattern\">Pattern:</label>");
        out.println("<input type=\"text\" id=\"pattern\" name=\"pattern\" value=\"" + htmlEscape(patternVal) + "\"/>");
        out.println("<input type=\"submit\" value=\"Submit\"/>");
        out.println("<br/>");
        out.println("</form>");
        List<Job> jobs = new ArrayList<Job>(storage.getJobs());
        Collections.sort(jobs);
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Last Result</th>");
        out.println("<th>Name</th>");
        out.println("</tr>");
        String regex = patternVal;
        Pattern p = (regex == null || regex.isEmpty()) ? null : Pattern.compile(regex);
        for (Job job : jobs) {
            String jobName = job.getName();
            if (p != null && !p.matcher(jobName).find()) {
                continue;
            }
            out.println("<tr>");
            out.println("<td>");
            Run run = storage.getJobLatestRun(job);
            if (run != null && run.isFinished()) {
                printStatus(out, run.getStatus());
            } else {
                List<Run> runs = new ArrayList<Run>(storage.getJobRuns(job));
                int runIdx = runs.size() - 1;
                if (runIdx >= 0) {
                    Collections.sort(runs);
                    Run lastrun = runs.get(runIdx);
                    if (!lastrun.isFinished()) {
                        if (runIdx > 0) {
                            --runIdx;
                        }
                        lastrun = runs.get(runIdx);
                    }
                    printStatus(out, lastrun.getStatus());
                    out.println("&gt;");
                }
            }
            out.println("</td>");
            out.println("<td>");
            out.println("<a href=\"" + jenkinsUrl + "/job/" + urlEscape(jobName) + "\">");
            out.println(htmlEscape(jobName));
            out.println("</a>");
            out.println("</td>");
            out.println("</tr>");
        }
        out.println("</table>");
    }

    private void printPkgs(HttpServletRequest request, PrintWriter out) {
        final String pattern = request.getParameter("pattern");
        final String patternVal = pattern == null ? "" : pattern;
        out.println("<form class=\"filter-form\">");
        out.println("<label for=\"pattern\">Pattern:</label>");
        out.println("<input type=\"text\" id=\"pattern\" name=\"pattern\" value=\"" + htmlEscape(patternVal) + "\"/>");
        out.println("<input type=\"submit\" value=\"Submit\"/>");
        out.println("<br/>");
        out.println("</form>");
        List<Pkg> pkgs = new ArrayList<Pkg>(storage.getPkgs());
        Collections.sort(pkgs);
        out.println("<table>");
        out.println("<tr>");
        out.println("<th>Pkg</th>");
        out.println("<th>Runs</th>");
        out.println("</tr>");
        String regex = patternVal;
        Pattern p = (regex == null || regex.isEmpty()) ? null : Pattern.compile(regex);
        for (Pkg pkg : pkgs) {
            String pkgName = pkg.getStrId();
            if (p != null && !p.matcher(pkgName).find()) {
                continue;
            }
            out.println("<tr>");
            out.println("<td>");
            out.println(htmlEscape(pkgName));
            out.println("</td>");
            out.println("<td>");
            out.println("<a href=\"runs?pkg=" + urlEscape(pkgName) + "\">");
            out.println(storage.getPkgRunsCount(pkg));
            out.println("</a>");
            out.println("</td>");
            out.println("</tr>");
        }
        out.println("</table>");
    }

    void printPageTab(PrintWriter out, String name, boolean selected) {
        out.println("<a href=\"" + name + "\">"
            + (selected ? "<b>" : "")
            + name
            + (selected ? "</b>" : "")
            + "</a>");
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
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<meta charset=\"UTF-8\">");
            out.println("<title>ResultsView</title>");
            out.println("<style>");
            /* https://stackoverflow.com/a/14776179 */
            out.println("html { display: table; margin: auto; }");
            out.println("body { display: table-cell; vertical-align: middle; }");
            out.println("table { border: 1px solid; border-collapse: collapse; }");
            out.println("th, td { border: 1px solid; padding-left: 1em ; padding-right: 1em ; }");
            out.println("a { text-decoration: none; }");
            out.println(".filter-form { padding-top: 0.5em; padding-bottom: 0.5em; }");
            out.println(".rtxt-success { color: green; }");
            out.println(".rtxt-unstable { color: orangered; }");
            out.println(".rtxt-failure { color: purple; }");
            out.println(".rtxt-aborted { color: gray; }");
            out.println(".rtxt-running { color: blue; }");
            out.println("</style>");
            out.println("</head>");
            out.println("<body>");
            out.println("<div>");
            out.println("<div style=\"text-align: center; background-color: Silver;\">");
            String servletPath = request.getServletPath();
            printPageTab(out, "pkgs", servletPath.equals("/pkgs"));
            printPageTab(out, "jobs", servletPath.equals("/jobs"));
            printPageTab(out, "runs", servletPath.equals("/runs"));
            out.println("</div>");
            if (!initialPollDone) {
                out.println("Initial poll in progress...");
            }
            switch (servletPath) {
                case "/jobs":
                    printJobs(request, out);
                    break;
                case "/runs":
                    printRuns(request, out);
                    break;
                case "/pkgs":
                    printPkgs(request, out);
                    break;
                default:
                    out.println("Unexpected servlet path: " + servletPath);
                    break;
            }
            out.println("<br/>");
            out.println("</div>");
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

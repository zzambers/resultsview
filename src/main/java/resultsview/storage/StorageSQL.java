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
package resultsview.storage;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author zzambers
 */
public class StorageSQL implements StorageInterface {

    public void init() {

    }

    public static boolean tableExists(Connection conn, String name) throws Exception {
        String nameUpper = name.toUpperCase();
        DatabaseMetaData dbmd = conn.getMetaData();
        try (ResultSet rs = dbmd.getTables(null, null, nameUpper, null)) {
            if (rs.next()) {
                if (rs.getString("TABLE_NAME").equals(nameUpper)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void createDeployementsTable(Connection conn) throws Exception {
        if (tableExists(conn, "deployments")) {
            return;
        }
        String query = "CREATE TABLE deployments("
                + "depl_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "depl_config_id varchar(255) NOT NULL,"
                + "PRIMARY KEY (depl_id),"
                + "UNIQUE (depl_config_id)"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        }
    }

    public static void createJobsTable(Connection conn) throws Exception {
        if (tableExists(conn, "jobs")) {
            return;
        }
        String query = "CREATE TABLE jobs( "
                + "job_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "job_depl_id bigint NOT NULL,"
                + "job_strid varchar(255) NOT NULL,"
                + "job_name varchar(255) NOT NULL,"
                + "PRIMARY KEY (job_id),"
                + "FOREIGN KEY (job_depl_id) REFERENCES deployments(depl_id),"
                + "UNIQUE (job_strid)"
                + ")";
        // + "CONSTRAINT primary_key PRIMARY KEY (job_id),"
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        }
    }

    public static void createRunsTable(Connection conn) throws Exception {
        if (tableExists(conn, "runs")) {
            return;
        }
        String query = "CREATE TABLE runs( "
                + "run_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "run_job_id bigint NOT NULL,"
                + "run_strid varchar(255) NOT NULL,"
                + "run_name varchar(255) NOT NULL,"
                + "run_status int NOT NULL,"
                + "PRIMARY KEY (run_id),"
                + "FOREIGN KEY (run_job_id) REFERENCES jobs(job_id),"
                + "UNIQUE (run_strid)"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        }
    }

    public static void createPkgsTable(Connection conn) throws Exception {
        if (tableExists(conn, "pkgs")) {
            return;
        }
        String query = "CREATE TABLE pkgs( "
                + "pkg_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "pkg_strid varchar(255) NOT NULL,"
                + "PRIMARY KEY (pkg_id),"
                + "UNIQUE (pkg_strid)"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        }
    }

    public static void createJobsPkgsMapTable(Connection conn) throws Exception {
        if (tableExists(conn, "jobs_pkgs_map")) {
            return;
        }
        String query = "CREATE TABLE runs_pkgs_map( "
                + "runpkg_id bigint NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),"
                + "runpkg_run_id bigint NOT NULL,"
                + "runpkg_pkg_id bigint NOT NULL,"
                + "PRIMARY KEY (runpkg_id),"
                + "FOREIGN KEY (runpkg_job_id) REFERENCES runs(run_id),"
                + "FOREIGN KEY (runpkg_pkg_id) REFERENCES pkgs(pkg_id),"
                + "UNIQUE (runpkg_run_id,jobpkg_pkg_id)"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        }
    }

    /* inserts */
    public static void insertPluginConf(Connection conn, PluginConf d) throws Exception {
        String query = "insert into deployments (depl_config_id) values (?)";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            preparedStmt.setString(1, d.getConfigId());
            preparedStmt.executeUpdate();
            try (ResultSet rs = preparedStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    d.setId(id);
                }
            }
        }
    }

    public static void insertJob(Connection conn, Job job) throws Exception {
        String query = "insert into jobs (job_depl_id, job_strid, job_name) values (?, ?, ?)";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            preparedStmt.setLong(1, job.getDeployementId());
            preparedStmt.setString(2, job.getStrId());
            preparedStmt.setString(3, job.getName());
            preparedStmt.executeUpdate();
            try (ResultSet rs = preparedStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    job.setId(id);
                }
            }
        }
    }

    public static void insertRun(Connection conn, Run run) throws Exception {
        String query = "insert into runs (run_job_id, run_strid, run_name, run_status) values (?, ?, ?, ?)";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            preparedStmt.setLong(1, run.getJobId());
            preparedStmt.setString(2, run.getStrId());
            preparedStmt.setString(3, run.getName());
            preparedStmt.setInt(4, run.getStatus());
            preparedStmt.executeUpdate();
            try (ResultSet rs = preparedStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    run.setId(id);
                }
            }
        }
    }

    public static void insertPkg(Connection conn, Pkg pkg) throws Exception {
        String query = "insert into pkgs (pkg_strid) values (?)";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            preparedStmt.setString(1, pkg.getStrId());
            preparedStmt.executeUpdate();
            try (ResultSet rs = preparedStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    pkg.setId(id);
                }
            }
        }
    }

    public static void insertJobPkg(Connection conn, RunPkg jobpkg) throws Exception {
        String query = "insert into jobs_pkgs_map (jobpkg_job_id, jobpkg_pkg_id) values (?, ?)";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            preparedStmt.setLong(1, jobpkg.getRunId());
            preparedStmt.setLong(2, jobpkg.getRunId());
            preparedStmt.executeUpdate();
            try (ResultSet rs = preparedStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    Long id = rs.getLong(1);
                    jobpkg.setId(id);
                }
            }
        }
    }

    /* find by unique fields */
    public PluginConf findPluginConf(Connection conn, String configId, PluginConf pc) throws SQLException {
        String query = "select * from deployments WHERE depl_config_id = ?";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query)) {
            preparedStmt.setString(1, configId);
            try (ResultSet rs = preparedStmt.executeQuery()) {
                if (rs.next()) {
                    return getConfigRefFromRS(rs, pc);
                }
            }
        }
        return null;
    }

    public Job findJob(Connection conn, long configId, String strId, Job job) throws SQLException {
        String query = "select * from jobs WHERE job_depl_id = ? AND job_strid = ?";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query)) {
            preparedStmt.setLong(1, configId);
            preparedStmt.setString(2, strId);
            try (ResultSet rs = preparedStmt.executeQuery()) {
                if (rs.next()) {
                    return getJobFromRS(rs, job);
                }
            }
        }
        return null;
    }

    public Run findRun(Connection conn, long jobId, String strId, Run run) throws SQLException {
        String query = "select * from runs WHERE run_job_id = ? AND run_strid = ?";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query)) {
            preparedStmt.setLong(1, jobId);
            preparedStmt.setString(2, strId);
            try (ResultSet rs = preparedStmt.executeQuery()) {
                if (rs.next()) {
                    return getRunFromRS(rs, run);
                }
            }
        }
        return null;
    }

    public Pkg findPkg(Connection conn, long jobId, String strId, Pkg pkg) throws SQLException {
        String query = "select * from pkgs WHERE pkg_strid = ?";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query)) {
            preparedStmt.setString(1, strId);
            try (ResultSet rs = preparedStmt.executeQuery()) {
                if (rs.next()) {
                    return getPkgFromRS(rs, pkg);
                }
            }
        }
        return null;
    }
    
    public RunPkg findRunPkg(Connection conn, long runId, long pkgId, RunPkg jobpkg) throws SQLException {
        String query = "select * from jobs_pkgs_map WHERE jobpkg_run_id = ? AND jobpkg_pkg_id = ?";
        try (PreparedStatement preparedStmt = conn.prepareStatement(query)) {
            preparedStmt.setLong(1, runId);
            preparedStmt.setLong(2, pkgId);
            try (ResultSet rs = preparedStmt.executeQuery()) {
                if (rs.next()) {
                    return getRunPkgFromRS(rs, jobpkg);
                }
            }
        }
        return null;
    }

    /* extract from rs */
    public PluginConf getConfigRefFromRS(ResultSet rs, PluginConf d) throws SQLException {
        if (d == null) {
            d = new PluginConf(null);
        }
        d.setId(rs.getLong("depl_id"));
        d.setConfigId(rs.getString("depl_config_id"));
        return d;
    }

    public Job getJobFromRS(ResultSet rs, Job job) throws SQLException {
        if (job == null) {
            job = new Job(0L, null, null);
        }
        job.setId(rs.getLong("job_id"));
        job.setDeployementId(rs.getLong("job_depl_id"));
        job.setStrId(rs.getString("job_strid"));
        job.setName(rs.getString("job_name"));
        return job;
    }

    public Run getRunFromRS(ResultSet rs, Run run) throws SQLException {
        if (run == null) {
            run = new Run(0L, null, null);
        }
        run.setId(rs.getLong("run_id"));
        run.setJobId(rs.getLong("run_job_id"));
        run.setStrId(rs.getString("run_strid"));
        run.setName(rs.getString("run_name"));
        run.setStatus(rs.getInt("run_status"));
        return run;
    }

    public RunPkg getRunPkgFromRS(ResultSet rs, RunPkg runPkg) throws SQLException {
        if (runPkg == null) {
            runPkg = new RunPkg(0L, 0L);
        }
        runPkg.setId(rs.getLong("jobpkg_id"));
        runPkg.setRunId(rs.getLong("jobpkg_job_id"));
        runPkg.setPkgId(rs.getLong("jobpkg_pkg_id"));
        return runPkg;
    }

    public Pkg getPkgFromRS(ResultSet rs, Pkg pkg) throws SQLException {
        if (pkg == null) {
            pkg = new Pkg(null);
        }
        pkg.setId(rs.getLong("pkg_id"));
        pkg.setStrId(rs.getString("pkg_strid"));
        return pkg;
    }

    public List<Job> listJobs(Connection conn) throws Exception {
        List<Job> list = new ArrayList();

        String query = "select * from jobs";
        try (Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(query)) {
                while (rs.next()) {
                    Long id = rs.getLong("job_id");
                    long depl_id = rs.getLong("job_depl_id");
                    String strId = rs.getString("job_strid");
                    String jobName = rs.getString("job_name");
                    Job job = new Job(id, depl_id, strId, jobName);
                    list.add(job);
                }
            }
        }
        return list;
    }

    @Override
    public void storeJob(Job job) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void storeRun(Run run) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

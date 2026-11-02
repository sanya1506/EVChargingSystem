package com.abcgreen.evcharging.tools;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;

import com.abcgreen.evcharging.util.DBConnection;
import com.abcgreen.evcharging.util.SqlScriptRunner;

/**
 * ONE-CLICK DATABASE SETUP.
 * Right-click this file in Eclipse > Run As > Java Application.
 *
 * Creates the ev_charging_db database, all tables and loads the demo data
 * (database/01_schema.sql and database/02_seed_data.sql). Existing data is DELETED.
 * Uses JDBC, so the mysql command-line client is not required.
 */
public class DatabaseSetup {

    public static void main(String[] args) throws Exception {
        Path schema = Paths.get("database", "01_schema.sql");
        Path seed = Paths.get("database", "02_seed_data.sql");
        if (!Files.exists(schema) || !Files.exists(seed)) {
            System.err.println("Cannot find the SQL scripts. Run this from the project folder (Eclipse does this automatically).");
            System.exit(1);
        }
        long start = System.currentTimeMillis();
        System.out.println("Connecting to MySQL...");
        try (Connection conn = DBConnection.getServerConnection()) {
            System.out.println("Creating database and tables (" + schema + ")...");
            int n = SqlScriptRunner.run(conn, schema);
            System.out.println("  " + n + " statements executed.");
            System.out.println("Loading demo data (" + seed + ") - this takes a few seconds...");
            n = SqlScriptRunner.run(conn, seed);
            System.out.println("  " + n + " statements executed.");
        }
        System.out.printf("%nDatabase '%s' is ready in %.1f s.%n", DBConnection.getDatabaseName(), (System.currentTimeMillis() - start) / 1000.0);
        System.out.println("Logins:  admin / Admin@123   operator / Operator@123   aarav.sharma / Customer@123");
    }
}

package com.abcgreen.evcharging.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executes a .sql script statement by statement over JDBC.
 * Statements are separated by a ';' at the end of a line. Lines starting with "--" are comments.
 * Used by the database setup tool and by Restore Database (no need for the mysql command line on Windows).
 */
public final class SqlScriptRunner {

    private SqlScriptRunner() {
    }

    /** @return number of statements executed */
    public static int run(Connection conn, Path script) throws IOException, SQLException {
        int count = 0;
        StringBuilder statement = new StringBuilder();
        try (BufferedReader in = Files.newBufferedReader(script, StandardCharsets.UTF_8);
             Statement st = conn.createStatement()) {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                statement.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    String sql = statement.toString().trim();
                    st.execute(sql.substring(0, sql.length() - 1));
                    statement.setLength(0);
                    count++;
                }
            }
            if (!statement.toString().isBlank()) {
                st.execute(statement.toString());
                count++;
            }
        }
        return count;
    }
}

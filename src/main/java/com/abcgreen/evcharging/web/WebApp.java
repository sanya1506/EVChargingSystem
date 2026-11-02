package com.abcgreen.evcharging.web;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.StandardJarScanner;

import com.abcgreen.evcharging.service.QueueProcessor;
import com.abcgreen.evcharging.util.AppLogger;
import com.abcgreen.evcharging.util.DBConnection;

/**
 * WEB PORTAL ENTRY POINT - starts an embedded Apache Tomcat server with the Servlets and JSP pages.
 * Right-click > Run As > Java Application, then open the printed URL (normally http://localhost:8080) in a browser.
 * If port 8080 is busy the next free port (8081, 8082...) is used automatically.
 */
public class WebApp {

    public static final int DEFAULT_PORT = 8080;

    /** Returns the first free port starting at the preferred one (another program may already use 8080). */
    static int findFreePort(int preferred) {
        for (int port = preferred; port < preferred + 20; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                socket.setReuseAddress(true);
                return port;
            } catch (IOException e) {
                System.out.println("Port " + port + " is already in use, trying " + (port + 1) + "...");
            }
        }
        throw new IllegalStateException("No free port found between " + preferred + " and " + (preferred + 19));
    }

    public static void main(String[] args) throws Exception {
        int preferred = args.length > 0 ? Integer.parseInt(args[0]) : Integer.getInteger("port", DEFAULT_PORT);
        int port = findFreePort(preferred);
        File webappDir = new File("src/main/webapp");
        if (!webappDir.exists()) {
            System.err.println("src/main/webapp not found - run WebApp from the project folder (Eclipse does this by default).");
            System.exit(1);
        }
        if (!DBConnection.isAvailable()) {
            System.err.println("WARNING: cannot connect to MySQL. Check db.properties and run DatabaseSetup first.");
        }
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());
        tomcat.setPort(port);
        tomcat.getConnector(); // creates the default HTTP connector

        Context ctx = tomcat.addWebapp("", webappDir.getAbsolutePath());
        StandardJarScanner scanner = (StandardJarScanner) ctx.getJarScanner();
        scanner.setScanClassPath(false); // servlets are declared in web.xml - no need to scan jars (faster start)
        scanner.setScanManifest(false);

        QueueProcessor.start();
        tomcat.start();
        AppLogger.info("Web portal started on port " + port);
        System.out.println();
        System.out.println("==============================================================");
        System.out.println("  EV Charging web portal is running:  http://localhost:" + port);
        System.out.println("  Customer demo login: aarav.sharma / Customer@123");
        System.out.println("  Admin dashboard:     admin / Admin@123");
        System.out.println("  Stop the server with the red Terminate button in Eclipse.");
        System.out.println("==============================================================");
        tomcat.getServer().await();
    }
}

package com.abcgreen.evcharging;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.abcgreen.evcharging.service.QueueSimulator;
import com.abcgreen.evcharging.util.CsvUtil;

class CsvAndQueueTest {

    @TempDir
    Path tmp;

    @Test
    @DisplayName("TC-FILE-01 CSV export and import round trip (commas, quotes)")
    void csvRoundTrip() throws Exception {
        File f = tmp.resolve("report.csv").toFile();
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[] { "Koramangala, Bengaluru", 12, "He said \"hi\"" });
        rows.add(new Object[] { "Pune", 3.5, null });
        CsvUtil.write(f, new String[] { "station", "sessions", "note" }, rows);
        List<String[]> back = CsvUtil.read(f);
        assertEquals(2, back.size());
        assertEquals("Koramangala, Bengaluru", back.get(0)[0]);
        assertEquals("He said \"hi\"", back.get(0)[2]);
        assertEquals("3.5", back.get(1)[1]);
        assertTrue(Files.readString(f.toPath()).startsWith("station,sessions,note"));
    }

    @Test
    @DisplayName("TC-THR-01 Multithreaded queue simulation serves every vehicle exactly once")
    void simulation() throws Exception {
        List<String> log = Collections.synchronizedList(new ArrayList<>());
        new QueueSimulator().run(3, 10, 20, 50, log::add);
        long finished = log.stream().filter(l -> l.contains("Charger-") && l.contains(" finished ")).count();
        assertEquals(10, finished);
        assertTrue(log.get(log.size() - 1).contains("Served 10 vehicles"));
    }
}

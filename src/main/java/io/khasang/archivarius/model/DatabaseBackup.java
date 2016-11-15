package io.khasang.archivarius.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*  Изучив вопрос, пришел к выводу:
*   нет смысла делать несколько реализаций под разные ОС
*   сервер на ставится одновременно на Linux, Windows и iOS.
*   Реализация позаимстована у SNet, потому что является самой оптимальной
*   из того, что нашел в Интернете.
*   Путь до утилиты pg_dump для выполения резервной копии базы и папки сохранения
*   инкапсулированны в backup.properties и позволяют гибко работать под любой ОС
*/
public class DatabaseBackup {
    @Autowired
    Environment environment;

    public String backup() {
        String pgDump = environment.getProperty("jdbc.postgresql.dumpAppPath");
        String dumpFile = environment.getProperty("jdbc.postgresql.dumpFolder") + getBackupFileName();
        //Add commands to start pg_dump
        final List<String> baseCmds = new ArrayList<>();
        //Path to pg_dump
        baseCmds.add(pgDump);
        baseCmds.add("-h");
        baseCmds.add("localhost");
        //Port
        baseCmds.add("-p");
        baseCmds.add("5432");
        //User
        baseCmds.add("-U");
        baseCmds.add(environment.getProperty("jdbc.postgresql.username"));
        //Add BLOB object into dump file
        baseCmds.add("-b");
        baseCmds.add("-v");
        //Path to dump file
        baseCmds.add("-f");
        baseCmds.add(dumpFile);
        //Base name
        baseCmds.add("archivarius");
        final ProcessBuilder processBuilder = new ProcessBuilder(baseCmds);
        //Password for PostgreSQL user
        final Map<String, String> env = processBuilder.environment();
        env.put("PGPASSWORD", environment.getProperty("jdbc.postgresql.password"));
        try {
            final Process process = processBuilder.start();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = reader.readLine();
            while (line != null) {
                System.err.println(line);
                line = reader.readLine();
            }
            reader.close();

            final int dcertExitCode = process.waitFor();
            if (dcertExitCode == 0) {
                return "Backup complete " + dumpFile;
            } else {
                return "Backup failed";
            }
        } catch (IOException | InterruptedException e) {
            return e.toString();
        }
    }

    private String getBackupFileName() {
        long currentTime = System.currentTimeMillis();
        String currentStringDate = new SimpleDateFormat("yyyy_MM_dd_HH-mm").format(currentTime);
        return "archivarius_" + currentStringDate + ".backup";
    }

}

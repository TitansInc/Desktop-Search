/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package desktopsearch;

/**
 *
 * @author zubair
 */

import java.nio.file.*;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.nio.file.LinkOption.*;
import java.nio.file.attribute.*;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;


public class WatchDir {

    private final WatchService watcher;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;
    private boolean trace = false;
    private Connection connection;
    private Statement statement;

    @SuppressWarnings("unchecked")
    static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    /**
     * Register the given directory with the WatchService
     */
    private void register(Path dir) throws IOException {
        WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        if (trace) {
            Path prev = keys.get(key);
            if (prev == null) {
                System.out.format("register: %s\n", dir);
            } else if (!dir.equals(prev)) {
                System.out.format("update: %s -> %s\n", prev, dir);
            }
        }
        keys.put(key, dir);
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) {
        // register directory and sub-directories
        try {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a WatchService and registers the given directory
     */
    WatchDir(Path dir, boolean recursive, Connection connection, Statement statement) throws IOException {

        this.watcher = FileSystems.getDefault().newWatchService();
        this.keys = new HashMap<WatchKey, Path>();
        this.recursive = recursive;
        this.connection = connection;
        this.statement = statement;

        if (recursive) {
            System.out.format("Scanning %s ...\n", dir);
            registerAll(dir);
            System.out.println("Done.");
        } else {
            register(dir);
        }

        // enable trace after initial registration
        this.trace = true;
    }

    /**
     * Process all events for keys queued to the watcher
     */
    void processEvents() {
        for (;;) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // TBD - provide example of how OVERFLOW event is handled
                if (kind == OVERFLOW) {
                    continue;
                }

                // Context for directory entry event is the file name of entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (recursive && (kind == ENTRY_CREATE)) {
                    try {
                        if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } catch (Exception x) {
                        // ignore to keep sample readbale
                    }
                }

                // print out event
                //System.out.format("%s: %s\n", event.kind().name(), child);
                try {
                    UpdateDB(event.kind().name(), child);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    private void UpdateDB(String Event, Path FullLocation) throws SQLException, IOException {

        File file = new File(FullLocation.toString());
        String path = file.getParent();
        String Name = file.getName();

        if (Name.contains("'")) {
            Name = Name.replace("'", "''");
        }
        if (path.contains("'")) {
            path = path.replace("'", "''");
        }

        if (Event.equals("ENTRY_CREATE") && file.exists()) {
            FileTime LastModifiedTime = Files.getLastModifiedTime(FullLocation);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(LastModifiedTime.toMillis());
            String Query;
            if (file.isFile()) {
                String Type = FilenameUtils.getExtension(FullLocation.toString());
                if (Type.endsWith("~") || Type.equals("")) {
                    Type = "Text";
                }
                Query = "INSERT INTO FileInfo VALUES('" + path + "','" + Name + "','" + Type + "','" + calendar.getTime() + "'," + (file.length() / 1024) + ");";
            } else if (!Files.isSymbolicLink(FullLocation)) {
                long size = FileUtils.sizeOfDirectory(file);
                Query = "INSERT INTO FileInfo VALUES('" + path + "','" + Name + "','FOLDER','" + calendar.getTime() + "'," + size + ");";
            } else {
                Query = "INSERT INTO FileInfo VALUES('" + path + "','" + Name + "','LINK','" + calendar.getTime() + "',0);";
            }
            statement.executeUpdate(Query);
            
        } else if (Event.equals("ENTRY_MODIFY")) {
            System.out.println(path+"/"+Name);
            FileTime LastModifiedTime = Files.getLastModifiedTime(FullLocation);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(LastModifiedTime.toMillis());
            String Query="UPDATE FileInfo SET LastModified='"+calendar.getTime()+"' WHERE FileLocation='"+path+"' and FileName='"+Name+"';";
            System.out.println(Query);
            statement.executeUpdate(Query);
        
        } else if (Event.equals("ENTRY_DELETE")) {
        
            String Query = "DELETE FROM FileInfo WHERE FileLocation = '" + path + "' AND FileName = '" + Name + "';";
            statement.executeUpdate(Query);
        
        }
        connection.commit();

    }

    static void usage() {
        System.err.println("usage: java WatchDir [-r] dir");
        System.exit(-1);
    }

}

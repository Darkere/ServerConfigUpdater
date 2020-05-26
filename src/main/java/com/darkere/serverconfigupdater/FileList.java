package com.darkere.serverconfigupdater;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileList extends SimpleFileVisitor<Path>{
    private Map<Path, Path> files = new HashMap<>();

    FileList(){
        try {
            Files.walkFileTree(Paths.get(""),this);
        } catch (IOException e) {
            ServerConfigUpdater.LOGGER.warn("Error traversing files");
            e.printStackTrace();
        }
    }

    public void tryDeletingFiles(){
        List<Path> filesToDelete =  ServerConfigUpdater.COMMON_CONFIG.getFilesToDelete();
        ServerConfigUpdater.LOGGER.info("Attempting to delete " + filesToDelete.size() + " files/folders defined in config");
        for (Path path : filesToDelete) {
            if(files.containsKey(path.getFileName())){
                if(files.get(path.getFileName()).equals(path)){
                    try {
                        File file = new File(String.valueOf(path));
                        if(file.isDirectory()){
                            //noinspection ConstantConditions
                            if(file.listFiles().length != 0){
                                if(ServerConfigUpdater.COMMON_CONFIG.shouldDeleteFolders()){
                                    FileUtils.deleteDirectory(file);
                                    ServerConfigUpdater.LOGGER.info("Successfully deleted folder " + path);
                                    continue;
                                }
                            }
                        }
                        Files.delete(path);
                        ServerConfigUpdater.LOGGER.info("Successfully deleted " + path);
                    } catch (IOException e) {
                        ServerConfigUpdater.LOGGER.error("error deleting files");
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) {
        if (attr.isRegularFile()) {
           files.put(file.getFileName(),file);
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        if(dir.endsWith("saves") || dir.endsWith("world")){
            return FileVisitResult.SKIP_SUBTREE;
        }
        return FileVisitResult.CONTINUE;
    }

    // Print each directory visited.
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if(!dir.equals(Paths.get(""))){
            files.put(dir.getFileName(),dir);
        }
        return FileVisitResult.CONTINUE;
    }

    // If there is some error accessing
    // the file, let the user know.
    // If you don't override this method
    // and an error occurs, an IOException
    // is thrown.
    @Override
    public FileVisitResult visitFileFailed(Path file,
                                           IOException exc) {
        System.err.println(exc);
        return FileVisitResult.CONTINUE;
    }
}


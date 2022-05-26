package com.we1u.filesync;


import com.we1u.filesync.configs.FileComparatorConfig;
import com.we1u.filesync.logging.LogSeverity;
import com.we1u.filesync.logging.Logger;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.*;

public class DirComparator extends SimpleFileVisitor<Path> {

    public List<Path> dirPaths = new ArrayList<>();
    public List<Path> filePaths = new ArrayList<>();
    private final Path originalRootPath;
    private final Path newRootPath;
    private final FileComparatorConfig config;
    private final Logger logger;
    public boolean terminated;

    public DirComparator(Path _originalRootPath,
                         Path _newRootPath,
                         FileComparatorConfig _config){
        this.originalRootPath = _originalRootPath;
        this.newRootPath = _newRootPath;
        this.config = _config;
        this.logger = Main.mainInstance.logger;
        this.terminated = false;
    }

    @Override
    public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attr) throws IOException {
        if (Thread.currentThread().isInterrupted()){
            terminated = true;
            return TERMINATE;
        }
        Path relPath = newRootPath.relativize(file);
        Path otherFile = originalRootPath.resolve(relPath);
        String fileName = file.getFileName().toString();
        if (fileName.contains(".")){
            String fileExtension = fileName.substring(fileName.lastIndexOf("."));
            if (!config.extensions.isEmpty()){
                if (config.excludeExtension == config.extensions.contains(fileExtension)){
                    return CONTINUE;
                }
            }
        }
        if (Files.exists(otherFile)){
            BasicFileAttributes otherAttr = Files.readAttributes(otherFile, BasicFileAttributes.class);
            if (config.checkSize) {
                if (attr.size() != otherAttr.size()){
                    filePaths.add(relPath);
                    return CONTINUE;
                }
            }
            if (config.checkModTime){
                if (attr.lastModifiedTime().toMillis() != otherAttr.lastModifiedTime().toMillis()){
                    filePaths.add(relPath);
                    return CONTINUE;
                }
            }
            if (config.checkContent){
               try{
                   if (!sameContent(file, otherFile)){
                       filePaths.add(relPath);
                       return CONTINUE;
                   }
               } catch (IOException e){
                   logger.log(e.toString(), LogSeverity.ERROR);
               }
            }
        }
        else{
            filePaths.add(relPath);
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir,
                                             BasicFileAttributes attrs){
        if (Thread.currentThread().isInterrupted()){
            terminated = true;
            return TERMINATE;
        }
        Path relPath = newRootPath.relativize(dir);
        Path otherFile = originalRootPath.resolve(relPath);
        if (!Files.exists(otherFile)){
            dirPaths.add(relPath);
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir,
                                              IOException e) {
        if (Thread.currentThread().isInterrupted()){
            terminated = true;
            return TERMINATE;
        }
        return CONTINUE;
    }


    @Override
    public FileVisitResult visitFileFailed(Path file,
                                           IOException e) {
        if (Thread.currentThread().isInterrupted()){
            terminated = true;
            return TERMINATE;
        }
        logger.log(e.toString(), LogSeverity.ERROR);
        return CONTINUE;
    }

    private boolean sameContent(Path path1, Path path2) throws IOException{
        try (BufferedInputStream fis1 = new BufferedInputStream(new FileInputStream(path1.toFile()));
             BufferedInputStream fis2 = new BufferedInputStream(new FileInputStream(path2.toFile()))) {
            int ch;
            while ((ch = fis1.read()) != -1) {
                if (ch != fis2.read()) {
                    return false;
                }
            }
            return fis2.read() == -1;
        }
    }
}

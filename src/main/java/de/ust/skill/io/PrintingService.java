package de.ust.skill.io;

import java.io.File;
import java.util.HashSet;

/**
 * This service holds state of file system interaction of a single generator
 * run. It encapsulates PrintWriter allocation so that can know the output
 * directories and hence, can clean it from all foreign files.
 * 
 * @note The primary intention of this class is to minimize file overwrite to
 *       improve compile time in the presence of incremental compilation.
 * 
 * @author Timm Felden
 */
public class PrintingService {

    private final File outPath;
    private final String header;

    /**
     * Create a new printing service. One service should exist for each
     * generator invocation.
     * 
     * @param outPath
     *            BasePath for generated output as specified by the user
     * @param header
     *            The header that is added to each generated file
     */
    public PrintingService(File outPath, String header) {
        this.outPath = outPath;
        this.header = header;
    }

    /**
     * touched files not including directories
     */
    private final HashSet<File> files = new HashSet<>();

    /**
     * Open a print writer that will cache the output and leave the original
     * file untouched if it would not change.
     * 
     * The new file will contain the specified header.
     */
    public PrintWriter open(String path) {
        File target = new File(outPath, path);
        files.add(target);
        return new PrintWriter(target, header);
    }

    /**
     * Open a print writer that will cache the output and leave the original
     * file untouched if it would not change.
     * 
     * The new file will be empty, i.e. the default header will be missing.
     */
    public PrintWriter openRaw(String path) {
        // remove characters that destroy NTFS
        path = path.replaceAll("[\\\\:\\*\\?\"<>\\|]", "_");
        File target = new File(outPath, path);
        files.add(target);
        return new PrintWriter(target, "");
    }

    /**
     * deletes all foreign files. A file is foreign, iff it resides in a folder
     * that received
     * 
     * @param deleteDirectories
     *            also delete foreign directories if true
     */
    public void deleteForeignFiles(boolean deleteDirectories) {
        // TODO implementation
        throw null;
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory())
            for (File f : file.listFiles())
                deleteRecursively(f);

        if (file.exists() && !file.delete())
            throw new RuntimeException("Unable to delete " + file.getAbsolutePath());
    }

}

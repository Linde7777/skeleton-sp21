package gitlet;

import java.io.File;

public class StudentUtils {

    /**
     * we have already designed that in certain directory,
     * there is only one file, we call this function to get that file
     *
     * @param dir the directory
     * @return the only file in the directory
     */
    public static File getTheOnlyFileInDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files.length != 1) {
                throw new GitletException("This directory has more than one file");
            } else {
                return files[0];
            }
        } else {
            throw new GitletException("This is not a directory");
        }
    }
}

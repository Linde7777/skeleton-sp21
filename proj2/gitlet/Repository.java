package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static gitlet.Utils.*;

// TODO: any imports you need here

/**
 * Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author Linde
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     * <p>
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty(System.getProperty("user.dir")));

    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * The .gitlet/stageForAdd directory,
     * used to store the files readied for commit
     */
    public static final File GITLET_STAGE_FOR_ADD_DIR = join(GITLET_DIR, "stageForAdd");

    /**
     * The .gitlet/blobs directory, where store the files of different version
     */
    public static final File GITLET_BLOBS_DIR = join(GITLET_DIR, "blobs");

    /**
     * The sha1 value of master branch pointer
     */
    public String masterSha1;

    /**
     * The sha1 value of HEAD pointer
     */
    public String HEADSha1;

    public void init() throws IOException {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
        } else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }

        if (!GITLET_STAGE_FOR_ADD_DIR.exists()) {
            GITLET_STAGE_FOR_ADD_DIR.mkdir();
        }

        if (!GITLET_BLOBS_DIR.exists()) {
            GITLET_BLOBS_DIR.mkdir();
        }

        File sentinelFile = new File(".gitletTemp.txt");
        sentinelFile.createNewFile();
        add(sentinelFile.toString());
        commit("init commit", null);
        sentinelFile.delete();
    }

    public void add(String filename) throws IOException {
        File file = new File(filename);
        String fileSha1 = sha1(file);
        //todo: check if this file is identical to the current commit

        Path src = file.toPath();
        Path dest = GITLET_STAGE_FOR_ADD_DIR.toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    public void commit(String message, String parentSha1) throws IOException {


    }


}

package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

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
    public static final File CWD = new File(System.getProperty("user.dir"));

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
     * The .gitlet/commits directoru, where store the serialized Commits
     */
    public static final File GITLET_COMMITS_DIR = join(GITLET_DIR, "commits");

    /**
     * The sha1 value of master branch pointer
     */
    public static String masterSha1;

    /**
     * The sha1 value of HEAD pointer
     */
    public static String HEADSha1;

    public static void init() throws IOException {
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

        if (!GITLET_COMMITS_DIR.exists()) {
            GITLET_COMMITS_DIR.mkdir();
        }

        File file = new File(".gitletTempFile");
        file.createNewFile();
        add(file.getName());
        commit("init commit", null);
    }

    public static void add(String filename) throws IOException {
        File file = new File(filename);
        String fileSha1 = sha1(file);
        //todo: check if this file is identical to the current commit

        Path src = file.toPath();
        Path dest = GITLET_STAGE_FOR_ADD_DIR.toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void commit(String message) throws IOException {
        commit(message, HEADSha1);
    }

    /**
     * initliaze a Commit class with given variable,
     * then delete files in stageForAdd directory,
     * then serialize Commit, which is named after its sha1 value,
     * put it in .gitlet/commits
     * <p>
     * e.g.
     * We initialize a Commit, whose sha1 value is a154ccd,
     * then we will serialize this commit, this serialized file
     * will be named after a154ccd, then we put it in .gitlet/commits
     *
     * @param message
     * @param parentSha1
     * @throws IOException
     */
    private static void commit(String message, String parentSha1) throws IOException {
        Commit commit = new Commit(message, parentSha1,
                GITLET_STAGE_FOR_ADD_DIR, GITLET_BLOBS_DIR);

        for (File file : Objects.requireNonNull(GITLET_STAGE_FOR_ADD_DIR.listFiles())) {
            file.delete();
        }

        // todo: may need to be modify when dealing with checkout
        masterSha1 = commit.getCommitSha1();
        HEADSha1 = commit.getCommitSha1();

        writeObject(join(GITLET_COMMITS_DIR, commit.getCommitSha1()), commit);
    }


}

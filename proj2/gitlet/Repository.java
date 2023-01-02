package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

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
     * where store the sha1 value of master as file
     */
    public static File master_FILE = join(GITLET_DIR, "master");

    /**
     * The sha1 value of HEAD pointer
     */
    public static String HEADSha1;

    /**
     * where store the sha1 value of HEAD as a file
     */
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");

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

        if (!master_FILE.exists()) {
            master_FILE.createNewFile();
        }

        if (!HEAD_FILE.exists()) {
            HEAD_FILE.createNewFile();
        }

        // since add and commit must contain file,
        // here we need to make a temp file
        File file = join(CWD, ".gitletTempFile");
        file.createNewFile();
        add(file.getName());
        commit("init commit", null);
        file.delete();
    }

    /**
     * copy the file to the GITLET_STAGE_FOR_ADD_DIR
     *
     * @param filename the file we want to add
     * @throws IOException
     */
    public static void add(String filename) throws IOException {
        File file = join(CWD, filename);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        // check if this file is identical to the current commit
        String fileSha1 = sha1((Object) readContents(file));
        String currentCommitSha1 = readContentsAsString(HEAD_FILE);
        File commitFile = join(GITLET_COMMITS_DIR, currentCommitSha1);
        Commit currentCommit = readObject(commitFile, Commit.class);
        if (currentCommit.blobSha1List.contains(fileSha1)) {
            System.exit(0);
        }

        Path src = file.toPath();
        Path dest = join(GITLET_STAGE_FOR_ADD_DIR, file.getName()).toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * notice that it is public method
     *
     * @param message
     * @throws IOException
     */
    public static void commit(String message) throws IOException {
        commit(message, HEADSha1);
    }

    /**
     * call commit constructor, which will create a Commit instance
     * and put itself seriliaze file in .gitlet/commits.
     * then delete files in stageForAdd directory.
     * <p>
     *
     * @param message
     * @param parentSha1
     * @throws IOException
     */
    private static void commit(String message, String parentSha1) throws IOException {
        //TODO: copy from parent commit then modify its message and blobs
        Commit commit = new Commit(message, parentSha1,
                GITLET_STAGE_FOR_ADD_DIR, GITLET_BLOBS_DIR, GITLET_COMMITS_DIR);

        for (File file : Objects.requireNonNull(GITLET_STAGE_FOR_ADD_DIR.listFiles())) {
            file.delete();
        }

        // the newest file in GITLET_COMMITS_DIR is the commit we just created
        // where the HEAD should point to

        // copy from https://www.baeldung.com/java-last-modified-file
        Path dirPath = GITLET_COMMITS_DIR.toPath();
        Optional<Path> opPath = Files.list(dirPath).filter(p -> !Files.isDirectory(p))
                .sorted((p1, p2) -> Long.valueOf(p2.toFile().lastModified())
                        .compareTo(p1.toFile().lastModified()))
                .findFirst();
        File theNewestFile;
        if (opPath.isPresent()) {
            theNewestFile = opPath.get().toFile();
        } else {
            throw new GitletException("find the newest commit failed");
        }

        // todo: may need to be modify when dealing with checkout
        //todo: write it into a file
        HEADSha1 = theNewestFile.getName();
        masterSha1 = HEADSha1;
        writeContents(HEAD_FILE, HEADSha1);
        writeContents(master_FILE, masterSha1);
    }


}

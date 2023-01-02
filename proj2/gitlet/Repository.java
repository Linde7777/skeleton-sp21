package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
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
     * The .gitlet/stageForAdd directory, where store the files readied for commit
     */
    public static final File GITLET_STAGE_FOR_ADD_DIR = join(GITLET_DIR, "stageForAdd");

    /**
     * The .gitlet/stageForRemove directory, where store the files readied for remove
     */
    public static final File GITLET_STAGE_FOR_REMOVE = join(GITLET_DIR, "stageForRemove");

    /**
     * The .gitlet/blobs directory, where store the files of different version
     */
    public static final File GITLET_BLOBS_DIR = join(GITLET_DIR, "blobs");

    /**
     * The .gitlet/commits directory, where store the serialized Commits
     */
    public static final File GITLET_COMMITS_DIR = join(GITLET_DIR, "commits");

    /**
     * The .gitlet/branches directory, where store the master, HEAD files
     */
    public static final File GITLET_BRANCHES_DIR = join(GITLET_DIR, "branches");


    /**
     * where store the sha1 value of master as file
     */
    public static File master_FILE = join(GITLET_BRANCHES_DIR, "master");

    /**
     * where store the sha1 value of HEAD as a file
     */
    public static final File HEAD_FILE = join(GITLET_BRANCHES_DIR, "HEAD");

    private static void checkInitialize() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    public static void init() throws IOException {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
        } else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITLET_STAGE_FOR_ADD_DIR.mkdir();
        GITLET_STAGE_FOR_REMOVE.mkdir();
        GITLET_BLOBS_DIR.mkdir();
        GITLET_COMMITS_DIR.mkdir();
        GITLET_BRANCHES_DIR.mkdir();
        master_FILE.createNewFile();
        HEAD_FILE.createNewFile();

        // since add and commit must contain file,
        // here we need to make a temp file
        File file = join(CWD, ".gitletTempFile");
        file.createNewFile();
        add(file.getName());
        commit("init commit", null);
        file.delete();
    }

    /**
     * copy the file in CWD to the GITLET_STAGE_FOR_ADD_DIR,
     * if the file we add in to GITLET_STAGE_FOR_ADD is identical
     * to one of the files in current commit, we won't add it.
     *
     * @param filename the file we want to add
     * @throws IOException
     */
    public static void add(String filename) throws IOException {
        checkInitialize();
        File file = join(CWD, filename);
        if (!file.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        // if currentCommit is null, that means nothing has been committed yet,
        // so we don't need to care about whether the file we add is identical
        // to one of the files in the current Commit
        String fileSha1 = sha1((Object) readContents(file));
        Commit currentCommit = getCurrentCommit();
        if (currentCommit != null && currentCommit.blobSha1List.contains(fileSha1)) {
            System.exit(0);
        }

        Path src = file.toPath();
        Path dest = join(GITLET_STAGE_FOR_ADD_DIR, file.getName()).toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    private static Commit getCurrentCommit() {
        String commitSha1 = readContentsAsString(HEAD_FILE);
        // if there is no commit.
        if (commitSha1.equals("")) {
            return null;
        }
        return readObject(join(GITLET_COMMITS_DIR, commitSha1), Commit.class);
    }

    /**
     * notice that it is public method
     *
     * @param message
     * @throws IOException
     */
    public static void commit(String message) throws IOException {
        checkInitialize();
        String HEADSha1 = readContentsAsString(HEAD_FILE);
        commit(message, HEADSha1);
    }

    /**
     * call commit constructor, which will create a Commit instance
     * then delete files in stageForAdd directory.
     * <p>
     * after that,we will serialize it and put it in commitsDir
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
        checkInitialize();
        if (GITLET_STAGE_FOR_ADD_DIR.list().length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        //TODO: copy from parent commit then modify its message and blobs
        Commit commit = new Commit(message, parentSha1,
                GITLET_STAGE_FOR_ADD_DIR, GITLET_BLOBS_DIR);

        File commitSerializedFile = join(GITLET_COMMITS_DIR, "tempCommitName");
        writeObject(commitSerializedFile, commit);
        String commitSha1 = sha1((Object) readContents(commitSerializedFile));
        File renameCommitSerializedFile = join(GITLET_COMMITS_DIR, commitSha1);
        boolean flag = commitSerializedFile.renameTo(renameCommitSerializedFile);
        if (!flag) {
            throw new GitletException("rename serialized Commit file failed");
        }
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
        File theNewestCommitFile;
        if (opPath.isPresent()) {
            theNewestCommitFile = opPath.get().toFile();
        } else {
            throw new GitletException("find the newest commit failed");
        }
        // todo: may need to be modify when dealing with checkout
        String HEADSha1 = theNewestCommitFile.getName();
        String masterSha1 = HEADSha1;
        writeContents(HEAD_FILE, HEADSha1);
        writeContents(master_FILE, masterSha1);
    }

    /**
     * If the file exists in GITLET_STAGE_FOR_ADD_DIR, we remove it.
     * If the file is tracked in the current commit, stage it for removal
     * and remove the file from CWD if the user has not already done so
     *
     * @param filename the name of the file that we want to remove
     */
    public static void remove(String filename) throws IOException {
        for (File file : Objects.requireNonNull(GITLET_STAGE_FOR_ADD_DIR.listFiles())) {
            if (filename.equals(file.getName())) {
                file.delete();
            }
        }

        Commit currentCommit = getCurrentCommit();
        if (currentCommit == null) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
        for (String blobDirPath : currentCommit.blobSha1List) {
            File file = getTheOnlyFileInDir(blobDirPath);
            if (file.getName().equals(filename)) {
                Path src = file.toPath();
                Path dest = join(GITLET_STAGE_FOR_REMOVE, filename).toPath();
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }


    }

    /**
     * If we has already design that in certain directory,
     * there is only one file, we call this function to get that file
     *
     * @param dirString the directory path, e.g. "home/john/folder1"
     * @return the only file in the directory
     */
    private static File getTheOnlyFileInDir(String dirString) {
        File dir = new File(dirString);
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

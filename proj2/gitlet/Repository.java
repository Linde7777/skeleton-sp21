package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static gitlet.StudentUtils.getTheNewestFileInDir;
import static gitlet.StudentUtils.getTheOnlyFileInDir;
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
     * The .gitlet/branches/activeBranch file, where store the name of the active branch
     */
    public static final File GITLET_ACTIVE_BRANCH_FILE = join(GITLET_BRANCHES_DIR, "activeBranch");

    /**
     * The .gitlet/branches/master file, where store the sha1 value of master as file
     */
    public static File master_FILE = join(GITLET_BRANCHES_DIR, "master");

    /**
     * The .gitlet/branches/HEAD file, where store the sha1 value of HEAD as a file
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
        GITLET_ACTIVE_BRANCH_FILE.mkdir();
        master_FILE.createNewFile();
        HEAD_FILE.createNewFile();

        writeContents(GITLET_ACTIVE_BRANCH_FILE, "master");

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
        Commit currentCommit = getHeadCommit();
        if (currentCommit != null && currentCommit.blobSha1List.contains(fileSha1)) {
            System.exit(0);
        }

        Path src = file.toPath();
        Path dest = join(GITLET_STAGE_FOR_ADD_DIR, file.getName()).toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * the sha1 value of current commit is store at HEAD_FILE
     *
     * @return
     */
    private static Commit getHeadCommit() {
        String commitSha1 = getHeadCommitSha1();
        // if there is no commit.
        if (commitSha1.equals("")) {
            return null;
        }
        return readObject(join(GITLET_COMMITS_DIR, commitSha1), Commit.class);
    }

    private static String getHeadCommitSha1() {
        return readContentsAsString(HEAD_FILE);
    }

    /**
     * notice that this is the public method
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
     * if it is the first commit, we will call commit constructor,
     * otherwise we will copy a commit then modify it.
     * Then delete files in stageForAdd directory.
     * After that, we will serialize it and put it in commitsDir,
     * and set HEAD point to active branch.
     * <p>
     * For example:
     * We initialize a Commit, whose sha1 value is a154ccd,
     * then we will serialize this commit, this serialized file
     * will be named after a154ccd, then we put it in .gitlet/commits
     *
     * @param message    the message of this commit
     * @param parentSha1 the sha1 value of the parent commit
     * @throws IOException
     */
    private static void commit(String message, String parentSha1) throws IOException {
        checkInitialize();
        if (GITLET_STAGE_FOR_ADD_DIR.list().length == 0
                && GITLET_STAGE_FOR_REMOVE.list().length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit commit;
        if (parentSha1 != null) {
            commit = getHeadCommit();
            assert commit != null;
            commit.modifyCommit(message, parentSha1);
            commit.addBlobs(GITLET_STAGE_FOR_ADD_DIR, GITLET_BLOBS_DIR);
            commit.removeBlobs(GITLET_STAGE_FOR_REMOVE, GITLET_BLOBS_DIR);
        } else {
            commit = new Commit(message, parentSha1);
            commit.addBlobs(GITLET_STAGE_FOR_ADD_DIR, GITLET_BLOBS_DIR);
        }

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

        setupBranch();
    }

    private static void setupBranch() throws IOException {
        // the newest file in GITLET_COMMITS_DIR is the commit we just created,
        // where the HEAD and active branch should point to
        File theNewestCommitFile = getTheNewestFileInDir(GITLET_COMMITS_DIR);

        // recall that in .gitlet/commits, the commits are named by its sha1
        String theNewestCommitSha1 = theNewestCommitFile.getName();
        String activeBranchName = readContentsAsString(GITLET_ACTIVE_BRANCH_FILE);
        File activeBranchFile = join(GITLET_BRANCHES_DIR, activeBranchName);
        writeContents(activeBranchFile, theNewestCommitSha1);
        writeContents(HEAD_FILE, theNewestCommitSha1);
    }

    /**
     * If the file exists in GITLET_STAGE_FOR_ADD_DIR, we remove it.
     * If the file is tracked in the current commit, stage it for removal
     * and remove the file from working directory if the user has not already done so
     * (do not remove it unless it is tracked in the current commit).
     *
     * @param filename the name of the file that we want to remove
     */
    public static void remove(String filename) throws IOException {
        boolean findFileInStageForAddDir = false;
        File[] filesInStageForAddDir = GITLET_STAGE_FOR_ADD_DIR.listFiles();
        if (filesInStageForAddDir != null) {
            for (File file : filesInStageForAddDir) {
                if (filename.equals(file.getName())) {
                    findFileInStageForAddDir = true;
                    file.delete();
                }
            }
        }

        boolean findFileInCurrentCommit = false;
        Commit currentCommit = getHeadCommit();
        if (currentCommit != null) {
            for (String currCommitBlobSha1 : currentCommit.blobSha1List) {
                File currCommitBlobFile =
                        getTheOnlyFileInDir(join(GITLET_BLOBS_DIR, currCommitBlobSha1));

                if (currCommitBlobFile.getName().equals(filename)) {
                    findFileInCurrentCommit = true;
                    Path src = currCommitBlobFile.toPath();
                    Path dest = join(GITLET_STAGE_FOR_REMOVE, filename).toPath();
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);

                    File file = join(CWD, filename);
                    if (file.exists()) {
                        file.delete();
                    }
                    // since we have found it, we don't need to search anymore
                    break;
                }

            }
        }

        if (!findFileInStageForAddDir && !findFileInCurrentCommit) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }
    }

    public static void log() {
        //todo: dealing with merge

        String currentCommitSha1 = getHeadCommitSha1();
        while (currentCommitSha1 != null) {
            File currentCommitFile = join(GITLET_COMMITS_DIR, currentCommitSha1);
            Commit currentCommit = readObject(currentCommitFile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + currentCommitSha1);
            // why time zone offset is deprecated?
            Date date = currentCommit.getTimeStamp();

            String formattedDateString = String.format("%1$ta %1$tb %1$td %1$tk:%1$tM:%1$tS %1$tY %1$tz", date);
            /*
            the code above is equal to the following code:
            SimpleDateFormat formatter =
                                 new SimpleDateFormat("E MMM dd hh:mm:ss yyyy Z");
            String formattedDateString = formatter.format(date);

            Since gitlet document say I should use java.util.formatter,
            I didn't use SimpleDateFormat.

            notice that in Windows's git bash, it might have problem to display
            weekday and month, you can try to run this program on Ubuntu
            */

            System.out.println("Date: " + formattedDateString);
            System.out.println(currentCommit.getMessage());
            System.out.println();
            currentCommitSha1 = currentCommit.getParentSha1();
        }

    }

    public static void branch() {

    }

}

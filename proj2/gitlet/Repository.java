package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static gitlet.StudentUtils.*;
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

    /**
     * notice that we won't call add() then call commit(),
     * we will call setUpCommit() instead.
     */
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
        GITLET_ACTIVE_BRANCH_FILE.createNewFile();
        master_FILE.createNewFile();
        HEAD_FILE.createNewFile();
        writeContents(GITLET_ACTIVE_BRANCH_FILE, "master");
        setUpFirstCommit();
    }

    private static void setUpFirstCommit() throws IOException {
        String message = "init commit";
        Commit commit = new Commit(message);
        String commitSha1 = serializeCommit(commit);
        setupBranch(commitSha1);
    }

    /**
     * copy the file in CWD to the GITLET_STAGE_FOR_ADD_DIR,
     * if the file we add in to GITLET_STAGE_FOR_ADD is identical
     * to one of the files in current commit, we won't add it.
     *
     * @param filename the file we want to add
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
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());
        if (currentCommit != null && currentCommit.blobSha1List.contains(fileSha1)) {
            System.exit(0);
        }

        Path src = file.toPath();
        Path dest = join(GITLET_STAGE_FOR_ADD_DIR, file.getName()).toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String getHeadCommitSha1() {
        String sha1 = readContentsAsString(HEAD_FILE);
        if (sha1.equals("")) {
            throw new GitletException("HEAD_FILE is empty");
        }
        return sha1;
    }

    private static Commit getCommitBySha1(String commitSha1) {
        if (commitSha1.length() < 40) {
            return null;
        }
        File file = join(GITLET_COMMITS_DIR, commitSha1.substring(0, 2), commitSha1);
        if (!file.exists()) {
            return null;
        }
        return readObject(file, Commit.class);
    }

    /**
     * notice that this is the public method
     */
    public static void setUpCommit(String message) throws IOException {
        checkInitialize();
        String HEADSha1 = getHeadCommitSha1();
        setUpCommit(message, HEADSha1);
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
     */
    private static void setUpCommit(String message, String parentSha1) throws IOException {
        checkInitialize();
        if (Objects.requireNonNull(GITLET_STAGE_FOR_ADD_DIR.list()).length == 0
                && Objects.requireNonNull(GITLET_STAGE_FOR_REMOVE.list()).length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit commit = getCommitBySha1(getHeadCommitSha1());
        assert commit != null;
        commit.modifyCommit(message, parentSha1);
        commit.addBlobs(GITLET_STAGE_FOR_ADD_DIR, GITLET_BLOBS_DIR);
        commit.removeBlobs(GITLET_STAGE_FOR_REMOVE);

        String commitSha1 = serializeCommit(commit);

        setupBranch(commitSha1);

        deleteAllFilesInDir(GITLET_STAGE_FOR_ADD_DIR);
        deleteAllFilesInDir(GITLET_STAGE_FOR_REMOVE);
    }


    /**
     * serialize a Commit class in the GITLET_COMMITS_DIR/[first 2 sha1 digit]/[40 bit sha1 digit]
     * and return its sha1 value.
     * <p>
     * for example:
     * We serialize a Commit class, and get its sha1: a1fb321c,
     * this file's path will be .gitlet/commits/a1/a1fb321c
     *
     * @param commit the commit we want to serialize
     * @return the sha1 of the commit
     */
    private static String serializeCommit(Commit commit) throws IOException {
        File commitFile = join(GITLET_COMMITS_DIR, "tempCommitName");
        writeObject(commitFile, commit);
        String commitSha1 = sha1((Object) readContents(commitFile));

        File commitDir = join(GITLET_COMMITS_DIR, commitSha1.substring(0, 2));
        if (!commitDir.exists()) {
            commitDir.mkdir();
        }
        Path src = commitFile.toPath();
        Path dest = join(commitDir, commitSha1).toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);

        return commitSha1;
    }

    /**
     * set HEAD and active branch point to the newest commit
     */
    private static void setupBranch(String theNewestCommitSha1) {
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
        // if filesInStageForAddDir is null, it is ok, we don't need to do anything,
        // and then we move down to check if we need to delete file from current commit.
        if (filesInStageForAddDir != null) {
            for (File file : filesInStageForAddDir) {
                if (filename.equals(file.getName())) {
                    findFileInStageForAddDir = true;
                    file.delete();
                }
            }
        }

        boolean findFileInCurrentCommit = false;
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());

        // if currentCommit is null, it is ok, we don't need to do anything
        // then we move down to check the next condition.
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
            Commit currentCommit = getCommitBySha1(currentCommitSha1);
            Date date = currentCommit.getTimeStamp();
            String formattedDateString = formatDate(date);

            System.out.println("===");
            System.out.println("commit " + currentCommitSha1);
            System.out.println("Date: " + formattedDateString);
            System.out.println(currentCommit.getMessage());
            System.out.println();

            currentCommitSha1 = currentCommit.getParentSha1();
        }

    }

    private static String formatDate(Date date) {
        // FYI: https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html
        return String.format("%1$ta %1$tb %1$td %1$tk:%1$tM:%1$tS %1$tY %1$tz", date);
        /*
            you can also use the following code to get the same output:
            SimpleDateFormat formatter =
                                 new SimpleDateFormat("E MMM dd hh:mm:ss yyyy Z");
            String formattedDateString = formatter.format(date);

            Since gitlet document say I should use java.util.formatter,
            I didn't use SimpleDateFormat.
            if your Operating System's language is not English,
            it might have problem to display weekday and month,
            because weekday and month in String will be other language(e.g. Chinese),
            and the terminal may have problem to display that.
            you can try to run this program on Ubuntu
            */
    }

    public static void branch(String branchName) throws IOException {
        File branchFile = join(GITLET_BRANCHES_DIR, branchName);
        if (!branchFile.exists()) {
            branchFile.createNewFile();
        } else {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        String currentCommitSha1 = getHeadCommitSha1();
        writeContents(branchFile, currentCommitSha1);
    }

    public static void removeBranch(String branchName) {
        if (readContentsAsString(GITLET_ACTIVE_BRANCH_FILE).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        File branchFile = join(GITLET_BRANCHES_DIR, branchName);
        if (branchFile.exists()) {
            branchFile.delete();
        } else {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    /**
     * Takes the version of the file as it exists in the head commit and
     * puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one.
     * The new version of the file is not staged.
     */
    public static void checkoutFilename(String filename) throws IOException {
        boolean findThisFileInCurrentCommit = false;
        Commit commit = getCommitBySha1(getHeadCommitSha1());
        for (String blobSha1 : commit.blobSha1List) {
            // recall that blob are stored like: .gitlet/blobs/40 bit sha1 value/hello.txt
            File blobFile = getTheOnlyFileInDir(join(GITLET_BLOBS_DIR, blobSha1));
            if (blobFile.getName().equals(filename)) {
                findThisFileInCurrentCommit = true;
                Path src = blobFile.toPath();
                Path dest = join(CWD, blobFile.getName()).toPath();
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        if (!findThisFileInCurrentCommit) {
            System.out.println("File does not exist in that commit.");
        }
    }

    /**
     * Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     */
    public static void checkoutCommitAndFilename(String commitId, String filename) throws IOException {
        Commit commit = getCommitBySha1(commitId);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
        boolean findThisFileInCurrentCommit = false;
        for (String blobSha1 : commit.blobSha1List) {
            // recall that blob are stored like: .gitlet/blobs/40 bit sha1 value/hello.txt
            File blobFile = getTheOnlyFileInDir(join(GITLET_BLOBS_DIR, blobSha1));
            if (blobFile.getName().equals(filename)) {
                findThisFileInCurrentCommit = true;
                Path src = blobFile.toPath();
                Path dest = join(CWD, blobFile.getName()).toPath();
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        if (!findThisFileInCurrentCommit) {
            System.out.println("File does not exist in that commit.");
        }

    }

    public static void checkoutBranchName(String branchName) {

    }
}

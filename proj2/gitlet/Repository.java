package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static gitlet.Utils.*;

import static gitlet.StudentUtils.*;

/**
 * Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author Linde
 */
public class Repository {
    //TODO: what about move commit related function to Commit.java ?
    // and also let Commit.java have GITLET_BLOBS_DIR and GITLET_COMMITS_DIR
    // since many commit related function need these two directory

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
    public static final File GITLET_STAGE_FOR_REMOVE_DIR = join(GITLET_DIR, "stageForRemove");

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
    public static void init() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
        } else {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        GITLET_STAGE_FOR_ADD_DIR.mkdir();
        GITLET_STAGE_FOR_REMOVE_DIR.mkdir();
        GITLET_BLOBS_DIR.mkdir();
        GITLET_COMMITS_DIR.mkdir();
        GITLET_BRANCHES_DIR.mkdir();
        try {
            GITLET_ACTIVE_BRANCH_FILE.createNewFile();
            master_FILE.createNewFile();
            HEAD_FILE.createNewFile();
            writeContents(GITLET_ACTIVE_BRANCH_FILE, "master");
            setUpFirstCommit();
        } catch (IOException excp) {
            throw new GitletException(excp.getMessage());
        }

    }

    private static void setUpFirstCommit() {
        String message = "initial commit";
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
    public static void add(String filename) {
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
        if (currentCommit != null && currentCommit.getBlobSha1List().contains(fileSha1)) {
            System.exit(0);
        }

        Path src = file.toPath();
        Path dest = join(GITLET_STAGE_FOR_ADD_DIR, file.getName()).toPath();
        try {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException excp) {
            throw new GitletException(excp.getMessage());
        }
    }

    private static String getHeadCommitSha1() {
        String sha1 = readContentsAsString(HEAD_FILE);
        if (sha1.equals("")) {
            throw new GitletException("HEAD_FILE is empty");
        }
        return sha1;
    }

    //TODO: need to throw execption instead return null?
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
    public static void setUpCommit(String message) {
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
    private static void setUpCommit(String message, String parentSha1) {
        checkCommitFailureCases();
        Commit commit = getCommitBySha1(getHeadCommitSha1());
        assert commit != null;
        commit.modifyCommit(message, parentSha1);
        try {
            commit.addBlobs(GITLET_STAGE_FOR_ADD_DIR, GITLET_BLOBS_DIR);
            commit.removeBlobs(GITLET_STAGE_FOR_REMOVE_DIR);

            String commitSha1 = serializeCommit(commit);
            setupBranch(commitSha1);
        } catch (IOException excp) {
            throw new GitletException(excp.getMessage());
        }

        deleteAllFilesInDir(GITLET_STAGE_FOR_ADD_DIR);
        deleteAllFilesInDir(GITLET_STAGE_FOR_REMOVE_DIR);
    }

    private static void checkCommitFailureCases() {
        if (Objects.requireNonNull(GITLET_STAGE_FOR_ADD_DIR.list()).length == 0
                && Objects.requireNonNull(GITLET_STAGE_FOR_REMOVE_DIR.list()).length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
    }

    private static File getBlobFile(String blobSha1) {
        return getTheOnlyFileInDir(join(GITLET_BLOBS_DIR, blobSha1));
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
    private static String serializeCommit(Commit commit) {
        File commitFile = join(GITLET_COMMITS_DIR, "tempCommitName");
        writeObject(commitFile, commit);
        String commitSha1 = sha1((Object) readContents(commitFile));

        File commitDir = join(GITLET_COMMITS_DIR, commitSha1.substring(0, 2));
        if (!commitDir.exists()) {
            commitDir.mkdir();
        }
        Path src = commitFile.toPath();
        Path dest = join(commitDir, commitSha1).toPath();
        try {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            commitFile.delete();
        } catch (IOException excp) {
            throw new GitletException(excp.getMessage());
        }

        return commitSha1;
    }

    /**
     * set HEAD and active branch point to the newest commit.
     * recall that GITLET_ACTIVE_BRANCH_FILE store the name of the active branch.
     */
    private static void setupBranch(String theNewestCommitSha1) {
        String theNameOfTheActiveBranch = readContentsAsString(GITLET_ACTIVE_BRANCH_FILE);
        File activeBranchFile = join(GITLET_BRANCHES_DIR, theNameOfTheActiveBranch);
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
    public static void remove(String filename) {
        checkInitialize();
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
            for (String currCommitBlobSha1 : currentCommit.getBlobSha1List()) {
                File currCommitBlobFile = getBlobFile(currCommitBlobSha1);
                if (currCommitBlobFile.getName().equals(filename)) {
                    findFileInCurrentCommit = true;
                    Path src = currCommitBlobFile.toPath();
                    Path dest = join(GITLET_STAGE_FOR_REMOVE_DIR, filename).toPath();
                    try {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException excp) {
                        throw new GitletException(excp.getMessage());
                    }

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
        checkInitialize();
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

            // in log(), if a commit have multiple parents,
            // we only print the first parent
            if (!currentCommit.getParentSha1List().isEmpty()) {
                currentCommitSha1 = currentCommit.getParentSha1List().get(0);
            } else {
                currentCommitSha1 = null;
            }
        }

    }

    private static String formatDate(Date date) {
        // FYI: https://docs.oracle.com/javase/7/docs/api/java/util/Formatter.html
        return String.format("%1$ta %1$tb %1$te %1$tH:%1$tM:%1$tS %1$tY %1$tz", date);
        /*
            you can also use the following code to get the same output:
            SimpleDateFormat formatter =
                                 new SimpleDateFormat("E MMM dd hh:mm:ss yyyy Z");
            String formattedDateString = formatter.format(date);

            Since gitlet spec say I should use java.util.formatter,
            I didn't use SimpleDateFormat.
            if your Operating System's language is not English,
            it might have problem to display weekday and month,
            because weekday and month in String will be other language(e.g. Chinese),
            and the terminal may have problem to display that.
            you can try to run this program on Ubuntu
            */
    }

    public static void branch(String branchName) {
        checkInitialize();
        File branchFile = join(GITLET_BRANCHES_DIR, branchName);
        if (!branchFile.exists()) {
            try {
                branchFile.createNewFile();
            } catch (IOException excp) {
                throw new GitletException(excp.getMessage());
            }
        } else {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }

        String currentCommitSha1 = getHeadCommitSha1();
        writeContents(branchFile, currentCommitSha1);
    }

    public static void removeBranch(String branchName) {
        checkInitialize();
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
     * Copied from gitlet spec:
     * Takes the version of the file as it exists in the head commit and
     * puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one.
     * The new version of the file is not staged.
     */
    public static void checkoutFilename(String filename) {
        checkInitialize();
        boolean findThisFileInCurrentCommit = false;
        Commit commit = getCommitBySha1(getHeadCommitSha1());
        for (String blobSha1 : commit.getBlobSha1List()) {
            // recall that blob are stored like: .gitlet/blobs/40 bit sha1 value/hello.txt
            File blobFile = getBlobFile(blobSha1);
            if (blobFile.getName().equals(filename)) {
                findThisFileInCurrentCommit = true;
                Path src = blobFile.toPath();
                Path dest = join(CWD, blobFile.getName()).toPath();
                try {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException excp) {
                    throw new GitletException(excp.getMessage());
                }
            }
        }

        if (!findThisFileInCurrentCommit) {
            System.out.println("File does not exist in that commit.");
        }
    }

    /**
     * Copied from gitlet spec:
     * Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     */
    public static void checkoutCommitAndFilename(String commitId, String filename) {
        checkInitialize();
        Commit commit = getCommitBySha1(getCompletedSha1(commitId));
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        boolean findThisFileInCurrentCommit = false;
        for (String blobSha1 : commit.getBlobSha1List()) {
            // recall that blob are stored like: .gitlet/blobs/40 bit sha1 value/hello.txt
            File blobFile = getBlobFile(blobSha1);
            if (blobFile.getName().equals(filename)) {
                findThisFileInCurrentCommit = true;
                Path src = blobFile.toPath();
                Path dest = join(CWD, blobFile.getName()).toPath();
                try {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException excp) {
                    throw new GitletException(excp.getMessage());
                }
            }
        }

        if (!findThisFileInCurrentCommit) {
            System.out.println("File does not exist in that commit.");
        }

    }

    /**
     * @param commitId the abbreviated commit sha1
     */
    private static String getCompletedSha1(String commitId) {
        String completedSha1 = null;
        int len = commitId.length();
        if (commitId.length() < 2) {
            throw new GitletException("The commit id must have at least 2 digits in length.");
        }
        String firstTwoSha1 = commitId.substring(0, 2);
        File commitDir = join(GITLET_COMMITS_DIR, firstTwoSha1);
        List<String> filenamesInCommitDir = plainFilenamesIn(commitDir);
        if (filenamesInCommitDir == null) {
            return null;
        }

        /*
        let's say commitId is 3ac
        and in the .gitlet/commits/3a/ directory
        there are two files: 3acb12 and 3ac891
        3ac is not long enough to distinguish the two files,
        we don't know what commit should we pick.
         */
        boolean foundAFileSimilarToCommitId = false;
        for (String filename : filenamesInCommitDir) {
            if (filename.substring(0, len).equals(commitId)) {
                // if it has already found a file similar to commit id,
                // and now it found again, that means there are at least
                // two files that are similar to commit id
                if (foundAFileSimilarToCommitId) {
                    throw new GitletException("commit id is not long enough to distinguish a commit");
                } else {
                    foundAFileSimilarToCommitId = true;
                    completedSha1 = filename;
                }
            }
        }

        return completedSha1;
    }

    /**
     * Copied from gitlet spec:
     * Takes all files in the commit at the head of the given branch,
     * and puts them in the working directory, overwriting the versions
     * of the files that are already there if they exist.
     * Also, at the end of this command, the given branch will now be
     * considered the current branch (HEAD). Any files that are tracked
     * in the current branch but are not present in the checked-out branch
     * are deleted. The staging area is cleared, unless the checked-out
     * branch is the current branch
     */
    public static void checkoutBranchName(String targetBranchName) {
        checkInitialize();
        File targetBranchFile = join(GITLET_BRANCHES_DIR, targetBranchName);
        if (!targetBranchFile.exists()) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        String theNameOfTheActiveBranch = readContentsAsString(GITLET_ACTIVE_BRANCH_FILE);
        if (targetBranchName.equals(theNameOfTheActiveBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        String commitAtTargetBranchSha1 = readContentsAsString(targetBranchFile);
        Commit commitAtTargetBranch = getCommitBySha1(commitAtTargetBranchSha1);
        checkIfCWDFileWillBeOverwrittenByCommit(commitAtTargetBranch);

        // Any files that are tracked in the current branch
        // but are not present in the checked-out branch are deleted.
        Commit commitAtCurrentBranch = getCommitBySha1(getHeadCommitSha1());
        List<String> filenamesInCurrCommit = getFilenamesInCommit(commitAtCurrentBranch);
        List<String> filenamesInTargetCommit = getFilenamesInCommit(commitAtTargetBranch);
        for (String filenameInCurrCommit : filenamesInCurrCommit) {
            if (!filenamesInTargetCommit.contains(filenameInCurrCommit)) {
                join(CWD, filenameInCurrCommit).delete();
            }
        }

        for (String blobSha1 : commitAtTargetBranch.getBlobSha1List()) {
            File file = getBlobFile(blobSha1);
            Path src = file.toPath();
            Path dest = join(CWD, file.getName()).toPath();
            try {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException excp) {
                throw new GitletException(excp.getMessage());
            }
        }

        writeContents(GITLET_ACTIVE_BRANCH_FILE, targetBranchName);
        writeContents(HEAD_FILE, commitAtTargetBranchSha1);
        deleteAllFilesInDir(GITLET_STAGE_FOR_ADD_DIR);
        deleteAllFilesInDir(GITLET_STAGE_FOR_REMOVE_DIR);
    }

    public static void status() {
        checkInitialize();
        String theNameOfTheActiveBranch = readContentsAsString(GITLET_ACTIVE_BRANCH_FILE);
        System.out.println("=== Branches ===");
        System.out.println("*" + theNameOfTheActiveBranch);
        for (String filename : Objects.requireNonNull(plainFilenamesIn(GITLET_BRANCHES_DIR))) {
            if (filename.equals(theNameOfTheActiveBranch) ||
                    filename.equals("HEAD") || filename.equals("activeBranch")) {
                continue;
            }
            System.out.println(filename);
        }
        System.out.println();

        System.out.println("== Staged Files ===");
        for (String filename : Objects.requireNonNull(plainFilenamesIn(GITLET_STAGE_FOR_ADD_DIR))) {
            System.out.println(filename);
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        for (String filename : Objects.requireNonNull(plainFilenamesIn(GITLET_STAGE_FOR_REMOVE_DIR))) {
            System.out.println(filename);
        }
        System.out.println();

        // TreeMap is sorted
        TreeMap<String, String> map = getModifiedButNotStagedFilesInCWD();
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String filename : map.keySet()) {
            System.out.println(filename + "(" + map.get(filename) + ")");
        }
        System.out.println();


        // The final category ("Untracked Files") is for files present in the
        // working directory but neither staged for addition nor tracked.
        // This includes files that have been staged for removal,
        // but then re-created without Gitlet’s knowledge.
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());
        List<String> filenamesInCommit = getFilenamesInCommit(currentCommit);
        System.out.println("=== Untracked Files ===");
        for (File CWDFile : CWD.listFiles()) {
            // if a file is present in the CWD but neither stagedForAddDir nor tracked
            boolean condition1 = !filenamesInCommit.contains(CWDFile.getName())
                    && !join(GITLET_STAGE_FOR_ADD_DIR, CWDFile.getName()).exists();
            // if there is a file both exist in CWD and stagedForRemoveDir
            boolean condition2 = join(GITLET_STAGE_FOR_REMOVE_DIR, CWDFile.getName()).exists();
            if (condition1 || condition2) {
                // TODO: I don't know why .gitlet is not hidden and it will be viewed as a file
                if (CWDFile.getName().equals(".gitlet")) {
                    continue;
                }
                System.out.println(CWDFile.getName());
            }
        }
        System.out.println();

    }

    private static List<File> getFilesInCommit(Commit commit) {
        List<File> list = new ArrayList<>();
        for (String blobSha1 : commit.getBlobSha1List()) {
            File file = getBlobFile(blobSha1);
            list.add(file);
        }
        Collections.sort(list);
        return list;
    }

    private static List<String> getFilenamesInCommit(Commit commit) {
        List<String> list = new ArrayList<>();
        List<File> filesInCommit = getFilesInCommit(commit);
        for (File fileInCommit : filesInCommit) {
            list.add(fileInCommit.getName());
        }

        return list;
    }

    /**
     * Copied from gitlet spec:
     * A file in the working directory is "modified but not staged" if it is:
     * Tracked in the current commit, changed in the working directory, but not staged; or
     * Staged for addition, but with different contents than in the working directory; or
     * Staged for addition, but deleted in the working directory; or
     * Not staged for removal, but tracked in the current commit and deleted from the working directory.
     *
     * @return the names of the files and the states of the files
     */
    private static TreeMap<String, String> getModifiedButNotStagedFilesInCWD() {

        TreeMap<String, String> map = new TreeMap<>();
        //List<String> list = new ArrayList<>();
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());

        for (String blobSha1 : currentCommit.getBlobSha1List()) {
            File blobFile = getBlobFile(blobSha1);
            File CWDFileTrackedInCurrentCommit = join(CWD, blobFile.getName());
            // if a file in CWD is tracked in the current commit
            if (CWDFileTrackedInCurrentCommit.exists()) {
                // and it is changed in the working directory
                if (!sha1(readContentsAsString(CWDFileTrackedInCurrentCommit)).equals(blobSha1)) {
                    // but it is not staged
                    if (!join(GITLET_STAGE_FOR_ADD_DIR, blobFile.getName()).exists()
                            || !join(GITLET_STAGE_FOR_REMOVE_DIR, blobFile.getName()).exists()) {
                        map.put(blobFile.getName(), "modified");
                    }
                }
            }
        }

        for (File file : Objects.requireNonNull(GITLET_STAGE_FOR_ADD_DIR.listFiles())) {
            if (join(CWD, file.getName()).exists()) {
                // if the file is staged for addition,
                // but with different contents than in the working directory
                if (!sha1(readContentsAsString(file)).equals(
                        sha1(readContentsAsString(join(CWD, file.getName()))))) {
                    map.put(file.getName(), "modified");
                }
            } else {
                // if the "file in stagedForAddDir" is deleted
                // in the working directory
                map.put(file.getName(), "deleted");
            }
        }

        // there is a file tracked in current commit, but it disappears in CWD,
        // and it is not in stageForRemoveDir
        for (String blobSha1 : currentCommit.getBlobSha1List()) {
            File blobFile = getBlobFile(blobSha1);
            if (!join(CWD, blobFile.getName()).exists() &&
                    !join(GITLET_STAGE_FOR_REMOVE_DIR, blobFile.getName()).exists()) {
                map.put(blobFile.getName(), "deleted");
            }
        }
        //Collections.sort(list);
        return map;
    }

    /**
     * Copied from gitlet spec:
     * Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch’s head to that commit node.
     *
     * @param commitId commitId can be abbreviated as for checkout
     */
    public static void reset(String commitId) {
        checkInitialize();
        String targetCommitId = getCompletedSha1(commitId);
        Commit targetCommit = getCommitBySha1(targetCommitId);
        if (targetCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        checkIfCWDFileWillBeOverwrittenByCommit(targetCommit);

        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());
        for (File CWDFile : CWD.listFiles()) {
            String CWDFileSha1 = sha1(readContents(CWDFile));
            // if a CWDFile is tracked in currentCommit,
            // but not in targetCommit, then we remove it
            if (currentCommit.getBlobSha1List().contains(CWDFileSha1)
                    && !targetCommit.getBlobSha1List().contains(CWDFileSha1)) {
                CWDFile.delete();
            }
        }

        // check out all the files in the targetCommit
        for (String blobSha1 : targetCommit.getBlobSha1List()) {
            File blobFile = getBlobFile(blobSha1);
            Path src = blobFile.toPath();
            Path dest = join(CWD, blobFile.getName()).toPath();
            try {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException excp) {
                throw new GitletException(excp.getMessage());
            }
        }

        // Also moves the current branch’s head to that commit node.
        writeContents(HEAD_FILE, targetCommitId);
        String theNameOfActiveBranch = readContentsAsString(GITLET_ACTIVE_BRANCH_FILE);
        File activeBranchFile = join(GITLET_BRANCHES_DIR, theNameOfActiveBranch);
        writeContents(activeBranchFile, targetCommit);

        // you may ask here we modify HEAD_FILE, but why we don't modify ACTIVE_BRANCH_FILE?
        // recall that if HEAD is in branch_A, and then it points to branch_B, in this case we
        // need to modify ACTIVE_BRANCH_FILE,
        // now what HEAD doing is to point to a previous commit of a branch,
        // it doesn't point to another branch, so we don't need to modify ACTIVE_BRANCH
    }

    /**
     * if we gonna switch to a certain commit, and that commit will overwrite
     * a file which is untracked by current commit, we will exit the entire program
     */
    private static void checkIfCWDFileWillBeOverwrittenByCommit(Commit targetCommit) {
        List<String> filenamesInTargetCommit = getFilenamesInCommit(targetCommit);
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());
        List<String> filenamesInCurrCommit = getFilenamesInCommit(currentCommit);

        for (File CWDFile : CWD.listFiles()) {
            if (CWDFile.getName().equals(".gitlet")) {
                continue;
            }
            boolean condition1 = !filenamesInCurrCommit.contains(CWDFile.getName());
            boolean condition2 = filenamesInTargetCommit.contains(CWDFile.getName());
            // if a CWDFile is untracked by current commit
            // and the target commit will overwrite the CWDFile
            if (condition1 && condition2) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(0);
            }
        }


        //TODO: in the comment, it says mention "current commit",
        // but the code doesn't have currentCommit

    }

    public static void merge(String targetBranchName) {
        checkMergeFailureCases(targetBranchName);
        Commit commitAtTargetBranch = getCommitAtTargetBranch(targetBranchName);
        Commit commitAtCurrentBranch = getCommitBySha1(getHeadCommitSha1());
        Commit commitAtSplitPoint = getCommitAtSplitPoint(commitAtTargetBranch, commitAtCurrentBranch);

        if (commitAtSplitPoint == commitAtTargetBranch) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (commitAtSplitPoint == commitAtCurrentBranch) {
            checkoutBranchName(targetBranchName);
            System.exit(0);
        }

        // case 1
        for (String blobSha1 : commitAtTargetBranch.getBlobSha1List()) {
            File blobFile = getBlobFile(blobSha1);

        }

    }

    private static void checkMergeFailureCases(String targetBranchName) {
        if (GITLET_STAGE_FOR_ADD_DIR.list().length != 0
                || GITLET_STAGE_FOR_REMOVE_DIR.list().length != 0) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }

        File targetBranchFile = join(GITLET_BRANCHES_DIR, targetBranchName);
        if (!targetBranchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }

        String theNameOfActiveBranch = readContentsAsString(GITLET_ACTIVE_BRANCH_FILE);
        if (targetBranchName.equals(theNameOfActiveBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        checkCommitFailureCases();

        Commit commitAtTargetBranch = getCommitAtTargetBranch(targetBranchName);
        checkIfCWDFileWillBeOverwrittenByCommit(commitAtTargetBranch);

    }

    private static Commit getCommitAtTargetBranch(String targetBranchName) {
        File targetBranchFile = join(GITLET_BRANCHES_DIR, targetBranchName);
        return getCommitBySha1(readContentsAsString(targetBranchFile));
    }


    /**
     * This is similar to find the latest common ancestor of two linked-list
     */
    private static Commit getCommitAtSplitPoint(Commit commit1, Commit commit2) {
        Commit p1 = commit1;
        Commit p2 = commit2;
        while (p1 != p2) {
            p1 = (p1 != null ? getTheFirstParentOfGivenCommit(p1) : commit2);
            p2 = (p2 != null ? getTheFirstParentOfGivenCommit(p2) : commit1);
        }
        return p1;
    }

    /**
     * commit can have more than one parent,
     * this function will return the first parent
     */
    private static Commit getTheFirstParentOfGivenCommit(Commit commit) {
        String firstParentSha1 = commit.getParentSha1List().get(0);
        return getCommitBySha1(firstParentSha1);
    }


}

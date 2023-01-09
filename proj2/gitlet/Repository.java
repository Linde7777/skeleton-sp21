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
     * we copy blobs from .gitlet/blobs and paste them in this directory,
     * then rename it(because blobs is just content, their name is just their sha1)
     */
    public static final File GITLET_BLOBS_CONVERT_DIR = join(GITLET_DIR, "convert");

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
        GITLET_BLOBS_CONVERT_DIR.mkdir();
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
     * add file to stagedForAddDir
     *
     * @param CWDFilename the file we want to add
     */
    public static void add(String CWDFilename) {
        checkInitialize();
        File CWDFile = join(CWD, CWDFilename);
        if (!CWDFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        String CWDFileSha1 = sha1((Object) readContents(CWDFile));
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());
        TreeMap<String, String> map = currentCommit.getMap();
        // If the current working version of the file is identical to the
        // version in the current commit, do not stage it to be added,
        if (map.containsKey(CWDFilename) && map.get(CWDFilename).equals(CWDFileSha1)) {
            // and remove it from the staging area if it is already
            // there (as can happen when a file is changed, added,
            // and then changed back to it’s original version).
            File fileInStagedForAdd = join(GITLET_STAGE_FOR_ADD_DIR, CWDFilename);
            if (fileInStagedForAdd.exists()) {
                fileInStagedForAdd.delete();
            }

            // The file will no longer be staged for removal (see gitlet rm),
            // if it was at the time of the command.
            File fileInStagedForRemove = join(GITLET_STAGE_FOR_REMOVE_DIR, CWDFilename);
            if (fileInStagedForRemove.exists()) {
                fileInStagedForRemove.delete();
            }
        } else {
            // if a file haven't been tracked
            // or a file is tracked, but it has been modified
            // we need to add it to staging area
            Path src = CWDFile.toPath();
            Path dest = join(GITLET_STAGE_FOR_ADD_DIR, CWDFile.getName()).toPath();
            try {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException excp) {
                throw new GitletException(excp.getMessage());
            }
        }


    }

    /**
     * notice that this is the public method
     */
    public static void setUpCommit(String message) {
        if (message == null || message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        checkInitialize();
        checkIfStagedDirsAreEmpty();
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
        Commit commit = getCommitBySha1(getHeadCommitSha1());
        commit.modifyCommit(message, parentSha1,
                GITLET_STAGE_FOR_ADD_DIR, GITLET_BLOBS_DIR, GITLET_STAGE_FOR_REMOVE_DIR);
        String commitSha1 = serializeCommit(commit);
        setupBranch(commitSha1);
        deleteAllFilesInDir(GITLET_STAGE_FOR_ADD_DIR);
        deleteAllFilesInDir(GITLET_STAGE_FOR_REMOVE_DIR);
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
     * @param targetFilename the name of the file that we want to remove
     */
    public static void remove(String targetFilename) {
        checkInitialize();
        boolean findFileInStageForAddDir = false;
        File[] filesInStageForAddDir = GITLET_STAGE_FOR_ADD_DIR.listFiles();
        // if filesInStageForAddDir is empty, it is ok, we don't need to do anything,
        // and then we move down to check if we need to delete file from current commit.
        for (File file : filesInStageForAddDir) {
            if (targetFilename.equals(file.getName())) {
                findFileInStageForAddDir = true;
                file.delete();
            }
        }

        boolean findFileInCurrentCommit = false;
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());
        List<String> filenamesList = getFilenamesInCommit(currentCommit);
        if (filenamesList.contains(targetFilename)) {
            findFileInCurrentCommit = true;
            String blobSha1 = currentCommit.getMap().get(targetFilename);
            File blob = getBlob(blobSha1);
            Path src = blob.toPath();
            Path dest = join(GITLET_STAGE_FOR_REMOVE_DIR, targetFilename).toPath();
            try {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException excp) {
                throw new GitletException(excp.getMessage());
            }

            if (join(CWD, targetFilename).exists()) {
                join(CWD, targetFilename).delete();
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
     * Takes the version of the file as it exists in the commit with the given id,
     * and puts it in the working directory, overwriting the version of the file
     * that’s already there if there is one. The new version of the file is not staged.
     */
    public static void checkoutCommitAndFilename(String targetCommitId, String targetFilename) {
        checkInitialize();
        Commit targetCommit = getCommitBySha1(getCompletedSha1(targetCommitId));
        checkoutCommitAndFilename(targetCommit, targetFilename);
    }

    private static void checkoutCommitAndFilename(Commit targetCommit, String targetFilename) {
        if (targetCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        List<String> filenamesList = getFilenamesInCommit(targetCommit);
        if (!filenamesList.contains(targetFilename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        TreeMap<String, String> map = targetCommit.getMap();
        String blobSha1 = map.get(targetFilename);
        File blob = getBlob(blobSha1);
        Path src = blob.toPath();
        Path dest = join(CWD, targetFilename).toPath();
        try {
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException excp) {
            throw new GitletException(excp.getMessage());
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
        Commit headCommit = getCommitBySha1(getHeadCommitSha1());
        checkoutCommitAndFilename(headCommit, filename);
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

        String targetCommitSha1 = readContentsAsString(targetBranchFile);
        Commit targetCommit = getCommitBySha1(targetCommitSha1);

        checkoutAllFilesInCommit(targetCommit);

        writeContents(GITLET_ACTIVE_BRANCH_FILE, targetBranchName);
        writeContents(HEAD_FILE, targetCommitSha1);
        deleteAllFilesInDir(GITLET_STAGE_FOR_ADD_DIR);
        deleteAllFilesInDir(GITLET_STAGE_FOR_REMOVE_DIR);
    }

    private static void checkoutAllFilesInCommit(Commit targetCommit) {
        checkIfUntrackedFileWillBeOverwrittenByCommit(targetCommit);

        // Any files that are tracked in the current branch
        // but are not present in the checked-out branch are deleted.
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());
        List<String> filenamesInCurrCommit = getFilenamesInCommit(currentCommit);
        List<String> filenamesInTargetCommit = getFilenamesInCommit(targetCommit);
        for (String filenameInCurrCommit : filenamesInCurrCommit) {
            if (!filenamesInTargetCommit.contains(filenameInCurrCommit)) {
                join(CWD, filenameInCurrCommit).delete();
            }
        }

        TreeMap<String, String> map = targetCommit.getMap();
        for (String filename : map.keySet()) {
            String fileSha1 = map.get(filename);
            File blob = getBlob(fileSha1);
            Path src = blob.toPath();
            Path dest = join(CWD, filename).toPath();
            try {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException excp) {
                throw new GitletException(excp.getMessage());
            }
        }

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

        System.out.println("=== Staged Files ===");
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
            // TODO: I don't know why .gitlet is not hidden and it will be viewed as a file
            if (CWDFile.getName().equals(".gitlet")) {
                continue;
            }
            // if a file is present in the CWD but neither stagedForAddDir nor tracked
            boolean condition1 = !filenamesInCommit.contains(CWDFile.getName())
                    && !join(GITLET_STAGE_FOR_ADD_DIR, CWDFile.getName()).exists();
            // if there is a file both exist in CWD and stagedForRemoveDir
            boolean condition2 = join(GITLET_STAGE_FOR_REMOVE_DIR, CWDFile.getName()).exists();
            if (condition1 || condition2) {
                System.out.println(CWDFile.getName());
            }
        }
        System.out.println();

    }

    /**
     * Copied from gitlet spec:
     * Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch’s head to that commit node.
     *
     * @param uncompletedCommitId commitId can be abbreviated as for checkout
     */
    public static void resetWithUncompletedCommitId(String uncompletedCommitId) {
        checkInitialize();
        String completedCommitId = getCompletedSha1(uncompletedCommitId);
        resetWithCompletedCommitId(completedCommitId);
    }

    private static void resetWithCompletedCommitId(String targetCommitId) {
        Commit targetCommit = getCommitBySha1(targetCommitId);
        if (targetCommit == null) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        checkoutAllFilesInCommit(targetCommit);
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
    private static void checkIfUntrackedFileWillBeOverwrittenByCommit(Commit targetCommit) {
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
    }

    public static void merge(String targetBranchName) {
        checkMergeFailureCases(targetBranchName);
        Commit targetCommit = getCommitAtTargetBranch(targetBranchName);
        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());
        Commit spiltPointCommit = getCommitAtSplitPoint(targetCommit, currentCommit);

        if (spiltPointCommit == targetCommit) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (spiltPointCommit == currentCommit) {
            checkoutBranchName(targetBranchName);
            System.exit(0);
        }

        List<String> filenamesAtSpiltPoint = getFilenamesInCommit(spiltPointCommit);
        List<String> filenamesAtCurrCommit = getFilenamesInCommit(currentCommit);
        List<String> filenamesAtTargetCommit = getFilenamesInCommit(targetCommit);
        TreeMap<String, String> spiltMap = spiltPointCommit.getMap();
        TreeMap<String, String> currMap = currentCommit.getMap();
        TreeMap<String, String> targetMap = targetCommit.getMap();

        mergeCase1(spiltPointCommit, currentCommit, targetCommit);
        mergeCase2(spiltPointCommit, currentCommit, targetCommit);
        mergeCase3(spiltPointCommit, currentCommit, targetCommit);
        //mergeCase4(spiltPointCommit, currentCommit, targetCommit);
        mergeCase5(spiltPointCommit, targetCommit);
        mergeCase6(spiltPointCommit, currentCommit, targetCommit);
        //mergeCase7(spiltPointCommit, currentCommit, targetCommit);
    }

    /**
     * Any files modified in different ways in the current and given
     * branches are in conflict. "Modified in different ways" can mean
     * that the contents of both are changed and different from other,
     * or the contents of one are changed and the other file is deleted,
     * or the file was absent at the split point and has different contents
     * in the given and current branches.
     * In this case, replace the contents of the conflicted file with some
     * certain symbols and words
     */
    private static void mergeCase8(Commit spiltPointCommit,
                                   Commit currentCommit, Commit targetCommit) {

    }

    /**
     * Any files present at the split point, unmodified in the given branch,
     * and absent in the current branch should remain absent.
     */
    private static void mergeCase7(Commit spiltPointCommit,
                                   Commit currentCommit, Commit targetCommit) {
        TreeMap<String, String> spiltMap = spiltPointCommit.getMap();
        TreeMap<String, String> currMap = currentCommit.getMap();
        TreeMap<String, String> targetMap = targetCommit.getMap();
        // we don't need to do anything
    }

    /**
     * Any files present at the split point, unmodified in the current
     * branch, and absent in the given branch should be removed (and untracked).
     */
    private static void mergeCase6(Commit spiltPointCommit,
                                   Commit currentCommit, Commit targetCommit) {
        TreeMap<String, String> spiltMap = spiltPointCommit.getMap();
        TreeMap<String, String> currMap = currentCommit.getMap();
        TreeMap<String, String> targetMap = targetCommit.getMap();
        for (String spiltFilename : spiltMap.keySet()) {
            String spiltFileSha1 = spiltMap.get(spiltFilename);
            if (currMap.containsKey(spiltFilename) && !targetMap.containsKey(spiltFilename)) {
                String currFileSha1 = currMap.get(spiltFilename);
                if (currFileSha1.equals(spiltFileSha1)) {
                    remove(spiltFilename);
                }
            }
        }
    }

    /**
     * Any files that were not present at the split point and
     * are present only in the given branch should be checked out and staged.
     */
    private static void mergeCase5(Commit spiltPointCommit,
                                   Commit targetCommit) {
        TreeMap<String, String> spiltMap = spiltPointCommit.getMap();
        TreeMap<String, String> targetMap = targetCommit.getMap();
        for (String targetFilename : targetMap.keySet()) {
            if (!spiltMap.containsKey(targetFilename)) {
                checkoutCommitAndFilename(targetCommit, targetFilename);
                Path src = join(CWD, targetFilename).toPath();
                Path dest = join(GITLET_STAGE_FOR_ADD_DIR, targetFilename).toPath();
                try {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException excp) {
                    throw new GitletException(excp.getMessage());
                }
            }
        }

    }

    /**
     * Any files that were not present at the split point and are
     * present only in the current branch should remain as they are.
     */
    private static void mergeCase4(Commit spiltPointCommit,
                                   Commit currentCommit, Commit targetCommit) {
        // we don't need to do anything
    }

    /**
     * Any files that have been modified in both the current and given
     * branch in the same way (i.e., both files now have the same
     * content or were both removed) are left unchanged by the merge.
     * If a file was removed from both the current and given branch,
     * but a file of the same name is present in the working directory,
     * it is left alone and continues to be absent (not tracked nor staged) in the merge.
     */
    private static void mergeCase3(Commit spiltPointCommit,
                                   Commit currentCommit, Commit targetCommit) {
        TreeMap<String, String> spiltMap = spiltPointCommit.getMap();
        TreeMap<String, String> currMap = currentCommit.getMap();
        TreeMap<String, String> targetMap = targetCommit.getMap();
        for (String targetFilename : targetMap.keySet()) {
            if (spiltMap.containsKey(targetFilename) && currMap.containsKey(targetFilename)) {
                //both be modified
                String targetFileSha1 = targetMap.get(targetFilename);
                String currFileSha1 = currMap.get(targetFilename);
                String spiltFileSha1 = spiltMap.get(targetFilename);
                if (targetFileSha1.equals(currFileSha1) && !spiltFileSha1.equals(targetFileSha1)) {
                    // are left unchanged by the merge
                    // i.e. we don't need to do anything
                    // TODO: should I delete this function?
                }
            }

        }

        for (String spiltFilename : spiltMap.keySet()) {
            if (!targetMap.containsKey(spiltFilename) && !currMap.containsKey(spiltFilename)) {
                // the file will be left alone and continues to be absent
                // (not tracked nor staged) in the merge.
                // i.e. do nothing
                //TODO: should I delete this code?
            }
        }
    }

    /**
     * Any files that have been modified in the current branch but
     * not in the given branch since the split point should stay as they are.
     */
    private static void mergeCase2(Commit spiltPointCommit,
                                   Commit currentCommit, Commit targetCommit) {
        TreeMap<String, String> spiltMap = spiltPointCommit.getMap();
        TreeMap<String, String> currMap = currentCommit.getMap();
        TreeMap<String, String> targetMap = targetCommit.getMap();
        for (String targetFilename : targetMap.keySet()) {
            if (spiltMap.containsKey(targetFilename) && currMap.containsKey(targetFilename)) {
                String currFileSha1 = currMap.get(targetFilename);
                String targetFileSha1 = targetMap.get(targetFilename);
                String spiltFileSha1 = spiltMap.get(targetFilename);
                if (!currFileSha1.equals(spiltFileSha1) && targetFileSha1.equals(spiltFileSha1)) {
                    // TODO: should I delete this function?
                    // we do nothing
                }
            }
        }
    }

    /**
     * Any files that have been modified in the given branch since the split point,
     * but not modified in the current branch since the split point should be changed
     * to their versions in the given branch (checked out from the commit at the
     * front of the given branch). These files should then all be automatically staged.
     */
    private static void mergeCase1(Commit spiltPointCommit,
                                   Commit currentCommit, Commit targetCommit) {
        TreeMap<String, String> spiltMap = spiltPointCommit.getMap();
        TreeMap<String, String> currMap = currentCommit.getMap();
        TreeMap<String, String> targetMap = targetCommit.getMap();
        for (String targetFilename : targetMap.keySet()) {
            if (spiltMap.containsKey(targetFilename) && currMap.containsKey(targetFilename)) {
                String targetFileSha1 = targetMap.get(targetFilename);
                String spiltFileSha1 = spiltMap.get(targetFilename);
                String currFileSha1 = currMap.get(targetFilename);
                if (!targetFileSha1.equals(spiltFileSha1) && currFileSha1.equals(spiltFileSha1)) {
                    checkoutCommitAndFilename(targetCommit, targetFilename);
                    Path src = join(CWD, targetFilename).toPath();
                    Path dest = join(GITLET_STAGE_FOR_ADD_DIR, targetFilename).toPath();
                    try {
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException excp) {
                        throw new GitletException(excp.getMessage());
                    }
                }
            }
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

        checkIfStagedDirsAreEmpty();

        Commit commitAtTargetBranch = getCommitAtTargetBranch(targetBranchName);
        checkIfUntrackedFileWillBeOverwrittenByCommit(commitAtTargetBranch);

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

    private static void checkInitialize() {
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    private static List<String> getFilenamesInCommit(Commit commit) {
        List<String> filenamesList = new ArrayList<>();
        TreeMap<String, String> map = commit.getMap();
        filenamesList.addAll(map.keySet());

        return filenamesList;
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

        // filename->"modified"     filename->"deleted"
        TreeMap<String, String> fileStateMap = new TreeMap<>();

        Commit currentCommit = getCommitBySha1(getHeadCommitSha1());

        // Tracked in the current commit, changed in the working directory, but not staged
        List<String> filenamesList = getFilenamesInCommit(currentCommit);
        TreeMap<String, String> commitMap = currentCommit.getMap();
        for (String filename : filenamesList) {
            File CWDFile = join(CWD, filename);
            if (!CWDFile.exists()) {
                continue;
            }
            String trackedFileSha1 = commitMap.get(filename);
            String CWDFileSha1 = sha1((Object) readContents(join(CWD, filename)));
            if (!CWDFileSha1.equals(trackedFileSha1)) {
                if (!join(GITLET_STAGE_FOR_ADD_DIR, filename).exists()) {
                    fileStateMap.put(filename, "modified");
                }
            }
        }

        for (File file : Objects.requireNonNull(GITLET_STAGE_FOR_ADD_DIR.listFiles())) {
            if (join(CWD, file.getName()).exists()) {
                // if the file is staged for addition,
                // but with different contents than in the working directory
                if (!sha1(readContentsAsString(file)).equals(
                        sha1(readContentsAsString(join(CWD, file.getName()))))) {
                    fileStateMap.put(file.getName(), "modified");
                }
            } else {
                // Staged for addition, but deleted in the working directory
                fileStateMap.put(file.getName(), "deleted");
            }
        }

        // there is a file tracked in current commit, but it disappears in CWD,
        // and it is not in stageForRemoveDir
        for (String filename : filenamesList) {
            if (!join(CWD, filename).exists()
                    && !join(GITLET_STAGE_FOR_REMOVE_DIR, filename).exists()) {
                fileStateMap.put(filename, "deleted");
            }
        }

        return fileStateMap;
    }

    private static String getHeadCommitSha1() {
        return readContentsAsString(HEAD_FILE);
    }

    private static Commit getCommitBySha1(String commitSha1) {
        if (commitSha1.length() < 40) {
            throw new GitletException("Commit Sha1 is too short");
        }
        File file = join(GITLET_COMMITS_DIR, commitSha1.substring(0, 2), commitSha1);
        if (!file.exists()) {
            throw new GitletException("Commit File does not exist");
        }
        return readObject(file, Commit.class);
    }

    private static File getBlob(String blobSha1) {
        return join(GITLET_BLOBS_DIR, blobSha1);
    }

    private static void checkIfStagedDirsAreEmpty() {
        if (Objects.requireNonNull(GITLET_STAGE_FOR_ADD_DIR.list()).length == 0
                && Objects.requireNonNull(GITLET_STAGE_FOR_REMOVE_DIR.list()).length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
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

    /**
     * @param commitId the abbreviated commit sha1
     */
    private static String getCompletedSha1(String commitId) {
        String completedSha1 = null;
        int len = commitId.length();
        if (commitId.length() < 2) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
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
                    System.out.println("No commit with that id exists.");
                    System.exit(0);
                } else {
                    foundAFileSimilarToCommitId = true;
                    completedSha1 = filename;
                }
            }
        }

        return completedSha1;
    }

}

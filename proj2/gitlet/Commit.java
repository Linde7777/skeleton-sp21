package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import java.util.TreeMap;

import static gitlet.Utils.*;

/**
 * Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 * @author Linde
 */
public class Commit implements Serializable {
    /**
     * The message of this Commit.
     */
    private String message;

    public String getMessage() {
        return message;
    }

    /**
     * The time when this Commit was created.
     */
    private Date timeStamp;

    public Date getTimeStamp() {
        return timeStamp;
    }

    /**
     * where store the mapping of filename and its blob
     */
    private TreeMap<String, String> map = new TreeMap<>();

    public TreeMap<String, String> getMap() {
        return map;
    }

    /**
     * where store the sha1 values of parents of this commit
     */
    private LinkedList<String> parentSha1List;

    public LinkedList<String> getParentSha1List() {
        return parentSha1List;
    }

    /**
     * it will be only called when we initialize a gitlet repo,
     * i.e. when we create the first commit.
     * it will initialize Commit with given variable,
     * the timestamp will be set as
     * Thu Jan 01 00:00:00 CST 1970
     * <p>
     *
     * @param message The message of the commit
     */
    public Commit(String message) {
        this.timeStamp = new Date(0);
        this.message = message;
        this.map = new TreeMap<>();
        this.parentSha1List = new LinkedList<>();
    }

    public void modifyCommit(String message, String parentSha1) {
        this.message = message;
        this.timeStamp = new Date();
        // this.parentSha1List is copied from its parent,
        // it needs to be flushed
        this.parentSha1List = new LinkedList<>();
        this.parentSha1List.add(parentSha1);
    }

    /**
     * Add files into commit.
     * <p>
     * Recall that Repository.add() have make sure that the
     * files in stagedForAdd is "tracked but be modified" or "untracked".
     * <p>
     * this function will put the filename->fileSha1 mapping into this commit.
     * and copy files from the stagedForAdd directory
     * to the .gitlet/blobs directory,
     * <p>
     * e.g.
     * hello.txt's sha1 is 7afbac, we call it hello.txt version 1,
     * when this function is executed, we will create a mapping hello.txt->7afbac
     * and the content of hello.txt will be stored at .gitlet/blobs/7afbac,
     * <p>
     * now we modify the content of hello.txt, its sha1 is a127db,
     * we call it hello.txt version 2, when this function is executed,
     * we will update the mapping hello.txt->a127db,
     * and the content of hello.txt will be stored at .gitlet/blobs/a127db
     */
    public void addBlobsToCommit(File stagedForAddDir, File blobsDir) throws IOException {
        for (File stagedFile : Objects.requireNonNull(stagedForAddDir.listFiles())) {
            String stagedFileSha1 = sha1((Object) readContents(stagedFile));
            String stagedFileName = stagedFile.getName();
            // Recall that Repository.add() have make sure that the
            // files in stagedForAdd is "tracked but be modified" or "untracked".

            // if it is tracked, it must be modified
            if (map.containsKey(stagedFileName)) {
                String oldSha1Value = map.get(stagedFileName);
                // we can only replace the reference, we can not replace the blob
                // because other commit may refer the blob
                map.replace(stagedFileName, oldSha1Value, stagedFileSha1);
            } else {
                map.put(stagedFileName, stagedFileSha1);
            }

            Path src = stagedFile.toPath();
            Path dest = join(blobsDir, stagedFileSha1).toPath();
            try {
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException excp) {
                throw new GitletException(excp.getMessage());
            }

        }
    }


    /**
     * if there are files in stageForRemoveDir,
     * remove their reference from the current Commit,
     * we can not remove the blobs, because other commit may refer it.
     * <p>
     * recall that Repository.remove() have make sure that the
     * files in stagedForRemoveDir exist in the current commit.
     * <p>
     */
    public void removeBlobs(File stagedForRemoveDir) {
        for (File fileInStagedDir : Objects.requireNonNull(stagedForRemoveDir.listFiles())) {
            String fileInStagedDirSha1 = sha1((Object) readContents(fileInStagedDir));
            this.blobSha1List.remove(fileInStagedDirSha1);
        }
    }

}

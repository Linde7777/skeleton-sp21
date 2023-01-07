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
import static gitlet.StudentUtils.*;

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
     * this function will create blobs from the files in stagedForAdd directory,
     * then put the filename and it's sha1 values into Commit.map
     * <p>
     * e.g.
     * hello.txt's sha1 is 7afbac, we call it hello.txt version 1,
     * when this function is executed, its content will be stored at the .gitlet/blobs/7afbac,
     * and we will create a mapping hello.txt->7afbac
     * <p>
     * now we modify the content of hello.txt, its sha1 is a127db,
     * we call it hello.txt version 2,
     * when this function is executed, its content will be stored at the .gitlet/blobs/a127db
     * we will update the mapping hello.txt->a127db
     */
    public void addBlobs(File stagedForAddDir, File blobsDir) throws IOException {
        for (File stagedFile : Objects.requireNonNull(stagedForAddDir.listFiles())) {
            String stagedFileSha1 = sha1((Object) readContents(stagedFile));

            // if a staged file haven't been tracked, track it
            if(!map.containsKey(stagedFile.getName())){

            }

            // if a staged file haven't been modified, ignore it
            if (map.get(stagedFile.getName()).equals(stagedFileSha1)) {
                continue;
            }

            // if a tracked file have been modified, update the filename->sha1 mapping.
            // we can't delete the file of version 1, because other commit may refer it
            if(map.containsKey(stagedFile.getName())&&)

        }
    }

    private void addFileInDir(File dir, File file) throws IOException {
        if (!dir.exists()) {
            dir.mkdir();
        }

        Path src = file.toPath();
        Path dest = join(dir, file.getName()).toPath();
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
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

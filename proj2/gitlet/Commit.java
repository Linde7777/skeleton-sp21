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
     * where store the sha1 value of blobs
     */
    private LinkedList<String> blobSha1List;

    public LinkedList<String> getBlobSha1List() {
        return blobSha1List;
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
        this.blobSha1List = new LinkedList<>();
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
     * and put the sha1 values into Commit.blobSha1List
     * In order to prevent filename conflict, each file is stored in a
     * directory which named after this file's sha1 value.
     * <p>
     * e.g.
     * hello.txt's sha1 is 7afbac, we call it hello.txt version 1,
     * when this function is executed, in the .gitlet/blobs/7afbac directory,
     * there is a hello.txt(version 1)
     * <p>
     * now we modify the content of hello.txt, we call it hello.txt version 2
     * and its sha1 value is a127db
     * when this function is executed, in the .gitlet/blobs/a127db directory,
     * there is a hello.txt(version 2)
     * <p>
     * do distinguish them:
     * blobsDir is .gitlet/blobs/
     * blobDir is .gitlet/blobs/[sha1 value]
     * <p>
     *
     * @param stagedForAddDir the files in this directory will be committed
     * @param blobsDir        where store the blobs
     */
    public void addBlobs(File stagedForAddDir, File blobsDir) throws IOException {
        for (File stagedFile : Objects.requireNonNull(stagedForAddDir.listFiles())) {
            String fileSha1 = sha1((Object) readContents(stagedFile));
            if (this.blobSha1List.contains(fileSha1)) {
                continue;
            }

            // if in the previous commit, there is hello.txt (version 1),
            // and we are going to add hello.txt (version 2) to the current commit,
            // we need to delete the reference of version 1
            // (we can't delete the file of version 1, because other commit may refer it)
            for (String blobSha1 : blobSha1List) {
                //TODO: it is similiar to the getBlobFile() in Repository
                // can we optimize it?
                File blobDir = join(blobsDir, blobSha1);
                File blobFile = getTheOnlyFileInDir(blobDir);
                if (blobFile.getName().equals(stagedFile.getName())) {
                    this.blobSha1List.remove(blobSha1);
                }
            }

            this.blobSha1List.add(fileSha1);
            addFileInDir(join(blobsDir, fileSha1), stagedFile);
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

package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
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
    LinkedList<String> blobSha1List;

    /**
     * The sha1 value of the parent of this Commit.
     */
    private String parentSha1;

    public String getParentSha1() {
        return parentSha1;
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
        this.message = message;
        this.parentSha1 = null;
        this.blobSha1List = new LinkedList<>();
    }

    public void modifyCommit(String message, String parentSha1) {
        this.message = message;
        this.parentSha1 = parentSha1;
        this.timeStamp = new Date();
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
     * In order to implement the commit sha1 autocompletion, we need to make an optimization,
     * we will take the first two digit from sha1, name it as another directory,
     * let's say hello.txt's sha1 is 1a3abd2, we will put it in:
     * .gitlet/blobs/1a/1a3abd2
     * <p>
     * do distinguish them:
     * blobsDir is .gitlet/blobs/
     * blobDir is .gitlet/blobs/[first 2 bit sha1]/[40 bit sha1]
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
            // and in the current commit, we are going to add hello.txt (version 2),
            // we need to delete the reference of version 1
            // (we can't delete the file of version 1, because other commit may refer it)
            for (String blobSha1 : blobSha1List) {
                File blobDir = join(blobsDir, blobSha1.substring(0, 2), blobSha1);
                File blobFile = getTheOnlyFileInDir(blobDir);
                if (blobFile.getName().equals(stagedFile.getName())) {
                    this.blobSha1List.remove(blobSha1);
                }
            }

            this.blobSha1List.add(fileSha1);
            File blobDir = join(blobsDir, fileSha1.substring(0, 2), fileSha1);
            addFileInDir(blobDir, stagedFile);
        }
    }

    private void addFileInDir(File dir, File file) throws IOException {
        if (!dir.exists()) {
            dir.mkdir();
        } else {
            throw new GitletException("sha1 value conflict");
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

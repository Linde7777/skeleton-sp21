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

    /**
     * The time when this Commit was created.
     */
    private Date timeStamp;

    /**
     * where store the sha1 value of blobs
     */
    LinkedList<String> blobSha1List;

    /**
     * The sha1 value of the parent of this Commit.
     */
    private String parentSha1;

    /**
     * initialize Commit with given variable,
     * the timestamp will be initialized by the current time
     * if the parent is null, the timestamp will be set as
     * //TODO: format of timestamp
     * "00:00:00 UTC, Thursday, 1 January 1970"
     * <p>
     *
     *
     * @param message         The message of the commit
     * @param parentSha1      the sha1 value of the parent of this Commit
     * @param stagedForAddDir will be used by setupBlobs(), see its comment
     * @param blobsDir        will be used by setupBlobs(), see its comment
     * @throws IOException the exception that setupBlobs will throw
     */
    public Commit(String message, String parentSha1,
                  File stagedForAddDir, File blobsDir) throws IOException {
        if (parentSha1 != null) {
            this.timeStamp = new Date();
        } else {
            this.timeStamp = new Date(0);
        }

        this.message = message;
        this.parentSha1 = parentSha1;
        this.blobSha1List = new LinkedList<>();
        setupBlobs(stagedForAddDir, blobsDir);

    }

    /**
     * A private helper function of Commit constructor.
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
     *
     * @param stagedForAddDir the files in this directory will be committed
     * @param blobsDir        where store the blobs
     */
    private void setupBlobs(File stagedForAddDir, File blobsDir) throws IOException {
        for (File file : Objects.requireNonNull(stagedForAddDir.listFiles())) {
            String fileSha1 = sha1((Object) readContents(file));
            this.blobSha1List.add(fileSha1);
            File blobDir = join(blobsDir, fileSha1);
            if (!blobDir.exists()) {
                blobDir.mkdir();
            } else {
                throw new GitletException("sha1 value conflict: " + fileSha1);
            }

            Path src = file.toPath();
            Path dest = join(blobDir, file.getName()).toPath();
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }

}

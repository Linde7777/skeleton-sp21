package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

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
     * TODO: add instance variables here.
     * <p>
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /**
     * The message of this Commit.
     */
    private String message;

    /**
     * The time when this Commit was created.
     */
    private String timeStamp;

    /**
     * The sha1 value of this Commit
     */
    private String commitSha1;

    /**
     * where store the blobs
     */
    LinkedList<String> blobSha1 = new LinkedList<>();

    /**
     * The sha1 value of the parent of this Commit.
     */
    private String parentSha1;

    public Commit(String message, String parentSha1, File stageForAddDir) {
        if (message == null || message.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        this.message = message;
        this.parentSha1 = parentSha1;
        this.commitSha1 = sha1(this);
        this.timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss")
                .format(new Date());

        File[] files = stageForAddDir.listFiles();
        if (files.length == 0) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        for (File file : files) {
            blobSha1.add(sha1(file));
        }
    }

}

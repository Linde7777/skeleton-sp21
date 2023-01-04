package gitlet;

import java.io.File;
import java.io.IOException;

import static gitlet.Utils.join;

/**
 * Driver class for Gitlet, a subset of the Git version-control system.
 *
 * @author Linde
 */
public class Main {
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND1> <OPERAND2> ...
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];
        switch (firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                String filenameForAdd = args[1];
                Repository.add(filenameForAdd);
                break;
            case "commit":
                String message = args[1];
                Repository.setUpCommit(message);
                break;
            case "rm":
                String filenameForRemove = args[1];
                Repository.remove(filenameForRemove);
                break;
            case "log":
                Repository.log();
                break;
            case "checkout":
                if (args[1].equals("--")) {
                    String filename = args[2];
                    Repository.checkoutFilename(filename);
                } else if (args[2].equals("--")) {
                    String commitId = args[1];
                    String filename = args[3];
                    Repository.checkoutCommitAndFilename(commitId, filename);
                } else {
                    String branchName = args[1];
                    Repository.checkoutBranchName(branchName);
                }
                break;
            case "branch":
                String branchToBeCreatedName = args[1];
                Repository.branch(branchToBeCreatedName);
                break;
            case "rm-branch":
                String branchToBeRemovedName = args[1];
                Repository.removeBranch(branchToBeRemovedName);

            default:
                System.out.println("No command with that name exists.");
                break;
        }


    }


}

package capers;

import java.io.File;

import static capers.Utils.*;

/**
 * A repository for Capers
 *
 * @author Linde
 * The structure of a Capers Repository is as follows:
 * <p>
 * .capers/ -- top level folder for all persistent data in your lab12 folder
 * - dogs/ -- folder containing all of the persistent data for dogs
 * - story -- file containing the current story
 * <p>
 * TODO: change the above structure if you do something different.
 */
public class CapersRepository {
    /**
     * Current Working Directory.
     */
    static final File CWD = new File(System.getProperty("user.dir"));

    /**
     * Main metadata folder.
     */
    static final File CAPERS_FOLDER = join(System.getProperty("user.dir"), ".capers");
    // TODO Hint: look at the `join` function in Utils

    /**
     * Does required filesystem operations to allow for persistence.
     * (creates any necessary folders or files)
     * Remember: recommended structure (you do not have to follow):
     * <p>
     * .capers/ -- top level folder for all persistent data in your lab12 folder
     * - dogs/ -- folder containing all of the persistent data for dogs
     * - story -- file containing the current story
     */
    public static void setupPersistence() {
        if (!CAPERS_FOLDER.exists()) {
            CAPERS_FOLDER.mkdir();
        }

    }

    /**
     * Appends the first non-command argument in args
     * to a file called `story` in the .capers directory.
     *
     * @param text String of the text to be appended to the story
     */
    public static void writeStory(String text) {
        File storyFile = Utils.join(System.getProperty("user.dir"), ".capers", "story");
        String formerText="";
        if (storyFile.exists()) {
            formerText = Utils.readContentsAsString(storyFile);
            formerText += "\n";
        }
        Utils.writeContents(storyFile, formerText, text);
        System.out.println(readContentsAsString(storyFile));
    }

    /**
     * Creates and persistently saves a dog using the first
     * three non-command arguments of args (name, breed, age).
     * Also prints out the dog's information using toString().
     */
    public static void makeDog(String name, String breed, int age) {
        Dog dog = new Dog(name, breed, age);
        dog.saveDog();
        System.out.println(dog.toString());
    }

    /**
     * Advances a dog's age persistently and prints out a celebratory message.
     * Also prints out the dog's information using toString().
     * Chooses dog to advance based on the first non-command argument of args.
     *
     * @param name String name of the Dog whose birthday we're celebrating.
     */
    public static void celebrateBirthday(String name) {
        Dog dog = Dog.fromFile(name);
        dog.haveBirthday();
        dog.saveDog();
    }
}

package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/** Repo class for my gitlet project.
 * @author Bella Chang
 */
public class Repo {

    /** Current working directory. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** Gitlet path in CWD. */
    static final File GITLET = Utils.join(CWD, ".gitlet");

    /** Commits directory. */
    static final File COMMITSDIR = Utils.join(GITLET, "commits");

    /** Blobs folder. */
    static final File BLOBS = Utils.join(GITLET, "blobs");

    /** Adding map. */
    static final File ADDING = Utils.join(GITLET, "adding");

    /** Removal map. */
    static final File REMOVAL = Utils.join(GITLET, "removal");

    /** Branches map. */
    static final File BRANCHES = Utils.join(GITLET, "branches");

    /** Current branch string. */
    static final File CURRBRANCH = Utils.join(GITLET, "currentBranch");

    /** To avoid magic number thing. */
    static final int A = 40;

    /** Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit: a commit that
     * contains no files and has the commit message initial commit
     * (just like that, with no punctuation). It will have a single branch:
     * master, which initially points to this initial commit, and master will
     * be the current branch. The timestamp for this initial commit will
     * be 00:00:00 UTC, Thursday, 1 January 1970 in whatever format you choose
     * for dates (this is called "The (Unix) Epoch", represented
     * internally by the time 0.) Since the initial commit in all repositories
     * created by Gitlet will have exactly the same content, it follows that
     * all repositories will automatically share this commit (they will all
     * have the same UID) and all commits in all repositories will trace back
     * to it. */
    public void init() throws IOException {
        if (GITLET.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET.mkdir();
            COMMITSDIR.mkdir();
            BLOBS.mkdir();

            ADDING.createNewFile();
            Utils.writeObject(ADDING, new LinkedHashMap<>());
            REMOVAL.createNewFile();
            Utils.writeObject(REMOVAL, new LinkedHashMap<>());

            Commit initial = new Commit("initial commit", "");
            File commit0 = Utils.join(COMMITSDIR, initial.toString());
            commit0.createNewFile();
            Utils.writeObject(commit0, initial);

            LinkedHashMap<String, String> branchesMap = new LinkedHashMap<>();
            branchesMap.put("master", initial.toString());
            BRANCHES.createNewFile();
            Utils.writeObject(BRANCHES, branchesMap);

            CURRBRANCH.createNewFile();
            Utils.writeObject(CURRBRANCH, "master");
        }
    }

    /** Adds a copy of the file as it currently exists to the staging area
     * (see the description of the commit command). For this reason, adding
     * a file is also called staging the file for addition. Staging an
     * already-staged file overwrites the previous entry in the staging area
     * with the new contents. The staging area should be somewhere
     * in .gitlet. If the current working version of the file is identical
     * to the version in the current commit, do not stage it to be added,
     * and remove it from the staging area if it is already there (as can happen
     * when a file is changed, added, and then changed back). The file will
     * no longer be staged for removal (see gitlet rm), if it was at the
     * time of the command.
     * @param fileName file name;
     * */
    @SuppressWarnings("unchecked")
    public void add(String fileName) throws IOException {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        File filePath = Utils.join(CWD, fileName);
        if (!filePath.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            byte[] blobContents = Utils.readContents(filePath);
            String blobID = Utils.sha1(blobContents);

            LinkedHashMap addMap = getAdding();
            LinkedHashMap removeMap = getRemoval();

            Commit headCommit = getHead();
            if (headCommit.getFileTracker().containsKey(fileName)
                    && headCommit.getFileTracker().get(fileName)
                    .equals(blobID)) {
                if (addMap.containsKey(fileName)
                        && addMap.containsValue(blobID)) {
                    addMap.remove(fileName, blobID);
                }
                if (removeMap.containsKey(fileName)
                        && removeMap.containsValue(blobID)) {
                    removeMap.remove(fileName, blobID);
                    Utils.writeObject(REMOVAL, removeMap);
                }
            } else {
                File blobPath = Utils.join(BLOBS, blobID);
                blobPath.createNewFile();
                Utils.writeContents(blobPath, blobContents);
                addMap.put(fileName, blobID);
            }
            Utils.writeObject(ADDING, addMap);
        }
    }

    /** Saves a snapshot of tracked files in the current commit
     * and staging area so they can be restored at a
     * later time, creating a new commit. The commit is said to be
     * tracking the saved files. By default, each
     * commit's snapshot of files will be exactly the same as its parent
     * commit's snapshot of files; it will
     * keep versions of files exactly as they are, and not update them.
     * A commit will only update the contents
     * of files it is tracking that have been staged for addition at the
     * time of commit, in which case the
     * commit will now include the version of the file that was
     * staged instead of the version it got from its
     * parent. A commit will save and start tracking any files that
     * were staged for addition but weren't tracked
     * by its parent. Finally, files tracked in the current commit
     * may be untracked in the new commit as a result
     * being staged for removal by the rm command (below).
     * @param message message;
     * */
    @SuppressWarnings("unchecked")
    public void commit(String message) throws IOException {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (message.isBlank()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }
        if (getAdding().isEmpty() && getRemoval().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }

        Commit headClone = new Commit(message, new Date(),
                getHead().toString(), getHead().getFileTracker());

        for (Object fileName : getAdding().keySet()) {
            String blobID = (String) getAdding().get(fileName);
            headClone.addToFileTracker((String) fileName, blobID);
        }

        for (Object fileToRemove : getRemoval().keySet()) {
            String rBlobID = (String) getRemoval().get(fileToRemove);
            headClone.removeFromFileTracker((String) fileToRemove, rBlobID);
        }

        File commitsPath = Utils.join(COMMITSDIR, headClone.toString());
        commitsPath.createNewFile();
        Utils.writeObject(commitsPath, headClone);

        Utils.writeObject(ADDING, new LinkedHashMap<>());
        Utils.writeObject(REMOVAL, new LinkedHashMap<>());

        LinkedHashMap branchMap = getBranches();
        String currBranchStr = Utils.readObject(CURRBRANCH, String.class);
        branchMap.put(currBranchStr, headClone.toString());
        Utils.writeObject(BRANCHES, branchMap);
    }

    /** Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for
     * removal and remove the file from the working
     * directory if the user has not already done so
     * (do not remove it unless it is tracked in the current
     * @param fileName file name;
     * commit). */
    @SuppressWarnings("unchecked")
    public void rm(String fileName) {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        LinkedHashMap addMap = getAdding();
        LinkedHashMap removeMap = getRemoval();

        if (!addMap.containsKey(fileName)
                && !getHead().getFileTracker().containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

        if (addMap.containsKey(fileName)) {
            addMap.remove(fileName);
        }

        if (getHead().getFileTracker().containsKey(fileName)) {
            removeMap.put(fileName, getHead().getFileTracker().get(fileName));

            File cwdFile = Utils.join(CWD, fileName);
            if (cwdFile.exists()) {
                cwdFile.delete();
            }
        }
        Utils.writeObject(ADDING, addMap);
        Utils.writeObject(REMOVAL, removeMap);
    }

    /** Starting at the current head commit, display information
     * about each commit backwards along the
     * commit tree until the initial commit, following the
     * first parent commit links, ignoring any
     * second parents found in merge commits. (In regular Git,
     * this is what you get with git log
     * --first-parent). This set of commit nodes is called the
     * commit's history. For every node in this
     * history, the information it should display is the commit id,
     * the time the commit was made, and
     * the commit message. */
    public void log() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        Formatter out = new Formatter();

        Commit currCommit = getHead();
        while (!currCommit.getParent().equals("")) {
            out.format("===\n");
            out.format("commit %s\n", currCommit);
            out.format("Date: %s\n",
                    currCommit.dateFormat(currCommit.getTimestamp()));
            out.format("%s\n", currCommit.getMessage());
            out.format("\n");
            File commitPath = Utils.join(COMMITSDIR, currCommit.getParent());
            currCommit = Utils.readObject(commitPath, Commit.class);
        }
        out.format("===\n");
        out.format("commit %s\n", currCommit);
        out.format("Date: %s\n",
                currCommit.dateFormat(currCommit.getTimestamp()));
        out.format("%s\n", currCommit.getMessage());
        System.out.print(out);
    }

    /** Like log, except displays information about all
     * commits ever made. The order of the commits
     * does not matter. Hint: there is a useful
     * method in gitlet.Utils that will help you
     * iterate over files within a directory. */
    public void globalLog() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        Formatter out = new Formatter();
        Commit initial = null;
        for (String commitName : Utils.plainFilenamesIn(COMMITSDIR)) {
            Commit c = Utils.readObject(
                    Utils.join(COMMITSDIR, commitName), Commit.class);
            if (!c.getMessage().equals("initial commit")) {
                out.format("===\n");
                out.format("commit %s\n", c);
                out.format("Date: %s\n", c.dateFormat(c.getTimestamp()));
                out.format("%s\n", c.getMessage());
                out.format("\n");
            } else {
                initial = c;
            }
        }
        out.format("===\n");
        out.format("commit %s\n", initial.toString());
        out.format("Date: %s\n", initial.dateFormat(initial.getTimestamp()));
        out.format("%s\n", "initial commit");

        System.out.print(out);
    }

    /** Displays what branches currently exist, and
     * marks the current branch with a *.
     *  Also displays what files have been staged for
     *  addition or removal. There is an
     *  empty line between sections. Entries should be
     *  listed in lexicographic order,
     *  using the Java string-comparison order
     *  (the asterisk doesn't count). */
    @SuppressWarnings("unchecked")
    public void status() {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        Formatter out = new Formatter();
        LinkedHashMap branchesMap = getBranches();
        LinkedHashMap addingMap = getAdding();
        LinkedHashMap removalMap = getRemoval();
        LinkedHashMap headMap = getHead().getFileTracker();
        out.format("=== Branches ===\n");
        ArrayList<String> branchesList = new ArrayList<>();
        branchesList.addAll(branchesMap.keySet());
        Collections.sort(branchesList);
        String currBranchStr = Utils.readObject(CURRBRANCH, String.class);
        for (String branchName : branchesList) {
            if (branchName.equals(currBranchStr)) {
                branchName = "*" + branchName;
                out.format("%s\n", branchName);
            } else {
                out.format("%s\n", branchName);
            }
        }
        out.format("\n");
        out.format("=== Staged Files ===\n");
        ArrayList<String> stagedFilesList = new ArrayList<>();
        stagedFilesList.addAll(addingMap.keySet());
        Collections.sort(stagedFilesList);
        for (String s : stagedFilesList) {
            out.format("%s\n", s);
        }
        out.format("\n");
        out.format("=== Removed Files ===\n");
        ArrayList<String> removedFilesList = new ArrayList<>();
        removedFilesList.addAll(removalMap.keySet());
        Collections.sort(removedFilesList);
        for (String r : removedFilesList) {
            out.format("%s\n", r);
        }
        out.format("\n");
        status2(out, headMap, addingMap, removalMap);
    }

    /** 2nd part of status.
     * @param out out;
     * @param headMap head map;
     * @param addingMap adding map;
     * @param removalMap removal map;
     * @return;
     * */
    public void status2(Formatter out, LinkedHashMap<String, String> headMap,
                        LinkedHashMap<String, String> addingMap,
                        LinkedHashMap<String, String> removalMap) {
        out.format("=== Modifications Not Staged For Commit ===\n");
        ArrayList<String> modificationsList = new ArrayList<>();
        for (String f : Utils.plainFilenamesIn(CWD)) {
            if (headMap.containsKey(f)
                    && !addingMap.containsKey(f)
                    && !removalMap.containsKey(f)
                    && !headMap.get(f).equals(
                    Utils.sha1(Utils.readContents(
                            Utils.join(CWD, f))))) {
                modificationsList.add(f);
            } else if (addingMap.containsKey(f)
                    && !addingMap.get(f).equals(
                    Utils.sha1(Utils.readContents(
                            Utils.join(CWD, f))))) {
                modificationsList.add(f);
            }
        }
        for (Object addKey : addingMap.keySet()) {
            if (!Utils.join(CWD, (String) addKey).exists()) {
                modificationsList.add((String) addKey);
            }
        }
        for (Object k : headMap.keySet()) {
            if (!removalMap.containsKey(k)
                    && !Utils.join(CWD, (String) k).exists()) {
                modificationsList.add((String) k);
            }
        }
        Collections.sort(modificationsList);
        for (String s : modificationsList) {
            out.format("%s\n", s);
        }
        out.format("\n");
        out.format("=== Untracked Files ===\n");
        ArrayList<String> untrackedList = new ArrayList<>();
        for (String file : Utils.plainFilenamesIn(CWD)) {
            if (!addingMap.containsKey(file) && !headMap.containsKey(file)) {
                untrackedList.add(file);
            }
        }
        Collections.sort(untrackedList);
        for (String file : untrackedList) {
            out.format("%s\n", file);
        }
        out.format("\n");
        System.out.print(out);
    }

    /** Prints out the ids of all commits that have the
     * given commit message, one per line.
     * If there are multiple such commits, it prints the
     * ids out on separate lines. The
     * commit message is a single operand; to indicate a
     * multiword message, put the operand
     * in quotation marks, as for the commit command above.
     * @param message message;
     * */
    public void find(String message) {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        Formatter out = new Formatter();
        for (String commitName : Utils.plainFilenamesIn(COMMITSDIR)) {
            File commitPath = Utils.join(COMMITSDIR, commitName);
            Commit commit = Utils.readObject(commitPath, Commit.class);
            if (commit.getMessage().equals(message)) {
                out.format(commit + "\n");
            }
        }
        if (!out.toString().equals("")) {
            System.out.print(out);
        } else {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /** Checkout is a kind of general command that can do a few different things
     * depending on what its arguments are.
     * Usages:
     java gitlet.Main checkout -- [file name]
     java gitlet.Main checkout [commit id] -- [file name]
     java gitlet.Main checkout [branch name]
     Descriptions:
     1) Takes the version of the file as it exists in the head commit,
     the front of the current branch,
     and puts it in the working directory, overwriting the version
     of the file that's already there if
     there is one. The new version of the file is not staged.
     2) Takes the version of the file as it exists in the commit with the
     given id, and puts it in the
     working directory, overwriting the version of the file that's already
     there if there is one. The
     new version of the file is not staged.
     3) Takes all files in the commit at the head of the given branch, and
     puts them in the working
     directory, overwriting the versions of the files that are already
     there if they exist. Also, at
     the end of this command, the given branch will now be considered the
     current branch (HEAD). Any
     files that are tracked in the current branch but are not present in the
     checked-out branch are
     deleted. The staging area is cleared, unless the checked-out branch
     is the current branch
     (see Failure cases below).
     @param args args;
     */
    @SuppressWarnings("unchecked")
    public void checkout(String[] args) {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if ((args.length == 3 && !args[1].equals("--"))
                || (args.length == 4 && !args[2].equals("--"))) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }

        if (args[1].equals("--")) {
            String fileName = args[2];

            if (!getHead().getFileTracker().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }

            String blobName = getHead().getFileTracker().get(fileName);
            File blobPath = Utils.join(BLOBS, blobName);
            byte[] blobContents = Utils.readContents(blobPath);

            File cwdFile = Utils.join(CWD, fileName);
            Utils.writeContents(cwdFile, blobContents);
        } else if (args.length == 4) {
            String commitID = args[1];
            if (commitID.length() < A) {
                for (String c : Utils.plainFilenamesIn(COMMITSDIR)) {
                    if (c.startsWith(commitID)) {
                        commitID = c;
                    }
                }
            }
            String fileName = args[3];

            File desiredCommit = Utils.join(COMMITSDIR, commitID);
            if (!desiredCommit.exists()) {
                System.out.println("No commit with that id exists.");
                System.exit(0);
            }
            Commit actualCommit = Utils.readObject(
                    desiredCommit, Commit.class);
            if (!actualCommit.getFileTracker().containsKey(fileName)) {
                System.out.println("File does not exist in that commit.");
                System.exit(0);
            }
            String blobName = actualCommit.
                    getFileTracker().get(fileName);
            File blobPath = Utils.join(BLOBS, blobName);
            byte[] blobContents = Utils.readContents(blobPath);

            File cwdFile = Utils.join(CWD, fileName);
            Utils.writeContents(cwdFile, blobContents);
        } else {
            checkoutBranch(args);
        }
    }

    /** Checkout branch case.
     * @param args args;
     * @return
     * */
    private void checkoutBranch(String[] args) {
        String branchName = args[1];
        LinkedHashMap branchesMap = Utils.readObject(
                BRANCHES, LinkedHashMap.class);
        if (!branchesMap.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }
        String currBranchStr = Utils.readObject(CURRBRANCH, String.class);
        Commit currCommit = Utils.readObject(
                Utils.join(COMMITSDIR, (String) branchesMap.get(
                        currBranchStr)), Commit.class);
        Commit otherBranchHead = Utils.readObject(Utils.join(
                        COMMITSDIR, (String) branchesMap.get(branchName)),
                Commit.class);
        for (String workingFile : Utils.plainFilenamesIn(CWD)) {
            File filePath = Utils.join(CWD, workingFile);
            byte[] fileContents = Utils.readContents(filePath);
            String workingSHA1 = Utils.sha1(fileContents);
            if (!currCommit.getFileTracker().containsKey(workingFile)) {
                if (!otherBranchHead.getFileTracker().get(
                        workingFile).equals(workingSHA1)) {
                    System.out.println("There is an untracked file "
                            + "in the way; delete it, or add and "
                            + "commit it first.");
                    System.exit(0);
                }
            }
        }
        String desiredCommitID = (String) branchesMap.get(branchName);
        File commitPath = Utils.join(COMMITSDIR, desiredCommitID);
        Commit desiredCommit = Utils.readObject(commitPath, Commit.class);
        Commit headCommit = getHead();
        for (String trackedFile : headCommit.getFileTracker().keySet()) {
            if (!desiredCommit.getFileTracker().containsKey(trackedFile)) {
                File cwdPath = Utils.join(CWD, trackedFile);
                cwdPath.delete();
            }
        }
        for (String fileName : desiredCommit.getFileTracker().keySet()) {
            File blobPath = Utils.join(BLOBS, desiredCommit.
                    getFileTracker().get(fileName));
            byte[] blobContents = Utils.readContents(blobPath);
            File cwdFile = Utils.join(CWD, fileName);
            Utils.writeContents(cwdFile, blobContents);
        }
        if (currBranchStr.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        } else {
            Utils.writeObject(ADDING, new LinkedHashMap<>());
            Utils.writeObject(REMOVAL, new LinkedHashMap<>());
        }
        Utils.writeObject(CURRBRANCH, branchName);
        Utils.writeObject(BRANCHES, branchesMap);
    }

    /** Creates a new branch with the given name, and points
     * it at the current head node.
     * A branch is nothing more than a name for a reference
     * (a SHA-1 identifier) to a
     * commit node. This command does NOT immediately switch
     * to the newly created branch
     * (just as in real Git). Before you ever call branch,
     * your code should be running
     * with a default branch called "master".
     * @param branchName branch name;
     * */
    @SuppressWarnings("unchecked")
    public void branch(String branchName) {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }

        LinkedHashMap branchesMap = getBranches();
        if (branchesMap.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        String headSHA1 = getHead().toString();
        branchesMap.put(branchName, headSHA1);
        Utils.writeObject(BRANCHES, branchesMap);
    }

    /** Deletes the branch with the given name.
     * This only means to delete the pointer
     * associated with the branch; it does not mean
     * to delete all commits that were
     * created under the branch, or anything like
     * that.
     * @param branchName branch name;
     * */
    @SuppressWarnings("unchecked")
    public void rmBranch(String branchName) {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (!getBranches().containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (getBranches().get(branchName).equals(getHead().toString())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }

        LinkedHashMap branchMap = getBranches();
        branchMap.remove(branchName);
        Utils.writeObject(BRANCHES, branchMap);
    }

    /** Checks out all the files tracked by the given commit. Removes tracked
     * files that are not present in that commit. Also moves the current
     * branch's head to that commit node. See the intro for an example of what
     * happens to the head pointer after using reset. The [commit id] may be
     * abbreviated as for checkout. The staging area is cleared. The command
     * is essentially checkout of an arbitrary commit that also changes the
     * current branch head.
     * @param commitID commit ID;
     * */
    @SuppressWarnings("unchecked")
    public void reset(String commitID) {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        File commitPath = Utils.join(COMMITSDIR, commitID);
        if (!commitPath.exists()) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }

        Commit commit = Utils.readObject(commitPath, Commit.class);
        String commitSHA1 = commit.toString();

        for (String workingFile : Utils.plainFilenamesIn(CWD)) {
            byte[] fileContents = Utils.readContents(
                    Utils.join(CWD, workingFile));
            String fileSHA1 = Utils.sha1(fileContents);
            if (!getAdding().containsKey(workingFile)
                    && !getHead().getFileTracker()
                    .containsKey(workingFile)
                    && !commit.getFileTracker().get(workingFile)
                    .equals(fileSHA1)) {
                System.out.println("There is an untracked file "
                        + "in the way; delete it, or add and "
                        + "commit it first.");
                System.exit(0);
            }
        }

        for (String fileTracked : commit.getFileTracker().keySet()) {
            String[] args = new String[4];
            args[0] = "checkout";
            args[1] = commitSHA1;
            args[2] = "--";
            args[3] = fileTracked;
            checkout(args);
        }
        for (String f : Utils.plainFilenamesIn(CWD)) {
            if (!commit.getFileTracker().containsKey(f)) {
                File filePath = Utils.join(CWD, f);
                filePath.delete();
            }
        }

        Utils.writeObject(ADDING, new LinkedHashMap<>());
        Utils.writeObject(REMOVAL, new LinkedHashMap<>());

        LinkedHashMap branchesMap = getBranches();
        String currBranchStr = Utils.readObject(CURRBRANCH, String.class);
        branchesMap.put(currBranchStr, commitID);
        Utils.writeObject(BRANCHES, branchesMap);
    }

    /** Merging 2 branches together: given and current.
     * @param branchName branch name;
     * */
    @SuppressWarnings("unchecked")
    public void merge(String branchName) throws IOException {
        if (!GITLET.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        if (!getBranches().containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String currBranchStr = Utils.readObject(CURRBRANCH, String.class);
        File givenBranchPath = Utils.join(COMMITSDIR, (String)
                getBranches().get(branchName));
        File currBranchPath = Utils.join(COMMITSDIR, (String)
                getBranches().get(currBranchStr));
        Commit givenBranchCom = Utils.readObject(givenBranchPath,
                Commit.class);
        Commit currBranchCom = Utils.readObject(currBranchPath,
                Commit.class);
        if (!getAdding().isEmpty() || !getRemoval().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        for (String workingFile : Utils.plainFilenamesIn(CWD)) {
            File filePath = Utils.join(CWD, workingFile);
            byte[] fileContents = Utils.readContents(filePath);
            String workingSHA1 = Utils.sha1(fileContents);
            if (!currBranchCom.getFileTracker().containsKey(workingFile)) {
                if (!givenBranchCom.getFileTracker().get(workingFile)
                        .equals(workingSHA1)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }
        if (!getBranches().containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(currBranchStr)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Commit splitBranchCom = splitPoint(currBranchCom, givenBranchCom);
        if (splitBranchCom.toString().equals(givenBranchCom.toString())) {
            System.out.println("Given branch is an ancestor of the current "
                    + "branch.");
            System.exit(0);
        }
        if (splitBranchCom.toString().equals(currBranchCom.toString())) {
            String[] checkoutArr = new String[2];
            checkoutArr[0] = "checkout";
            checkoutArr[1] = branchName;
            checkout(checkoutArr);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        }
        caseChecker(branchName, currBranchStr, currBranchCom,
                splitBranchCom, givenBranchCom);
    }

    /** Merge helper for each of the cases.
     * @param branchName branch name;
     * @param currBranchStr current branch string;
     * @param currBranchCom current branch commit;
     * @param splitBranchCom split branch commit;
     * @param givenBranchCom given branch commit;
     * @return
     * */
    @SuppressWarnings("unchecked")
    private void caseChecker(String branchName, String currBranchStr,
                             Commit currBranchCom, Commit splitBranchCom,
                             Commit givenBranchCom) throws IOException {
        LinkedHashMap<String, String> addingMap = getAdding();
        LinkedHashMap<String, String> removalMap = getRemoval();
        LinkedHashMap<String, String> mergeTrackedFiles = new LinkedHashMap<>();
        for (String key : currBranchCom.getFileTracker().keySet()) {
            mergeTrackedFiles.put(key, currBranchCom.getFileTracker().get(key));
        }
        for (String f : splitBranchCom.getFileTracker().keySet()) {
            if (currBranchCom.getFileTracker().containsKey(f)
                    && currBranchCom.getFileTracker().get(f).equals(
                    splitBranchCom.getFileTracker().get(f))) {
                if (!givenBranchCom.getFileTracker().containsKey(f)) {
                    mergeTrackedFiles.remove(f);
                } else if (!givenBranchCom.getFileTracker().get(f).equals(
                        splitBranchCom.getFileTracker().get(f))) {
                    mergeTrackedFiles.put(f, givenBranchCom.
                            getFileTracker().get(f));
                }
            } else if (currBranchCom.getFileTracker().containsKey(f)
                    && currBranchCom.getFileTracker().get(f).equals(
                    splitBranchCom.getFileTracker().get(f))
                    && !givenBranchCom.getFileTracker().containsKey(f)) {
                mergeTrackedFiles.remove(f);
            }
        }
        caseChecker2(splitBranchCom, addingMap, removalMap, currBranchCom,
                givenBranchCom, branchName, currBranchStr, mergeTrackedFiles);
    }

    /** Helper to finish the cases.
     * @param splitBranchCom split branch commit;
     * @param addingMap adding map;
     * @param removalMap removalMap;
     * @param currBranchCom current branch commit;
     * @param givenBranchCom given branch commit;
     * @param branchName branch name;
     * @param currBranchStr current branch string;
     * @param mergeTrackedFiles map for merge commit files;
     * @return
     * */
    private void caseChecker2(Commit splitBranchCom,
                              LinkedHashMap<String, String> addingMap,
                              LinkedHashMap<String, String> removalMap,
                              Commit currBranchCom,
                              Commit givenBranchCom, String branchName,
                              String currBranchStr,
                              LinkedHashMap<String, String>
                                      mergeTrackedFiles) throws IOException {
        boolean mergeConflict = false;
        ArrayList<String> fileNames = new ArrayList<>();
        fileNames.addAll(splitBranchCom.getFileTracker().keySet());
        fileNames.addAll(currBranchCom.getFileTracker().keySet());
        fileNames.addAll(givenBranchCom.getFileTracker().keySet());
        Set<String> s = new HashSet<>(fileNames);
        fileNames = new ArrayList<>(s);
        for (String f : fileNames) {
            String contents1; String contents2;
            if (modifiedInDifferentWays(givenBranchCom,
                    currBranchCom, splitBranchCom, f)) {
                if (currBranchCom.getFileTracker().containsKey(f)) {
                    File currBrPath = Utils.join(BLOBS, currBranchCom.
                            getFileTracker().get(f));
                    contents1 = Utils.readContentsAsString(currBrPath);
                } else {
                    contents1 = "";
                }
                if (givenBranchCom.getFileTracker().containsKey(f)) {
                    File givenBrPath = Utils.join(BLOBS,
                            givenBranchCom.getFileTracker().get(f));
                    contents2 = Utils.readContentsAsString(givenBrPath);
                } else {
                    contents2 = "";
                }
                String newContents = "<<<<<<< HEAD\n" + contents1
                        + "=======\n" + contents2 + ">>>>>>>\n";
                byte[] newContentsBytes = newContents.getBytes();
                File newFilePath = Utils.join(BLOBS,
                        Utils.sha1(newContentsBytes));
                newFilePath.createNewFile();
                Utils.writeContents(newFilePath, newContentsBytes);
                mergeTrackedFiles.put(f, Utils.sha1(newContentsBytes));
                mergeConflict = true;
            }
        }
        for (String f : givenBranchCom.getFileTracker().keySet()) {
            if (!currBranchCom.getFileTracker().containsKey(f)
                    && !splitBranchCom.getFileTracker().containsKey(f)) {
                mergeTrackedFiles.put(f, givenBranchCom.
                        getFileTracker().get(f));
                String[] stringArr = new String[]{"checkout",
                        givenBranchCom.toString(), "--", f};
                checkout(stringArr);
            }
        }
        Utils.writeObject(ADDING, addingMap);
        Utils.writeObject(REMOVAL, removalMap);
        finishingMerge(mergeConflict, currBranchCom,
                givenBranchCom, branchName, currBranchStr, mergeTrackedFiles);
    }

    /** Final helper for merge.
     * @param mergeConflict shows if there's conflict;
     * @param currBranchCom current branch commit;
     * @param givenBranchCom given branch commit;
     * @param branchName branch name;
     * @param currBranchStr current branch string;
     * @param mergeTrackedFiles map for merge commit's tracked files;
     * @return
     * */
    @SuppressWarnings("unchecked")
    private void finishingMerge(boolean mergeConflict, Commit currBranchCom,
                                Commit givenBranchCom, String branchName,
                                String currBranchStr,
                                LinkedHashMap<String, String> mergeTrackedFiles)
            throws IOException {
        String mergeMessage = "Merged " + branchName
                + " into " + currBranchStr + ".";
        Commit mergeCommit = new Commit(mergeMessage,
                new Date(),
                currBranchCom.toString(),
                givenBranchCom.toString(),
                mergeTrackedFiles);
        if (mergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
        for (String file : Utils.plainFilenamesIn(CWD)) {
            File filePath = Utils.join(CWD, file);
            if (!mergeTrackedFiles.containsKey(file)) {
                filePath.delete();
            } else {
                byte[] fileContents = Utils.readContents(
                        Utils.join(BLOBS, mergeTrackedFiles.get(file)));
                Utils.writeContents(filePath, fileContents);
            }
        }

        File mergedCommitPath = Utils.join(COMMITSDIR, mergeCommit.toString());
        mergedCommitPath.createNewFile();
        Utils.writeObject(mergedCommitPath, mergeCommit);

        LinkedHashMap branchesMap = getBranches();
        branchesMap.put(currBranchStr, mergeCommit.toString());
        Utils.writeObject(BRANCHES, branchesMap);
    }

    /** Helper to find the split point (commit) of two branches.
     * Reminder for self: currBranch is our FOCUS the given branch
     * is just for finding common ancestors.
     * @param currCommit current commit;
     * @param givenCommit given commit;
     * @return
     * */
    public Commit splitPoint(Commit currCommit, Commit givenCommit) {
        LinkedHashMap<String, Integer> currAncestors = new LinkedHashMap<>();
        int distanceFromHead = 0;
        currAncestors.put(currCommit.toString(), distanceFromHead);
        while (currCommit.hasParent()) {
            distanceFromHead++;
            currAncestors.put(currCommit.getParent(), distanceFromHead);

            int mergedCount = distanceFromHead;
            if (!currCommit.getMergedParent().equals("")) {
                mergedCount++;
                Commit mergedCommit = Utils.readObject(
                        Utils.join(COMMITSDIR, currCommit.
                                getMergedParent()), Commit.class);
                currAncestors.put(mergedCommit.toString(), mergedCount);
                while (mergedCommit.hasParent()) {
                    mergedCount++;
                    currAncestors.put(mergedCommit.getParent(), mergedCount);
                    mergedCommit = Utils.readObject(Utils.join(COMMITSDIR,
                            mergedCommit.getParent()), Commit.class);
                }
            }

            currCommit = Utils.readObject(Utils.join(COMMITSDIR,
                    currCommit.getParent()), Commit.class);
        }
        currAncestors.put(currCommit.toString(), distanceFromHead + 1);

        ArrayList<String> givenAncestors = new ArrayList<>();
        givenAncestors.add(givenCommit.toString());
        while (givenCommit.hasParent()) {
            givenAncestors.add(givenCommit.getParent());
            if (!givenCommit.getMergedParent().equals("")) {
                Commit mergedGivenCommit = Utils.readObject(
                        Utils.join(COMMITSDIR, givenCommit.getMergedParent()),
                        Commit.class);
                givenAncestors.add(mergedGivenCommit.toString());
                while (mergedGivenCommit.hasParent()) {
                    givenAncestors.add(mergedGivenCommit.getParent());
                    mergedGivenCommit = Utils.readObject(Utils.join(
                                    COMMITSDIR, mergedGivenCommit.getParent()),
                            Commit.class);
                }
            }
            givenCommit = Utils.readObject(Utils.join(COMMITSDIR,
                    givenCommit.getParent()), Commit.class);
        }
        givenAncestors.add(givenCommit.toString());
        return splitPoint2(currAncestors, givenAncestors);
    }

    /** Continuing merge from a second split point.
     * @param currAncestors current ancestors;
     * @param givenAncestors given ancestors;
     * @return
     * */
    private Commit splitPoint2(LinkedHashMap<String, Integer> currAncestors,
                               ArrayList<String> givenAncestors) {
        LinkedHashMap<String, Integer> commonAncestors = new LinkedHashMap<>();
        for (String ancestor : currAncestors.keySet()) {
            if (givenAncestors.contains(ancestor)) {
                commonAncestors.put(ancestor, currAncestors.get(ancestor));
            }
        }

        LinkedHashMap<String, Integer> allSplitPoints = new LinkedHashMap<>();
        ArrayList<String> commonAncestorParents = new ArrayList<>();

        for (String key : commonAncestors.keySet()) {
            Commit ancestorCom = Utils.readObject(
                    Utils.join(COMMITSDIR, key), Commit.class);
            commonAncestorParents.add(ancestorCom.getParent());
            if (!ancestorCom.getMergedParent().equals("")) {
                commonAncestorParents.add(ancestorCom.getMergedParent());
            }
        }

        for (String commonAncestor : commonAncestors.keySet()) {
            if (!commonAncestorParents.contains(commonAncestor)) {
                allSplitPoints.put(commonAncestor,
                        commonAncestors.get(commonAncestor));
            }
        }

        String splitPoint = "";
        int currMin = Integer.MAX_VALUE;
        for (String commitSplit : allSplitPoints.keySet()) {
            if (allSplitPoints.get(commitSplit) < currMin) {
                currMin = allSplitPoints.get(commitSplit);
                splitPoint = commitSplit;
            }
        }

        return Utils.readObject(Utils.join(COMMITSDIR, splitPoint),
                Commit.class);
    }

    /** Helper method to determine "modified in diff ways".
     * @param commit1 commit 1;
     * @param commit2 commit 2;
     * @param split split point;
     * @param fileName file name;
     * @return
     * */
    private boolean modifiedInDifferentWays(Commit commit1, Commit commit2,
                                            Commit split, String fileName) {
        LinkedHashMap<String, String> commit1FT = commit1.getFileTracker();
        LinkedHashMap<String, String> commit2FT = commit2.getFileTracker();
        LinkedHashMap<String, String> splitFT = split.getFileTracker();

        if (commit1FT.containsKey(fileName)
                && commit2FT.containsKey(fileName)
                && splitFT.containsKey(fileName)
                && !commit1FT.get(fileName).equals(splitFT.get(fileName))
                && !commit2FT.get(fileName).equals(splitFT.get(fileName))
                && !commit1FT.get(fileName).equals(commit2FT.get(fileName))) {
            return true;
        } else if (commit1FT.containsKey(fileName)
                && !commit2FT.containsKey(fileName)
                && splitFT.containsKey(fileName)
                && !commit1FT.get(fileName).equals(splitFT.get(fileName))) {
            return true;
        } else if (commit2FT.containsKey(fileName)
                && !commit1FT.containsKey(fileName)
                && splitFT.containsKey(fileName)
                && !commit2FT.get(fileName).equals(splitFT.get(fileName))) {
            return true;
        } else {
            return !splitFT.containsKey(fileName)
                    && commit1FT.containsKey(fileName)
                    && commit2FT.containsKey(fileName)
                    && !commit1FT.get(fileName).equals(commit2FT.get(fileName));
        }
    }

    /** Helper method to get the head commit of the current branch.
     * @return
     */
    private Commit getHead() {
        LinkedHashMap branchesMap = Utils.readObject(BRANCHES,
                LinkedHashMap.class);
        String currBranchStr = Utils.readObject(CURRBRANCH, String.class);
        String commitSHA1 = (String) branchesMap.get(currBranchStr);
        File headPath = Utils.join(COMMITSDIR, commitSHA1);
        return Utils.readObject(headPath, Commit.class);
    }

    /** Helper method to get the adding hash map.
     * @return
     */
    private LinkedHashMap getAdding() {
        return Utils.readObject(ADDING, LinkedHashMap.class);
    }

    /** Helper method to get the removal hash map.
     * @return
     */
    private LinkedHashMap getRemoval() {
        return Utils.readObject(REMOVAL, LinkedHashMap.class);
    }

    /** Helper method to get the branches hash map.
     * @return
     */
    private LinkedHashMap getBranches() {
        return Utils.readObject(BRANCHES, LinkedHashMap.class);
    }
}


package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Bella Chang
 *  Collaborated with: Marcus Cheung, Tess U-Vongchaeron, Pavan Murugesh
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        Repo current = new Repo();
        if (args[0].equals("init")) {
            current.init();
        } else if (args[0].equals("add")) {
            current.add(args[1]);
        } else if (args[0].equals("commit")) {
            if (args.length == 2) {
                current.commit(args[1]);
            } else {
                String s = "";
                int i = 1;
                while (args[i] != null) {
                    s += args[i];
                    i++;
                }
                current.commit(s);
            }
        } else if (args[0].equals("checkout")) {
            current.checkout(args);
        } else if (args[0].equals("global-log")) {
            current.globalLog();
        } else if (args[0].equals("status")) {
            current.status();
        } else if (args[0].equals("log")) {
            current.log();
        } else if (args[0].equals("rm")) {
            current.rm(args[1]);
        } else if (args[0].equals("branch")) {
            current.branch(args[1]);
        } else if (args[0].equals("find")) {
            if (args.length == 2) {
                current.find(args[1]);
            } else {
                String s = "";
                int i = 1;
                while (args[i] != null) {
                    s += args[i];
                    i++;
                }
                current.find(s);
            }
        } else if (args[0].equals("rm-branch")) {
            current.rmBranch(args[1]);
        } else if (args[0].equals("reset")) {
            current.reset(args[1]);
        } else if (args[0].equals("merge")) {
            current.merge(args[1]);
        } else {
            System.out.println("No command with that name exists.");
            System.exit(0);
        }
    }
}


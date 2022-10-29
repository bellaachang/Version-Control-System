package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

/** Creating a commit class.
 * @author Bella Chang
 * */
public class Commit implements Serializable {

    /** Message of the commit. */
    private String _message;

    /** Timestamp of the commit. */
    private Date _timestamp;

    /** Parent SHA-1 ID. */
    private String _parent;

    /** Merged parent SHA-1 ID. */
    private String _mergedParent;

    /** File tracker map. */
    private LinkedHashMap<String, String> _fileTracker;

    /** Constructor for a new commit.
     * @param message message;
     * @param parent SHA-1 of parent;
     * */
    public Commit(String message, String parent) {
        _message = message;
        _parent = parent;
        if (_parent.equals("")) {
            _timestamp = new Date(0);
        } else {
            _timestamp = new Date();
        }
        _fileTracker = new LinkedHashMap<>();
        _mergedParent = "";
    }

    /** Copy constructor for a commit.
     * @param message message;
     * @param timestamp timestamp;
     * @param parent SHA-1 of parent;
     * @param parentTracker map of parent;
     * */
    public Commit(String message, Date timestamp, String parent,
                  LinkedHashMap<String, String> parentTracker) {
        _message = message;
        _parent = parent;
        _timestamp = timestamp;
        _fileTracker = new LinkedHashMap<>();
        for (String key : parentTracker.keySet()) {
            _fileTracker.put(key, parentTracker.get(key));
        }
        _mergedParent = "";
    }

    /** Constructor for a merged commit.
     * @param message message;
     * @param timestamp timestamp;
     * @param parent parent SHA-1;
     * @param mergedParent merged parent SHA-1;
     * @param trackedFiles map of commit;
     * */
    public Commit(String message, Date timestamp, String parent, String
            mergedParent, LinkedHashMap<String, String> trackedFiles) {
        _message = message;
        _parent = parent;
        _timestamp = timestamp;
        _fileTracker = trackedFiles;
        _mergedParent = mergedParent;
    }

    /** Format for a date object (for log).
     * @param date date;
     * @return
     * */
    public String dateFormat(Date date) {
        SimpleDateFormat f = new SimpleDateFormat("EEE MMM dd HH:mm:ss YYYY Z");
        return f.format(date);
    }

    /** Getter method for the message string.
     * @return
     * */
    public String getMessage() {
        return _message;
    }

    /** Getter method for the timestamp.
     * @return
     * */
    public Date getTimestamp() {
        return _timestamp;
    }

    /** Add to file tracker map.
     * @param file file;
     * @param sha1 sha1;
     * @return
     * */
    public void addToFileTracker(String file, String sha1) {
        _fileTracker.put(file, sha1);
    }

    /** Remove to file tracker map.
     * @param file file;
     * @param sha1 sha1;
     * @return
     * */
    public void removeFromFileTracker(String file, String sha1) {
        _fileTracker.remove(file, sha1);
    }

    /** Getter method for the parent SHA-1 ID.
     * @return
     * */
    public String getParent() {
        return _parent;
    }

    /** Checks to see if a commit has a parent.
     * @return
     * */
    public boolean hasParent() {
        return !(_parent.equals(""));
    }

    /** Getter method for merged parent SHA-1 ID.
     * @return
     * */
    public String getMergedParent() {
        return _mergedParent;
    }

    /** Getter method for file tracker map.
     * @return
     * */
    public LinkedHashMap<String, String> getFileTracker() {
        return _fileTracker;
    }

    /** Gets SHA-1 ID of a commit.
     * @return
     * */
    public String toString() {
        String timeInString = _timestamp.toString();
        String files = _fileTracker.toString();
        return Utils.sha1(files, _parent, _message, timeInString);
    }
}

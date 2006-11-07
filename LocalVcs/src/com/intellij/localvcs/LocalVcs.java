package com.intellij.localvcs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalVcs {
  private ChangeList myChangeList = new ChangeList();
  private RootEntry myRoot = new RootEntry();

  private List<Change> myPendingChanges = new ArrayList<Change>();

  public LocalVcs() {}

  public LocalVcs(File dir) throws IOException {
    File f = new File(dir, "content");
    FileInputStream fs = new FileInputStream(f);
    try {
      Stream s = new Stream(fs);

      myChangeList = s.readChangeList();
      myRoot = (RootEntry)s.readEntry(); // todo cast!!!
    } finally {
      // todo dont forget to test stream closing...
      fs.close();
    }
  }

  public void store(File dir) throws IOException {
    File f = new File(dir, "content");
    f.createNewFile();

    FileOutputStream fs = new FileOutputStream(f);
    try {
      Stream s = new Stream(fs);

      s.writeChangeList(myChangeList);
      s.writeEntry(myRoot);
    } finally {
      // todo dont forget to test stream closing...
      fs.close();
    }
  }

  public boolean hasEntry(Path path) {
    return myRoot.hasEntry(path);
  }

  public Entry getEntry(Path path) {
    return myRoot.getEntry(path);
  }

  public List<Entry> getEntryHistory(Path path) {
    //todo optimize it
    List<Entry> result = new ArrayList<Entry>();

    // todo clean up this mess
    // todo should we raise exception?
    if (!myRoot.hasEntry(path)) return result;

    Integer id = myRoot.getEntry(path).getObjectId();

    for (RootEntry r : getHistory()) {
      if (!r.hasEntry(id)) break;
      result.add(r.getEntry(id));
    }

    return result;
  }

  public void createFile(Path path, String content) {
    myPendingChanges.add(new CreateFileChange(path, content));
  }

  public void changeFileContent(Path path, String content) {
    myPendingChanges.add(new ChangeFileContentChange(path, content));
  }

  public void rename(Path path, String newName) {
    myPendingChanges.add(new RenameChange(path, newName));
  }

  public void delete(Path path) {
    myPendingChanges.add(new DeleteChange(path));
  }

  public void commit() {
    myRoot = myChangeList.applyChangeSetOn(
        myRoot, new ChangeSet(myPendingChanges));
    clearPendingChanges();
  }

  public void revert() {
    clearPendingChanges();

    RootEntry reverted = myChangeList.revertOn(myRoot);
    if (reverted == null) return;
    myRoot = reverted;
  }

  private void clearPendingChanges() {
    myPendingChanges = new ArrayList<Change>();
  }

  public Boolean isClean() {
    return myPendingChanges.isEmpty();
  }

  public void putLabel(String label) {
    myChangeList.setLabel(myRoot, label);
  }

  public RootEntry getSnapshot(String label) {
    for (RootEntry r : getHistory()) {
      if (label.equals(myChangeList.getLabel(r))) return r;
    }
    return null;
  }

  public List<RootEntry> getHistory() {
    List<RootEntry> result = new ArrayList<RootEntry>();

    RootEntry r = myRoot;
    while (r != null) {
      result.add(r);
      r = myChangeList.revertOn(r);
    }

    // todo bad hack, maybe replace with EmptySnapshot class
    result.remove(result.size() - 1);

    return result;
  }
}

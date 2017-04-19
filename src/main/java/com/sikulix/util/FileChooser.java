/*
 * Copyright (c) 2017 - sikulix.com - MIT license
 */

package com.sikulix.util;

import com.sikulix.api.Do;
import com.sikulix.core.SX;
import com.sikulix.core.SXLog;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

//import java.io.FilenameFilter;

public class FileChooser {

  private static SXLog log = SX.getSXLog("SX.FileChooser");

  public static final int FILES = JFileChooser.FILES_ONLY;
  public static final int DIRS = JFileChooser.DIRECTORIES_ONLY;
  public static final int DIRSANDFILES = JFileChooser.FILES_AND_DIRECTORIES;
  static final int SAVE = FileDialog.SAVE;
  static final int LOAD = FileDialog.LOAD;

  boolean checkSikuli = false;

  public FileChooser setCheckSikuli() {
    checkSikuli = true;
    return this;
  }

  int type = DIRSANDFILES;

  public FileChooser setType(int type) {
    this.type = type;
    return this;
  }

  private Frame parent = null;

  public FileChooser setParent(Frame parent) {
    this.parent = parent;
    return this;
  }

  private String title = "";

  public FileChooser setTitle(String title) {
    this.title = title;
    return this;
  }

  private File folder = null;

  public FileChooser setFolder(File folder) {
    this.folder = folder;
    return this;
  }

  private FileChooser() {
  }

  private FileChooser(Object... args) {
    for (Object arg : args) {
      if (arg instanceof Frame) {
        setParent((Frame) arg);
      } else if (arg instanceof String) {
        setTitle((String) arg);
      } else if (arg instanceof File) {
        setFolder((File) arg);
      }
    }
  }

  public static File show(Object... args) {
    File ret = new FileChooser(args).showFileChooser(DIRSANDFILES, "Select a file or folder", LOAD, null);
    return ret;
  }

  public static File folder(Object... args) {
    File ret = new FileChooser(args).showFileChooser(DIRS, "Select a folder", LOAD, null);
    return ret;
  }

  public static File load(Object... args) {
    String fileType = "Sikuli Script (*.sikuli, *.skl, *.jar)";
    String title = "Open a Sikuli Script";
    File ret = new FileChooser(args).setCheckSikuli().showFileChooser(DIRSANDFILES, title, LOAD,
            new SikulixFileFilter(fileType, "o"));
    return ret;
  }

  public static File save(Object... args) {
    String fileType = "Sikuli Script (*.sikuli, *.jar)";
    String title = "Save a Sikuli Script";
    File ret = new FileChooser(args).setCheckSikuli().showFileChooser(DIRSANDFILES, title, SAVE,
            new SikulixFileFilter(fileType, "s"));
    return ret;
  }

  public static File loadImage(Object... args) {
    File ret = new FileChooser(args).showFileChooser(FILES, "Load Image File", LOAD,
            new FileNameExtensionFilter("Image files (jpg, png)", "jpg", "jpeg", "png"));
    return ret;
  }

  private File showFileChooser(int type, String title, int mode, Object... filters) {
    if (SX.isNotSet(title)) {
      if (SX.isNotSet(this.title)) {
        title = "Select file or folder";
      }
    }
    String last_dir;
    if (SX.isNotNull(folder)) {
      last_dir = folder.getAbsolutePath();
    } else {
      last_dir = SX.getOption("LAST_OPEN_DIR", "NotSet");
    }
    log.debug("showFileChooser: %s at %s", title.split(" ")[0], last_dir);
    JFileChooser fchooser = null;
    File fileChoosen = null;
    while (true) {
      fchooser = new JFileChooser();
      if (SX.isMac()) {
        fchooser.putClientProperty("JFileChooser.packageIsTraversable", "always");
      }
      if (!"NotSet".equals(last_dir)) {
        fchooser.setCurrentDirectory(new File(last_dir));
      }
      fchooser.setSelectedFile(null);
      if (SX.isMac() && type == DIRS) {
        fchooser.setFileSelectionMode(DIRSANDFILES);
      } else {
        fchooser.setFileSelectionMode(type);
      }
      fchooser.setDialogTitle(title);
      String btnApprove = "Select";
      if (mode == FileDialog.SAVE) {
        fchooser.setDialogType(JFileChooser.SAVE_DIALOG);
        btnApprove = "Save";
      }
      if (SX.isNull(filters) || filters.length == 0) {
        fchooser.setAcceptAllFileFilterUsed(true);
      } else {
        fchooser.setAcceptAllFileFilterUsed(false);
        for (Object filter : filters) {
          if (filter instanceof SikulixFileFilter) {
            fchooser.addChoosableFileFilter((SikulixFileFilter) filter);
          } else {
            fchooser.setFileFilter((FileNameExtensionFilter) filter);
            checkSikuli = false;
          }
        }
      }
      if (fchooser.showDialog(parent, btnApprove) != JFileChooser.APPROVE_OPTION) {
        return null;
      }
      fileChoosen = fchooser.getSelectedFile();
      if (mode == FileDialog.LOAD) {
        if (checkSikuli && !isValidScript(fileChoosen)) {
          // folders must contain a valid scriptfile
          Do.popError("Folder not a valid SikuliX script\nTry again.");
          last_dir = fileChoosen.getAbsolutePath();
          continue;
        }
        break;
      }
    }
    File selected = new File(fileChoosen.getAbsolutePath());
    if (!selected.isDirectory() || type == DIRS) {
      selected = selected.getParentFile();
    }
    SX.setOption("LAST_OPEN_DIR", selected.getAbsolutePath());
    return fileChoosen;
  }

  private static boolean isValidScript(File f) {
    String[] endings = new String[]{".py", ".rb", ".js"};
    String fName = f.getName();
    if (fName.endsWith(".skl")) {
      return true;
    }
    if (fName.endsWith(".sikuli")) {
      fName = fName.substring(0, fName.length() - 7);
    }
    boolean valid = false;
    for (String ending : endings) {
      if (new File(f, fName + ending).exists()) {
        return true;
      }
    }
    return false;
  }

  private static boolean isExt(String fName, String givenExt) {
    int i = fName.lastIndexOf('.');
    if (i > 0) {
      if (fName.substring(i + 1).toLowerCase().equals(givenExt)) {
        return true;
      }
    }
    return false;
  }

  static class SikulixFileFilter extends FileFilter {

    private String type, desc;

    public SikulixFileFilter(String desc, String type) {
      this.type = type;
      this.desc = desc;
    }

    @Override
    public boolean accept(File f) {
      if ("o".equals(type) && (isExt(f.getName(), "sikuli") || isExt(f.getName(), "skl"))) {
        return true;
      }
      if ("s".equals(type) && isExt(f.getName(), "sikuli")) {
        return true;
      }
      if ("e".equals(type)) {
        if (isExt(f.getName(), "skl")) {
          return true;
        }
        if (SX.isMac() && isExt(f.getName(), "sikuli")) {
          return false;
        }
      }
      if (f.isDirectory()) {
        return true;
      }
      return false;
    }

    @Override
    public String getDescription() {
      return desc;
    }
  }
}

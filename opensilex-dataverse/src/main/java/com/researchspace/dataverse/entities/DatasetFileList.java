// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
package com.researchspace.dataverse.entities;

import java.util.List;

/**
 * DatasetFileList is the response from uploading a file using the native API
 */
public class DatasetFileList {
    List<DatasetFile> files;

    @SuppressWarnings("all")
    public DatasetFileList() {
    }

    @SuppressWarnings("all")
    public List<DatasetFile> getFiles() {
        return this.files;
    }

    @SuppressWarnings("all")
    public void setFiles(final List<DatasetFile> files) {
        this.files = files;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof DatasetFileList)) return false;
        final DatasetFileList other = (DatasetFileList) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$files = this.getFiles();
        final Object other$files = other.getFiles();
        if (this$files == null ? other$files != null : !this$files.equals(other$files)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof DatasetFileList;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $files = this.getFiles();
        result = result * PRIME + ($files == null ? 43 : $files.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "DatasetFileList(files=" + this.getFiles() + ")";
    }
}

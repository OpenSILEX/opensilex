// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
package com.researchspace.dataverse.http;

import java.util.List;

/**
 * Request object for metadata included with native file upload to an existing dataset
 */
public class FileUploadMetadata {
    private String description;
    private String directoryLabel;
    private List<String> categories;
    private boolean restrict;
    private boolean tabIngest;

    @SuppressWarnings("all")
    FileUploadMetadata(final String description, final String directoryLabel, final List<String> categories, final boolean restrict, final boolean tabIngest) {
        this.description = description;
        this.directoryLabel = directoryLabel;
        this.categories = categories;
        this.restrict = restrict;
        this.tabIngest = tabIngest;
    }


    @SuppressWarnings("all")
    public static class FileUploadMetadataBuilder {
        @SuppressWarnings("all")
        private String description;
        @SuppressWarnings("all")
        private String directoryLabel;
        @SuppressWarnings("all")
        private List<String> categories;
        @SuppressWarnings("all")
        private boolean restrict;
        @SuppressWarnings("all")
        private boolean tabIngest;

        @SuppressWarnings("all")
        FileUploadMetadataBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileUploadMetadataBuilder description(final String description) {
            this.description = description;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileUploadMetadataBuilder directoryLabel(final String directoryLabel) {
            this.directoryLabel = directoryLabel;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileUploadMetadataBuilder categories(final List<String> categories) {
            this.categories = categories;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileUploadMetadataBuilder restrict(final boolean restrict) {
            this.restrict = restrict;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @SuppressWarnings("all")
        public FileUploadMetadataBuilder tabIngest(final boolean tabIngest) {
            this.tabIngest = tabIngest;
            return this;
        }

        @SuppressWarnings("all")
        public FileUploadMetadata build() {
            return new FileUploadMetadata(this.description, this.directoryLabel, this.categories, this.restrict, this.tabIngest);
        }

        @Override
        @SuppressWarnings("all")
        public String toString() {
            return "FileUploadMetadata.FileUploadMetadataBuilder(description=" + this.description + ", directoryLabel=" + this.directoryLabel + ", categories=" + this.categories + ", restrict=" + this.restrict + ", tabIngest=" + this.tabIngest + ")";
        }
    }

    @SuppressWarnings("all")
    public static FileUploadMetadataBuilder builder() {
        return new FileUploadMetadataBuilder();
    }

    @SuppressWarnings("all")
    public String getDescription() {
        return this.description;
    }

    @SuppressWarnings("all")
    public String getDirectoryLabel() {
        return this.directoryLabel;
    }

    @SuppressWarnings("all")
    public List<String> getCategories() {
        return this.categories;
    }

    @SuppressWarnings("all")
    public boolean isRestrict() {
        return this.restrict;
    }

    @SuppressWarnings("all")
    public boolean isTabIngest() {
        return this.tabIngest;
    }

    @SuppressWarnings("all")
    public void setDescription(final String description) {
        this.description = description;
    }

    @SuppressWarnings("all")
    public void setDirectoryLabel(final String directoryLabel) {
        this.directoryLabel = directoryLabel;
    }

    @SuppressWarnings("all")
    public void setCategories(final List<String> categories) {
        this.categories = categories;
    }

    @SuppressWarnings("all")
    public void setRestrict(final boolean restrict) {
        this.restrict = restrict;
    }

    @SuppressWarnings("all")
    public void setTabIngest(final boolean tabIngest) {
        this.tabIngest = tabIngest;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof FileUploadMetadata)) return false;
        final FileUploadMetadata other = (FileUploadMetadata) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.isRestrict() != other.isRestrict()) return false;
        if (this.isTabIngest() != other.isTabIngest()) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description)) return false;
        final Object this$directoryLabel = this.getDirectoryLabel();
        final Object other$directoryLabel = other.getDirectoryLabel();
        if (this$directoryLabel == null ? other$directoryLabel != null : !this$directoryLabel.equals(other$directoryLabel)) return false;
        final Object this$categories = this.getCategories();
        final Object other$categories = other.getCategories();
        if (this$categories == null ? other$categories != null : !this$categories.equals(other$categories)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof FileUploadMetadata;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.isRestrict() ? 79 : 97);
        result = result * PRIME + (this.isTabIngest() ? 79 : 97);
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $directoryLabel = this.getDirectoryLabel();
        result = result * PRIME + ($directoryLabel == null ? 43 : $directoryLabel.hashCode());
        final Object $categories = this.getCategories();
        result = result * PRIME + ($categories == null ? 43 : $categories.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "FileUploadMetadata(description=" + this.getDescription() + ", directoryLabel=" + this.getDirectoryLabel() + ", categories=" + this.getCategories() + ", restrict=" + this.isRestrict() + ", tabIngest=" + this.isTabIngest() + ")";
    }
}

// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
package com.researchspace.dataverse.entities;

import com.researchspace.dataverse.http.FileUploadMetadata;

/**
 * Checksum is part of the response from
 * {@link com.researchspace.dataverse.api.v1.DatasetOperations#uploadNativeFile(byte[], FileUploadMetadata, Identifier, String)}
 */
public class Checksum {
    private String type;
    private String value;

    @SuppressWarnings("all")
    public Checksum() {
    }

    @SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    @SuppressWarnings("all")
    public String getValue() {
        return this.value;
    }

    @SuppressWarnings("all")
    public void setType(final String type) {
        this.type = type;
    }

    @SuppressWarnings("all")
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof Checksum)) return false;
        final Checksum other = (Checksum) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$type = this.getType();
        final Object other$type = other.getType();
        if (this$type == null ? other$type != null : !this$type.equals(other$type)) return false;
        final Object this$value = this.getValue();
        final Object other$value = other.getValue();
        if (this$value == null ? other$value != null : !this$value.equals(other$value)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof Checksum;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $type = this.getType();
        result = result * PRIME + ($type == null ? 43 : $type.hashCode());
        final Object $value = this.getValue();
        result = result * PRIME + ($value == null ? 43 : $value.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "Checksum(type=" + this.getType() + ", value=" + this.getValue() + ")";
    }
}

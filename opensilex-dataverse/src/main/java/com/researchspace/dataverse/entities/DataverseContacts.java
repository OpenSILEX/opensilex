// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities;

/**
 * <pre>
 * Copyright 2016 ResearchSpace
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </pre>
 */
public class DataverseContacts {
    private String contactEmail;

    @SuppressWarnings("all")
    public String getContactEmail() {
        return this.contactEmail;
    }

    @SuppressWarnings("all")
    public void setContactEmail(final String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof DataverseContacts)) return false;
        final DataverseContacts other = (DataverseContacts) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$contactEmail = this.getContactEmail();
        final Object other$contactEmail = other.getContactEmail();
        if (this$contactEmail == null ? other$contactEmail != null : !this$contactEmail.equals(other$contactEmail)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof DataverseContacts;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $contactEmail = this.getContactEmail();
        result = result * PRIME + ($contactEmail == null ? 43 : $contactEmail.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "DataverseContacts(contactEmail=" + this.getContactEmail() + ")";
    }

    @SuppressWarnings("all")
    public DataverseContacts(final String contactEmail) {
        this.contactEmail = contactEmail;
    }

    @SuppressWarnings("all")
    public DataverseContacts() {
    }
}

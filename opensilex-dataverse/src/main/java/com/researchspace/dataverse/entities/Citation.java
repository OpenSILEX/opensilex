// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities;

import java.util.List;

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
public class Citation {
	private String displayName = "Citation Metadata";
	private List<CitationField> fields;

	@SuppressWarnings("all")
	public Citation() {
	}

	@SuppressWarnings("all")
	public String getDisplayName() {
		return this.displayName;
	}

	@SuppressWarnings("all")
	public List<CitationField> getFields() {
		return this.fields;
	}

	@SuppressWarnings("all")
	public void setDisplayName(final String displayName) {
		this.displayName = displayName;
	}

	@SuppressWarnings("all")
	public void setFields(final List<CitationField> fields) {
		this.fields = fields;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof Citation)) return false;
		final Citation other = (Citation) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$displayName = this.getDisplayName();
		final Object other$displayName = other.getDisplayName();
		if (this$displayName == null ? other$displayName != null : !this$displayName.equals(other$displayName)) return false;
		final Object this$fields = this.getFields();
		final Object other$fields = other.getFields();
		if (this$fields == null ? other$fields != null : !this$fields.equals(other$fields)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof Citation;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $displayName = this.getDisplayName();
		result = result * PRIME + ($displayName == null ? 43 : $displayName.hashCode());
		final Object $fields = this.getFields();
		result = result * PRIME + ($fields == null ? 43 : $fields.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "Citation(displayName=" + this.getDisplayName() + ", fields=" + this.getFields() + ")";
	}
}

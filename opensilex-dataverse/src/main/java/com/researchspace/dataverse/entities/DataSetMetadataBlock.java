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
public class DataSetMetadataBlock {
	private Citation citation;

	@SuppressWarnings("all")
	public DataSetMetadataBlock() {
	}

	@SuppressWarnings("all")
	public Citation getCitation() {
		return this.citation;
	}

	@SuppressWarnings("all")
	public void setCitation(final Citation citation) {
		this.citation = citation;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DataSetMetadataBlock)) return false;
		final DataSetMetadataBlock other = (DataSetMetadataBlock) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$citation = this.getCitation();
		final Object other$citation = other.getCitation();
		if (this$citation == null ? other$citation != null : !this$citation.equals(other$citation)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DataSetMetadataBlock;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $citation = this.getCitation();
		result = result * PRIME + ($citation == null ? 43 : $citation.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DataSetMetadataBlock(citation=" + this.getCitation() + ")";
	}
}

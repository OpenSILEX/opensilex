// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities.facade;

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
public class DatasetAuthor {
	private String authorName;
	private String authorAffiliation;
	private String authorIdentifierScheme;
	private String authorIdentifier;

	@SuppressWarnings("all")
	DatasetAuthor(final String authorName, final String authorAffiliation, final String authorIdentifierScheme, final String authorIdentifier) {
		if (authorName == null) {
			throw new NullPointerException("authorName is marked non-null but is null");
		}
		this.authorName = authorName;
		this.authorAffiliation = authorAffiliation;
		this.authorIdentifierScheme = authorIdentifierScheme;
		this.authorIdentifier = authorIdentifier;
	}


	@SuppressWarnings("all")
	public static class DatasetAuthorBuilder {
		@SuppressWarnings("all")
		private String authorName;
		@SuppressWarnings("all")
		private String authorAffiliation;
		@SuppressWarnings("all")
		private String authorIdentifierScheme;
		@SuppressWarnings("all")
		private String authorIdentifier;

		@SuppressWarnings("all")
		DatasetAuthorBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetAuthorBuilder authorName(final String authorName) {
			if (authorName == null) {
				throw new NullPointerException("authorName is marked non-null but is null");
			}
			this.authorName = authorName;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetAuthorBuilder authorAffiliation(final String authorAffiliation) {
			this.authorAffiliation = authorAffiliation;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetAuthorBuilder authorIdentifierScheme(final String authorIdentifierScheme) {
			this.authorIdentifierScheme = authorIdentifierScheme;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetAuthorBuilder authorIdentifier(final String authorIdentifier) {
			this.authorIdentifier = authorIdentifier;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetAuthor build() {
			return new DatasetAuthor(this.authorName, this.authorAffiliation, this.authorIdentifierScheme, this.authorIdentifier);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetAuthor.DatasetAuthorBuilder(authorName=" + this.authorName + ", authorAffiliation=" + this.authorAffiliation + ", authorIdentifierScheme=" + this.authorIdentifierScheme + ", authorIdentifier=" + this.authorIdentifier + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetAuthorBuilder builder() {
		return new DatasetAuthorBuilder();
	}

	@SuppressWarnings("all")
	public String getAuthorName() {
		return this.authorName;
	}

	@SuppressWarnings("all")
	public String getAuthorAffiliation() {
		return this.authorAffiliation;
	}

	@SuppressWarnings("all")
	public String getAuthorIdentifierScheme() {
		return this.authorIdentifierScheme;
	}

	@SuppressWarnings("all")
	public String getAuthorIdentifier() {
		return this.authorIdentifier;
	}

	@SuppressWarnings("all")
	public void setAuthorName(final String authorName) {
		if (authorName == null) {
			throw new NullPointerException("authorName is marked non-null but is null");
		}
		this.authorName = authorName;
	}

	@SuppressWarnings("all")
	public void setAuthorAffiliation(final String authorAffiliation) {
		this.authorAffiliation = authorAffiliation;
	}

	@SuppressWarnings("all")
	public void setAuthorIdentifierScheme(final String authorIdentifierScheme) {
		this.authorIdentifierScheme = authorIdentifierScheme;
	}

	@SuppressWarnings("all")
	public void setAuthorIdentifier(final String authorIdentifier) {
		this.authorIdentifier = authorIdentifier;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetAuthor)) return false;
		final DatasetAuthor other = (DatasetAuthor) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$authorName = this.getAuthorName();
		final Object other$authorName = other.getAuthorName();
		if (this$authorName == null ? other$authorName != null : !this$authorName.equals(other$authorName)) return false;
		final Object this$authorAffiliation = this.getAuthorAffiliation();
		final Object other$authorAffiliation = other.getAuthorAffiliation();
		if (this$authorAffiliation == null ? other$authorAffiliation != null : !this$authorAffiliation.equals(other$authorAffiliation)) return false;
		final Object this$authorIdentifierScheme = this.getAuthorIdentifierScheme();
		final Object other$authorIdentifierScheme = other.getAuthorIdentifierScheme();
		if (this$authorIdentifierScheme == null ? other$authorIdentifierScheme != null : !this$authorIdentifierScheme.equals(other$authorIdentifierScheme)) return false;
		final Object this$authorIdentifier = this.getAuthorIdentifier();
		final Object other$authorIdentifier = other.getAuthorIdentifier();
		if (this$authorIdentifier == null ? other$authorIdentifier != null : !this$authorIdentifier.equals(other$authorIdentifier)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetAuthor;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $authorName = this.getAuthorName();
		result = result * PRIME + ($authorName == null ? 43 : $authorName.hashCode());
		final Object $authorAffiliation = this.getAuthorAffiliation();
		result = result * PRIME + ($authorAffiliation == null ? 43 : $authorAffiliation.hashCode());
		final Object $authorIdentifierScheme = this.getAuthorIdentifierScheme();
		result = result * PRIME + ($authorIdentifierScheme == null ? 43 : $authorIdentifierScheme.hashCode());
		final Object $authorIdentifier = this.getAuthorIdentifier();
		result = result * PRIME + ($authorIdentifier == null ? 43 : $authorIdentifier.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetAuthor(authorName=" + this.getAuthorName() + ", authorAffiliation=" + this.getAuthorAffiliation() + ", authorIdentifierScheme=" + this.getAuthorIdentifierScheme() + ", authorIdentifier=" + this.getAuthorIdentifier() + ")";
	}
}

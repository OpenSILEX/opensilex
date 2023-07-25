// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities.facade;

import java.net.URL;

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
public class DatasetPublication {
	private String publicationCitation;
	private String publicationIdNumber;
	private PublicationIDType publicationIDType;
	private URL publicationURL;

	@SuppressWarnings("all")
	DatasetPublication(final String publicationCitation, final String publicationIdNumber, final PublicationIDType publicationIDType, final URL publicationURL) {
		this.publicationCitation = publicationCitation;
		this.publicationIdNumber = publicationIdNumber;
		this.publicationIDType = publicationIDType;
		this.publicationURL = publicationURL;
	}


	@SuppressWarnings("all")
	public static class DatasetPublicationBuilder {
		@SuppressWarnings("all")
		private String publicationCitation;
		@SuppressWarnings("all")
		private String publicationIdNumber;
		@SuppressWarnings("all")
		private PublicationIDType publicationIDType;
		@SuppressWarnings("all")
		private URL publicationURL;

		@SuppressWarnings("all")
		DatasetPublicationBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetPublicationBuilder publicationCitation(final String publicationCitation) {
			this.publicationCitation = publicationCitation;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetPublicationBuilder publicationIdNumber(final String publicationIdNumber) {
			this.publicationIdNumber = publicationIdNumber;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetPublicationBuilder publicationIDType(final PublicationIDType publicationIDType) {
			this.publicationIDType = publicationIDType;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetPublicationBuilder publicationURL(final URL publicationURL) {
			this.publicationURL = publicationURL;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetPublication build() {
			return new DatasetPublication(this.publicationCitation, this.publicationIdNumber, this.publicationIDType, this.publicationURL);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetPublication.DatasetPublicationBuilder(publicationCitation=" + this.publicationCitation + ", publicationIdNumber=" + this.publicationIdNumber + ", publicationIDType=" + this.publicationIDType + ", publicationURL=" + this.publicationURL + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetPublicationBuilder builder() {
		return new DatasetPublicationBuilder();
	}

	@SuppressWarnings("all")
	public String getPublicationCitation() {
		return this.publicationCitation;
	}

	@SuppressWarnings("all")
	public String getPublicationIdNumber() {
		return this.publicationIdNumber;
	}

	@SuppressWarnings("all")
	public PublicationIDType getPublicationIDType() {
		return this.publicationIDType;
	}

	@SuppressWarnings("all")
	public URL getPublicationURL() {
		return this.publicationURL;
	}

	@SuppressWarnings("all")
	public void setPublicationCitation(final String publicationCitation) {
		this.publicationCitation = publicationCitation;
	}

	@SuppressWarnings("all")
	public void setPublicationIdNumber(final String publicationIdNumber) {
		this.publicationIdNumber = publicationIdNumber;
	}

	@SuppressWarnings("all")
	public void setPublicationIDType(final PublicationIDType publicationIDType) {
		this.publicationIDType = publicationIDType;
	}

	@SuppressWarnings("all")
	public void setPublicationURL(final URL publicationURL) {
		this.publicationURL = publicationURL;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetPublication)) return false;
		final DatasetPublication other = (DatasetPublication) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$publicationCitation = this.getPublicationCitation();
		final Object other$publicationCitation = other.getPublicationCitation();
		if (this$publicationCitation == null ? other$publicationCitation != null : !this$publicationCitation.equals(other$publicationCitation)) return false;
		final Object this$publicationIdNumber = this.getPublicationIdNumber();
		final Object other$publicationIdNumber = other.getPublicationIdNumber();
		if (this$publicationIdNumber == null ? other$publicationIdNumber != null : !this$publicationIdNumber.equals(other$publicationIdNumber)) return false;
		final Object this$publicationIDType = this.getPublicationIDType();
		final Object other$publicationIDType = other.getPublicationIDType();
		if (this$publicationIDType == null ? other$publicationIDType != null : !this$publicationIDType.equals(other$publicationIDType)) return false;
		final Object this$publicationURL = this.getPublicationURL();
		final Object other$publicationURL = other.getPublicationURL();
		if (this$publicationURL == null ? other$publicationURL != null : !this$publicationURL.equals(other$publicationURL)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetPublication;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $publicationCitation = this.getPublicationCitation();
		result = result * PRIME + ($publicationCitation == null ? 43 : $publicationCitation.hashCode());
		final Object $publicationIdNumber = this.getPublicationIdNumber();
		result = result * PRIME + ($publicationIdNumber == null ? 43 : $publicationIdNumber.hashCode());
		final Object $publicationIDType = this.getPublicationIDType();
		result = result * PRIME + ($publicationIDType == null ? 43 : $publicationIDType.hashCode());
		final Object $publicationURL = this.getPublicationURL();
		result = result * PRIME + ($publicationURL == null ? 43 : $publicationURL.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetPublication(publicationCitation=" + this.getPublicationCitation() + ", publicationIdNumber=" + this.getPublicationIdNumber() + ", publicationIDType=" + this.getPublicationIDType() + ", publicationURL=" + this.getPublicationURL() + ")";
	}
}

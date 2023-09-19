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
public class DatasetProducer {
	private String name;
	private String affiliation;
	private String abbreviation;
	private URL url;
	private URL logoURL;

	@SuppressWarnings("all")
	DatasetProducer(final String name, final String affiliation, final String abbreviation, final URL url, final URL logoURL) {
		this.name = name;
		this.affiliation = affiliation;
		this.abbreviation = abbreviation;
		this.url = url;
		this.logoURL = logoURL;
	}


	@SuppressWarnings("all")
	public static class DatasetProducerBuilder {
		@SuppressWarnings("all")
		private String name;
		@SuppressWarnings("all")
		private String affiliation;
		@SuppressWarnings("all")
		private String abbreviation;
		@SuppressWarnings("all")
		private URL url;
		@SuppressWarnings("all")
		private URL logoURL;

		@SuppressWarnings("all")
		DatasetProducerBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetProducerBuilder name(final String name) {
			this.name = name;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetProducerBuilder affiliation(final String affiliation) {
			this.affiliation = affiliation;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetProducerBuilder abbreviation(final String abbreviation) {
			this.abbreviation = abbreviation;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetProducerBuilder url(final URL url) {
			this.url = url;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetProducerBuilder logoURL(final URL logoURL) {
			this.logoURL = logoURL;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetProducer build() {
			return new DatasetProducer(this.name, this.affiliation, this.abbreviation, this.url, this.logoURL);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetProducer.DatasetProducerBuilder(name=" + this.name + ", affiliation=" + this.affiliation + ", abbreviation=" + this.abbreviation + ", url=" + this.url + ", logoURL=" + this.logoURL + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetProducerBuilder builder() {
		return new DatasetProducerBuilder();
	}

	@SuppressWarnings("all")
	public String getName() {
		return this.name;
	}

	@SuppressWarnings("all")
	public String getAffiliation() {
		return this.affiliation;
	}

	@SuppressWarnings("all")
	public String getAbbreviation() {
		return this.abbreviation;
	}

	@SuppressWarnings("all")
	public URL getUrl() {
		return this.url;
	}

	@SuppressWarnings("all")
	public URL getLogoURL() {
		return this.logoURL;
	}

	@SuppressWarnings("all")
	public void setName(final String name) {
		this.name = name;
	}

	@SuppressWarnings("all")
	public void setAffiliation(final String affiliation) {
		this.affiliation = affiliation;
	}

	@SuppressWarnings("all")
	public void setAbbreviation(final String abbreviation) {
		this.abbreviation = abbreviation;
	}

	@SuppressWarnings("all")
	public void setUrl(final URL url) {
		this.url = url;
	}

	@SuppressWarnings("all")
	public void setLogoURL(final URL logoURL) {
		this.logoURL = logoURL;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetProducer)) return false;
		final DatasetProducer other = (DatasetProducer) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
		final Object this$affiliation = this.getAffiliation();
		final Object other$affiliation = other.getAffiliation();
		if (this$affiliation == null ? other$affiliation != null : !this$affiliation.equals(other$affiliation)) return false;
		final Object this$abbreviation = this.getAbbreviation();
		final Object other$abbreviation = other.getAbbreviation();
		if (this$abbreviation == null ? other$abbreviation != null : !this$abbreviation.equals(other$abbreviation)) return false;
		final Object this$url = this.getUrl();
		final Object other$url = other.getUrl();
		if (this$url == null ? other$url != null : !this$url.equals(other$url)) return false;
		final Object this$logoURL = this.getLogoURL();
		final Object other$logoURL = other.getLogoURL();
		if (this$logoURL == null ? other$logoURL != null : !this$logoURL.equals(other$logoURL)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetProducer;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $affiliation = this.getAffiliation();
		result = result * PRIME + ($affiliation == null ? 43 : $affiliation.hashCode());
		final Object $abbreviation = this.getAbbreviation();
		result = result * PRIME + ($abbreviation == null ? 43 : $abbreviation.hashCode());
		final Object $url = this.getUrl();
		result = result * PRIME + ($url == null ? 43 : $url.hashCode());
		final Object $logoURL = this.getLogoURL();
		result = result * PRIME + ($logoURL == null ? 43 : $logoURL.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetProducer(name=" + this.getName() + ", affiliation=" + this.getAffiliation() + ", abbreviation=" + this.getAbbreviation() + ", url=" + this.getUrl() + ", logoURL=" + this.getLogoURL() + ")";
	}
}

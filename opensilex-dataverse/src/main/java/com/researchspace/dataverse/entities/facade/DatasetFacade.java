// Modified by Valentin Rigolle (valentin.rigolle@inrae.fr) using delombok. Date : 2023-07-24T14:52:00+0200
/*
 * 
 */
package com.researchspace.dataverse.entities.facade;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * /** <pre>
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
 * Simple POJO to set info for Dataset. 
 * @author rspace
 */
public class DatasetFacade {
	private String title;
	private List<DatasetAuthor> authors;
	private List<DatasetContact> contacts;
	private String subject;
	private List<DatasetDescription> descriptions;
	private String depositor;
	private String subtitle;
	private String alternativeTitle;
	private URL alternativeURL;
	private List<DatasetKeyword> keywords;
	private List<DatasetTopicClassification> topicClassifications;
	private List<DatasetPublication> publications;
	private List<DatasetProducer> producers;
	private String note;
	private List<String> languages = new ArrayList<>();
	private Date productionDate;
	private String productionPlace;
	private List<DatasetContributor> contributors;

	/**
	 * Returns a copy if the internally stored Date
	 * @return
	 */
	public Date getProductionDate() {
		if (productionDate != null) {
			return new Date(productionDate.getTime());
		} else {
			return null;
		}
	}

	/**
	 * Sets this object's date as a copy of the parameter Date.
	 * @param date
	 */
	public void setProductionDate(Date date) {
		this.productionDate = new Date(date.getTime());
	}

	/*
      * For testing
      */
	DatasetFacade() {
		// TODO Auto-generated constructor stub
	}


	@SuppressWarnings("all")
	public static class DatasetFacadeBuilder {
		@SuppressWarnings("all")
		private String title;
		@SuppressWarnings("all")
		private ArrayList<DatasetAuthor> authors;
		@SuppressWarnings("all")
		private ArrayList<DatasetContact> contacts;
		@SuppressWarnings("all")
		private String subject;
		@SuppressWarnings("all")
		private ArrayList<DatasetDescription> descriptions;
		@SuppressWarnings("all")
		private String depositor;
		@SuppressWarnings("all")
		private String subtitle;
		@SuppressWarnings("all")
		private String alternativeTitle;
		@SuppressWarnings("all")
		private URL alternativeURL;
		@SuppressWarnings("all")
		private ArrayList<DatasetKeyword> keywords;
		@SuppressWarnings("all")
		private ArrayList<DatasetTopicClassification> topicClassifications;
		@SuppressWarnings("all")
		private ArrayList<DatasetPublication> publications;
		@SuppressWarnings("all")
		private ArrayList<DatasetProducer> producers;
		@SuppressWarnings("all")
		private String note;
		@SuppressWarnings("all")
		private List<String> languages;
		@SuppressWarnings("all")
		private Date productionDate;
		@SuppressWarnings("all")
		private String productionPlace;
		@SuppressWarnings("all")
		private ArrayList<DatasetContributor> contributors;

		@SuppressWarnings("all")
		DatasetFacadeBuilder() {
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder title(final String title) {
			if (title == null) {
				throw new NullPointerException("title is marked non-null but is null");
			}
			this.title = title;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder author(final DatasetAuthor author) {
			if (this.authors == null) this.authors = new ArrayList<DatasetAuthor>();
			this.authors.add(author);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder authors(final java.util.Collection<? extends DatasetAuthor> authors) {
			if (authors == null) {
				throw new NullPointerException("authors cannot be null");
			}
			if (this.authors == null) this.authors = new ArrayList<DatasetAuthor>();
			this.authors.addAll(authors);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder clearAuthors() {
			if (this.authors != null) this.authors.clear();
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder contact(final DatasetContact contact) {
			if (this.contacts == null) this.contacts = new ArrayList<DatasetContact>();
			this.contacts.add(contact);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder contacts(final java.util.Collection<? extends DatasetContact> contacts) {
			if (contacts == null) {
				throw new NullPointerException("contacts cannot be null");
			}
			if (this.contacts == null) this.contacts = new ArrayList<DatasetContact>();
			this.contacts.addAll(contacts);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder clearContacts() {
			if (this.contacts != null) this.contacts.clear();
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder subject(final String subject) {
			if (subject == null) {
				throw new NullPointerException("subject is marked non-null but is null");
			}
			this.subject = subject;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder description(final DatasetDescription description) {
			if (this.descriptions == null) this.descriptions = new ArrayList<DatasetDescription>();
			this.descriptions.add(description);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder descriptions(final java.util.Collection<? extends DatasetDescription> descriptions) {
			if (descriptions == null) {
				throw new NullPointerException("descriptions cannot be null");
			}
			if (this.descriptions == null) this.descriptions = new ArrayList<DatasetDescription>();
			this.descriptions.addAll(descriptions);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder clearDescriptions() {
			if (this.descriptions != null) this.descriptions.clear();
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder depositor(final String depositor) {
			this.depositor = depositor;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder subtitle(final String subtitle) {
			this.subtitle = subtitle;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder alternativeTitle(final String alternativeTitle) {
			this.alternativeTitle = alternativeTitle;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder alternativeURL(final URL alternativeURL) {
			this.alternativeURL = alternativeURL;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder keyword(final DatasetKeyword keyword) {
			if (this.keywords == null) this.keywords = new ArrayList<DatasetKeyword>();
			this.keywords.add(keyword);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder keywords(final java.util.Collection<? extends DatasetKeyword> keywords) {
			if (keywords == null) {
				throw new NullPointerException("keywords cannot be null");
			}
			if (this.keywords == null) this.keywords = new ArrayList<DatasetKeyword>();
			this.keywords.addAll(keywords);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder clearKeywords() {
			if (this.keywords != null) this.keywords.clear();
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder topicClassification(final DatasetTopicClassification topicClassification) {
			if (this.topicClassifications == null) this.topicClassifications = new ArrayList<DatasetTopicClassification>();
			this.topicClassifications.add(topicClassification);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder topicClassifications(final java.util.Collection<? extends DatasetTopicClassification> topicClassifications) {
			if (topicClassifications == null) {
				throw new NullPointerException("topicClassifications cannot be null");
			}
			if (this.topicClassifications == null) this.topicClassifications = new ArrayList<DatasetTopicClassification>();
			this.topicClassifications.addAll(topicClassifications);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder clearTopicClassifications() {
			if (this.topicClassifications != null) this.topicClassifications.clear();
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder publication(final DatasetPublication publication) {
			if (this.publications == null) this.publications = new ArrayList<DatasetPublication>();
			this.publications.add(publication);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder publications(final java.util.Collection<? extends DatasetPublication> publications) {
			if (publications == null) {
				throw new NullPointerException("publications cannot be null");
			}
			if (this.publications == null) this.publications = new ArrayList<DatasetPublication>();
			this.publications.addAll(publications);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder clearPublications() {
			if (this.publications != null) this.publications.clear();
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder producer(final DatasetProducer producer) {
			if (this.producers == null) this.producers = new ArrayList<DatasetProducer>();
			this.producers.add(producer);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder producers(final java.util.Collection<? extends DatasetProducer> producers) {
			if (producers == null) {
				throw new NullPointerException("producers cannot be null");
			}
			if (this.producers == null) this.producers = new ArrayList<DatasetProducer>();
			this.producers.addAll(producers);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder clearProducers() {
			if (this.producers != null) this.producers.clear();
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder note(final String note) {
			this.note = note;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder languages(final List<String> languages) {
			this.languages = languages;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder productionDate(final Date productionDate) {
			this.productionDate = productionDate;
			return this;
		}

		/**
		 * @return {@code this}.
		 */
		@SuppressWarnings("all")
		public DatasetFacadeBuilder productionPlace(final String productionPlace) {
			this.productionPlace = productionPlace;
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder contributor(final DatasetContributor contributor) {
			if (this.contributors == null) this.contributors = new ArrayList<DatasetContributor>();
			this.contributors.add(contributor);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder contributors(final java.util.Collection<? extends DatasetContributor> contributors) {
			if (contributors == null) {
				throw new NullPointerException("contributors cannot be null");
			}
			if (this.contributors == null) this.contributors = new ArrayList<DatasetContributor>();
			this.contributors.addAll(contributors);
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacadeBuilder clearContributors() {
			if (this.contributors != null) this.contributors.clear();
			return this;
		}

		@SuppressWarnings("all")
		public DatasetFacade build() {
			List<DatasetAuthor> authors;
			switch (this.authors == null ? 0 : this.authors.size()) {
			case 0: 
				authors = java.util.Collections.emptyList();
				break;
			case 1: 
				authors = java.util.Collections.singletonList(this.authors.get(0));
				break;
			default: 
				authors = java.util.Collections.unmodifiableList(new ArrayList<DatasetAuthor>(this.authors));
			}
			List<DatasetContact> contacts;
			switch (this.contacts == null ? 0 : this.contacts.size()) {
			case 0: 
				contacts = java.util.Collections.emptyList();
				break;
			case 1: 
				contacts = java.util.Collections.singletonList(this.contacts.get(0));
				break;
			default: 
				contacts = java.util.Collections.unmodifiableList(new ArrayList<DatasetContact>(this.contacts));
			}
			List<DatasetDescription> descriptions;
			switch (this.descriptions == null ? 0 : this.descriptions.size()) {
			case 0: 
				descriptions = java.util.Collections.emptyList();
				break;
			case 1: 
				descriptions = java.util.Collections.singletonList(this.descriptions.get(0));
				break;
			default: 
				descriptions = java.util.Collections.unmodifiableList(new ArrayList<DatasetDescription>(this.descriptions));
			}
			List<DatasetKeyword> keywords;
			switch (this.keywords == null ? 0 : this.keywords.size()) {
			case 0: 
				keywords = java.util.Collections.emptyList();
				break;
			case 1: 
				keywords = java.util.Collections.singletonList(this.keywords.get(0));
				break;
			default: 
				keywords = java.util.Collections.unmodifiableList(new ArrayList<DatasetKeyword>(this.keywords));
			}
			List<DatasetTopicClassification> topicClassifications;
			switch (this.topicClassifications == null ? 0 : this.topicClassifications.size()) {
			case 0: 
				topicClassifications = java.util.Collections.emptyList();
				break;
			case 1: 
				topicClassifications = java.util.Collections.singletonList(this.topicClassifications.get(0));
				break;
			default: 
				topicClassifications = java.util.Collections.unmodifiableList(new ArrayList<DatasetTopicClassification>(this.topicClassifications));
			}
			List<DatasetPublication> publications;
			switch (this.publications == null ? 0 : this.publications.size()) {
			case 0: 
				publications = java.util.Collections.emptyList();
				break;
			case 1: 
				publications = java.util.Collections.singletonList(this.publications.get(0));
				break;
			default: 
				publications = java.util.Collections.unmodifiableList(new ArrayList<DatasetPublication>(this.publications));
			}
			List<DatasetProducer> producers;
			switch (this.producers == null ? 0 : this.producers.size()) {
			case 0: 
				producers = java.util.Collections.emptyList();
				break;
			case 1: 
				producers = java.util.Collections.singletonList(this.producers.get(0));
				break;
			default: 
				producers = java.util.Collections.unmodifiableList(new ArrayList<DatasetProducer>(this.producers));
			}
			List<DatasetContributor> contributors;
			switch (this.contributors == null ? 0 : this.contributors.size()) {
			case 0: 
				contributors = java.util.Collections.emptyList();
				break;
			case 1: 
				contributors = java.util.Collections.singletonList(this.contributors.get(0));
				break;
			default: 
				contributors = java.util.Collections.unmodifiableList(new ArrayList<DatasetContributor>(this.contributors));
			}
			return new DatasetFacade(this.title, authors, contacts, this.subject, descriptions, this.depositor, this.subtitle, this.alternativeTitle, this.alternativeURL, keywords, topicClassifications, publications, producers, this.note, this.languages, this.productionDate, this.productionPlace, contributors);
		}

		@Override
		@SuppressWarnings("all")
		public String toString() {
			return "DatasetFacade.DatasetFacadeBuilder(title=" + this.title + ", authors=" + this.authors + ", contacts=" + this.contacts + ", subject=" + this.subject + ", descriptions=" + this.descriptions + ", depositor=" + this.depositor + ", subtitle=" + this.subtitle + ", alternativeTitle=" + this.alternativeTitle + ", alternativeURL=" + this.alternativeURL + ", keywords=" + this.keywords + ", topicClassifications=" + this.topicClassifications + ", publications=" + this.publications + ", producers=" + this.producers + ", note=" + this.note + ", languages=" + this.languages + ", productionDate=" + this.productionDate + ", productionPlace=" + this.productionPlace + ", contributors=" + this.contributors + ")";
		}
	}

	@SuppressWarnings("all")
	public static DatasetFacadeBuilder builder() {
		return new DatasetFacadeBuilder();
	}

	@SuppressWarnings("all")
	public String getTitle() {
		return this.title;
	}

	@SuppressWarnings("all")
	public List<DatasetAuthor> getAuthors() {
		return this.authors;
	}

	@SuppressWarnings("all")
	public List<DatasetContact> getContacts() {
		return this.contacts;
	}

	@SuppressWarnings("all")
	public String getSubject() {
		return this.subject;
	}

	@SuppressWarnings("all")
	public List<DatasetDescription> getDescriptions() {
		return this.descriptions;
	}

	@SuppressWarnings("all")
	public String getDepositor() {
		return this.depositor;
	}

	@SuppressWarnings("all")
	public String getSubtitle() {
		return this.subtitle;
	}

	@SuppressWarnings("all")
	public String getAlternativeTitle() {
		return this.alternativeTitle;
	}

	@SuppressWarnings("all")
	public URL getAlternativeURL() {
		return this.alternativeURL;
	}

	@SuppressWarnings("all")
	public List<DatasetKeyword> getKeywords() {
		return this.keywords;
	}

	@SuppressWarnings("all")
	public List<DatasetTopicClassification> getTopicClassifications() {
		return this.topicClassifications;
	}

	@SuppressWarnings("all")
	public List<DatasetPublication> getPublications() {
		return this.publications;
	}

	@SuppressWarnings("all")
	public List<DatasetProducer> getProducers() {
		return this.producers;
	}

	@SuppressWarnings("all")
	public String getNote() {
		return this.note;
	}

	@SuppressWarnings("all")
	public List<String> getLanguages() {
		return this.languages;
	}

	@SuppressWarnings("all")
	public String getProductionPlace() {
		return this.productionPlace;
	}

	@SuppressWarnings("all")
	public List<DatasetContributor> getContributors() {
		return this.contributors;
	}

	@SuppressWarnings("all")
	public void setTitle(final String title) {
		if (title == null) {
			throw new NullPointerException("title is marked non-null but is null");
		}
		this.title = title;
	}

	@SuppressWarnings("all")
	public void setAuthors(final List<DatasetAuthor> authors) {
		if (authors == null) {
			throw new NullPointerException("authors is marked non-null but is null");
		}
		this.authors = authors;
	}

	@SuppressWarnings("all")
	public void setContacts(final List<DatasetContact> contacts) {
		if (contacts == null) {
			throw new NullPointerException("contacts is marked non-null but is null");
		}
		this.contacts = contacts;
	}

	@SuppressWarnings("all")
	public void setSubject(final String subject) {
		if (subject == null) {
			throw new NullPointerException("subject is marked non-null but is null");
		}
		this.subject = subject;
	}

	@SuppressWarnings("all")
	public void setDescriptions(final List<DatasetDescription> descriptions) {
		if (descriptions == null) {
			throw new NullPointerException("descriptions is marked non-null but is null");
		}
		this.descriptions = descriptions;
	}

	@SuppressWarnings("all")
	public void setDepositor(final String depositor) {
		this.depositor = depositor;
	}

	@SuppressWarnings("all")
	public void setSubtitle(final String subtitle) {
		this.subtitle = subtitle;
	}

	@SuppressWarnings("all")
	public void setAlternativeTitle(final String alternativeTitle) {
		this.alternativeTitle = alternativeTitle;
	}

	@SuppressWarnings("all")
	public void setAlternativeURL(final URL alternativeURL) {
		this.alternativeURL = alternativeURL;
	}

	@SuppressWarnings("all")
	public void setKeywords(final List<DatasetKeyword> keywords) {
		this.keywords = keywords;
	}

	@SuppressWarnings("all")
	public void setTopicClassifications(final List<DatasetTopicClassification> topicClassifications) {
		this.topicClassifications = topicClassifications;
	}

	@SuppressWarnings("all")
	public void setPublications(final List<DatasetPublication> publications) {
		this.publications = publications;
	}

	@SuppressWarnings("all")
	public void setProducers(final List<DatasetProducer> producers) {
		this.producers = producers;
	}

	@SuppressWarnings("all")
	public void setNote(final String note) {
		this.note = note;
	}

	@SuppressWarnings("all")
	public void setLanguages(final List<String> languages) {
		this.languages = languages;
	}

	@SuppressWarnings("all")
	public void setProductionPlace(final String productionPlace) {
		this.productionPlace = productionPlace;
	}

	@SuppressWarnings("all")
	public void setContributors(final List<DatasetContributor> contributors) {
		this.contributors = contributors;
	}

	@Override
	@SuppressWarnings("all")
	public boolean equals(final Object o) {
		if (o == this) return true;
		if (!(o instanceof DatasetFacade)) return false;
		final DatasetFacade other = (DatasetFacade) o;
		if (!other.canEqual((Object) this)) return false;
		final Object this$title = this.getTitle();
		final Object other$title = other.getTitle();
		if (this$title == null ? other$title != null : !this$title.equals(other$title)) return false;
		final Object this$authors = this.getAuthors();
		final Object other$authors = other.getAuthors();
		if (this$authors == null ? other$authors != null : !this$authors.equals(other$authors)) return false;
		final Object this$contacts = this.getContacts();
		final Object other$contacts = other.getContacts();
		if (this$contacts == null ? other$contacts != null : !this$contacts.equals(other$contacts)) return false;
		final Object this$subject = this.getSubject();
		final Object other$subject = other.getSubject();
		if (this$subject == null ? other$subject != null : !this$subject.equals(other$subject)) return false;
		final Object this$descriptions = this.getDescriptions();
		final Object other$descriptions = other.getDescriptions();
		if (this$descriptions == null ? other$descriptions != null : !this$descriptions.equals(other$descriptions)) return false;
		final Object this$depositor = this.getDepositor();
		final Object other$depositor = other.getDepositor();
		if (this$depositor == null ? other$depositor != null : !this$depositor.equals(other$depositor)) return false;
		final Object this$subtitle = this.getSubtitle();
		final Object other$subtitle = other.getSubtitle();
		if (this$subtitle == null ? other$subtitle != null : !this$subtitle.equals(other$subtitle)) return false;
		final Object this$alternativeTitle = this.getAlternativeTitle();
		final Object other$alternativeTitle = other.getAlternativeTitle();
		if (this$alternativeTitle == null ? other$alternativeTitle != null : !this$alternativeTitle.equals(other$alternativeTitle)) return false;
		final Object this$alternativeURL = this.getAlternativeURL();
		final Object other$alternativeURL = other.getAlternativeURL();
		if (this$alternativeURL == null ? other$alternativeURL != null : !this$alternativeURL.equals(other$alternativeURL)) return false;
		final Object this$keywords = this.getKeywords();
		final Object other$keywords = other.getKeywords();
		if (this$keywords == null ? other$keywords != null : !this$keywords.equals(other$keywords)) return false;
		final Object this$topicClassifications = this.getTopicClassifications();
		final Object other$topicClassifications = other.getTopicClassifications();
		if (this$topicClassifications == null ? other$topicClassifications != null : !this$topicClassifications.equals(other$topicClassifications)) return false;
		final Object this$publications = this.getPublications();
		final Object other$publications = other.getPublications();
		if (this$publications == null ? other$publications != null : !this$publications.equals(other$publications)) return false;
		final Object this$producers = this.getProducers();
		final Object other$producers = other.getProducers();
		if (this$producers == null ? other$producers != null : !this$producers.equals(other$producers)) return false;
		final Object this$note = this.getNote();
		final Object other$note = other.getNote();
		if (this$note == null ? other$note != null : !this$note.equals(other$note)) return false;
		final Object this$languages = this.getLanguages();
		final Object other$languages = other.getLanguages();
		if (this$languages == null ? other$languages != null : !this$languages.equals(other$languages)) return false;
		final Object this$productionDate = this.getProductionDate();
		final Object other$productionDate = other.getProductionDate();
		if (this$productionDate == null ? other$productionDate != null : !this$productionDate.equals(other$productionDate)) return false;
		final Object this$productionPlace = this.getProductionPlace();
		final Object other$productionPlace = other.getProductionPlace();
		if (this$productionPlace == null ? other$productionPlace != null : !this$productionPlace.equals(other$productionPlace)) return false;
		final Object this$contributors = this.getContributors();
		final Object other$contributors = other.getContributors();
		if (this$contributors == null ? other$contributors != null : !this$contributors.equals(other$contributors)) return false;
		return true;
	}

	@SuppressWarnings("all")
	protected boolean canEqual(final Object other) {
		return other instanceof DatasetFacade;
	}

	@Override
	@SuppressWarnings("all")
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $title = this.getTitle();
		result = result * PRIME + ($title == null ? 43 : $title.hashCode());
		final Object $authors = this.getAuthors();
		result = result * PRIME + ($authors == null ? 43 : $authors.hashCode());
		final Object $contacts = this.getContacts();
		result = result * PRIME + ($contacts == null ? 43 : $contacts.hashCode());
		final Object $subject = this.getSubject();
		result = result * PRIME + ($subject == null ? 43 : $subject.hashCode());
		final Object $descriptions = this.getDescriptions();
		result = result * PRIME + ($descriptions == null ? 43 : $descriptions.hashCode());
		final Object $depositor = this.getDepositor();
		result = result * PRIME + ($depositor == null ? 43 : $depositor.hashCode());
		final Object $subtitle = this.getSubtitle();
		result = result * PRIME + ($subtitle == null ? 43 : $subtitle.hashCode());
		final Object $alternativeTitle = this.getAlternativeTitle();
		result = result * PRIME + ($alternativeTitle == null ? 43 : $alternativeTitle.hashCode());
		final Object $alternativeURL = this.getAlternativeURL();
		result = result * PRIME + ($alternativeURL == null ? 43 : $alternativeURL.hashCode());
		final Object $keywords = this.getKeywords();
		result = result * PRIME + ($keywords == null ? 43 : $keywords.hashCode());
		final Object $topicClassifications = this.getTopicClassifications();
		result = result * PRIME + ($topicClassifications == null ? 43 : $topicClassifications.hashCode());
		final Object $publications = this.getPublications();
		result = result * PRIME + ($publications == null ? 43 : $publications.hashCode());
		final Object $producers = this.getProducers();
		result = result * PRIME + ($producers == null ? 43 : $producers.hashCode());
		final Object $note = this.getNote();
		result = result * PRIME + ($note == null ? 43 : $note.hashCode());
		final Object $languages = this.getLanguages();
		result = result * PRIME + ($languages == null ? 43 : $languages.hashCode());
		final Object $productionDate = this.getProductionDate();
		result = result * PRIME + ($productionDate == null ? 43 : $productionDate.hashCode());
		final Object $productionPlace = this.getProductionPlace();
		result = result * PRIME + ($productionPlace == null ? 43 : $productionPlace.hashCode());
		final Object $contributors = this.getContributors();
		result = result * PRIME + ($contributors == null ? 43 : $contributors.hashCode());
		return result;
	}

	@Override
	@SuppressWarnings("all")
	public String toString() {
		return "DatasetFacade(title=" + this.getTitle() + ", authors=" + this.getAuthors() + ", contacts=" + this.getContacts() + ", subject=" + this.getSubject() + ", descriptions=" + this.getDescriptions() + ", depositor=" + this.getDepositor() + ", subtitle=" + this.getSubtitle() + ", alternativeTitle=" + this.getAlternativeTitle() + ", alternativeURL=" + this.getAlternativeURL() + ", keywords=" + this.getKeywords() + ", topicClassifications=" + this.getTopicClassifications() + ", publications=" + this.getPublications() + ", producers=" + this.getProducers() + ", note=" + this.getNote() + ", languages=" + this.getLanguages() + ", productionDate=" + this.getProductionDate() + ", productionPlace=" + this.getProductionPlace() + ", contributors=" + this.getContributors() + ")";
	}

	@SuppressWarnings("all")
	public DatasetFacade(final String title, final List<DatasetAuthor> authors, final List<DatasetContact> contacts, final String subject, final List<DatasetDescription> descriptions, final String depositor, final String subtitle, final String alternativeTitle, final URL alternativeURL, final List<DatasetKeyword> keywords, final List<DatasetTopicClassification> topicClassifications, final List<DatasetPublication> publications, final List<DatasetProducer> producers, final String note, final List<String> languages, final Date productionDate, final String productionPlace, final List<DatasetContributor> contributors) {
		if (title == null) {
			throw new NullPointerException("title is marked non-null but is null");
		}
		if (authors == null) {
			throw new NullPointerException("authors is marked non-null but is null");
		}
		if (contacts == null) {
			throw new NullPointerException("contacts is marked non-null but is null");
		}
		if (subject == null) {
			throw new NullPointerException("subject is marked non-null but is null");
		}
		if (descriptions == null) {
			throw new NullPointerException("descriptions is marked non-null but is null");
		}
		this.title = title;
		this.authors = authors;
		this.contacts = contacts;
		this.subject = subject;
		this.descriptions = descriptions;
		this.depositor = depositor;
		this.subtitle = subtitle;
		this.alternativeTitle = alternativeTitle;
		this.alternativeURL = alternativeURL;
		this.keywords = keywords;
		this.topicClassifications = topicClassifications;
		this.publications = publications;
		this.producers = producers;
		this.note = note;
		this.languages = languages;
		this.productionDate = productionDate;
		this.productionPlace = productionPlace;
		this.contributors = contributors;
	}
}

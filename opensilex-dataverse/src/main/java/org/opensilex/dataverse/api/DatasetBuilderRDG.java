package org.opensilex.dataverse.api;

import com.researchspace.dataverse.entities.*;
import com.researchspace.dataverse.entities.facade.*;

import java.util.*;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class DatasetBuilderRDG extends DatasetBuilder {

    private static final String CONTRIBUTOR = "contributor";
    private static final String CONTRIBUTOR_TYPE = "contributorType";
    private static final String CONTRIBUTOR_NAME = "contributorName";
    private static final String PRODUCER_LOGO_URL = "producerLogoURL";
    private static final String PRODUCER_URL = "producerURL";
    private static final String PRODUCER_AFFILIATION = "producerAffiliation";
    private static final String PRODUCER_ABBREVIATION = "producerAbbreviation";
    private static final String PRODUCER_NAME = "producerName";
    private static final String PUBLICATION_URL = "publicationURL";
    private static final String PUBLICATION_ID = "publicationIDNumber";
    private static final String PUBLICATION_ID_TYPE = "publicationIDType";
    private static final String PUBLICATION_CITATION= "publicationCitation";

    private static final String KEYWORD_VOCABULARY_URI = "keywordVocabularyURI";
    private static final String KEYWORD_VOCABULARY = "keywordVocabulary";
    private static final String KEYWORD_VALUE = "keywordValue";
    private static final String TOPIC_VOCABULARY_URI = "topicClassVocabURI";
    private static final String TOPIC_VOCABULARY = "topicClassVocab";
    private static final String TOPIC_VALUE = "topicClassValue";
    private static final String DATASET_DESC_VALUE = "dsDescriptionValue";
    private static final String DATASET_DESC_DATE = "dsDescriptionDate";
    private static final String DATASET_CONTACT_EMAIL = "datasetContactEmail";
    private static final String DATASET_CONTACT_NAME = "datasetContactName";
    private static final String DATASET_CONTACT_AFFILIATION = "datasetContactAffiliation";
    private static final String AUTHOR_IDENTIFIER_SCHEME = "authorIdentifierScheme";
    private static final String AUTHOR_IDENTIFIER = "authorIdentifier";
    private static final String AUTHOR_AFFILIATION = "authorAffiliation";
    private static final String AUTHOR_NAME = "authorName";

    @Override
    public Dataset build(DatasetFacade facade) {
        DatasetVersion dv = new DatasetVersion();
        DataSetMetadataBlock blocks = new DataSetMetadataBlock();
        Citation citation = new Citation();

        List<CitationField> fields = createFields(facade, citation);

        dv.setMetadataBlocks(blocks);
        blocks.setCitation(citation);
        citation.setFields(fields);
        DatasetRDG toSubmit = new DatasetRDG();
        toSubmit.setDatasetVersion(dv);
        toSubmit.setMetadataLanguage("en");
        return toSubmit;
    }
    @Override
    protected List<CitationField> createFields(DatasetFacade facade, Citation citation) {
        List<CitationField> fields = new ArrayList<>();
        //mandatory fields
        addTitle(facade, fields);
        addAuthors(facade, fields);
        addDescription(facade, fields);
        addKeywords(facade, fields);
        addTopicClassifications(facade, fields);
        addPublications(facade, fields);
        addLanguages(facade, fields);
        addNotes(facade, fields);
        addProducers(facade, fields);
        addProductionDate(facade, fields);
        addProductionPlace(facade, fields);
        addContributor(facade, fields);
        addSubject(facade, fields);
        addContacts(facade, fields);
        addKindOfData(facade, fields);

        //optional fields
        addDepositor(facade, fields);
        addSubTitle(facade, fields);
        addAlternativeTitle(facade, fields);
        addAlternativeURL(facade, fields);
        return fields;
    }

    private void addKindOfData(DatasetFacade facade, List<CitationField> fields) {
        List<String> kindOfDataList = new ArrayList<>();
        kindOfDataList.add("Audiovisual");
        kindOfDataList.add("Collection");
        CitationField kindOfData = createControlledVocabField("kindOfData", true, kindOfDataList);
        fields.add(kindOfData);
    }
}

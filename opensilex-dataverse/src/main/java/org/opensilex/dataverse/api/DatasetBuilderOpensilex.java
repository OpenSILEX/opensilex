package org.opensilex.dataverse.api;

import com.researchspace.dataverse.entities.*;
import com.researchspace.dataverse.entities.facade.DatasetBuilder;
import com.researchspace.dataverse.entities.facade.DatasetFacade;

import java.util.ArrayList;
import java.util.List;

public class DatasetBuilderOpensilex extends DatasetBuilder {

    public Dataset build(DatasetFacade facade, String metadataLanguage) {
        DatasetVersion dv = new DatasetVersion();
        DataSetMetadataBlock blocks = new DataSetMetadataBlock();
        Citation citation = new Citation();

        List<CitationField> fields = createFields(facade, citation);

        dv.setMetadataBlocks(blocks);
        blocks.setCitation(citation);
        citation.setFields(fields);
        DatasetOpensilex toSubmit = new DatasetOpensilex();
        toSubmit.setDatasetVersion(dv);
        toSubmit.setMetadataLanguage(metadataLanguage);
        return toSubmit;
    }

    @Override
    protected List<CitationField> createFields(DatasetFacade facade, Citation citation) {
        List<CitationField> fields = new ArrayList<>();
        //mandatory fields
        this.addTitle(facade, fields);
        this.addAuthors(facade, fields);
        this.addDescription(facade, fields);
        this.addKeywords(facade, fields);
        this.addTopicClassifications(facade, fields);
        this.addPublications(facade, fields);
        this.addLanguages(facade, fields);
        this.addNotes(facade, fields);
        this.addProducers(facade, fields);
        this.addProductionDate(facade, fields);
        this.addProductionPlace(facade, fields);
        this.addContributor(facade, fields);
        this.addSubject(facade, fields);
        this.addContacts(facade, fields);
        this.addKindOfData(facade, fields);

        //optional fields
        this.addDepositor(facade, fields);
        this.addSubTitle(facade, fields);
        this.addAlternativeTitle(facade, fields);
        this.addAlternativeURL(facade, fields);
        return fields;
    }

    private void addKindOfData(DatasetFacade facade, List<CitationField> fields) {
        List<String> kindOfDataList = new ArrayList<>();
        kindOfDataList.add("Collection");
        CitationField kindOfData = createControlledVocabField("kindOfData", true, kindOfDataList);
        fields.add(kindOfData);
    }
}

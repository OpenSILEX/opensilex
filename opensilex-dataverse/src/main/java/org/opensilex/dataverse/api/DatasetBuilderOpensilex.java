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
        List<CitationField> fields = super.createFields(facade, citation);
        this.addKindOfData(facade, fields);
        return fields;
    }

    private void addKindOfData(DatasetFacade facade, List<CitationField> fields) {
        List<String> kindOfDataList = new ArrayList<>();
        kindOfDataList.add("Collection");
        CitationField kindOfData = createControlledVocabField("kindOfData", true, kindOfDataList);
        fields.add(kindOfData);
    }
}

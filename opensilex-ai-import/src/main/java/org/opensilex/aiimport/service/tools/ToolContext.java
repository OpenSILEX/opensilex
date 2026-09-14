//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service.tools;

import org.opensilex.aiimport.create.AiImportCreationService;
import org.opensilex.aiimport.mapping.ColumnMapping;
import org.opensilex.aiimport.resolve.ResolutionReport;
import org.opensilex.aiimport.resolve.SharedResourceVariableLookup;
import org.opensilex.aiimport.workbook.WorkbookStructure;
import org.opensilex.fs.service.FileStorageService;
import org.opensilex.nosql.mongodb.MongoDBService;
import org.opensilex.security.account.dal.AccountModel;
import org.opensilex.sparql.service.SPARQLService;

import java.util.Collections;
import java.util.List;

/**
 * What a tool is allowed to reach.
 * <p>
 * The account is the one that opened the session, so a tool sees exactly what its user would see
 * through the ordinary API and never more.
 *
 * @author Arnaud Charleroy
 */
public class ToolContext {

    private final SPARQLService sparql;
    private final MongoDBService nosql;
    private final FileStorageService fs;
    private final AccountModel currentUser;
    private final WorkbookStructure workbook;
    private final SharedResourceVariableLookup sharedResources;

    /**
     * The analysis of the uploaded file. The prompt only carries a summary of these, so the tools
     * that serve the detail read them from here.
     */
    private final ResolutionReport report;
    private final List<ColumnMapping> mappings;

    /**
     * The conversation itself, so a tool can record a draft on it.
     */
    private final org.opensilex.aiimport.service.AiImportSession session;

    private final AiImportCreationService creationService;

    public ToolContext(SPARQLService sparql,
                       MongoDBService nosql,
                       FileStorageService fs,
                       AccountModel currentUser,
                       WorkbookStructure workbook,
                       SharedResourceVariableLookup sharedResources,
                       ResolutionReport report,
                       List<ColumnMapping> mappings,
                       org.opensilex.aiimport.service.AiImportSession session,
                       AiImportCreationService creationService) {
        this.sparql = sparql;
        this.nosql = nosql;
        this.fs = fs;
        this.currentUser = currentUser;
        this.workbook = workbook;
        this.sharedResources = sharedResources;
        this.report = report;
        this.mappings = mappings == null ? Collections.emptyList() : mappings;
        this.session = session;
        this.creationService = creationService;
    }

    public SPARQLService getSparql() {
        return sparql;
    }

    public MongoDBService getNosql() {
        return nosql;
    }

    public FileStorageService getFs() {
        return fs;
    }

    public AccountModel getCurrentUser() {
        return currentUser;
    }

    public WorkbookStructure getWorkbook() {
        return workbook;
    }

    public SharedResourceVariableLookup getSharedResources() {
        return sharedResources;
    }

    public ResolutionReport getReport() {
        return report;
    }

    public List<ColumnMapping> getMappings() {
        return mappings;
    }

    public org.opensilex.aiimport.service.AiImportSession getSession() {
        return session;
    }

    public AiImportCreationService getCreationService() {
        return creationService;
    }
}

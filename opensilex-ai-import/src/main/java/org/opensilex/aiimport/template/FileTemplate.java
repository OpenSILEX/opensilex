//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.template;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.opensilex.aiimport.profile.custom.DescribedSheet;
import org.opensilex.aiimport.profile.custom.SheetDescription;
import org.opensilex.aiimport.service.AiImportSession;
import org.opensilex.aiimport.workbook.SheetStructure;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * What a user said of a file, kept to be proposed for the next file of the same shape.
 * <p>
 * Independent of any exchange format: CSVW and RML are written from it, and nothing here knows
 * them. It is built from a conversation, and applying it is the user's choice, sheet by sheet —
 * the module proposes, it never decides for them.
 *
 * @author Arnaud Charleroy
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
public class FileTemplate {

    private String id;
    private URI author;

    @JsonProperty("author_name")
    private String authorName;

    private String name;
    private boolean shared;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    private List<TemplateSheet> sheets = new ArrayList<>();

    public FileTemplate() {
    }

    /**
     * Brings together what the user said of every table of the conversation: each sheet as it is
     * read now — the kind and the roles the user chose, and those that were guessed and they let
     * stand — so that the template says the whole of what was imported.
     */
    public static FileTemplate of(AiImportSession session, String name, URI author, String authorName) {
        FileTemplate template = new FileTemplate();
        template.id = UUID.randomUUID().toString();
        template.author = author;
        template.authorName = authorName;
        template.name = name;
        template.createdAt = Instant.now();
        template.updatedAt = template.createdAt;
        template.sheets = sheetsOf(session);
        return template;
    }

    /**
     * A template made of sheets already described, as when one is read from a document: it is the
     * caller's to give an author before it is kept.
     */
    public static FileTemplate fromSheets(String name, List<TemplateSheet> sheets) {
        FileTemplate template = new FileTemplate();
        template.id = UUID.randomUUID().toString();
        template.name = name;
        template.createdAt = Instant.now();
        template.updatedAt = template.createdAt;
        template.sheets = new ArrayList<>(sheets);
        return template;
    }

    /**
     * Gives a template read from a document to the user who brought it.
     */
    public FileTemplate ownedBy(URI newAuthor, String newAuthorName) {
        author = newAuthor;
        authorName = newAuthorName;
        return this;
    }

    /**
     * Replaces the sheets by what the conversation says now: the template follows the user's last word.
     */
    public FileTemplate update(AiImportSession session) {
        sheets = sheetsOf(session);
        updatedAt = Instant.now();
        return this;
    }

    private static List<TemplateSheet> sheetsOf(AiImportSession session) {
        List<TemplateSheet> sheets = new ArrayList<>();
        if (session.getWorkbook() == null) {
            return sheets;
        }
        for (SheetStructure sheet : session.getWorkbook().getSheets()) {
            if (!sheet.isTabular()) {
                continue;
            }
            SheetDescription said = session.getDescription().find(sheet.getName()).orElse(null);
            DescribedSheet described = DescribedSheet.of(sheet, said);
            sheets.add(new TemplateSheet(sheet.getName(), sheet.getHeaders(), described.kind(), described.roles(),
                    said == null ? null : said.getType(),
                    said == null ? Map.of() : said.getProperties()));
        }
        return sheets;
    }

    public FileTemplate rename(String newName) {
        name = newName;
        updatedAt = Instant.now();
        return this;
    }

    public FileTemplate share(boolean newShared) {
        shared = newShared;
        updatedAt = Instant.now();
        return this;
    }

    public String getId() {
        return id;
    }

    public URI getAuthor() {
        return author;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getName() {
        return name;
    }

    public boolean isShared() {
        return shared;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<TemplateSheet> getSheets() {
        return sheets;
    }

    /**
     * @return whether this user may change or delete it: only its author does
     */
    public boolean isOwnedBy(URI user) {
        return author != null && author.equals(user);
    }

    /**
     * @return whether this user may read it: its author, and everyone once it is shared
     */
    public boolean isVisibleTo(URI user) {
        return shared || isOwnedBy(user);
    }
}

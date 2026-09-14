//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.create;

import org.opensilex.aiimport.service.AiImportSession;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Turns the values the assistant proposed into a validated draft.
 * <p>
 * Validation happens here rather than at write time so the assistant learns what is wrong while it
 * is still talking: a field name it invented is refused, a malformed date is refused, and a required
 * field left empty comes back named, all in the same turn.
 *
 * @author Arnaud Charleroy
 */
public class ProposalBuilder {

    private final AiImportCreationService creationService;

    public ProposalBuilder(AiImportCreationService creationService) {
        this.creationService = creationService;
    }

    /**
     * @param proposed the values the assistant supplied, keyed by field name
     * @return a draft, never null; look at its blockers and missing fields rather than expecting an
     * exception
     * @throws IllegalArgumentException when the assistant used a field that does not exist, or a
     * value that cannot be read. These are the assistant's mistakes, not the user's, so they are
     * reported to it rather than shown as a form error.
     */
    public CreationProposal build(AiImportSession session, CreationTarget target,
                                  Map<String, String> proposed, String rationale) {
        CreationRequirements requirements = creationService.requirementsFor(target, session);
        Map<String, RequiredField> known = byName(requirements);

        Map<String, String> supplied = proposed == null ? new LinkedHashMap<>() : proposed;
        for (String name : supplied.keySet()) {
            if (!known.containsKey(name)) {
                throw new IllegalArgumentException("There is no field named '" + name + "' for a "
                        + target.name().toLowerCase() + ". Known fields: "
                        + String.join(", ", known.keySet()) + ".");
            }
        }

        CreationProposal proposal = new CreationProposal(UUID.randomUUID().toString(), target)
                .setRationale(rationale);
        proposal.getBlockers().addAll(requirements.getBlockers());

        // Fields are walked in the order the requirements declare them, not the order the assistant
        // happened to send them, so the card always reads the same way.
        for (RequiredField field : requirements.getFields()) {
            String value = normalise(field, supplied.get(field.getName()));

            if (value == null && field.getSuggestedValue() != null) {
                // Nothing proposed for a field the file can answer: fall back to the file rather
                // than asking the user for something already written down.
                proposal.put(field.getName(), field.getSuggestedValue(), CreationProposal.FieldSource.FILE);
            } else if (value != null) {
                proposal.put(field.getName(), value, sourceOf(field, value));
            }

            if (!field.isSatisfiedBy(proposal.getFields().get(field.getName()))) {
                proposal.getMissingRequired().add(field.getName());
            }
        }
        return proposal;
    }

    /**
     * A value equal to what the file suggested came from the file, whoever typed it. Anything else
     * the assistant supplied is its own, and is labelled as such.
     */
    private CreationProposal.FieldSource sourceOf(RequiredField field, String value) {
        return value.equals(field.getSuggestedValue())
                ? CreationProposal.FieldSource.FILE
                : CreationProposal.FieldSource.ASSISTANT;
    }

    private String normalise(RequiredField field, String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("date".equals(field.getKind())) {
            try {
                return LocalDate.parse(trimmed).toString();
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("The field '" + field.getName()
                        + "' must be a date written as yyyy-MM-dd, and '" + trimmed + "' is not.");
            }
        }
        return trimmed;
    }

    private Map<String, RequiredField> byName(CreationRequirements requirements) {
        Map<String, RequiredField> known = new LinkedHashMap<>();
        requirements.getFields().forEach(field -> known.put(field.getName(), field));
        return known;
    }

}

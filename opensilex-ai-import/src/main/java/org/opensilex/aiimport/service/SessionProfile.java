//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.aiimport.service;

import org.opensilex.aiimport.profile.GenericTabularProfile;
import org.opensilex.aiimport.profile.ImportProfile;
import org.opensilex.aiimport.profile.ImportProfileRegistry;
import org.opensilex.aiimport.profile.custom.UserDefinedProfile;

import java.util.Optional;

/**
 * The profile of a conversation. A profile that knows a template is the same for everyone; the
 * profile {@code custom} knows only what its conversation's user told it, so it is built from that
 * conversation. Everything that needs "the profile of this session" asks here, instead of looking
 * an identifier up in the registry — which would hand back one that knows nothing.
 *
 * @author Arnaud Charleroy
 */
public final class SessionProfile {

    private SessionProfile() {
    }

    /**
     * @return whether the user describes this conversation's files themselves — which is the case
     * of a file no template reads. A file a template reads is known to the module, sheet by sheet.
     */
    public static boolean isDescribable(AiImportSession session) {
        return UserDefinedProfile.ID.equals(session.getProfileId())
                || GenericTabularProfile.ID.equals(session.getProfileId());
    }

    /**
     * @return the profile of the session, or empty when its identifier names none
     */
    public static Optional<ImportProfile> of(AiImportSession session) {
        return bind(session, new ImportProfileRegistry().getById(session.getProfileId()).orElse(null));
    }

    /**
     * @param profile a profile of the registry, or {@code null}
     * @return the profile as this session uses it
     */
    public static Optional<ImportProfile> bind(AiImportSession session, ImportProfile profile) {
        if (profile == null) {
            return Optional.empty();
        }
        return Optional.of(UserDefinedProfile.ID.equals(profile.getId())
                ? new UserDefinedProfile(session.getDescription())
                : profile);
    }
}

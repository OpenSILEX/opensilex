package org.opensilex.core.variable.api;

import org.opensilex.core.species.api.SpeciesDTO;
import org.opensilex.core.species.dal.SpeciesModel;
import org.opensilex.core.variable.dal.ModerationActionModel;
import org.opensilex.security.user.dal.UserModel;

import java.net.URI;
import java.time.LocalDate;

public class ModerationActionDTO {

    protected LocalDate date;
    protected UserModel moderator;
    protected String moderationActionType;

    public LocalDate getDate() {
        return date;
    }

    public ModerationActionDTO setDate(LocalDate date) {
        this.date = date;
        return this;
    }

    public UserModel getModerator() {
        return moderator;
    }

    public ModerationActionDTO setModerator(UserModel moderator) {
        this.moderator = moderator;
        return this;
    }

    public String getModerationActionType() {
        return moderationActionType;
    }

    public ModerationActionDTO setModerationActionType(String moderationActionType) {
        this.moderationActionType = moderationActionType;
        return this;
    }



    public static ModerationActionDTO fromModel(ModerationActionModel model) {
        return new ModerationActionDTO()
                .setDate(model.getDate())
                .setModerator(model.getModerator())
                .setModerationActionType(model.getModerationActionType());
    }
}

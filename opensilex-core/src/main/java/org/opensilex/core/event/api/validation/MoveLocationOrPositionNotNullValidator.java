package org.opensilex.core.event.api.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilex.core.event.api.move.MoveCreationDTO;
import org.opensilex.core.geospatial.dal.GeospatialDAO;
import org.opensilex.core.position.api.ConcernedItemPositionCreationDTO;
import org.opensilex.core.position.api.PositionCreationDTO;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.URI;
import java.util.List;

/**
 * @author Renaud COLIN
 */
public class MoveLocationOrPositionNotNullValidator implements ConstraintValidator<MoveLocationOrPositionNotNullConstraint, MoveCreationDTO> {

    private boolean updateContextViolationTemplateWithMessage(ConstraintValidatorContext constraintValidatorContext, String message) {
        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext.buildConstraintViolationWithTemplate(message).addConstraintViolation();
        return false;
    }

    @Override
    public boolean isValid(MoveCreationDTO moveCreationDTO, ConstraintValidatorContext context) {

        List<ConcernedItemPositionCreationDTO> positions = moveCreationDTO.getConcernedItemPositions();

        if (moveCreationDTO.getTo() == null && moveCreationDTO.getFrom() == null && CollectionUtils.isEmpty(positions)) {
            return updateContextViolationTemplateWithMessage(context, "no location or position : to, from and targets_positions are null or empty");
        }

        if(positions == null){
            return true;
        }

        for (int i = 0; i < positions.size(); i++) {

            ConcernedItemPositionCreationDTO itemPosition = positions.get(i);
            if (itemPosition == null) {
                return updateContextViolationTemplateWithMessage(context, "(target,position) at index " + i + " is null");
            } else {

                URI item = itemPosition.getConcernedItem();
                if (item == null) {
                    return updateContextViolationTemplateWithMessage(context, "null target at index" + i);
                }

                PositionCreationDTO position = itemPosition.getPosition();
                if (position == null) {
                    return updateContextViolationTemplateWithMessage(context, "null position at index" + i);
                }


                if(position.getPoint() == null){
                    if (StringUtils.isEmpty(position.getDescription()) && position.getX() == null && position.getY() == null && position.getZ() == null) {
                        return updateContextViolationTemplateWithMessage(context, "Position at index " + i + " must have a non-null point,x,y,z or text. All property are null");
                    }
                }else {
                    try {
                        // try to transform geo json to point model and to update the dto in order to not parse json two time
                        com.mongodb.client.model.geojson.Point pointModel = (com.mongodb.client.model.geojson.Point) GeospatialDAO.geoJsonToGeometry(position.getPoint());
                        position.setPointModel(pointModel);

                    } catch (JsonProcessingException e) {
                        return updateContextViolationTemplateWithMessage(context, "Error on point parsing at index " + i + " : " + e.getMessage());
                    }
                }

            }
        }

        return true;
    }
}

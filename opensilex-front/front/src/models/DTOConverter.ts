import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";

/**
 * Transform the given properties of a DTO to URI properties. It is useful for example
 * to convert a GetDTO to a CreationDTO or an UpdateDTO and use it in forms. It is common for a GetDTO
 * to have a NamedResourceDTO as a field and the corresponding CreationDTO to take a simple URI instead.
 *
 * For example, consider the detail page of an experiment, which fetches an ExperimentGetDTO from the API. The DTO has
 * a "facilities" field which is an array of FacilityGetDTO. The detail page also has a modification form, which takes
 * an ExperimentCreationDTO as an input. In order to convert the ExperimentGetDTO to a CreationDTO, the properties like
 * "facilities" must be converted to an array of URI instead of DTO. This method does exactly that.
 *
 * Please note that the DTO passed as argument is not modified. A copy is created instead.
 *
 * @param getDto The DTO of which the properties must be converted
 * @param properties The set of properties to convert. If not specified, all properties of the object will be converted
 *  (if possible)
 */
function extractURIFromResourceProperties<T extends NamedResourceDTO, U extends NamedResourceDTO>(getDto: T, properties?: Array<string>): U {
    let creationDto: U = JSON.parse(JSON.stringify(getDto));

    // If the concerned properties are not defined, take all of them
    if (!Array.isArray(properties)) {
        properties = Object.getOwnPropertyNames(creationDto);
    }

    // Iterate over the properties to transform only the one corresponding to either a ResourceDTO or an array of
    // ResourceDTOs
    for (let key of properties) {
        if (Array.isArray(creationDto[key])) {
            // Case 1 : array of ResourceDTO (example : ExperimentGetDTO.facilities)
            creationDto[key] = creationDto[key].map((dto: NamedResourceDTO | string) => {
                return typeof dto === "object" && dto.uri ? dto.uri : dto;
            });
        } else if (creationDto[key] && creationDto[key].uri) {
            // Case 2 : ResourceDTO (example : VariableDetailsDTO.characteristic)
            creationDto[key] = creationDto[key].uri;
        }
    }

    return creationDto;
}

const DTOConverter = {
    extractURIFromResourceProperties
};

export default DTOConverter;
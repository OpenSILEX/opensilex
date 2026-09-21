import {DataFileGetDTO} from "../../../../../opensilex-core/front/src/lib";

/**
 * Specialization of {@link DataFileGetDTO}  which contains a string representation of the file content
 * associated to {@link DataFileGetDTO#uri}
 *
 * @author rcolin
 */
export interface DataFileImageDTO extends DataFileGetDTO {
    url: string
}
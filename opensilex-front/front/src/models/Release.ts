import moment from 'moment'

// Ne fonctionne pas une fois compilé
// const version = require('../../package.json').version;

/**
 * Information about the current release of the application
 */
export class Release {

    /**
     * Version number of the release
     */
    version: string;

    /**
     * Date of the release
     */
    date: string;

    constructor() {
        this.version = "4.0.0";
        this.date = moment().format("DD/MM/YYYY"); // TODO: AT add release date
    }

}
import moment from 'moment'

const version = require('../../package.json').version;

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
        this.version = version;
        this.date = moment().format("DD/MM/YYYY"); // TODO: AT add release date
    }

}
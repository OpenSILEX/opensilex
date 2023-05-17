import IHttpClient from "../lib/IHttpClient";
import "whatwg-fetch";
import HttpResponse, {MetadataDTO, StatusDTO} from "../lib/HttpResponse";
import {injectable} from "inversify";
import {Headers} from "../lib/Headers";
import OpenSilexVuePlugin from './OpenSilexVuePlugin';
import {User} from './User';

declare var window: any;

/**
 * Map the response metadata status level to a toast variant for display. The DEBUG level is not included, because it
 * should not be displayed
 */
const statusLevelToastVariantMap: Map<StatusDTO.LevelEnum, string> = new Map<StatusDTO.LevelEnum, string>([
    [StatusDTO.LevelEnum.ERROR, "danger"],
    [StatusDTO.LevelEnum.WARNING, "warning"],
    [StatusDTO.LevelEnum.INFO, "info"]
]);

@injectable()
class OpenSilexHttpClient implements IHttpClient {

    private opensilex: OpenSilexVuePlugin;

    constructor(opensilex: OpenSilexVuePlugin) {
        this.opensilex = opensilex;
    }

    get(url: string, headers?: Headers): Promise<HttpResponse> {
        return this.performNetworkCall(url, "get", undefined, headers);
    }

    post(url: string, body: any, headers?: Headers): Promise<HttpResponse> {
        return this.performNetworkCall(url, "post", this.getJsonBody(body), this.addJsonHeaders(headers));
    }

    put(url: string, body: any, headers?: Headers): Promise<HttpResponse> {
        return this.performNetworkCall(url, "put", this.getJsonBody(body), this.addJsonHeaders(headers));
    }

    delete(url: string, headers?: Headers): Promise<HttpResponse> {
        return this.performNetworkCall(url, "delete", undefined, headers);
    }

    private getJsonBody(body: any) {
        return JSON.stringify(body);
    }

    private addJsonHeaders(headers?: Headers) {
        return Object.assign({}, {
            "Accept": "application/json",
            "Content-Type": "application/json"
        }, headers);
    }

    private performNetworkCall(url: string, method: string, body?: any, headers?: Headers): Promise<HttpResponse> {
        let fetch: any;
        if (typeof (window) == 'undefined') {
            fetch = require('node-fetch');
        } else {
            fetch = window.fetch;
        }

        let $opensilex = this.opensilex;

        let user: User = this.opensilex.getUser();
        if (user != User.ANONYMOUS()) {
            headers["Authorization"] = user.getAuthorizationHeader();
        }

        if (user.getLocale() != null) {
            headers["Accept-Language"] = user.getLocale();
        }

        $opensilex.showLoader();

        return new Promise((resolve, reject) => {
            let promise = fetch(url, {
                method: method,
                body: body,
                mode: 'cors',
                headers: <any>headers
            }).then(response => {
                let headers: Headers = {};
                response.headers.forEach((value, name) => {
                    headers[name.toString().toLowerCase()] = value;
                });
                return response.text().then(text => {
                    let contentType = headers["content-type"] || "";
                    let content: any;
                    let metadata: MetadataDTO;
                    if (contentType.match("application/json")) {
                        try {
                            content = JSON.parse(text);
                            if (content.metadata) {
                                metadata = content.metadata as MetadataDTO;
                            }
                        } catch (error) {
                            console.error("Error while parsing JSON request result: " + url, error);
                            throw error;
                        }
                    } else {
                        content = text;
                    }

                    let httpResponse = new HttpResponse(content, response.status, headers);

                    // If present, handle the metadata status
                    if (metadata) {
                        for (let status of metadata.status) {
                            if (status.translationKey && statusLevelToastVariantMap.has(status.level)) {
                                // Status has a translation key, it probably should be shown to the user
                                this.opensilex.showToast(
                                    this.opensilex.$i18n.t(status.translationKey, status.translationValues).toString(),
                                    {
                                        variant: statusLevelToastVariantMap.get(status.level)
                                    }
                                );
                            }
                        }
                    }

                    if (response.status >= 400)
                        throw httpResponse;
                    return httpResponse;
                });
            });

            promise
                .then((result) => {
                    $opensilex.hideLoader();
                    resolve(result);
                })
                .catch((error) => {
                    $opensilex.hideLoader();
                    reject(error);
                });
        })
    }
}

export default OpenSilexHttpClient

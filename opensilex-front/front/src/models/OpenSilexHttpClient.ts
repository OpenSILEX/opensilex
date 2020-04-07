import IHttpClient from "../lib/IHttpClient";
import "whatwg-fetch";
import HttpResponse from "../lib/HttpResponse";
import { injectable, inject } from "inversify";
import { Headers } from "../lib/Headers";
import OpenSilexVuePlugin from './OpenSilexVuePlugin';

declare var window: any;

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
                    let metadata: any = {};
                    if (contentType.match("application/json")) {
                        content = JSON.parse(text);
                        if (content.metadata) {
                            metadata = content.metadata;
                        }
                    } else {
                        content = text;
                    }

                    let httpResponse = new HttpResponse(content, response.status, headers);

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

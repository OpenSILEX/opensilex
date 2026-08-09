import HttpResponse from "./HttpResponse";
import { Headers } from "./Headers";

export interface IHttpClient {
    get(url: string, headers?: Headers): Promise<HttpResponse>;
    post(url: string, body: any, headers?: Headers): Promise<HttpResponse>;
    put(url: string, body: any, headers?: Headers): Promise<HttpResponse>;
    delete(url: string, headers?: Headers): Promise<HttpResponse>;
}

export default IHttpClient;

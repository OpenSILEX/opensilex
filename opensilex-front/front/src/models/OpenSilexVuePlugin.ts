import {Container} from 'inversify';
import {VueJsOntologyExtensionService} from './../lib/api/vueJsOntologyExtension.service';
import {SystemService} from '../../../../opensilex-core/front/src/lib/api/system.service';
import Vue from 'vue';
import {VueCookies} from 'vue-cookies';
import VueI18n from 'vue-i18n';
import {Store} from 'vuex';
import {
    ApiServiceBinder,
    FrontConfigDTO,
    IAPIConfiguration,
    ThemeConfigDTO,
    VueDataTypeDTO,
    VueObjectTypeDTO
} from '../lib';
import IHttpClient from '../lib/IHttpClient';
import Oeso from '../ontologies/Oeso';
import Foaf from '../ontologies/Foaf';
import Org from '../ontologies/Org';
import Oeev from '../ontologies/Oeev';
import Time from '../ontologies/Time';
import Rdfs from '../ontologies/Rdfs';

import {ModuleComponentDefinition} from './ModuleComponentDefinition';
import OpenSilexHttpClient from './OpenSilexHttpClient';
import {UploadFileBody} from './UploadFileBody';
import {User} from './User';
import {ResourceDagDTO} from "opensilex-core/model/resourceDagDTO";
import {ServiceBinder} from "../services/ServiceBinder";
import {OntologyService, VariableDatatypeDTO, VariablesService} from 'opensilex-core/index';
import DateTimeFormatter from "./DateTimeFormatter";
import NumberFormatter from "./NumberFormatter";
import {BvToastOptions} from "bootstrap-vue/src/components/toast";
import HttpResponse, {OpenSilexResponse} from "../lib/HttpResponse";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";

declare var $cookies: VueCookies;

declare var window: Window | any;

export interface TreeOption<T extends TreeOption<T>> {
    id: string,
    label: string,
    isDefaultExpanded: boolean,
    isDisabled: boolean,
    children: Array<T>,
    title?: string,
    data?: ResourceDagDTO,
    isLeaf?: boolean,
    isSelected?: boolean,
    isExpanded?: boolean,
    isDraggable?: boolean,
    isSelectable?: boolean
}

export type GenericTreeOption = TreeOption<GenericTreeOption>;

export default class OpenSilexVuePlugin {

    private DEFAULT_ICON = "folder";
    private static DEFAULT_LANG: string = "en";
    private container: Container;
    private baseApi: string;
    private config: FrontConfigDTO;
    private themeConfig: ThemeConfigDTO;

    public $store: Store<any>;
    public $i18n: VueI18n;
    public $bvToast: any;
    public $dateTimeFormatter: DateTimeFormatter;
    public $numberFormatter: NumberFormatter;

    public Oeso = Oeso;
    public Foaf = Foaf;
    public Org = Org;
    public Oeev = Oeev;
    public Time = Time;
    public Rdfs = Rdfs;

    constructor(baseApi: string, store: Store<any>, i18n: VueI18n) {
        this.container = new Container();
        this.container.bind<OpenSilexVuePlugin>(OpenSilexVuePlugin).toConstantValue(this);
        this.container.bind<IHttpClient>("IApiHttpClient").toConstantValue(new OpenSilexHttpClient(this));
        this.container.bind<IAPIConfiguration>("IAPIConfiguration").toConstantValue({
            basePath: baseApi
        });
        this.baseApi = baseApi;
        this.$store = store;
        this.$i18n = i18n;
        this.$dateTimeFormatter = new DateTimeFormatter(i18n);
        this.$numberFormatter = new NumberFormatter(i18n);
        ApiServiceBinder.with(this.container);
        ServiceBinder.with(this.container);
    }

    getUser() {
        return this.$store.state.user;
    }

    getLang() {
        return this.$store.state.lang;
    }

    getBaseAPI() {
        return this.baseApi;
    }

    // get ressources linked to loaded theme
    getResourceURI(path: string, acceptedExt: Array<string> = []): string {
        if (this.config.themeModule && this.config.themeName) {
            let resourceURI = this.baseApi + "/vuejs/theme/" + encodeURIComponent(this.config.themeModule) + "/" + encodeURIComponent(this.config.themeName) + "/resource";
            let args = "?filePath=" + encodeURIComponent(path);
            for (let ext of acceptedExt) {
                args += "&acceptedExtensions=" + encodeURIComponent(ext)
            }
            return resourceURI + args;
        } else {
            return this.getURL(path);
        }
    }

    /**
     * Get the route's path that corresponds to a matching type within a given list.
     *
     * @param types the list of types to check into
     * @return if no type matches: return an empty string. Otherwise, return the path for the matching type.
     */
    getPathFromUriTypes(types) {
        let path = "";
        types.forEach(type => {
            let route = this.config.routes.find(route => this.checkURIs(route.rdfType, type))
            if (route) {
                path = route.path;
            }
        });
        return path;
    }

    /**
     * Gets the in-app navigation path of the uri in question from a pre-calculated uri-path map
     *
     * @param uri , uri of the target (any type) that we want to navigate to
     * @param context , uri of the containg experiment, null if no experiment
     * @param objectsPath , the pre-calculated uri to path map to search in
     */
    getTargetPath(uri: string, context: string, objectPath : string) : string {
        if(! objectPath){
            return "";
        }

        let osPath = objectPath.replace(':uri', encodeURIComponent(uri))

        // pass encoded experiment inside OS path URL,
        if(context && context.length > 0){
            //Replace :xpUri as well as :experiment, for some reason :xpUri is the used keyword for Factor and FactorLev,
            // I tried changing this in opensilex.front.yml but the Factor details page became blank
            osPath = osPath.replace(':xpUri', encodeURIComponent(context));
            return osPath.replace(':experiment', encodeURIComponent(context));
        }else{ // no experiment passed
            return osPath.replace(':experiment', "");
        }
    }

    /**
     *
     * @param type
     * @param uri
     * @return path if the type is an Entity, Entity of Interest, Characteristic, Method, Unit or Group of Variables, null otherwise
     *
     */
    getVariableComponentPath(type: string, uri:string): string{

        const paths = {
            [Oeso.ENTITY_TYPE_URI]: "Entity",
            [Oeso.ENTITY_OF_INTEREST_TYPE_URI]: "InterestEntity",
            [Oeso.CHARACTERISTIC_TYPE_URI]: "Characteristic",
            [Oeso.METHOD_TYPE_URI]: "Method",
            [Oeso.UNIT_TYPE_URI]: "Unit",
            [Oeso.VARIABLESGROUP_TYPE_URI]: "VariableGroup"
        };

        const elementType = Object.entries(paths).find(([key]) => this.checkURIs(type, key))?.[1];
        return elementType ? `/variables?elementType=${elementType}&selected=${encodeURIComponent(uri)}` : null;
    }

    /**
     * This is a function to get path for vocabulary pages from a type or domain and if the uri is a property or class
     *
     * @param uri the vocabulary we want to try and navigate to
     * @param rootClassUri to know which page to go to
     * @param isProperty the uri does not correspond to a class model but a property
     *
     */
    getVocabularyPath(uri: string, rootClassUri: string, isProperty: boolean): string{

        const paths = {
            [Oeso.SCIENTIFIC_OBJECT_TYPE_URI]: "scientific-object-types",
            [Oeev.EVENT_TYPE_URI]: "event-types",
            [Oeso.DEVICE_TYPE_URI]: "device-types",
            [Oeso.FACILITY_TYPE_URI]: "facilities-types",
            [Oeso.FACTOR_CATEGORY_URI]: "factor-category-types"
        };

        const elementType: string = Object.entries(paths).find(([key]) => this.checkURIs(rootClassUri, key))?.[1];
        const propertyPath: string = isProperty ? '/properties' : '';
        return elementType ? `/${elementType}${propertyPath}?selected=${encodeURIComponent(uri)}` : null;
    }

    /**
     * Creates strings of format 'name (type)', places them in a uri to result map with vue.set
     *
     * @param objectsToLoad , the uris of ontology objects that we want to create labels for
     * @param context , experimental context
     */
    loadOntologyLabelsWithType(objectsToLoad: Array<string>, context: string, uriResultMap : {[x: string]:string}, ontologyService: OntologyService):  Promise<void | HttpResponse<OpenSilexResponse<NamedResourceDTO[]>>>{
        return ontologyService
            .getURILabelsList(objectsToLoad, context, true)
            .then((httpObj) => {
                for (let obj of httpObj.response.result) {
                    Vue.set(uriResultMap, obj.uri, obj.name + " (" + obj.rdf_type_name + ")");
                }
            });
    }

    // get front ressources depending to a specific module theme
    getModuleFrontResourceURI(moduleName: string, themeName: string, path: string): string {
        if (moduleName && themeName) {
            // if module and theme are named, get ressource from them
            let resourceURI = this.baseApi + "/vuejs/theme/" + encodeURIComponent(moduleName) + "/" + encodeURIComponent(themeName) + "/resource";
            return resourceURI + "?filePath=" + encodeURIComponent(path);
        } else {
            // search the resource into the main module theme
            let resourceURI = this.baseApi + "/vuejs/theme/opensilex-front/opensilex/resource";
            return resourceURI + "?filePath=" + encodeURIComponent(path);
        }
    }

    getURL(path: string): string {
        return this.config.pathPrefix + "/app/" + path;
    }

    setConfig(config: FrontConfigDTO) {
        this.config = config;
    }

    getConfig() {
        return this.config;
    }

    setThemeConfig(themeConfig: ThemeConfigDTO) {
        this.themeConfig = themeConfig;
    }

    getRDFIcon(type) {
        let icon = this.themeConfig.iconClassesRDF[type];

        if (!icon) {
            icon = this.DEFAULT_ICON;
        }

        return icon;
    }

    loaderEnabled = true;

    enableLoader() {
        this.loaderEnabled = true;
    }

    disableLoader() {
        this.loaderEnabled = false;
    }

    showLoader() {
        if (this.loaderEnabled) {
            this.$store.commit("showLoader");
        }
    }

    hideLoader() {
        this.$store.commit("hideLoader");
    }

    public install(Vue, options) {
        Vue.prototype.$opensilex = this;
        Vue.$opensilex = this;
    }

    public loadService<T>(id: string): Promise<T> {
        return new Promise((resolve, reject) => {
            try {
                let result: T | null = this.getServiceSync(id);
                if (result == null) {
                    let idParts = this.parseServiceId(id);
                    this.loadModule(idParts.module)
                        .then(() => {
                            resolve(this.getService(id));
                        })
                        .catch(reject)
                } else {
                    resolve(result);
                }
            } catch (error) {
                reject(error);
            }
        });

    }

    public getService<T>(id: string): T {
        let result: T | null = this.getServiceSync(id);
        if (result == null) {
            let errorMessage = "Module is not loaded for service " + id;
            console.error(errorMessage);
            throw new Error(errorMessage);
        }

        return result;
    }

    public parseServiceId(id) {
        let idParts = id.split(".");
        if (idParts.length == 1) {
            return {
                module: null,
                service: idParts[0]
            };
        } else if (idParts.length == 2) {
            return {
                module: idParts[0],
                service: idParts[1]
            };
        } else {
            let errorMessage = "Invalid service identifier: " + id;
            console.error(errorMessage);
            throw new Error(errorMessage);
        }
    }

    public getServiceSync<T>(id: string): T | null {
        console.debug("Get API service", this.baseApi, id);
        let idParts = this.parseServiceId(id);
        if (idParts.module == null) {
            return this.getServiceContainer().get<T>(idParts.service);
        } else {
            if (this.loadedModules.indexOf(idParts.module) >= 0) {
                return this.getServiceContainer().get<T>(idParts.service);
            } else {
                return null;
            }
        }
    }

    private loadedModules: Array<string> = [
        "opensilex", "opensilex-front"

    ];

    private loadingModules = {
        "opensilex": Promise.resolve(null),
        "opensilex-front": Promise.resolve(null)
    };

    public loadModules(modules: Array<string>) {
        let promises: Array<Promise<any>> = [];
        for (let i in modules) {
            let moduleName = modules[i];

            if (!this.loadingModules[moduleName]) {
                this.loadingModules[moduleName] = this.loadModule(moduleName);
            }

            promises.push(this.loadingModules[moduleName])
        }

        return Promise.all(promises);
    }

    public loadComponentModules(components: Array<ModuleComponentDefinition>) {
        let promises: Array<Promise<any>> = [];

        for (let i in components) {
            promises.push(this.loadComponentModule(components[i]))
        }

        return Promise.all(promises);
    }

    public loadComponentModule(componentDef: ModuleComponentDefinition) {
        console.debug("Load component", componentDef.getId());
        let moduleName = componentDef.getModule();

        if (!this.loadingModules[moduleName]) {
            this.loadingModules[moduleName] = this.loadModule(moduleName);
        }

        if (this.loadingModules[moduleName] instanceof Promise) {
            return this.loadingModules[moduleName];
        }

        return Promise.resolve(this.loadingModules[moduleName]);
    }

    private loadTranslations(lang) {
        for (let langId in lang) {
            let translations: any = lang[langId];
            this.$i18n.mergeLocaleMessage(langId, translations);
        }
    }

    public loadComponentTranslations(component) {
        if (component.options.__i18n) {
            let componentTranslations = JSON.parse(component.options.__i18n);
            this.loadTranslations(componentTranslations);
        }
    }

    public loadModule(name) {
        if (window[name]) return window[name];

        console.debug("Load module", name);
        this.showLoader();
        let url = this.baseApi + "/vuejs/extension/js/" + name + ".js";
        let cssURI = this.baseApi + "/vuejs/extension/css/" + name + ".css";
        let self = this;

        var link = document.createElement('link');
        link.setAttribute("rel", "stylesheet");
        link.setAttribute("type", "text/css");
        link.setAttribute("href", cssURI);
        document.getElementsByTagName("head")[0].appendChild(link);

        window[name] = new Promise((resolve, reject) => {
            const script = document.createElement('script');
            script.async = true;
            script.addEventListener('load', () => {
                self.loadedModules.push(name);
                const plugin = window[name].default;
                Vue.use(plugin);

                if (plugin.lang) {
                    self.loadTranslations(plugin.lang);
                }

                if (plugin.components) {
                    for (let componentId in plugin.components) {
                        this.loadComponentTranslations(plugin.components[componentId]);
                    }
                }

                self.initAsyncComponents(plugin.components)
                    .then(function () {
                        self.hideLoader();
                        resolve(plugin);
                    })
                    .catch(function (error) {
                        self.hideLoader();
                        reject(error);
                    });
            });
            script.addEventListener('error', () => {
                self.hideLoader();
                reject(new Error(`Error loading ${url}`));
            });
            script.src = url;
            document.head.appendChild(script);
        });

        return window[name];
    }

    public initAsyncComponents(components) {
        let promises: Array<Promise<any>> = [];
        if (components) {
            for (let componentId in components) {
                let component = components[componentId];
                if (component.asyncInit) {
                    try {
                        console.debug("Start component async init...", componentId);
                        promises.push(component.asyncInit(this));
                    } catch (error) {
                        promises.push(Promise.reject(error));
                    }
                }
                console.debug("Register component", componentId, component);
                Vue.component(componentId, components[componentId]);
            }
        }

        return new Promise((resolve, reject) => {
            Promise.all(promises)
                .then(() => {
                    console.debug("All components in module are initialized !");
                    resolve(true);
                })
                .catch(reject);
        });

    }

    public getServiceContainer() {
        return this.container;
    }

    public get user(): User {
        return this.$store.state.user;
    }


    private static COOKIE_NAME = "opensilex-token";

    private getCookieName() {
        let cookieName = OpenSilexVuePlugin.COOKIE_NAME;
        // If a prefix exists, add it to the cookie name
        let pathPrefix = this.getPathPrefix();
        if (pathPrefix) {
            cookieName += pathPrefix.replace('/', '-');
        }
        console.debug("Read cookie name:", cookieName);
        return cookieName;
    }

    public clearCookie() {
        console.debug("Clear cookie " + this.getCookieName() + " with path " + this.getPathPrefix());
        $cookies.remove(this.getCookieName(), this.getPathPrefix(), (window as Window).location.hostname);
    }

    public loadUserFromCookie(): User {
        let token = $cookies.get(this.getCookieName());
        console.debug("Loaded token from cookie", token, this.getCookieName());
        let user: User = User.ANONYMOUS();
        if (token != null) {
            try {
                user = User.fromToken(token);
            } catch (error) {
                console.error(error);
            }
        }

        return user;
    }

    public setCookieValue(user: User) {
        this.clearCookie();
        let secure: boolean = ('https:' == document.location.protocol);
        console.debug("Set cookie value:", this.getCookieName(), this.getPathPrefix(), user.getToken());
        let domain = location.hostname;
        $cookies.set(this.getCookieName(), user.getToken(), user.getDurationUntilExpirationSeconds() + "s",
            this.getPathPrefix(), domain, secure);
    }

    private getPathPrefix(): string {
        if (this.getConfig() && this.getConfig().pathPrefix) {
            return this.getConfig().pathPrefix;
        }
        return undefined;
    }

    public static hashCode(str: string) {
        let hash = 0;
        if (str.length === 0) return hash;
        for (let i = 0; i < str.length; i++) {
            let chr = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + chr;
            hash |= 0; // Convert to 32bit integer
        }
        return hash;
    }

    private handleError(error, message?) {
        if (!message && !!error.response && !!error.response.result && !!error.response.result.translationKey) {
            message = this.$i18n.t(error.response.result.translationKey, error.response.result.translationValues);
        } else if (error.response.result.message) {
            message = error.response.result.message;
        }

        this.enableLoader();
        switch (error.status) {
            case 400:
                console.error("Constraint validation error", message);
                this.handleConstraintError(error, message);
                break;
            case 401:
                console.error("Unhautorized error", error);
                this.handleUnauthorizedError(error, message);
                this.$store.commit("logout");
                break;
            case 403:
                console.error("Forbidden error", error);
                this.handleForbiddenError(error, message);
                break;
            case 500:
                console.error("Internal server error", error);
                this.handleServerError(error, message);
                break;
            default:
                console.error("Unhandled error", error);
                this.handleUnexpectedError(error, message);
                break;
        }
        return false;
    }

    public errorHandler = this.handleError.bind(this);

    public handleConstraintError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.constraint-error");
        }
        this.showErrorToast(message);
    }

    public handleForbiddenError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.forbidden-error");
        }
        console.warn(message);
        this.showErrorToast(message);
    }

    public handleServerError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.server-error");
        }
        this.showErrorToast(message);
    }

    public handleUnexpectedError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.unexpected-error");
        }
        this.showErrorToast(message);
    }

    public handleUnauthorizedError(error, message?) {
        if (message == null) {
            message = this.$i18n.t("component.common.errors.unauthorized-error");
        }
        this.showErrorToast(message);
    }

    public showErrorToast(message: string) {
        this.showToast(message, {
            variant: "danger",
            title: this.$i18n.t("component.common.errors.error-title").toString()
        });
    }

    public showSuccessToast(message: string) {
        this.showToast(message, {
            variant: "success",
            autoHideDelay: 2500,
        });
    }

    public showSuccessToastWithDelay(message: string, delay: number) {
        if (delay == null) {
            delay = 2500
        }
        this.showToast(message, {
            variant: "success",
            autoHideDelay: delay,
        });
    }

    public showInfoToast(message: string) {
        this.showToast(message, {
            variant: "info",
            autoHideDelay: 8000,
        });
    }

    public showInfoToastWithoutDelay(message: string) {
        this.showToast(message, {
            variant: "info",
            appendToast: false,
            solid: false,
            autoHideDelay: 2000,
        });
    }

    public showWarningToast(message: string) {
        this.showToast(message, {
            variant: "warning",
            autoHideDelay: 2000,
        });
    }

    public showToast(message: string, options?: BvToastOptions) {
        const defaultOptions = {
            toaster: "b-toaster-top-center",
            appendToast: true,
            solid: true,
            noCloseButton: true
        };
        options = {
            ...defaultOptions,
            ...options
        };

        let toastID = this.computeToastID(message, options);
        options.id = toastID;
        let toastElement = document.getElementById(toastID);

        if (!toastElement) {
            this.$bvToast.toast(message, options);
        }
    }

    private computeToastID(message: string, options: any): string {
        return "OPENSILEX-TOAST" + OpenSilexVuePlugin.hashCode(message + "|" + options.title + "|" + options.variant);
    }

    /**
     *
     * @param resourceTrees , the roots from where to build our tree of options
     * @param buildOptions , extra options : nodesToIgnoreList takes a list of long uris to not be added to the tree of options.
     */
    public buildTreeListOptions(resourceTrees: Array<any>, buildOptions?): Array<GenericTreeOption> {
        let options = [];

        buildOptions = buildOptions || {
            expanded: null,
            disableSubTree: null,
            nodesToIgnoreList: null
        };

        if (buildOptions.expanded == null) {
            buildOptions.expanded = true;
        }

        if (resourceTrees != null) {
            resourceTrees.forEach((resourceTree: any) => {
                let subOption = this.buildTreeOptions(resourceTree, buildOptions);
                if (!subOption.isDisabled || subOption.children) {
                    options.push(subOption);
                }
            });
        }

        return options;
    }

    public buildTreeOptions(resourceTree: any, buildOptions: any, disabled?: boolean): GenericTreeOption {

        let option = {
            id: resourceTree.uri,
            label: resourceTree.name,
            isDefaultExpanded: buildOptions.expanded,
            isDisabled: false,
            children: []
        };

        let disableChildren = false;

        if (buildOptions.disableSubTree != null) {
            disableChildren = (option.id == buildOptions.disableSubTree) || disabled;
            option.isDisabled = disableChildren;
        }

        resourceTree.children.forEach(child => {
            if(!buildOptions.nodesToIgnoreList || !buildOptions.nodesToIgnoreList.includes(this.getLongUri(child.uri))){
                let subOption = this.buildTreeOptions(child, buildOptions, disableChildren);
                if (!subOption.isDisabled || subOption.children) {
                    option.children.push(subOption);
                }
            }
        });

        if (resourceTree.disabled) {
            option.isDisabled = true;
        }

        if (option.children.length == 0) {
            delete option.children;
        }

        return option;
    }

    public mapTree<T extends TreeOption<T>, U extends TreeOption<U>>(tree: Array<T>, mappingFunction: (node: T, mappedChildren?: Array<U>) => U): Array<U> {
        return tree.map(rootNode => this.browseTreeRecursive(rootNode, mappingFunction));
    }

    private browseTreeRecursive<T extends TreeOption<T>, U extends TreeOption<U>>(node: T, mappingFunction: (node: T, mappedChildren?: Array<U>) => U): U {
        let mappedChildren: Array<U> | undefined = undefined;

        if (Array.isArray(node.children)) {
            mappedChildren = node.children.map(child => this.browseTreeRecursive(child, mappingFunction));
        }

        return mappingFunction(node, mappedChildren);
    }

    public buildTreeFromDag(dagList: Array<ResourceDagDTO>, buildOptions?: any): Array<GenericTreeOption> {
        if (!dagList || dagList.length === 0) {
            return [];
        }

        let dagMap = new Map<string, any>();
        for (let dag of dagList) {
            dagMap.set(dag.uri, dag);
        }

        //Filter out the parents/children that does not exist (e.g. if use doesn't have the rights to see them)
        dagList.forEach(dagNode => {
            dagNode.parents = dagNode.parents.filter(parentUri => dagMap.has(parentUri));
            dagNode.children = dagNode.children.filter(childUri => dagMap.has(childUri));
        });

        let rootNodes: Array<GenericTreeOption> = [];

        dagList.forEach(dagNode => {
            if (!dagNode.parents || dagNode.parents.length === 0) {
                rootNodes.push(this.buildTreeFromDagNode(dagNode.uri, dagMap, buildOptions || {}))
            }
        });

        return rootNodes;
    }

    private buildTreeFromDagNode(nodeUri: string, dagMap: Map<string, ResourceDagDTO>, buildOptions: any, disabled: boolean = false): GenericTreeOption {
        let dagNode = dagMap.get(nodeUri);

        let treeNode: GenericTreeOption = {
            id: dagNode.uri,
            label: dagNode.name,
            isDefaultExpanded: buildOptions.expanded != undefined ? buildOptions.expanded : true,
            isDisabled: disabled,
            children: [],
            title: dagNode.name,
            data: dagNode,
            isDraggable: buildOptions.draggable != undefined ? buildOptions.draggable : false,
            isExpanded: buildOptions.expanded != undefined ? buildOptions.expanded : true,
            isLeaf: true,
            isSelectable: buildOptions.selectable != undefined ? buildOptions.selectable : true,
            isSelected: buildOptions.selected != undefined ? buildOptions.selected : false
        };

        let disableChildren = disabled;
        if (buildOptions.disableSubTree === nodeUri) {
            disableChildren = true;
            treeNode.isDisabled = true;
            treeNode.isExpanded = false;
            treeNode.isDefaultExpanded = false;
        }

        dagNode.children.forEach(childUri => {
            let subTreeNode = this.buildTreeFromDagNode(childUri, dagMap, buildOptions, disableChildren);
            treeNode.children.push(subTreeNode)
            treeNode.isLeaf = false;
        });

        if (treeNode.children.length === 0) {
            delete treeNode.children;
        }

        return treeNode;
    }

    private flatOntologies = {};

    setOntologyClasses(result: any[]) {
        result.forEach(item => {
            this.flatOntologies[item.uri] = item;
            this.setOntologyClasses(item.children);
        });
    }

    getOntologyLabel(uri) {
        if (this.flatOntologies[uri]) {
            return this.flatOntologies[uri].name;
        } else {
            return uri;
        }
    }

    public generateID() {
        return '_' + Math.random().toString(36).substr(2, 9);
    }
    public updateURLParameter(key, value, defaultValue?) {
        try {
            let queryParams = new URLSearchParams(window.location.search);
            let rootQuery = window.location.pathname;
            if (!value || (defaultValue != null && value == defaultValue) || (Array.isArray(value) && value.length == 0)) {
                queryParams.delete(key);
            } else {
                if (Array.isArray(value)) {
                    queryParams.set(key, encodeURI(value.toString()));
                } else if (typeof (value) === 'object') {
                    queryParams.set(key, encodeURI(JSON.stringify(value)));
                } else {
                    queryParams.set(key, encodeURI(value));
                }
            }

            let queryParamString = queryParams.toString();
            let url = rootQuery;
            if (queryParamString) {
                url += "?" + queryParamString;
            }
            window.history.replaceState(queryParams.toString(), document.title, url);
        } catch (error) {
            console.error(error);
        }
    }

    public updateURLParameters(filter) {
        for (let [key, value] of Object.entries(filter)) {
            this.updateURLParameter(key, value, "");
        }
    }

    public updateFiltersFromURL(query, filter) {
        for (let [key, value] of Object.entries(filter)) {
            if (query[key]) {
                if (Array.isArray(filter[key])) {
                    filter[key] = decodeURIComponent(query[key]).split(",");
                } else if (typeof (filter[key]) === 'object') {
                    filter[key] = JSON.parse(query[key]);
                } else {
                    filter[key] = decodeURIComponent(query[key]);
                }
            }
        }
    }


    private credentials = null;

    public getCredentials() {
        if (this.credentials == null) {
            this.credentials = new Promise((resolve, reject) => {
                console.debug("Loading credentials list...");
                this.getService<any>(
                    "opensilex-security.AuthenticationService"
                ).getCredentialsGroups().then((http) => {
                    this.credentials = http.response.result;
                    console.debug("Credentials list loaded !", this.credentials);
                    resolve(http.response.result);
                }).catch(this.errorHandler)

            })
            return this.credentials;
        } else if (this.credentials instanceof Promise) {
            return this.credentials;
        } else {
            return Promise.resolve(this.credentials);
        }
    }

    public fromToken(token: string) {
        return User.fromToken(token)
    }

    public getLocalLangCode(): string {
        let availableLocalesFiltered = this.$i18n.availableLocales.filter(
            function (value, index, arr) {
                return value != OpenSilexVuePlugin.DEFAULT_LANG;
            }
        );

        if (availableLocalesFiltered.length > 0) {
            let locale = availableLocalesFiltered[0];
            return locale;
        } else {
            return OpenSilexVuePlugin.DEFAULT_LANG;
        }
    }

    /**
     * Performs an HTTP call to the specified upload service with the given file and API query params.
     * @example
     * uploadFileToService("/core/scientific_object/import_csv", {description: {}, file: csvFile}, {custom_upload_setting : "custom_value"}, false)
     *
     * @param servicePath a string defines path which will
     * be combined with api base path. e.g. "/data-analysis/scientific-app/create" (required)
     * @param body description and file key are mandatory
     * @param queryParams Any object that define additional settings which are specific to the called API (optional)
     * @param isUpdated Determine if the called HTTP API accept POST (isUpdated=true or undefined) or PUT(isUpdated=false) HTTP method (optional)
     *
     */
    uploadFileToService(servicePath: string, body: UploadFileBody, queryParams: any, isUpdated?: boolean) {
        let formData = new FormData();

        // send form data  string part in json
        // and file as binary
        for (const name in body) {
            if (name != "file") {
                formData.append(name, new Blob([JSON.stringify(body[name])], {
                    type: "application/json"
                }), name);
            } else {
                formData.append(name, body[name]);
            }
        }

        let headers = {};
        let user: User = this.getUser();
        if (user != User.ANONYMOUS()) {
            headers["Authorization"] = user.getAuthorizationHeader();
        }

        if (user.getLocale() != null) {
            headers["Accept-Language"] = user.getLocale();
        }

        let options = {
            method: isUpdated ? 'PUT' : 'POST',
            body: formData,
            // If you add this, upload won't work
            headers
        };

        let url =
            this.baseApi +
            servicePath;

        if (queryParams != null) {
            Object.keys(queryParams).forEach((k) =>
                (
                    queryParams[k] == null
                    || queryParams[k] == null
                    || queryParams[k] == ""
                ) && delete queryParams[k]
            );
            let params = new URLSearchParams();
            for (let k of Object.keys(queryParams)) {
                if (queryParams[k] instanceof Array) {
                    for (let elt of queryParams[k]) {
                        params.append(k, elt)
                    }
                } else {
                    params.append(k, queryParams[k])
                }
            }
            let paramsSize = [...params].length;
            if (paramsSize > 0) {
                url = url + "?" + params.toString();
            }
        }

        this.showLoader();
        return new Promise((resolve, reject) => {
            let promise = fetch(url, options)
                .then((response) => {
                    return response.json().then(data => ({metadata: {status: response.status}, result: data.result}))
                })

            promise
                .then((result) => {
                    this.hideLoader();
                    resolve(result);
                })
                .catch((error) => {
                    this.hideLoader();
                    reject(error);
                });
        });
    }

    /**
     * Performs an HTTP call to the specified download service with the given file and API query params.
     *
     * @example
     * uploadFileToService("/core/scientific_object/export_csv", "os_export","csv", {custom_download_setting : "custom_value"})
     *
     * @param servicePath a string defines path which will
     * be combined with api base path. e.g. "/data-analysis/get/{uri}/download" (required)
     * @param name name of the returned file (required)
     * @param extension extension of the file (required)
     * @param queryParams Any object that define additional settings which are specific to the called API (optional)
     */
    downloadFilefromService(servicePath: string, name: string, extension: string, queryParams: any) {
        return this.downloadFilefromPostOrGetService(servicePath, name, extension, "GET", queryParams, null)
    }

    /**
     * @param servicePath a string defines path which will
     * @param name name of the returned file
     * @param extension extension of the file
     * @param body body of a post request
     */
    downloadFilefromPostService(servicePath: string, name: string, extension: string, body: any, lang: string) {
        return this.downloadFilefromPostOrGetService(servicePath, name, extension, "POST", null, body)
    }

    /**    *
     * @param servicePath a string defines path which will
     * @param name name of the returned file
     * @param extension extension of the file
     * @param method REST service method (POST or GET)
     * @param queryParams params of get request
     * @param body body of a post request
     */
    downloadFilefromPostOrGetService(servicePath: string, name: string, extension: string, method: string, queryParams: any, body: any) {
        return this.getBlobFileFromPostOrGetService(servicePath, method, queryParams, body)
            .then((result) => {
                if (result != undefined) {
                    let objectURL = URL.createObjectURL(result);
                    let link = document.createElement("a");
                    link.href = objectURL;
                    link.setAttribute("download", name + (extension === undefined ? "" : "." + extension));
                    document.body.appendChild(link);
                    link.click();
                    link.remove();
                }
            })
            .catch((error) => {
                this.errorHandler(error);
                throw error;
            });
    }

    public getBlobFileFromPostOrGetService(servicePath: string, method: string, queryParams: any, body: any) {
        this.showLoader();
        let url =
            this.baseApi +
            servicePath;
        let headers = {};

        let user = this.getUser();
        if (user != User.ANONYMOUS()) {
            headers["Authorization"] = user.getAuthorizationHeader();
        }

        headers["Accept-Language"] = this.getLang();

        let request: RequestInit = {
            method: method,
            headers: headers
        }

        if (body != null) {
            request['body'] = JSON.stringify(body)
            headers["Content-Type"] = "application/json";

        }

        if (queryParams != null) {
            let params = new URLSearchParams();
            Object.keys(queryParams).forEach((key) => {
                if (
                    queryParams[key] == undefined
                    || queryParams[key] == null
                    || queryParams[key] == ""
                ) {
                    delete queryParams[key];
                } else {
                    let value = queryParams[key]
                    if (Array.isArray(value)) {
                        value.forEach(item => params.append(key, item));
                    } else {
                        params.append(key, value);
                    }
                }
            });
            let paramsSize = [...params].length;
            if (paramsSize > 0) {
                url = url + "?" + params.toString();
            }
            console.debug("Query parameters", queryParams, "Generated URL", url)
        }

        let promise = fetch(url, request);

        return promise
            .then((response) => {
                // if status = 2xx (200, 201, 202, ...)
                if (response.status >= 200 && response.status < 300) {
                    return response.blob();
                }
                response.json().then((data) => {
                    this.showErrorToast(data.result.title + ": " + data.result.message)
                })
                this.errorHandler(response);
                return undefined;

            }).catch(this.errorHandler)
            .finally(() => {
                this.hideLoader();
            })
    }

    previewFilefromGetService(servicePath: string, name: string, extension: string) {
        this.showLoader();

        let url =
            this.baseApi +
            servicePath;
        let headers = {};

        let user = this.getUser();
        if (user != User.ANONYMOUS()) {
            headers["Authorization"] = user.getAuthorizationHeader();
        }

        headers["Accept-Language"] = this.getLang();

        let request: RequestInit = {
            method: "GET",
            headers: headers
        }

        let promise = fetch(url, request);

        return promise
            .then(function (response) {
                return response.blob();
            })
            .then((result) => {
                let file = result;
                let type = "";
                if (extension == "png" || extension == "jpg" || extension == "jpeg" || extension == "svg") {
                    type = "image/" + extension;
                } else if (extension == "pdf" || extension == "json" || extension == "xml") {
                    type = "application/" + extension;
                } else if (extension == "mp4" || extension == "mpeg") {
                    type = "video/" + extension;
                } else {
                    type = null;
                    let message = this.$i18n.t("component.document.nopreview");
                    let divPreview = document.getElementById("preview");
                    let content = document.createTextNode(message as string);
                    let error = document.createElement("p");
                    if (divPreview.hasChildNodes()) {
                        while (divPreview.firstChild) {
                            divPreview.removeChild(divPreview.firstChild);
                        }
                    }
                    divPreview.appendChild(error.appendChild(content));
                }
                let blob = new Blob([file as BlobPart], {type: type});

                if (type != null) {
                    let url = URL.createObjectURL(blob);
                    let iframe = document.createElement("iframe");
                    iframe.src = url;
                    iframe.width = "600";
                    iframe.height = "600";
                    document.getElementById("preview").appendChild(iframe);
                }
                this.hideLoader()
            })
            .catch(this.errorHandler);
    }

    viewImageFromGetService(servicePath: string) {

        let url =
            this.baseApi +
            servicePath;
        let headers = {};

        let user = this.getUser();
        if (user != User.ANONYMOUS()) {
            headers["Authorization"] = user.getAuthorizationHeader();
        }

        headers["Accept-Language"] = this.getLang();

        let request: RequestInit = {
            method: "GET",
            headers: headers
        }

        let promise = fetch(url, request);

        return promise
            .then(function (response) {
                return response.blob();

            }).catch(this.errorHandler).then((result) => {
                let file = result;
                let blob = new Blob([file as BlobPart]);
                let url = URL.createObjectURL(blob);
                return url;
            })
            .catch(this.errorHandler);
    }

    public datatypes: Array<VueDataTypeDTO> = [];
    private datatypesByURI: Map<string, VueDataTypeDTO> = new Map<string, VueDataTypeDTO>();

    public getDatatype(uri: string): VueDataTypeDTO {
        return this.datatypesByURI.get(uri);
    }

    public loadDataTypes() {
        return new Promise((resolve, reject) => {
            this.getService<VueJsOntologyExtensionService>("opensilex.VueJsOntologyExtensionService")
                .getDataTypes()
                .then((http) => {
                    this.datatypes = http.response.result;
                    this.datatypesByURI = new Map<string, VueDataTypeDTO>();

                    this.datatypes.forEach(dataType => {
                        this.datatypesByURI.set(dataType.uri, dataType);
                        this.datatypesByURI.set(dataType.short_uri, dataType);
                    });
                    resolve(this.datatypes);
                })
                .catch(reject);
        });
    }

    public variableDatatypes: Array<VariableDatatypeDTO> = [];


    /**
     * It takes a URI as input and returns the label of the variable datatype that corresponds to the URI
     * @param {string} uri - the uri of the variable datatype
     * @returns The label of the variable datatype.
     */
    public getVariableDatatypeLabel(uri: string): string {
        if (!uri) {
            return undefined;
        }
        let label = this.$i18n.t(this.variableDatatypes.find(elem => elem.uri === uri).name).toString();
        return label.charAt(0).toUpperCase() + label.slice(1);
    }

    /**
     * It loads the variable data types from the server and stores them in the variableDatatypes variable
     * @returns A promise that will resolve when the variable datatypes have been loaded.
     */
    public loadVariableDataTypes() {
        return new Promise((resolve, reject) => {
            this.getService<VariablesService>("opensilex.VariablesService")
                .getDatatypes()
                .then((http) => {
                    this.variableDatatypes = http.response.result;
                    resolve(this.variableDatatypes);
                })
                .catch(reject);
        });
    }

    public namespaces = {};

    /**
     * It returns a promise that will resolve to the list of namespaces
     * @returns A promise that will return the namespaces.
     */
    public loadNameSpaces() {
        return new Promise((resolve, reject) => {
            this.getService<OntologyService>("opensilex.OntologyService")
                .getNameSpace()
                .then((http) => {
                    this.namespaces = http.response.result;
                    resolve(this.namespaces);
                })
                .catch(reject);
        });
    }

    /**
     * It takes a URI and returns a short URI
     * @param {string} uri - The full URI of the resource.
     * @returns The short uri
     */
    public getShortUri(uri: string) {
        for (let prefix in this.namespaces) {
            if (uri.startsWith(this.namespaces[prefix])) {
                return uri.replace(this.namespaces[prefix], prefix + ":");
            }
        }
        return uri;
    }

    /**
     * It takes a URI and replaces the prefix with the full namespace
     * @param {string} uri - The uri to be converted
     * @returns The long URI of the given URI.
     */
    public getLongUri(uri: string) {
        for (let prefix in this.namespaces) {
            if (uri.startsWith(prefix)) {
                return uri.replace(prefix + ":", this.namespaces[prefix]);
            }
        }
        return uri;
    }

    /**
     * > This function checks if two URIs are the same
     * @param uri1 - The first URI to compare.
     * @param uri2 - The URI to check against.
     * @returns The short URI of the first URI is being compared to the short URI of the second URI.
     */
    public checkURIs(uri1, uri2) {
        return this.getShortUri(uri1) === this.getShortUri(uri2);
    }

    public versionInfo: any = [];

    public loadVersionInfo() {
        return new Promise((resolve, reject) => {
            this.getService<SystemService>("opensilex.SystemService")
                .getVersionInfo()
                .then((http) => {
                    this.versionInfo = http.response.result;
                    resolve(this.versionInfo);
                })
                .catch(reject);
        });
    }

    private categoriesNameByUri = {};

    public getFactorCategoryName(uri) {
        return this.categoriesNameByUri[uri];
    }

    private deepWalkObject(walkProperty: string, object: any, objectToFill: any, walkValidationFunction: Function, workFunction: Function) {

        if (walkValidationFunction(object)) {
            for (var childObject of object[walkProperty]) {
                this.deepWalkObject(walkProperty, childObject, objectToFill, walkValidationFunction, workFunction);
            }
        } else {
            workFunction(object, objectToFill);
        }
    }

    public loadFactorCategories() {
        return new Promise((resolve, reject) => {
            this.getService<any>("opensilex.FactorsService")
                .searchCategories(undefined, ["name=asc"])
                .then(
                    (http
                    ) => {
                        this.categoriesNameByUri = {};

                        http.response.result.forEach((categoryDto) => {
                            // fill this.categoriesNameByUri by traversing category dto-tree
                            this.deepWalkObject("children", categoryDto, this.categoriesNameByUri,
                                function (object) {
                                    if (object == undefined) {
                                        return false;
                                    } else {
                                        return (object.children.length > 0 ? true : false);
                                    }
                                }, function (object, arrayToFill) {
                                    arrayToFill[object.uri] = object.name;
                                })
                        });
                        resolve(this.categoriesNameByUri)
                    }
                )
                .catch(reject);
        });
    }

    public objectTypes: Array<VueObjectTypeDTO> = [];
    private objectTypesByURI: Map<string, VueObjectTypeDTO> = new Map<string, VueObjectTypeDTO>();

    public getObjectType(uri: string): VueObjectTypeDTO {
        return this.objectTypesByURI.get(uri);
    }

    public loadObjectTypes() {
        return new Promise((resolve, reject) => {
            this.getService<VueJsOntologyExtensionService>("opensilex.VueJsOntologyExtensionService")
                .getObjectTypes()
                .then((http) => {
                    this.objectTypes = http.response.result;
                    this.objectTypesByURI = new Map<string, VueObjectTypeDTO>();

                    this.objectTypes.forEach(objectType => {
                        this.objectTypesByURI.set(objectType.uri, objectType);
                        this.objectTypesByURI.set(objectType.short_uri, objectType);
                    });

                    resolve(this.objectTypes);
                })
                .catch(reject);
        });
    }

    getType(uri) {
        return this.getDatatype(uri) || this.getObjectType(uri);
    }

    iconIDs = [];
    selectIconIDs = [];

    public getIconIDs() {
        return this.iconIDs;
    }

    public setIconIDs(iconIDs) {
        this.iconIDs = iconIDs;
        this.selectIconIDs = [];
        for (let i in iconIDs) {
            let iconID = iconIDs[i];

            this.selectIconIDs.push({
                id: iconID,
                label: iconID
            })
        }
    }

    public getSelectIconIDs() {
        return this.selectIconIDs;
    }

    public prepareGetParameter(value) {
        if (value == null || value == undefined || value == '' || value == []) {
            return undefined;
        } else {
            return value;
        }
    }

    getGuideFile() {
        let path = this.getResourceURI('documents/GuideOpenSilex_V1-1Oct21.pdf');
        fetch(path)
            .then((response) => response.blob())
            .then(function (blob) {
                var fileURL = URL.createObjectURL(blob);
                var fileLink = document.createElement('a');
                fileLink.href = fileURL;
                fileLink.setAttribute('download', 'GuideOpenSilex_V1-1Oct21.pdf');
                document.body.appendChild(fileLink);
                fileLink.click();
            })
    }
}

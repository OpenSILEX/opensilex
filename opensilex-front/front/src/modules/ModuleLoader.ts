import { ModuleComponentDefinition } from './ModuleComponentDefinition';
import { FrontService } from '@/lib';
import Vue from 'vue';

declare var window: any;

export class ModuleLoader {

  private loadedModules = {
    "opensilex-front": Promise.resolve(null)
  };

  private frontService: FrontService;
  private baseUri: string;

  public constructor(baseUri: string, frontService: FrontService) {
    this.frontService = frontService;
    this.baseUri = baseUri;
  }

  public loadModules(modules: Array<string>) {
    let promises: Array<Promise<any>> = [];
    for (let i in modules) {
      let moduleName = modules[i];

      if (!this.loadedModules[moduleName]) {
        let moduleUri = this.baseUri + "/front/extension/" + moduleName + ".js";
        this.loadedModules[moduleName] = this.loadOpenSilexExtension(moduleName, moduleUri);
      }

      promises.push(this.loadedModules[moduleName])
    }

    return Promise.all(promises);
  }

  public loadComponentModules(components: Array<ModuleComponentDefinition>) {
    let componentByModuleMap = {};

    let promises: Array<Promise<any>> = [];

    for (let i in components) {
      let componentDef = components[i];
      let moduleName = componentDef.getModule();

      if (!componentByModuleMap[moduleName]) {
        componentByModuleMap[moduleName] = [];
      }

      [moduleName].push(componentDef.getName());

      if (!this.loadedModules[moduleName]) {
        let moduleUri = this.baseUri + "/front/extension/" + moduleName + ".js";
        this.loadedModules[moduleName] = this.loadOpenSilexExtension(moduleName, moduleUri);
      }

      promises.push(this.loadedModules[moduleName])
    }

    return Promise.all(promises);
  }

  public loadOpenSilexExtension(name, url) {
    if (window[name]) return window[name];

    window[name] = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.async = true;
      script.addEventListener('load', () => {
        Vue.use(window[name].default);
        resolve(window[name]);
      });
      script.addEventListener('error', () => {
        reject(new Error(`Error loading ${url}`));
      });
      script.src = url;
      document.head.appendChild(script);
    });

    return window[name];
  }

}
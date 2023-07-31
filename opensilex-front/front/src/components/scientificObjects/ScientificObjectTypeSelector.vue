<template>
  <opensilex-SelectForm
    :key="lang"
    :label="label"
    :selected.sync="typesURI"
    :multiple="multiple"
    :optionsLoadingMethod="loadTypes"
    :conversionMethod="conversionMethod"
    placeholder="ScientificObjectTypeSelector.placeholder"
    @select="select"
    @deselect="deselect"
    @keyup.enter.native="onEnter"
  ></opensilex-SelectForm>
</template>

<script lang="ts">
import { Component, Prop, PropSync, Watch } from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
// @ts-ignore
import { ListItemDTO } from "opensilex-core/index";
// @ts-ignore
import { ScientificObjectsService } from "opensilex-core/index";

@Component
export default class ScientificObjectTypeSelector extends Vue {
  $opensilex: any;
  service: ScientificObjectsService;

  @PropSync("types")
  typesURI;

  @Prop()
  experimentURI;

  @Prop()
  label;

  @Prop()
  multiple;

  get lang() {
    return this.$store.getters.language;
  }

  loadTypes(typesURI) {
    if (
      this.experimentURI == null ||
      this.experimentURI == "" ||
      this.experimentURI == undefined
    ) {
      return this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .getUsedTypes()
        .then((http: HttpResponse<OpenSilexResponse<Array<ListItemDTO>>>) => {
          return this.setUpFinalDto(http.response.result);
        });
    } else {
      return this.$opensilex
        .getService("opensilex.ScientificObjectsService")
        .getUsedTypes(this.experimentURI)
        .then((http: HttpResponse<OpenSilexResponse<Array<ListItemDTO>>>) => {
          return this.setUpFinalDto(http.response.result);
        });
    }
  }

  conversionMethod(data) {
    if (data && data.name && data.color) {
      return {
        id: data.uri,
        label: data.name,
        isDisabled: data.isDisabled ?? false,
        color: data.color
      };
    } else {
      return data;
    }
  }

  setUpFinalDto(httpResult) {
    let graphs = this.sortGraph(httpResult);
    let result = [];

    for(let dto of httpResult) {
      let newDto = dto;
      if(graphs.phis.has(dto.uri)) {
        newDto["color"] = "phis"
        result.push(newDto);
      }

      if(graphs.sixtine.has(dto.uri)) {
        newDto["color"] = "sixtine"
        result.push(newDto);
      }

      if(graphs.erreur.has(dto.uri)) {
        newDto["color"] = "error"
        result.push(newDto);
      }

      if(graphs.global.has(dto.uri)) {
        newDto["color"] = "global"
        result.push(newDto);
      }
    }

    return result;
  }

  sortGraph(data) {

    let sixtine = [];
    let phis = [];
    let global = [];

    let erreurFinal = new Set();
    for (let dto of data) {
      if(!dto.graph) {
        erreurFinal.add(dto.uri);
      } else {
        
        if(dto.graph.startsWith("http://phenome.inrae.fr/openstack-test/")) {
          phis.push(dto.uri);
        }

        if(dto.graph.startsWith("http://vegetalunit.inrae.fr/openstack-test/") || dto.graph.endsWith("oeso-sixtine")) {
          sixtine.push(dto.uri);
        }

        if(dto.graph === "http://www.opensilex.org/set/properties" || dto.graph.endsWith("oeso-ext")) {
          global.push(dto.uri);
        }
      }
    }

    let sixtineFinal = new Set();
    let phisFinal = new Set();
    let globalFinal = new Set();

    for (let uriSixtine of sixtine) {
      if(global.includes(uriSixtine)) {
        erreurFinal.add(uriSixtine);
      } else {
        sixtineFinal.add(uriSixtine);
      }
    }

    for (let uriPhis of phis) {
      if(global.includes(uriPhis)) {
        erreurFinal.add(uriPhis);
      } else {
        phisFinal.add(uriPhis);
      }
    }
    
    for (let uriGlobal of global) {
      if(!sixtineFinal.has(uriGlobal) && !phisFinal.has(uriGlobal) && !erreurFinal.has(uriGlobal)) {
        globalFinal.add(uriGlobal);
      }
    }
    
    return {
      erreur: erreurFinal,
      sixtine: sixtineFinal,
      phis: phisFinal,
      global: globalFinal
    }
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }

  onEnter() {
    this.$emit("handlingEnterKey")
  }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  ScientificObjectTypeSelector:
    placeholder: Select a type

fr:
  ScientificObjectTypeSelector:
    placeholder: Sélectionner un type
</i18n>
<template>
  <div>
    <opensilex-FormSelector
      :label="label"
      v-model:selected="siteURIsProxy"
      :multiple="multiple"
      :helpMessage="helpMessage"
      :placeholder="placeholder"
      :searchMethod="searchSites"
      :itemLoadingMethod="loadSites"
      :conversionMethod="siteToSelectNode"
      :disabled="disabled"
      noResultsText="SiteSelector.no-result"
      @select="select"
      @deselect="deselect"
    ></opensilex-FormSelector>
  </div>

</template>

<script setup lang="ts">
import {computed, inject, ref} from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {NamedResourceDTOSiteModel} from 'opensilex-core/index';
import {SiteGetListDTO} from "opensilex-core/model/siteGetListDTO";

//#region Constant values & Services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const organizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');
//#endregion
//#region Props

interface Props{
  siteURIs?: string[],
  label?: string,
  multiple?: boolean,
  helpMessage?: string,
  placeholder?: string,
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  siteURIs: () => [],
  placeholder: "SiteSelector.placeholder"
});
//#endregion
//#region Emits & EventHandling
const emit = defineEmits<{
  (e: 'update:siteURIs', value: string[]): void
  (e: 'select', value: any): void
  (e: 'deselect', value: any): void
}>();

function select(value: any) {
  emit("select", value);
}

function deselect(value: any) {
  emit("deselect", value);
}
//#endregion

//#region Computed

//This allows updating of siteURIs in parent component (instead of the old PropSync way)
const siteURIsProxy = computed({
  get: () => props.siteURIs,
  set: (value: string[]) => emit('update:siteURIs', value)
});
//#endregion

//#region  Refs
const siteByUriCache = ref<Map<string, NamedResourceDTOSiteModel>>(new Map<string, NamedResourceDTOSiteModel>());
//#endregion

//#region Functions & webservice calls
function searchSites(searchQuery: string, page: number, pageSize: number) {
  return organizationsService.searchSites(
    searchQuery, //name
    undefined,
    undefined,
    page,
    pageSize
  ).then((http: HttpResponse<OpenSilexResponse<Array<SiteGetListDTO>>>) => {

    if (http && http.response) {
      siteByUriCache.value.clear();
      http.response.result.forEach(dto => {
        siteByUriCache.value.set(dto.uri, dto);
      })
    }
    return http;
  }).catch($opensilex.errorHandler);
}

function loadSites(siteUris: any[]) {
  if (!Array.isArray(siteUris) || siteUris.length === 0) {
    return undefined;
  }

  if (siteByUriCache.value.size === 0) {
    let siteDtos = [];

    siteUris.forEach(site => {
      if (site.name && site.name.length > 0 && site.uri && site.uri.length > 0) {
        siteDtos.push(site);
      }
    });

    if (siteDtos.length > 0) {
      return siteDtos;
    }

    return organizationsService.getSitesByURI(siteUris)
      .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTOSiteModel>>>) =>
        (http && http.response) ? http.response.result : undefined
      );
  }

  return siteUris.map(siteUri => siteByUriCache.value.get(siteUri));
}

function siteToSelectNode(siteDto: NamedResourceDTOSiteModel) {
  if (!siteDto) {
    return undefined;
  }

  return {
    label: siteDto.name,
    id: siteDto.uri
  };
}
//#endregion

</script>

<style scoped>

</style>

<i18n>
en:
  SiteSelector:
    placeholder: Search and select a site
    no-result: No site found
fr:
  SiteSelector:
    placeholder: "Rechercher et sélectionner un site"
    no-result: "Aucun site trouvé"
</i18n>
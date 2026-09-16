<template>
  <div>
    <InfiteScrollDropdown
      v-model:selected="siteURIsProxy"
      :fetchPage="searchSites"
      :itemLoadingMethod="loadSites"
      :conversionMethod="siteToSelectOption"
      :label="label"
      :multiple="multiple"
      :helpMessage="helpMessage"
      :placeholder="t(placeholder)"
      :disabled="disabled"
      @selectionChange="(option) => emit('selectionChange', option)"
      @clear="emit('clear')"
    />
  </div>
</template>

<script setup lang="ts">
import {computed, inject} from "vue";
import {useI18n} from "vue-i18n";
import type {SelectOption} from "naive-ui";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {NamedResourceDTOSiteModel} from 'opensilex-core/index';
import {SiteGetListDTO} from "opensilex-core/model/siteGetListDTO";
import InfiteScrollDropdown from "@/components/common/forms/InfiteScrollDropdown.vue";

//#region Constant values & Services
const $opensilex = inject<OpenSilexVuePlugin>('$opensilex')!;
const organizationsService = $opensilex.getService<OrganizationsService>('opensilex.OrganizationsService');
const { t } = useI18n();
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
  (e: 'selectionChange', option: SelectOption | SelectOption[] | undefined): void
  (e: 'clear'): void
}>();
//#endregion

//#region Computed
//This allows updating of siteURIs in parent component (instead of the old PropSync way)
const siteURIsProxy = computed({
  get: () => props.siteURIs,
  set: (value: string[]) => emit('update:siteURIs', value)
});
//#endregion

//#region Functions & webservice calls
/** Loads one page of results. `page` is zero-based. */
function searchSites(searchQuery: string, page: number, pageSize: number) {
  return organizationsService.searchSites(
    searchQuery,     // pattern
    undefined,       // organizations
    ['name=asc'],    // order_by
    page,
    pageSize
  ).then((http: HttpResponse<OpenSilexResponse<Array<SiteGetListDTO>>>) => http)
    .catch($opensilex.errorHandler);
}

/** Loads the already selected elements (update form), so that their name can be displayed. */
function loadSites(siteUris: string[]) {
  if (!Array.isArray(siteUris) || siteUris.length === 0) {
    return undefined;
  }

  return organizationsService.getSitesByURI(siteUris)
    .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTOSiteModel>>>) =>
      (http && http.response) ? http.response.result : undefined
    );
}

const siteToSelectOption = (dto: NamedResourceDTOSiteModel): SelectOption => ({
  label: dto.name,
  value: dto.uri
});
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
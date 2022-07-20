<template>
  <div>
    <opensilex-SelectForm
        :label="label"
        :selected.sync="siteURIs"
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
    ></opensilex-SelectForm>
  </div>

</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {OrganizationsService} from "opensilex-core/api/organizations.service";
import {NamedResourceDTO, NamedResourceDTOSiteModel } from 'opensilex-core/index';

@Component
export default class SiteSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $service: OrganizationsService;

  @PropSync("sites", {default: () => []})
  siteURIs;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  helpMessage;

  @Prop({default: "FacilitySelector.placeholder"})
  placeholder;

  @Prop()
  disabled;

  siteByUriCache: Map<string, NamedResourceDTOSiteModel>;

  created() {
    this.$service = this.$opensilex.getService("opensilex.OrganizationsService");
    this.siteByUriCache = new Map<string, NamedResourceDTOSiteModel>();
  }

  searchSites(searchQuery, page, pageSize) {
    return this.$service.searchSites(
        searchQuery, //name
        undefined,
        undefined,
        page,
        pageSize
    ).then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {

      if (http && http.response) {
        this.siteByUriCache.clear();
        http.response.result.forEach(dto => {
          this.siteByUriCache.set(dto.uri, dto);
        })
      }
      return http;
    }).catch(this.$opensilex.errorHandler);
  }

  loadSites(siteUris) {
    if (!Array.isArray(siteUris) || siteUris.length === 0) {
      return undefined;
    }

    if (this.siteByUriCache.size === 0) {
      let siteDtos = [];

      siteUris.forEach(site => {
        if (site.name && site.name.length > 0 && site.uri && site.uri.length > 0) {
          siteDtos.push(site);
        }
      });

      if (siteDtos.length > 0) {
        return siteDtos;
      }

      return this.$service.getSitesByURI(siteUris)
        .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTOSiteModel>>>) =>
            (http && http.response) ? http.response.result : undefined
        );
    }

    return siteUris.map(siteUri => this.siteByUriCache.get(siteUri));
  }

  siteToSelectNode(siteDto: NamedResourceDTOSiteModel) {
    if (!siteDto) {
      return undefined;
    }

    return {
      label: siteDto.name,
      id: siteDto.uri
    };
  }

  select(value) {
    this.$emit("select", value);
  }

  deselect(value) {
    this.$emit("deselect", value);
  }
}
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
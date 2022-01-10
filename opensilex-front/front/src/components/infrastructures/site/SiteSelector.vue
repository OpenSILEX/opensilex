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
        noResultsText="InfrastructureFacilitySelector.no-result"
        @select="select"
        @deselect="deselect"
    ></opensilex-SelectForm>
  </div>

</template>

<script lang="ts">
import {Component, Prop, PropSync} from "vue-property-decorator";
import Vue from "vue";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {OrganisationsService} from "opensilex-core/api/organisations.service";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {NamedResourceDTOSiteModel} from "opensilex-core/model/namedResourceDTOSiteModel";

@Component
export default class SiteSelector extends Vue {
  $opensilex: OpenSilexVuePlugin;
  $service: OrganisationsService;

  @PropSync("sites", {default: () => []})
  siteURIs;

  @Prop()
  label;

  @Prop()
  multiple;

  @Prop()
  helpMessage;

  @Prop({default: "InfrastructureFacilitySelector.placeholder"})
  placeholder;

  @Prop()
  disabled;

  siteByUriCache: Map<string, NamedResourceDTOSiteModel>;

  created() {
    this.$service = this.$opensilex.getService("opensilex.OrganisationsService");
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
    if (this.siteByUriCache.size > 0) {
      return Array.from(this.siteByUriCache.values());
    }

    if (!Array.isArray(siteUris) || siteUris.length === 0) {
      return undefined;
    }

    if (typeof siteUris[0] === "object") {
      return siteUris;
    }

    return this.$service.getSitesByURI(siteUris);
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
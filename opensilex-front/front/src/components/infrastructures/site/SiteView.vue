<template>
  <div class="container-fluid" v-if="selected">
    <opensilex-PageHeader
        icon="ik#ik-globe"
        :title="selected.name"
        :description="selected.rdf_type_name"
    ></opensilex-PageHeader>
    <opensilex-PageActions :tabs="false" :returnButton="true">
    </opensilex-PageActions>
    <div class="row">
      <div class="col-md-12">
        <opensilex-SiteDetail
            :selected="selected"
            :withActions="true"
            @onUpdate="refresh"
        >
        </opensilex-SiteDetail>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import {Component} from "vue-property-decorator";
import Vue from "vue";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {SiteGetDTO} from "opensilex-core/model/siteGetDTO";
import {OrganisationsService} from "opensilex-core/api/organisations.service";

@Component
export default class SiteView extends Vue {
  $opensilex: any;

  selected: SiteGetDTO = null;
  uri = null;
  service: OrganisationsService;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.service = this.$opensilex.getService(
        "opensilex-core.OrganisationsService"
    );
    this.refresh();
  }

  refresh() {
    this.service
        .getSite(this.uri)
        .then((http: HttpResponse<OpenSilexResponse<SiteGetDTO>>) => {
          this.selected = http.response.result;
        });
  }
}
</script>

<style scoped lang="scss">
</style>


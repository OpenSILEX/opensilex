<template>
  <div class="container-fluid">
    <opensilex-PageHeader
      icon="fa#sun"
      title="component.menu.experimentalDesign.factors"
      description="component.factor.details.label"
    ></opensilex-PageHeader>
    <opensilex-NavBar returnTo="/factors">
      <template v-slot:linksLeft>
        <li class="active">
          <b-button
            class="mb-2 mr-2"
            variant="outline-primary"
          >{{$t('component.factor.details.label')}}</b-button>
        </li>
        <li>
          <b-button
            class="mb-2 mr-2"
            variant="outline-primary"
            disabled
          >{{$t('component.common.details.document')}}</b-button>
        </li>
        <li>
          <b-button
            class="mb-2 mr-2"
            variant="outline-primary"
            disabled
          >{{$t('component.common.details.experiment')}}</b-button>
        </li>
      </template>
    </opensilex-NavBar>
    <div class="container-fluid">
      <b-row>
        <b-col>
          <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
            <template v-slot:body>
              <opensilex-StringView label="component.factor.uri" :value="factor.uri"></opensilex-StringView>
              <opensilex-StringView label="component.factor.names.en" :value="factor.names.en"></opensilex-StringView>
              <opensilex-StringView
                label="component.factor.names.local-name"
                :value="factor.names[localNameCode]"
              ></opensilex-StringView>
              <opensilex-StringView label="component.factor.comment" :value="factor.comment"></opensilex-StringView>
            </template>
          </opensilex-Card>
        </b-col>
        <b-col>
          <opensilex-Card label="component.skos.ontologies-references-label" icon="ik#ik-clipboard">
            <template v-slot:body>
              <opensilex-ExternalReferencesDetails :skosReferences="factor"></opensilex-ExternalReferencesDetails>
            </template>
          </opensilex-Card>
        </b-col>
      </b-row>
      <b-row>
        <b-col>
          <opensilex-Card label="component.factor.details.factorLevels" icon="ik#ik-clipboard">
            <template v-slot:body>
              <b-table
                v-if="factor.factorLevels.length != 0"
                striped
                bordered
                :items="factor.factorLevels"
                :fields="factorLevelFields"
              >
                <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
                <template v-slot:head(name)="data">{{$t(data.label)}}</template>
                <template v-slot:head(comment)="data">{{$t(data.label)}}</template>
              </b-table>
              <p v-else>
                <strong>{{$t('component.skos.no-external-links-provided')}}</strong>
              </p>
            </template>
          </opensilex-Card>
        </b-col>
      </b-row>
    </div>
    <!-- <div class="tab-content">
      <div class="tab-pane fade show active" id="p1">Panneau 1</div>
      <div class="tab-pane fade" id="p2">Panneau 2</div>
    </div>-->

    <!-- <b-tabs content-class="mt-3" pills card>
      <b-tab :title="$t('component.factor.tabs.factor')" active>
        <div class="row"></div>
      </b-tab>
      <b-tab :title="$t('component.common.tabs.factorLevels')">
        <div>
          <b-table
            v-if="this.factor.factorLevels.length != 0"
            striped
            bordered
            :items="this.factor.factorLevels"
            :fields="this.factorLevelFields"
          >
            <template v-slot:head(uri)="data">{{$t(data.label)}}</template>
            <template v-slot:head(name)="data">{{$t(data.label)}}</template>
            <template v-slot:head(comment)="data">{{$t(data.label)}}</template>
          </b-table>
          <p v-else>
            <strong>{{$t('component.skos.no-external-links-provided')}}</strong>
          </p>
        </div>
      </b-tab>
      <b-tab :title="$t('component.skos.ontologies-references-label')">
        <opensilex-ExternalReferencesDetails :skosReferences="this.factor"></opensilex-ExternalReferencesDetails>
      </b-tab>
      <b-tab :title="$t('component.common.tabs.document')" disabled></b-tab>
      <b-tab :title="$t('component.common.tabs.experiment')" disabled></b-tab>
    </b-tabs>-->
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { FactorDetailsGetDTO, FactorsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class FactorDetails extends Vue {
  $opensilex: any;
  $store: any;
  $route: any;
  $t: any;
  $i18n: any;
  service: FactorsService;

  get user() {
    return this.$store.state.user;
  }

  factor: any = {
    uri: null,
    names: { },
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: [],
    factorLevels: []
  };

  get localNameCode(): string {
    return this.$opensilex.getLocalLangCode();
  }

  factorLevelFields: any[] = [
    {
      key: "uri",
      label: "component.factorLevel.uri",
      sortable: false
    },
    {
      key: "name",
      label: "component.factorLevel.name",
      sortable: true
    },
    {
      key: "comment",
      label: "component.factorLevel.comment",
      sortable: false
    }
  ];

  created() {
    this.service = this.$opensilex.getService("opensilex.FactorsService");

    let uri = this.$route.params.uri;
    this.loadFactor(uri);
  }

  loadFactor(uri: string) {
    this.service
      .getFactor(uri)
      .then((http: HttpResponse<OpenSilexResponse<FactorDetailsGetDTO>>) => {
        this.factor = http.response.result;
      })
      .catch(this.$opensilex.errorHandler);
  }
}
</script>

<style scoped lang="scss">
</style>

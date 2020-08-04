<template>
  <div class="container-fluid">
    <b-row>
      <b-col sm="5">
        <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
          <template v-slot:body>
            <opensilex-StringView label="component.factor.uri" :value="factor.uri"></opensilex-StringView>
            <opensilex-StringView label="component.factor.name" :value="factor.name"></opensilex-StringView>
            <opensilex-StringView
              label="component.factor.category"
              :value="$i18n.t(factorCategoriesMap[factor.category])"
            ></opensilex-StringView>
            <opensilex-StringView label="component.factor.comment" :value="factor.comment"></opensilex-StringView>
          </template>
        </opensilex-Card>
      </b-col>

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
              <template v-slot:head(category)="data">{{$t(data.label)}}</template>
              <template v-slot:head(comment)="data">{{$t(data.label)}}</template>
            </b-table>
            <p v-else>
              <strong>{{$t('component.factor.details.no-factorLevels-provided')}}</strong>
            </p>
          </template>
        </opensilex-Card>
      </b-col>
    </b-row>
    <b-row>
      <b-col>
        <opensilex-Card label="component.skos.ontologies-references-label" icon="ik#ik-clipboard">
          <template v-slot:body>
            <opensilex-ExternalReferencesDetails :skosReferences="factor"></opensilex-ExternalReferencesDetails>
          </template>
        </opensilex-Card>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import { Component, Prop, Ref } from "vue-property-decorator";
import Vue from "vue";
import { FactorDetailsGetDTO, FactorsService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";
import { FactorCategory } from "./FactorCategory";

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

  get credentials() {
    return this.$store.state.credentials;
  }
  isDetailsTab() {
    return this.$route.path.startsWith("/factor/");
  }

  factorCategoriesMap: Map<
    string,
    string
  > = FactorCategory.getFactorCategories();

  factor: any = {
    uri: null,
    name: null,
    category: null,
    comment: null,
    exactMatch: [],
    closeMatch: [],
    broader: [],
    narrower: [],
    factorLevels: [],
  };

  factorLevelFields: any[] = [
    {
      key: "uri",
      label: "component.factorLevel.uri",
      sortable: false,
    },
    {
      key: "name",
      label: "component.factorLevel.name",
      sortable: true,
    },
    {
      key: "comment",
      label: "component.factorLevel.comment",
      sortable: false,
    },
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

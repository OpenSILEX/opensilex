<template>
  <div class="container-fluid">
    <opensilex-PageHeader
        icon="fa#sun"
        description="VariableView.type"
        :title="variable.name"
    ></opensilex-PageHeader>

    <opensilex-PageActions :tabs="true" :returnButton="true">
      <template v-slot>
        <b-nav-item
            :active="isDetailsTab()"
            :to="{ path: '/variable/details/' + encodeURIComponent(uri) }"
        >{{ $t('component.common.details-label') }}
        </b-nav-item>
        <b-nav-item
            :active="isAnnotationTab()"
            :to="{ path: '/variable/annotations/' + encodeURIComponent(uri) }"
        >{{ $t("Annotation.list-title") }}
        </b-nav-item>
        <b-nav-item
            :active="isVisualizationTab()"
            :to="{ path: '/variable/visualization/' + encodeURIComponent(uri) }"
        >{{ $t('VariableDetails.visualization') }}
        </b-nav-item>

      </template>
    </opensilex-PageActions>
    <opensilex-PageContent>
      <template v-slot>
        <opensilex-VariableDetails
            v-if="isDetailsTab()"
            :variable="variable"
            @onUpdate="updateVariable($event)"
        ></opensilex-VariableDetails>

        <opensilex-AnnotationList
                v-else-if="isAnnotationTab()"
                ref="annotationList"
                :target="uri"
                :displayTargetColumn="false"
                :enableActions="true"
                :modificationCredentialId="credentials.CREDENTIAL_ANNOTATION_MODIFICATION_ID"
                :deleteCredentialId="credentials.CREDENTIAL_ANNOTATION_DELETE_ID"
        ></opensilex-AnnotationList>

        <opensilex-VariableVisualizationTab
            v-else-if="isVisualizationTab()"
            :variable="uri"
            :modificationCredentialId="credentials.CREDENTIAL_DEVICE_MODIFICATION_ID"
        ></opensilex-VariableVisualizationTab>
      </template>

    </opensilex-PageContent>
  </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
// @ts-ignore
import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
// @ts-ignore
import {VariablesService} from "opensilex-core/api/variables.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import AnnotationList from "../../annotations/list/AnnotationList.vue";

@Component
export default class VariableView extends Vue {

  $opensilex: OpenSilexVuePlugin;
  service: VariablesService;
  $store: any;
  $route: any;
  $router: any;

  $t: any;
  $i18n: any;

        static getEmptyDetailsDTO() : VariableDetailsDTO{
            return {
                uri: undefined,
                alternative_name: undefined,
                name: undefined,
                entity: undefined,
                entity_of_interest: undefined,
                characteristic: undefined,
                description: undefined,
                time_interval: undefined,
                sampling_interval: undefined,
                datatype: undefined,
                trait: undefined,
                trait_name: undefined,
                method: undefined,
                unit: undefined,
                exact_match: [],
                close_match: [],
                broad_match: [],
                narrow_match: [],
                species: undefined
            };
        }


  variable: VariableDetailsDTO = VariableView.getEmptyDetailsDTO();
  uri: string;

  @Ref("annotationList") readonly annotationList!: AnnotationList;

  get user() {
    return this.$store.state.user;
  }

  get credentials() {
    return this.$store.state.credentials;
  }

  created() {
    this.service = this.$opensilex.getService("opensilex.VariablesService");
    this.uri = decodeURIComponent(this.$route.params.uri);
    this.loadVariable(this.uri);
  }

  isDetailsTab() {
    return this.$route.path.startsWith("/variable/details/");
  }

  isAnnotationTab() {
    return this.$route.path.startsWith("/variable/annotations/");
  }

  isVisualizationTab() {
    return this.$route.path.startsWith("/variable/visualization/");
  }

  loadVariable(uri: string) {
    this.service.getVariable(uri).then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
      this.variable = http.response.result;
    }).catch(this.$opensilex.errorHandler);
  }

  updateVariable(variable) {
    this.uri = variable.uri;
    this.loadVariable(this.uri);
  }

}
</script>

<style scoped lang="scss">
</style>

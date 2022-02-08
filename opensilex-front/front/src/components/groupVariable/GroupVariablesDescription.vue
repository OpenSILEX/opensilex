<template>
    <div class="container-fluid">
<!-- 
        <opensilex-PageHeader
          icon="fa#sun"
          description="GroupVariablesDescription.type"
          :title="variablesGroup.name">
        </opensilex-PageHeader> -->

        <opensilex-PageActions :tabs="true" :returnButton="true">
            <template v-slot>
                <b-nav-item :active="isDetailsTab()" :to="{ path: '/variables_group/details/' + encodeURIComponent(uri) }">
                    {{ $t('component.common.details-label') }}
                </b-nav-item>

                <b-nav-item :active="isAnnotationTab()" :to="{ path: '/variables_group/annotations/' + encodeURIComponent(uri) }">
                    {{ $t("Annotation.list-title") }}
                </b-nav-item>
            </template>
        </opensilex-PageActions>

        <opensilex-PageContent>
            <template v-slot>
                <opensilex-GroupVariablesDetails
                        v-if="isDetailsTab()"
                        :variablesGroup="variablesGroup"
                        @onUpdate="updateVariablesGroup($event)">
                </opensilex-GroupVariablesDetails>

                <opensilex-AnnotationList
                        v-else-if="isAnnotationTab()"
                        ref="annotationList"
                        :target="uri"
                        :displayTargetColumn="false"
                        :enableActions="true"
                        :modificationCredentialId="credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID"
                        :deleteCredentialId="credentials.CREDENTIAL_VARIABLE_DELETE_ID">
                </opensilex-AnnotationList>
            </template>
        </opensilex-PageContent>

    </div>  
</template>

<script lang="ts">
import { Component, Ref } from "vue-property-decorator";
import Vue from "vue";
import {VariablesService, VariablesGroupGetDTO} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
import GroupVariablesForm from './GroupVariablesForm.vue'
import AnnotationList from "../annotations/list/AnnotationList.vue";

@Component
export default class GroupVariablesDescription extends Vue {
    $opensilex: any;
    service: VariablesService;
    $route: any;
    $t: any;

    static getEmptyDTO(){
      return GroupVariablesForm.getEmptyForm();
    }

    variablesGroup: VariablesGroupGetDTO = GroupVariablesDescription.getEmptyDTO();
    uri: string;

    @Ref("annotationList") readonly annotationList!: AnnotationList;

    get credentials() {
        return this.$store.state.credentials;
    }

    created() {
      this.service = this.$opensilex.getService("opensilex.VariablesService");
      this.uri = decodeURIComponent(this.$route.params.uri);
      this.loadVariablesGroup(this.uri);
    }

    isDetailsTab() {
        return this.$route.path.startsWith("/variables_group/details/");
    }

    isAnnotationTab(){
        return this.$route.path.startsWith("/variables_group/annotations/");
    }

    loadVariablesGroup(uri: string) {
      this.service.getVariablesGroup(uri).then((http: HttpResponse<OpenSilexResponse<VariablesGroupGetDTO>>) => {
        this.variablesGroup = http.response.result;
      }).catch(this.$opensilex.errorHandler);
    }

    updateVariablesGroup(variableGroup){
        this.uri = variableGroup.uri;        
        this.loadVariablesGroup(this.uri);
    }
}
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
  GroupVariablesDescription:
    type: Variables group
fr:
  GroupVariablesDescription:
    type: Groupe de variables
</i18n>

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
                        class="ml-3"
                        :active="isDetailsTab()"
                        :to="{ path: '/variable/details/' + encodeURIComponent(uri) }"
                >{{ $t('component.common.details-label') }}
                </b-nav-item>

                <b-nav-item
                        class="ml-3"
                        :active="isAnnotationTab()"
                        :to="{ path: '/variable/annotations/' + encodeURIComponent(uri) }"
                >{{ $t("Annotation.list-title") }}
                </b-nav-item>

                <opensilex-Button
                        v-if="isAnnotationTab() && user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        label="Annotation.add" variant="primary" :small="false" icon="fa#edit"
                        @click="annotationModalForm.showCreateForm()"
                ></opensilex-Button>

                <opensilex-AnnotationModalForm
                        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        ref="annotationModalForm"
                        :target="uri"
                        @onCreate="updateAnnotations"
                        @onUpdate="updateAnnotations"
                ></opensilex-AnnotationModalForm>

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
                        :modificationCredentialId="credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID"
                        :deleteCredentialId="credentials.CREDENTIAL_VARIABLE_DELETE_ID"
                        @onEdit="annotationModalForm.showEditForm($event)"
                ></opensilex-AnnotationList>
            </template>

        </opensilex-PageContent>
    </div>
</template>

<script lang="ts">
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
    import VariableForm from "../form/VariableForm.vue";
    import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
    import {VariablesService} from "opensilex-core/api/variables.service";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import AnnotationModalForm from "../../annotations/form/AnnotationModalForm.vue";
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

        variable : VariableDetailsDTO = VariableForm.getEmptyForm();
        uri: string;

        @Ref("annotationModalForm") readonly annotationModalForm!: AnnotationModalForm;
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

        isAnnotationTab(){
            return this.$route.path.startsWith("/variable/annotations/");
        }

        loadVariable(uri: string) {
            this.service.getVariable(uri).then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
                this.variable = http.response.result;
            }).catch(this.$opensilex.errorHandler);
        }

        updateVariable(variable){
            this.variable = variable;
        }

        updateAnnotations(){
            this.$nextTick(() => {
                this.annotationList.refresh();
            });
        }

    }
</script>

<style scoped lang="scss">
</style>

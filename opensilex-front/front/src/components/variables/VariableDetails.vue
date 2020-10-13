<template>
    <div>

        <div class="container-fluid">
            <opensilex-PageHeader
                    icon="fa#sun"
                    title="component.menu.variables"
                    :description="variable.name"
            ></opensilex-PageHeader>

            <opensilex-PageActions :returnButton="true">
                <template v-slot>

                    <opensilex-Button
                        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        label="VariableDetails.edit" variant="primary" :small="false" icon="fa#edit"
                        @click="showEditForm"
                    ></opensilex-Button>

                    <opensilex-VariableCreate
                        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        ref="variableForm"
                        @onUpdate="loadVariable"
                    ></opensilex-VariableCreate>

                    <opensilex-Button
                        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        label="VariableDetails.edit-references" :small="false" icon="fa#globe-americas"
                        @click="skosReferences.show()"
                    ></opensilex-Button>

                    <opensilex-ExternalReferencesModalForm
                        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        ref="skosReferences"
                        :references.sync="variable"
                        @onUpdate="updateReferences"
                    ></opensilex-ExternalReferencesModalForm>
                </template>
            </opensilex-PageActions>
        </div>


        <div class="container-fluid">
            <b-row>
                <b-col>
                    <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-UriView v-if="variable && variable.uri" :uri="variable.uri" :url="variable.uri"></opensilex-UriView>
                            <opensilex-StringView label="component.common.name" :value="variable.name"></opensilex-StringView>
                            <opensilex-StringView label="VariableForm.altName" :value="variable.longName"></opensilex-StringView>
                            <opensilex-TextView label="component.common.description" :value="variable.comment"></opensilex-TextView>
                        </template>
                    </opensilex-Card>
                </b-col>
                <b-col>
                    <opensilex-Card label="VariableDetails.structure" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-UriView title="VariableView.entity"  v-if="variable.entity"
                                               :value="variable.entity.name" :uri="getEntityPageUrl()" :url="getEntityPageUrl()"></opensilex-UriView>
                            <opensilex-UriView title="VariableView.quality"  v-if="variable.quality"
                                               :value="variable.quality.name" :uri="getQualityPageUrl()" :url="getQualityPageUrl()"></opensilex-UriView>
                            <opensilex-UriView title="VariableView.method"  v-if="variable.method"
                                               :value="variable.method.name" :uri="getMethodPageUrl()" :url="getMethodPageUrl()"></opensilex-UriView>
                            <opensilex-UriView title="VariableView.unit"  v-if="variable.unit"
                                               :value="variable.unit.name" :uri="getUnitPageUrl()" :url="getUnitPageUrl()"></opensilex-UriView>
                        </template>
                    </opensilex-Card>
                </b-col>
            </b-row>
            <b-row>
                <b-col>
                    <opensilex-Card label="VariableDetails.advanced" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-StringView label="OntologyPropertyForm.data-type" :value="getDataTypeLabel(variable.dataType)"></opensilex-StringView>
                            <opensilex-StringView label="VariableForm.time-interval" :value="variable.timeInterval"></opensilex-StringView>
                            <opensilex-StringView label="VariableForm.sampling-interval" :value="variable.samplingInterval"></opensilex-StringView>

                            <opensilex-UriView  v-if="variable && variable.traitUri" title="VariableForm.trait-uri"
                                               :uri="variable.traitUri" :url="variable.traitUri"></opensilex-UriView>
                            <opensilex-StringView v-if="variable && variable.traitUri"
                                                  label="VariableForm.trait-name" :value="variable.traitName"></opensilex-StringView>
                        </template>
                    </opensilex-Card>
                </b-col>
                <b-col>
                    <opensilex-Card label="component.skos.ontologies-references-label" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-ExternalReferencesDetails :skosReferences="variable"></opensilex-ExternalReferencesDetails>
                        </template>
                    </opensilex-Card>
                </b-col>
            </b-row>
        </div>
    </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, {OpenSilexResponse} from "../../lib/HttpResponse";
    import ExternalReferencesModalForm from "../common/external-references/ExternalReferencesModalForm.vue";
    import VariableView from "./VariableView.vue";
    import VariableCreate from "./form/VariableCreate.vue";
    import { VariablesService, VariableDetailsDTO } from "opensilex-core/index";

    @Component
    export default class VariableDetails extends Vue {
        $opensilex: any;
        $store: any;
        $route: any;
        $t: any;
        $i18n: any;
        service: VariablesService;
        $router: any;


        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        variable : VariableDetailsDTO = {
            uri : undefined,
            name: undefined,
            exactMatch: [],
            closeMatch: [],
            broader: [],
            narrower: []
        };

        @Ref("variableForm") readonly variableForm!: VariableCreate;

        @Ref("skosReferences") skosReferences!: ExternalReferencesModalForm;

        created() {
            this.service = this.$opensilex.getService("opensilex.VariablesService");
            this.loadVariable(this.$route.params.uri);
        }

        loadVariable(uri: string) {
            this.service.getVariable(uri).then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
                this.variable = http.response.result;
            }).catch(this.$opensilex.errorHandler);
        }

        showEditForm() {
            // make a deep copy of the variable in order to not change the current dto
            // In case a field has been updated into the form without confirmation (by sending update to the server)
            let variableDtoCopy = JSON.parse(JSON.stringify(this.variable));
            this.variableForm.showEditForm(variableDtoCopy);
        }

        updateReferences(variable){
            let formattedVariable = VariableCreate.formatVariableBeforeUpdate(variable);

            this.service.updateVariable(formattedVariable).then(() => {
                let message = this.$i18n.t("VariableView.name") + " " + formattedVariable.uri + " " + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);
                this.skosReferences.hide();
            }).catch(this.$opensilex.errorHandler);
        }

        getDataTypeLabel(dataTypeUri: string): string{
            if(! dataTypeUri){
                return undefined;
            }
            let label = this.$t(this.$opensilex.getDatatype(dataTypeUri).labelKey);
            return label.charAt(0).toUpperCase() + label.slice(1)
        }

        getEncodedUrlPage(elementType: string , uri : string) : string{
            return "/app/variables/?elementType="+elementType +"&selected="+encodeURIComponent(uri);
        }


        getEntityPageUrl(): string{
            return this.getEncodedUrlPage(VariableView.ENTITY_TYPE,this.variable.entity.uri);
        }

        getQualityPageUrl(): string{
            return this.getEncodedUrlPage(VariableView.QUALITY_TYPE,this.variable.quality.uri);
        }

        getMethodPageUrl(): string{
            return this.getEncodedUrlPage(VariableView.METHOD_TYPE,this.variable.method.uri);
        }

        getUnitPageUrl(): string{
            return this.getEncodedUrlPage(VariableView.UNIT_TYPE,this.variable.unit.uri);
        }

    }
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    VariableDetails:
        title: Detailled variable view
        entity-name: Entity name
        quality-name: Quality name
        method-name: Method name
        unit-name: Unit name
        structure: Structure
        advanced: Advanced informations
        edit: Edit variable
        edit-references: Edit references
fr:
    VariableDetails:
        title: Vue détaillée de la variable
        entity-name: Nom d'entité
        quality-name: Nom de qualité
        method-name: Nom de méthode
        unit-name: Nom d'unité
        structure: Structure
        advanced: Informations avancées
        edit: Editer la variable
        edit-references: Editer les références
</i18n>

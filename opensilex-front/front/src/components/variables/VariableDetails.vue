<template>

    <div>
        <b-row>
            <b-col>
                <opensilex-Card label="component.common.description" icon="ik#ik-clipboard">

                    <template v-slot:rightHeader>

                        <opensilex-EditButton
                            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                            label="VariableDetails.edit" variant="outline-primary"
                            @click="showEditForm"
                        ></opensilex-EditButton>

                        <opensilex-VariableCreate
                            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                            ref="variableForm"
                            @onUpdate="$emit('onUpdate', $event)"
                        ></opensilex-VariableCreate>

                        <opensilex-InteroperabilityButton
                            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                            label="VariableDetails.edit-references"
                            @click="skosReferences.show()"
                        ></opensilex-InteroperabilityButton>

                        <opensilex-ExternalReferencesModalForm
                            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                            ref="skosReferences"
                            :references.sync="variable"
                            @onUpdate="update"
                        ></opensilex-ExternalReferencesModalForm>

                        <opensilex-DeleteButton
                            v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_DELETE_ID)"
                            @click="deleteVariable"
                            label="component.common.list.buttons.delete"
                        ></opensilex-DeleteButton>

                    </template>

                    <template v-slot:body>
                        <opensilex-UriView v-if="variable && variable.uri" :uri="variable.uri"
                                           :url="variable.uri"></opensilex-UriView>
                        <opensilex-StringView label="component.common.name"
                                              :value="variable.name"></opensilex-StringView>
                        <opensilex-StringView label="VariableForm.altName"
                                              :value="variable.alternative_name"></opensilex-StringView>
                        <opensilex-TextView label="component.common.description"
                                            :value="variable.description"></opensilex-TextView>
                    </template>
                </opensilex-Card>
            </b-col>
            <b-col>
                <opensilex-Card label="VariableDetails.structure" icon="ik#ik-clipboard">
                    <template v-slot:body>
                        <opensilex-UriView title="VariableView.entity" v-if="variable.entity"
                                           :value="variable.entity.name" :uri="variable.entity.uri"
                                           :url="getEntityPageUrl()">
                        </opensilex-UriView>
                        <opensilex-UriView title="VariableView.characteristic" v-if="variable.characteristic"
                                           :value="variable.characteristic.name" :uri="variable.characteristic.uri"
                                           :url="getCharacteristicPageUrl()">
                        </opensilex-UriView>
                        <opensilex-UriView title="VariableView.method" v-if="variable.method"
                                           :value="variable.method.name" :uri="variable.method.uri"
                                           :url="getMethodPageUrl()">
                        </opensilex-UriView>
                        <opensilex-UriView title="VariableView.unit" v-if="variable.unit"
                                           :value="variable.unit.name" :uri="variable.unit.uri"
                                           :url="getUnitPageUrl()">
                        </opensilex-UriView>
                    </template>
                </opensilex-Card>
            </b-col>
        </b-row>
        <b-row>
            <b-col>
                <opensilex-Card label="component.skos.ontologies-references-label" icon="ik#ik-clipboard">
                    <template v-slot:body>
                        <opensilex-ExternalReferencesDetails
                            :skosReferences="variable">
                        </opensilex-ExternalReferencesDetails>
                    </template>
                </opensilex-Card>
            </b-col>
            <b-col>
                <opensilex-Card label="VariableDetails.advanced" icon="ik#ik-clipboard">
                    <template v-slot:body>
                        <opensilex-StringView label="OntologyPropertyForm.data-type"
                                              :value="getDataTypeLabel(variable.datatype)"></opensilex-StringView>
                        <opensilex-StringView label="VariableForm.time-interval"
                                              :value="variable.time_interval"></opensilex-StringView>
                        <opensilex-StringView label="VariableForm.sampling-interval"
                                              :value="variable.sampling_interval"></opensilex-StringView>

                        <opensilex-UriView v-if="variable && variable.trait" title="VariableForm.trait-uri"
                                           :uri="variable.trait" :url="variable.trait"></opensilex-UriView>
                        <opensilex-StringView v-if="variable && variable.trait"
                                              label="VariableForm.trait-name"
                                              :value="variable.trait_name"></opensilex-StringView>
                    </template>
                </opensilex-Card>
            </b-col>
        </b-row>
    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import ExternalReferencesModalForm from "../common/external-references/ExternalReferencesModalForm.vue";
import VariablesView from "./VariablesView.vue";
import VariableCreate from "./form/VariableCreate.vue";
import { VariablesService, VariableDetailsDTO } from "opensilex-core/index";
import VariableForm from "./form/VariableForm.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";

@Component
export default class VariableDetails extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;
    $route: any;
    $router: any;
    $t: any;
    $i18n: any;
    service: VariablesService;

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    @Prop({
        default: () => VariableForm.getEmptyForm()
    }) variable : VariableDetailsDTO ;

    @Ref("variableForm") readonly variableForm!: VariableCreate;

    @Ref("skosReferences") skosReferences!: ExternalReferencesModalForm;

    created() {
        this.service = this.$opensilex.getService("opensilex.VariablesService");
    }


    showEditForm() {
        // make a deep copy of the variable in order to not change the current dto
        // In case a field has been updated into the form without confirmation (by sending update to the server)
        let variableDtoCopy = JSON.parse(JSON.stringify(this.variable));
        this.variableForm.showEditForm(variableDtoCopy);
    }

    update(variable){
        let formattedVariable = VariableCreate.formatVariableBeforeUpdate(variable);

        this.service.updateVariable(formattedVariable).then(() => {
            let message = this.$i18n.t("VariableView.name") + " " + formattedVariable.name + " " + this.$i18n.t("component.common.success.update-success-message");
            this.$opensilex.showSuccessToast(message);
            this.$emit("onUpdate", variable);
        }).catch(this.$opensilex.errorHandler);
    }

    deleteVariable(){
        this.service.deleteVariable(this.variable.uri).then(() => {
            let message = this.$i18n.t("VariableView.name") + " " + this.variable.name + " " + this.$i18n.t("component.common.success.delete-success-message");
            this.$opensilex.showSuccessToast(message);
            this.$router.push({path: "/variables"});
        }).catch(this.$opensilex.errorHandler);
    }

    getDataTypeLabel(dataTypeUri: string): string{
        if(! dataTypeUri){
            return undefined;
        }
        let label = this.$t(this.$opensilex.getDatatype(dataTypeUri).label_key);
        return label.charAt(0).toUpperCase() + label.slice(1)
    }

    getEncodedUrlPage(elementType: string , uri : string) : string{
        return  this.$opensilex.getURL("variables/?elementType="+elementType +"&selected="+encodeURIComponent(uri));
    }


    getEntityPageUrl(): string{
        return this.getEncodedUrlPage(VariablesView.ENTITY_TYPE,this.variable.entity.uri);
    }

    getCharacteristicPageUrl(): string{
        return this.getEncodedUrlPage(VariablesView.CHARACTERISTIC_TYPE,this.variable.characteristic.uri);
    }

    getMethodPageUrl(): string{
        return this.getEncodedUrlPage(VariablesView.METHOD_TYPE,this.variable.method.uri);
    }

    getUnitPageUrl(): string{
        return this.getEncodedUrlPage(VariablesView.UNIT_TYPE,this.variable.unit.uri);
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
        characteristic-name: Characteristic name
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
        characteristic-name: Nom de la caractéristique
        method-name: Nom de méthode
        unit-name: Nom d'unité
        structure: Structure
        advanced: Informations avancées
        edit: Editer la variable
        edit-references: Editer les références
    </i18n>

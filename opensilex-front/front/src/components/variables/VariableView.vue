<template>
    <div class="container-fluid">
        <opensilex-PageHeader
                icon="ik#ik-layers"
                title="component.menu.variables"
                description="Variables.description"
        ></opensilex-PageHeader>
        <opensilex-PageActions
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
        >
            <template v-slot>
                <opensilex-CreateButton
                        @click="variableForm.showCreateForm()"
                        label="Variables.add"></opensilex-CreateButton>
            </template>
        </opensilex-PageActions>

        <opensilex-PageContent>
            <template v-slot>
                <opensilex-VariableList
                        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_READ_ID)"
                        ref="variableList"
                        @onEdit="showEditForm"
                        @onDetails="showDetails"
                        @onInteroperability="showSkosReferences"
                        @onDelete="callDeleteService"
                ></opensilex-VariableList>
            </template>
        </opensilex-PageContent>

        <opensilex-VariableForm
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                ref="variableForm"
                @onCreate="callCreateService"
                @onUpdate="callUpdateService"
        ></opensilex-VariableForm>

        <opensilex-ExternalReferencesModalForm
                ref="skosReferences"
                :references.sync="selectedVariable"
                @onUpdate="callUpdateService"
        ></opensilex-ExternalReferencesModalForm>
    </div>
</template>

<script lang="ts">
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {VariablesService} from "opensilex-core/api/variables.service";
    import {VariableDetailsDTO} from "opensilex-core/model/variableDetailsDTO";
    import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
    import {VariableCreationDTO} from "opensilex-core/model/variableCreationDTO";
    import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";
    import ExternalReferencesModalForm from "../common/external-references/ExternalReferencesModalForm.vue";

    @Component
    export default class VariableView extends Vue {
        $opensilex: any;
        $store: any;
        service: VariablesService;
        $t: any;
        $i18n: any;
        $router: any;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        @Ref("modalRef") readonly modalRef!: any;

        @Ref("variableForm") readonly variableForm!: any;

        @Ref("variableList") readonly variableList!: any;

        @Ref("skosReferences") skosReferences!: ExternalReferencesModalForm;

        created() {
            this.service = this.$opensilex.getService("opensilex.VariablesService");
        }

        selectedVariable: VariableDetailsDTO = {
            uri: undefined,
            exactMatch: [],
            closeMatch: [],
            broader: [],
            narrower: []
        };

        callCreateService(form: VariableCreationDTO) {
            this.service.createVariable(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    let uri = http.response.result;
                    this.variableList.refresh();
                    let message = this.$i18n.t("Variables.name") + " " + uri + " " + this.$i18n.t("component.common.success.creation-success-message");
                    this.$opensilex.showSuccessToast(message);
                })
                .catch(this.$opensilex.errorHandler);
        }

        callUpdateService(form) {

            if (form.entity.uri) {
                form.entity = form.entity.uri;
            }
            if (form.quality.uri) {
                form.quality = form.quality.uri;
            }
            if (form.method.uri) {
                form.method = form.method.uri;
            }
            if (form.unit.uri) {
                form.unit = form.unit.uri;
            }

            this.service.updateVariable(form)
                .then(() => {
                    this.variableList.refresh();
                    let message = this.$i18n.t("Variables.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.update-success-message");
                    this.$opensilex.showSuccessToast(message);
                })
                .catch(this.$opensilex.errorHandler);

        }

        callDeleteService(uri: string) {
            this.service.deleteVariable(uri)
                .then(() => {
                    this.variableList.refresh();
                    let message = this.$i18n.t("Variables.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
                    this.$opensilex.showSuccessToast(message);
                })
                .catch(this.$opensilex.errorHandler);
        }

        showCreateForm() {
            this.variableForm.showCreateForm();
        }

        showSkosReferences(uri: string) {
            this.service
                .getVariable(uri)
                .then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
                    let result = http.response.result;
                    if (result instanceof Promise) {
                        result.then(resolve => {
                            this.selectedVariable = result;
                            this.skosReferences.show();
                        });
                    } else {
                        this.selectedVariable = result;
                        this.skosReferences.show();
                    }
                })
                .catch(this.$opensilex.errorHandler);
        }

        showEditForm(uri: string) {
            this.service
                .getVariable(uri)
                .then((http: HttpResponse<OpenSilexResponse<VariableDetailsDTO>>) => {
                    this.variableForm.showEditForm(http.response.result);
                })
                .catch(this.$opensilex.errorHandler);
        }

        showDetails(uriResult: any) {
            uriResult.then(uri => {
                this.$router.push({path: "/variable/" + encodeURIComponent(uri)});
            });
        }

    }
</script>

<style scoped lang="scss">
</style>

<i18n>
en:
    Variables:
        name: The variable
        description: Manage and configure variables
        add: Create a variable
        edit: Edit a variable
        delete : Delete a variable
        label-filter: Search by name
        label-filter-placeholder: Enter a name
        entity: Entity
        quality: Quality
        method: Method
        unit: Unit
fr:
    Variables:
        name: La variable
        description: Gérer et configurer les variables
        add: Créer une variable
        edit: Éditer une variable
        delete : Supprimer une variable
        label-filter: Rechercher par nom
        label-filter-placeholder: Entrez un nom
        entity: Entité
        quality: Qualité
        method: Méthode
        unit: Unité
</i18n>


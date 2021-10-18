<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="UnitForm.add"
            editTitle="UnitForm.edit"
            icon="fa#vials"
            modalSize="lg"
            :initForm="getEmptyForm"
            :createAction="create"
            :updateAction="update"
            :static="false"
    >
        <template v-slot:icon></template>
    </opensilex-WizardForm>
</template>


<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {ExternalOntologies} from "../../../models/ExternalOntologies";
    
    import { VariablesService, UnitGetDTO, UnitCreationDTO, ObjectUriResponse } from "opensilex-core/index";
    import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

    @Component
    export default class UnitCreate extends Vue {

        steps = [
            {component: "opensilex-UnitForm"}
            ,{component : "opensilex-UnitExternalReferencesForm"}
        ];

        static selectedOntologies: string[] = [
            ExternalOntologies.AGROVOC,
            ExternalOntologies.AGROPORTAL,
            ExternalOntologies.BIOPORTAL,
            ExternalOntologies.QUDT,
            ExternalOntologies.UNIT_OF_MEASURE,
            ExternalOntologies.UNIT_OF_MEASUREMENT,
        ];

        title = "";
        uriGenerated = true;
        editMode = false;
        errorMsg: String = "";
        service: VariablesService;

        @Ref("wizardRef") readonly wizardRef!: any;

        created(){
            this.service = this.$opensilex.getService("opensilex.VariablesService");
        }

        handleErrorMessage(errorMsg: string) {
            this.errorMsg = errorMsg;
        }

        showCreateForm() {
            this.wizardRef.showCreateForm();
        }

        showEditForm(form : UnitGetDTO){
            this.wizardRef.showEditForm(form);
        }

        $opensilex: any;

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        getEmptyForm(): UnitCreationDTO {
            return {
                uri: null,
                name: null,
                description: null,
                symbol: null,
                alternative_symbol: null,
                exact_match: [],
                close_match: [],
                broad_match: [],
                narrow_match: []
            };
        }

        create(form){
            return this.service
                .createUnit(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("UnitForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
                    this.$opensilex.showSuccessToast(message);
                    this.$emit("onCreate", form);
                })
                .catch(error => {
                    if (error.status == 409) {
                        this.$opensilex.errorHandler(error, this.$i18n.t("component.common.errors.uri-already-exists"));
                    } else {
                        this.$opensilex.errorHandler(error);
                    }
                });
        }

        update(form){
            return this.service
                .updateUnit(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("UnitForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.update-success-message");
                    this.$opensilex.showSuccessToast(message);
                    this.$emit("onUpdate", form);
                })
                .catch(error => {
                    this.$opensilex.errorHandler(error);
                });
        }

        loadingWizard: boolean = false;

        setLoading(value: boolean) {
            this.loadingWizard = value;
        }

    }

</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    UnitForm:
        uri-help: "Uncheck this checkbox if you want to insert a concept from an existing ontology or if want to set a particular URI. Let it checked if you want to create a new unit with an auto-generated URI"
        ontologies-help: "Click on one of these reference ontologies. If an unit matches with the desired unit, uncheck the checkbox 'URI' and copy the corresponding URI in the 'URI' field. Also copy the name to the 'Name' field."
        name: The unit
        add: Add an unit
        edit: Edit an unit
        symbol: Symbol
        alternative-symbol: Alternative symbol
        name-placeholder: Kilogramm per hectare
        symbol-placeholder: kg ha-1
        alternative-symbol-placeholder: kg/ha
fr:
    UnitForm:
        uri-help: "Décocher si vous souhaitez ajouter une unité à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une unité avec une URI auto-generée"
        ontologies-help: "Cliquer sur une de ces ontologies de référence. Si une unité correspond à celle recherchée, décocher la checkbox 'URI' et copier l'URI correspondante dans le champ 'URI'. Copier aussi le nom de l'unité dans le champ 'Nom'."
        name: L'unité
        add: Ajouter une unité
        edit: Éditer une unité
        symbol: Symbole
        alternative-symbol: Symbole alternatif
        name-placeholder: Kilogramme par hectare
        symbol-placeholder: kg ha-1
        alternative-symbol-placeholder: kg/ha
</i18n>
<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="DimensionForm.add"
            editTitle="DimensionForm.edit"
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
    // @ts-ignore
    import { VariablesService, DimensionGetDTO, DimensionCreationDTO, ObjectUriResponse } from "opensilex-core/index";
    import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

    @Component
    export default class DimensionCreate extends Vue {

        steps = [
            {component: "opensilex-DimensionForm"}
            ,{component : "opensilex-DimensionExternalReferencesForm"}
        ];

        static selectedOntologies: string[] = [
            ExternalOntologies.AGROVOC,
            ExternalOntologies.AGROPORTAL,
            ExternalOntologies.BIOPORTAL,
            ExternalOntologies.CROP_ONTOLOGY,
            ExternalOntologies.PLANTEOME,
            ExternalOntologies.PLANT_ONTOLOGY
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

        showEditForm(form : DimensionGetDTO) {
            this.wizardRef.showEditForm(form);
        }

        $opensilex: any;

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        getEmptyForm(): DimensionCreationDTO {
            return {
                uri: null,
                name: null,
                description: null,
                datatype: null,
                exact_match: [],
                close_match: [],
                broad_match: [],
                narrow_match: []
            };
        }

        create(form){
            return this.service
                .createDimension(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("DimensionForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
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
                .updateDimension(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("DimensionForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.update-success-message");
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
    DimensionForm:
        uri-help: "Uncheck this checkbox if you want to insert a dimension from an existing ontology or if you want to set a particular URI. Let it checked if you want to create a new dimension with an auto-generated URI"
        ontologies-help: "Click on one of these reference ontologies. If an dimension matches with the desired dimension, uncheck the checkbox 'URI' and copy the corresponding URI in the 'URI' field. Also copy the name to the 'Name' field."
        name: The dimension
        add: Add dimension
        edit: Edit dimension
        datatype: Datatype
fr:
    DimensionForm:
        uri-help: "Décocher si vous souhaitez ajouter une dimension à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une dimension avec une URI auto-générée"
        ontologies-help: "Cliquer sur une de ces ontologies de référence. Si une dimension correspond à celle recherchée, décocher la checkbox 'URI' et copier l'URI correspondante dans le champ 'URI'. Copier aussi le nom de la dimension dans le champ 'Nom'."
        name: La dimension
        add: Ajouter une dimension
        edit: Éditer une dimension
        datatype: Type de données
</i18n>
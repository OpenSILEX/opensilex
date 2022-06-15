<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="InterestEntityForm.add"
            editTitle="InterestEntityForm.edit"
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
    import {Component, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {ExternalOntologies} from "../../../models/ExternalOntologies";
    // @ts-ignore
    import { VariablesService, InterestEntityGetDTO, InterestEntityCreationDTO, ObjectUriResponse } from "opensilex-core/index";
    import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

    @Component
    export default class InterestEntityCreate extends Vue {

        steps = [
            {component: "opensilex-InterestEntityForm"}
            ,{component : "opensilex-InterestEntityExternalReferencesForm"}
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

        showEditForm(form : InterestEntityGetDTO) {
            this.wizardRef.showEditForm(form);
        }

        $opensilex: any;

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        getEmptyForm(): InterestEntityCreationDTO {
            return {
                uri: null,
                name: null,
                description: null,
                exact_match: [],
                close_match: [],
                broad_match: [],
                narrow_match: []
            };
        }

        create(form){
            return this.service
                .createInterestEntity(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("InterestEntityForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
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
                .updateInterestEntity(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("InterestEntityForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.update-success-message");
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
    InterestEntityForm:
        uri-help: "Uncheck this checkbox if you want to insert an entity of interest from an existing ontology or if you want to set a particular URI. Let it checked if you want to create a new entity of interest with an auto-generated URI"
        ontologies-help: "Click on one of these reference ontologies. If an entity of interest matches with the desired one, uncheck the checkbox 'URI' and copy the corresponding URI in the 'URI' field. Also copy the name to the 'Name' field."
        name: The observation level
        add: Add an observation level
        edit: Edit an observation level
        name-placeholder: Plot
fr:
    InterestEntityForm:
        uri-help: "Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une entité avec une URI auto-générée"
        ontologies-help: "Cliquer sur une de ces ontologies de référence. Si une entité d'intérêt correspond à celle recherchée, décocher la checkbox 'URI' et copier l'URI correspondante dans le champ 'URI'. Copier aussi le nom de l'entité d'intérêt dans le champ 'Nom'."
        name: Le niveau d'observation
        add: Ajouter un niveau d'observation
        edit: Éditer un niveau d'observation
        name-placeholder: Parcelle
</i18n>
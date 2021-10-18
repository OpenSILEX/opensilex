<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="CharacteristicForm.add"
            editTitle="CharacteristicForm.edit"
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
    
    import { VariablesService, CharacteristicGetDTO, CharacteristicCreationDTO, ObjectUriResponse } from "opensilex-core/index";
    import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

    @Component
    export default class CharacteristicModalForm extends Vue {

        steps = [
            {component: "opensilex-CharacteristicForm"}
            ,{component: "opensilex-CharacteristicExternalReferencesForm"}
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

        showEditForm(form : CharacteristicGetDTO) {
            this.wizardRef.showEditForm(form);
        }

        $opensilex: any;

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        getEmptyForm(): CharacteristicCreationDTO {
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
            if(form.type){
                form.type = form.type.uri;
            }
            return this.service
                .createCharacteristic(form)
                .then((http: HttpResponse<OpenSilexResponse>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("CharacteristicForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
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
                .updateCharacteristic(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("CharacteristicForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.update-success-message");
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
    CharacteristicForm:
        uri-help: "Uncheck this checkbox if you want to insert a concept from an existing ontology or if want to set a particular URI. Let it checked if you want to create a new characteristic with an auto-generated URI"
        ontologies-help: "Click on one of these reference ontologies. If a characteristic matches with the desired characteristic, uncheck the checkbox 'URI' and copy the corresponding URI in the 'URI' field. Also copy the name to the 'Name' field."
        name: The characteristic
        add: Add a characteristic
        edit: Edit a characteristic
        name-placeholder: Height
fr:
    CharacteristicForm:
        uri-help: "Décocher si vous souhaitez ajouter une caractéristique à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une caractéristique avec une URI auto-generée"
        ontologies-help: "Cliquer sur une de ces ontologies de référence. Si une caractéristique correspond à celle recherchée, décocher la checkbox 'URI' et copier l'URI correspondante dans le champ 'URI'. Copier aussi le nom de la caractéristique dans le champ 'Nom'."
        name: La caractéristique
        add: Ajouter une caractéristique
        edit: Éditer une caractéristique
        name-placeholder: Hauteur
</i18n>
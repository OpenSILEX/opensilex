<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="EntityForm.add"
            editTitle="EntityForm.edit"
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
import {EntityCreationDTO, EntityGetDTO, VariablesService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {EntityUpdateDTO} from "opensilex-core/model/entityUpdateDTO";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import {EntityDetailsDTO} from "opensilex-core/model/entityDetailsDTO";

@Component
    export default class AgroportalEntityCreate extends Vue {

        steps = [
            {component: "opensilex-AgroportalEntityForm"}
            ,{component : "opensilex-AgroportalEntityEnrichForm"}
            ,{component : "opensilex-AgroportalEntityExternalReferencesForm"}
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

        showEditForm(form : EntityGetDTO) {
            this.wizardRef.showEditForm(form);
        }

        $opensilex: any;

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        getEmptyForm(): EntityAgroportalDTO {
            return {
                id: null,
                name: null,
                synonym: null,
                definitions: [],
                obsolete: null,
                type: null,
                links: null,
            };
        }

        create(form: EntityCreationDTO){
            return this.service
                .createEntity(form)
                .then((http: HttpResponse<OpenSilexResponse<string>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("EntityForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
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

        update(form: EntityUpdateDTO){
            return this.service
                .updateEntity(form)
                .then((http: HttpResponse<OpenSilexResponse<string>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("EntityForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.update-success-message");
                    this.$opensilex.showSuccessToast(message);
                    this.$emit("onUpdate", form);
                })
                .catch(error => {
                    this.$opensilex.errorHandler(error);
                });
        }

        private convertToEntityDTO(entity: EntityAgroportalDTO) : EntityDetailsDTO {
          let dto: EntityDetailsDTO = {};

          dto.uri = entity.id;
          dto.name = entity.name;
          if (entity.definitions.length > 0) {
            dto.description = entity.definitions[0];
          }

          return dto;
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
    AgroportalEntityForm:
        uri-help: "Uncheck this checkbox if you want to insert a concept from an existing ontology or if want to set a particular URI. Let it checked if you want to create a new entity with an auto-generated URI"
        ontologies-help: "Click on one of these reference ontologies. If an entity matches with the desired entity, uncheck the checkbox 'URI' and copy the corresponding URI in the 'URI' field. Also copy the name to the 'Name' field."
        name: The entity
        add: Add entity
        edit: Edit entity
        name-placeholder: Plant
        search-for-ontology-term: Search for ontology term
fr:
    AgroportalEntityForm:
        uri-help: "Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une entité avec une URI auto-générée"
        ontologies-help: "Cliquer sur une de ces ontologies de référence. Si une entité correspond à celle recherchée, décocher la checkbox 'URI' et copier l'URI correspondante dans le champ 'URI'. Copier aussi le nom de l'entité dans le champ 'Nom'."
        name: L'entité
        add: Ajouter une entité
        edit: Éditer une entité
        name-placeholder: Plante
        search-for-ontology-term: Rechercher un terme
</i18n>
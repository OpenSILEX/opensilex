<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="AgroportalEntityForm.add"
            editTitle="AgroportalEntityForm.edit"
            icon="fa#vials"
            modalSize="xl"
            :initForm="getEmptyForm"
            :createAction="create"
            :updateAction="update"
            :convertAction="convert"
            :static="false"
            :nextStepAction="nextStep"
            :customValidation="validateCustom"
            :isBlockingStep="false"
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
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";

@Component
    export default class AgroportalEntityCreate extends Vue {

        $opensilex: OpenSilexVuePlugin;

        entityService: VariablesService;
        agroportalService: AgroportalAPIService;

        static selectedOntologies: string[] = [
            ExternalOntologies.AGROVOC,
            ExternalOntologies.AGROPORTAL,
            ExternalOntologies.BIOPORTAL,
            ExternalOntologies.CROP_ONTOLOGY,
            ExternalOntologies.PLANTEOME,
            ExternalOntologies.PLANT_ONTOLOGY
        ];

        steps = [
            {component: "opensilex-AgroportalEntityForm",
              title: "AgroportalEntityForm.step1-title",
              finish: "AgroportalEntityForm.import-and-save",
              next: "AgroportalEntityForm.enrich",
              props: {
                ontologiesConfig: "entityOntologies"
              }
            }
            ,{component : "opensilex-AgroportalEntityEnrichForm",
              title: "AgroportalEntityForm.step2-title",
              finish: "AgroportalEntityForm.save"
            }
            ,{component : "opensilex-AgroportalEntityExternalReferencesForm",
              title: "AgroportalEntityForm.step3-title",
              props: {
                ontologiesConfig: "entityOntologies"
              }
            }
        ];

        title = "";
        uriGenerated = true;
        editMode = false;
        errorMsg: String = "";

        @Ref("wizardRef") readonly wizardRef!: any;

        checkAgroportalReachable() {
          return this.agroportalService.pingAgroportal(1000).then((http) => {
            if (http && http.response) {
              let isReachable = http.response.result;
              if (!isReachable) {
                this.wizardRef.skipStep();
              }
            }
          });
        }

        created(){
            this.entityService =
                this.$opensilex
                    .getService<VariablesService>("opensilex.VariablesService");
            this.agroportalService =
                this.$opensilex
                    .getService<AgroportalAPIService>("opensilex.AgroportalAPIService");
        }

        handleErrorMessage(errorMsg: string) {
            this.errorMsg = errorMsg;
        }

        showCreateForm() {
            this.checkAgroportalReachable();
            this.editMode = false;
            this.wizardRef.showCreateForm();
        }

        showEditForm(form : EntityGetDTO) {
            this.checkAgroportalReachable();
            this.editMode = true;
            this.wizardRef.showEditForm(form);
        }


        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        getEmptyForm(): EntityCreationDTO {
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

        create(form: EntityCreationDTO){
            return this.entityService
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

        update(form: EntityUpdateDTO) {
            return this.entityService
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

        convert(form, entity: EntityAgroportalDTO) {
            if (!this.editMode) {
              form.uri = entity.id;
            }
            form.name = entity.name;
            form.description = entity.definitions[0];
            form.exact_match = [];
            form.narrow_match = [];
            form.broad_match = [];
            form.close_match = [];
            return form;
        }

        validate() {
          return true;
        }

        validateCustom(form) {
          if (form.name != null) {
            return true;
          }
          return false;
        }

        nextStep(stepIndex, form, nextStepComponent, currentStepComponent) {
          if(stepIndex == 0 && form.uri != null) {
            if(this.editMode) return true;

            form.close_match.push(form.uri);
            form.uri = "";
            return true;
          }
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
        selected-term: Selected term
        step1-title: Search
        step2-title: Enrich
        step3-title: Mapping
        import-and-save: Import & Save
        save: Save
        enrich: Enrich
        skip: Skip
        no-selected-item: No selected term
fr:
    AgroportalEntityForm:
        uri-help: "Décocher si vous souhaitez ajouter une entité à partir d'une ontologie existante ou si vous souhaitez spécifier une URI particulière. Laisser coché si vous souhaitez ajouter une entité avec une URI auto-générée"
        ontologies-help: "Cliquer sur une de ces ontologies de référence. Si une entité correspond à celle recherchée, décocher la checkbox 'URI' et copier l'URI correspondante dans le champ 'URI'. Copier aussi le nom de l'entité dans le champ 'Nom'."
        name: L'entité
        add: Ajouter une entité
        edit: Éditer une entité
        name-placeholder: Plante
        search-for-ontology-term: Rechercher un terme
        selected-term: Terme sélectionné
        step1-title: Chercher
        step2-title: Enrichir
        step3-title: Mapper
        import-and-save: Importer & Enregistrer
        save: Enregistrer
        enrich: Enrichir
        skip: Passer
        no-selected-item: Aucun terme sélectionné
</i18n>
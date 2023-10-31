<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="AgroportalCharacteristicCreate.add"
            editTitle="AgroportalCharacteristicCreate.edit"
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
import {VariablesService} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";
import {CharacteristicUpdateDTO} from "opensilex-core/model/characteristicUpdateDTO";
import {CharacteristicCreationDTO} from "opensilex-core/model/characteristicCreationDTO";
import {CharacteristicGetDTO} from "opensilex-core/model/characteristicGetDTO";

@Component
    export default class AgroportalCharacteristicCreate extends Vue {

        $opensilex: OpenSilexVuePlugin;

        entityService: VariablesService;
        agroportalService: AgroportalAPIService;

        steps = [
            {component: "opensilex-AgroportalEntityForm",
              title: "AgroportalEntityForm.step1-title",
              finish: "AgroportalEntityForm.import-and-save",
              next: "AgroportalEntityForm.enrich",
              props: {
                ontologiesConfig: "traitOntologies",
                searchPlaceholder: "AgroportalCharacteristicCreate.name-placeholder"
              }
            }
            ,{component : "opensilex-AgroportalEntityEnrichForm",
              title: "AgroportalEntityForm.step2-title",
              finish: "AgroportalEntityForm.save",
              props: {
                namePlaceholder: "AgroportalCharacteristicCreate.name-placeholder"
              }
            }
            ,{component : "opensilex-AgroportalEntityExternalReferencesForm",
              title: "AgroportalEntityForm.step3-title",
              props: {
                ontologiesConfig: "traitOntologies",
                searchPlaceholder: "AgroportalCharacteristicCreate.name-placeholder"
              }
            }
        ];

        title = "";
        uriGenerated = true;
        editMode = false;
        errorMsg: String = "";

        @Ref("wizardRef") readonly wizardRef!: any;
        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

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
            this.wizardRef.showCreateForm();
        }

        showEditForm(form : CharacteristicGetDTO) {
            this.wizardRef.showEditForm(form);
        }
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

        create(form: CharacteristicCreationDTO){
            return this.entityService
                .createCharacteristic(form)
                .then((http: HttpResponse<OpenSilexResponse<string>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("AgroportalCharacteristicCreate.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
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

        update(form: CharacteristicUpdateDTO) {
            return this.entityService
                .updateCharacteristic(form)
                .then((http: HttpResponse<OpenSilexResponse<string>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("AgroportalCharacteristicCreate.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.update-success-message");
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
  AgroportalCharacteristicCreate:
    name: The characteristic
    add: Add a characteristic
    edit: Edit a characteristic
    name-placeholder: Height
fr:
  AgroportalCharacteristicCreate:
    name: La caractéristique
    add: Ajouter une caractéristique
    edit: Éditer une caractéristique
    name-placeholder: Hauteur
</i18n>
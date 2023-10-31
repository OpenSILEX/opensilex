<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="AgroportalMethodCreate.add"
            editTitle="AgroportalMethodCreate.edit"
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
import {MethodCreationDTO} from "opensilex-core/model/methodCreationDTO";
import {MethodUpdateDTO} from "opensilex-core/model/methodUpdateDTO";
import {MethodGetDTO} from "opensilex-core/model/methodGetDTO";

@Component
    export default class AgroportalMethodCreate extends Vue {

        $opensilex: OpenSilexVuePlugin;

        entityService: VariablesService;
        agroportalService: AgroportalAPIService;

        steps = [
            {component: "opensilex-AgroportalEntityForm",
              title: "AgroportalEntityForm.step1-title",
              finish: "AgroportalEntityForm.import-and-save",
              next: "AgroportalEntityForm.enrich",
              props: {
                ontologiesConfig: "methodOntologies",
                searchPlaceholder: "AgroportalMethodCreate.name-placeholder"
              }
            }
            ,{component : "opensilex-AgroportalEntityEnrichForm",
              title: "AgroportalEntityForm.step2-title",
              finish: "AgroportalEntityForm.save",
              props: {
                namePlaceholder: "AgroportalMethodCreate.name-placeholder"
              }
            }
            ,{component : "opensilex-AgroportalEntityExternalReferencesForm",
              title: "AgroportalEntityForm.step3-title",
              props: {
                ontologiesConfig: "methodOntologies",
                searchPlaceholder: "AgroportalMethodCreate.name-placeholder"
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
            this.wizardRef.showCreateForm();
        }

        showEditForm(form : MethodGetDTO) {
            this.wizardRef.showEditForm(form);
        }


        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        getEmptyForm(): MethodCreationDTO {
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

        create(form: MethodCreationDTO){
            return this.entityService
                .createMethod(form)
                .then((http: HttpResponse<OpenSilexResponse<string>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("AgroportalMethodCreate.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
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

        update(form: MethodUpdateDTO) {
            return this.entityService
                .updateMethod(form)
                .then((http: HttpResponse<OpenSilexResponse<string>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("AgroportalMethodCreate.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.update-success-message");
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
  AgroportalMethodCreate:
    name: The method
    add: Add a method
    edit: Edit a method
    name-placeholder: Image analysis
fr:
  AgroportalMethodCreate:
    name: La méthode
    add: Ajouter une méthode
    edit: Éditer une méthode
    name-placeholder: Analyse d'image
</i18n>
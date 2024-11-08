<template>
  <opensilex-WizardForm
      ref="wizardRef"
      :steps="steps"
      :createTitle="createTitle"
      :editTitle="editTitle"
      :icon="icon"
      modalSize="xl"
      :initForm="getEmptyForm"
      :createAction="create"
      :updateAction="update"
      :convertAction="convert"
      :static="false"
      :nextStepAction="nextStep"
      :validateAction="validateCustom"
      :isBlockingStep="false"
      @agroportalTermSelected="handleTermSelected"
      @agroportalTermUnselected="handleTermUnselected"
  >
    <template v-slot:createAdditionalFields="scope">
      <slot name="createAdditionalFields" v-bind="scope"></slot>
    </template>
    <template v-slot:icon></template>
  </opensilex-WizardForm>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import OpenSilexVuePlugin from "../../../../../models/OpenSilexVuePlugin";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import {Prop, Ref} from "vue-property-decorator";
import WizardForm, {WizardFormStep} from "../../../forms/WizardForm.vue";
import HttpResponse, {OpenSilexResponse} from "../../../../../lib/HttpResponse";
import {BaseExternalReferencesDTO, BaseExternalReferencesForm} from "../../ExternalReferencesTypes";
import {AgroportalTermDTO} from "opensilex-core/model/agroportalTermDTO";
import AgroportalSearchFormPart from "./AgroportalSearchFormPart.vue";

@Component({})
export default class AgroportalCreateForm<T extends BaseExternalReferencesDTO> extends Vue implements BaseExternalReferencesForm {
  //#region Plugins and services
  private readonly $opensilex: OpenSilexVuePlugin;
  private agroportalService: AgroportalAPIService;
  //#endregion

  //#region Props
  @Prop()
  private readonly ontologiesConfig: string;

  @Prop({default: false})
  private readonly requireCreate: boolean;

  @Prop()
  private readonly searchPlaceholder: string;

  @Prop()
  private readonly descriptionPlaceholder: string;

  @Prop()
  private readonly createTitle: string;

  @Prop()
  private readonly editTitle: string;

  @Prop({
    default: "fa#vials"
  })
  private readonly icon: string;

  @Prop()
  createMethod: (form: T) => Promise<HttpResponse<OpenSilexResponse<string>>>;

  @Prop()
  updateMethod: (form: T) => Promise<HttpResponse<OpenSilexResponse<string>>>;

  @Prop({
    default: () => {
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
  })
  private readonly emptyForm: T;
  //#endregion

  //#region Refs
  @Ref("wizardRef")
  private readonly wizardRef!: WizardForm;
  //#endregion

  //#region Data
  private editMode = false;
  private termIsSelected: boolean = false;
  //#endregion

  //#region Computed
  private handleTermSelected() {
    this.termIsSelected = true;
  }

  private handleTermUnselected(){
    this.termIsSelected = false;
  }

  /**
   * Steps of the wizard form. Computed so that it can rely on props such as `ontologiesConfig` and
   * `searchPlaceholder`.
   *
   * @private
   */
  private get steps(): Array<WizardFormStep> {
    return [
      {
        component: "opensilex-AgroportalSearchFormPart",
        title: "AgroportalSearchFormPart.step1-title",
        finish: this.requireCreate ? undefined : "AgroportalSearchFormPart.reuse",
        next: this.termIsSelected ? "AgroportalSearchFormPart.create" : "AgroportalSearchFormPart.createNew",
        props: {
          ontologiesConfig: this.ontologiesConfig,
          searchPlaceholder: this.searchPlaceholder
        }
      }, {
        component: "opensilex-AgroportalCreateFormPart",
        title: "AgroportalSearchFormPart.step2-title",
        finish: "AgroportalSearchFormPart.save",
        next: "AgroportalSearchFormPart.map",
        props: {
          namePlaceholder: this.searchPlaceholder,
          descriptionPlaceholder: this.descriptionPlaceholder
        },
        slots: [ "createAdditionalFields" ]
      }, {
        component: "opensilex-AgroportalMappingFormPart",
        title: "AgroportalSearchFormPart.step3-title",
        props: {
          ontologiesConfig: this.ontologiesConfig,
          searchPlaceholder: this.searchPlaceholder
        }
      }
    ];
  }
  //#endregion

  //#region Hooks
  created() {
    this.agroportalService = this.$opensilex
        .getService<AgroportalAPIService>("opensilex.AgroportalAPIService");
  }
  //#endregion

  //public Methods
  public showCreateForm() {
    this.checkAgroportalReachable();
    this.wizardRef.showCreateForm();
  }

  public showEditForm(form: T) {
    this.editMode = true;
    this.wizardRef.showEditForm(form);
  }
  //#endregion

  //#region Private methods
  private checkAgroportalReachable() {
    return this.agroportalService.pingAgroportal().then((http) => {
      if (http && http.response) {
        let isReachable = http.response.result;
        if (!isReachable) {
          this.wizardRef.skipStep();
        }
      }
    }).catch(this.agroportalErrorHandler);
  }

  private agroportalErrorHandler(error: HttpResponse) {
    if (error.status === 503) {
      this.wizardRef.skipStep();
      return;
    }
    this.$opensilex.errorHandler(error);
  }

  private getEmptyForm(): BaseExternalReferencesDTO {
    return JSON.parse(JSON.stringify(this.emptyForm));
  }

  private create(form: T) {
    return this.createMethod(form)
        .then(http => {
          form.uri = http.response.result;
          this.$opensilex.showSuccessToast(this.$t("component.common.success.creation-success-with-template", {
            uri: form.uri
          }).toString());
          this.$emit("onCreate", form);
        })
        .catch(this.$opensilex.errorHandler);
  }

  private update(form: T) {
    return this.updateMethod(form)
        .then(http => {
          form.uri = http.response.result;
          this.$opensilex.showSuccessToast(this.$t("component.common.success.update-success-with-template", {
            uri: form.uri
          }).toString());
          this.$emit("onUpdate", form);
        })
        .catch(this.$opensilex.errorHandler);
  }

  private convert(form: T, searchDTO: AgroportalTermDTO) {
    if (!this.editMode) {
      form.uri = searchDTO.id;
    }
    form.name = searchDTO.name;
    form.description = searchDTO.definitions[0];
    form.exact_match = [];
    form.narrow_match = [];
    form.broad_match = [];
    form.close_match = [];
    return form;
  }

  private validateCustom(form: T) {
    return Boolean(form.name);
  }

  private nextStep(stepIndex, form) {
    if (stepIndex == 0 && form.uri) {
      if (this.editMode) {
        return true;
      }
      form.close_match.push(form.uri);
      form.uri = null;
    }
    return true;
  }
  //#endregion
}
</script>


<style scoped lang="scss">

</style>
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
      :convertAction="convert"
      :static="false"
      :nextStepAction="nextStep"
      :customValidation="validateCustom"
      :isBlockingStep="false"
  >
    <template v-slot:additionalFields="scope">
      <slot name="enrichAdditionalFields" :form="scope.form"></slot>
    </template>
    <template v-slot:icon></template>
  </opensilex-WizardForm>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import OpenSilexVuePlugin from "../../../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import {Prop, Ref} from "vue-property-decorator";
import WizardForm from "../../../forms/WizardForm.vue";
import HttpResponse, {OpenSilexResponse} from "../../../../../lib/HttpResponse";
import {BaseExternalReferencesForm, BaseExternalReferencesDTO} from "../../ExternalReferencesTypes";
import {AgroportalTermDTO} from "opensilex-core/model/agroportalTermDTO";

@Component({})
export default class AgroportalCreateForm<T extends BaseExternalReferencesDTO> extends Vue implements BaseExternalReferencesForm {
  //region Plugins and services
  private readonly $opensilex: OpenSilexVuePlugin;
  private variablesService: VariablesService;
  private agroportalService: AgroportalAPIService;
  //endregion

  //region Props
  @Prop()
  private readonly ontologiesConfig: string;

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
  //endregion

  //region Refs
  @Ref("wizardRef")
  private readonly wizardRef!: WizardForm;
  //endregion

  //region Data
  private editMode = false;
  //endregion

  get steps() {
    return [
      {
        component: "opensilex-AgroportalSearchFormPart",
        title: "AgroportalSearchFormPart.step1-title",
        finish: "AgroportalSearchFormPart.import-and-save",
        next: "AgroportalSearchFormPart.enrich",
        props: {
          ontologiesConfig: this.ontologiesConfig,
          searchPlaceholder: this.searchPlaceholder
        }
      }, {
        component: "opensilex-AgroportalEnrichFormPart",
        title: "AgroportalSearchFormPart.step2-title",
        finish: "AgroportalSearchFormPart.save",
        props: {
          namePlaceholder: this.searchPlaceholder,
          descriptionPlaceholder: this.descriptionPlaceholder
        },
        slot: {
          name: "additionalFields",
          scope: "form"
        }
      }, {
        component: "opensilex-AgroportalExternalReferencesFormPart",
        title: "AgroportalSearchFormPart.step3-title",
        props: {
          ontologiesConfig: this.ontologiesConfig,
          searchPlaceholder: this.searchPlaceholder
        }
      }
    ];
  }

  created() {
    this.variablesService = this.$opensilex
        .getService<VariablesService>("opensilex.VariablesService");
    this.agroportalService = this.$opensilex
        .getService<AgroportalAPIService>("opensilex.AgroportalAPIService");
  }

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

  showCreateForm() {
    this.checkAgroportalReachable();
    this.wizardRef.showCreateForm();
  }

  showEditForm(form: T) {
    this.wizardRef.showEditForm(form);
  }

  getEmptyForm(): BaseExternalReferencesDTO {
    return JSON.parse(JSON.stringify(this.emptyForm));
  }

  create(form: T) {
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

  update(form: T) {
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

  convert(form: T, searchDTO: AgroportalTermDTO) {
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

  validateCustom(form: T) {
    return Boolean(form.name);
  }

  nextStep(stepIndex, form) {
    if (stepIndex == 0 && form.uri) {
      if (this.editMode) {
        return true;
      }
      form.close_match.push(form.uri);
      form.uri = "";
    }
    return true;
  }
}
</script>


<style scoped lang="scss">

</style>
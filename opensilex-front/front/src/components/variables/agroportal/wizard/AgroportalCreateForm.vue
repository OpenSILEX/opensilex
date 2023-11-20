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
    <template v-slot:icon></template>
  </opensilex-WizardForm>
</template>

<script lang="ts">
import Vue from 'vue';
import Component from 'vue-class-component';
import OpenSilexVuePlugin from "../../../../models/OpenSilexVuePlugin";
import {VariablesService} from "opensilex-core/api/variables.service";
import {AgroportalAPIService} from "opensilex-core/api/agroportalAPI.service";
import {Prop, Ref} from "vue-property-decorator";
import WizardForm from "../../../common/forms/WizardForm.vue";
import HttpResponse, {OpenSilexResponse} from "../../../../lib/HttpResponse";
import {EntityAgroportalDTO} from "opensilex-core/model/entityAgroportalDTO";

export interface BaseAgroportalCreationDTO {
  uri?: string,
  name?: string,
  description?: string,
  exact_match?: Array<string>,
  close_match?: Array<string>,
  broad_match?: Array<string>,
  narrow_match?: Array<string>
}

export interface BaseAgroportalCreateForm {
  showCreateForm: () => void,
  showEditForm: (dto: BaseAgroportalCreationDTO) => void
}

@Component({})
export default class AgroportalCreateForm<T extends BaseAgroportalCreationDTO> extends Vue implements
    BaseAgroportalCreateForm {
  $opensilex: OpenSilexVuePlugin;

  variablesService: VariablesService;
  agroportalService: AgroportalAPIService;

  @Prop()
  ontologiesConfig: string;

  @Prop()
  searchPlaceholder: string;

  @Prop()
  createTitle: string;

  @Prop()
  editTitle: string;

  @Prop({
    default: "fa#vials"
  })
  icon: string;

  @Prop()
  createMethod: (form: T) => Promise<HttpResponse<OpenSilexResponse<string>>>;

  @Prop()
  updateMethod: (form: T) => Promise<HttpResponse<OpenSilexResponse<string>>>;

  @Ref("wizardRef") readonly wizardRef!: WizardForm;

  editMode = false;

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
          namePlaceholder: this.searchPlaceholder
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

  created(){
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

  getEmptyForm(): BaseAgroportalCreationDTO {
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

  create(form: T) {
    return this.createMethod(form)
        .then(http => {
          form.uri = http.response.result;
          this.$opensilex.showSuccessToast("TODO success message"); //@todo
          this.$emit("onCreate", form);
        })
        .catch(this.$opensilex.errorHandler);
  }

  update(form: T) {
    return this.updateMethod(form)
        .then(http => {
          form.uri = http.response.result;
          this.$opensilex.showSuccessToast("TODO success message"); //@todo
          this.$emit("onUpdate", form);
        })
        .catch(this.$opensilex.errorHandler);
  }

  convert(form: T, searchDTO: EntityAgroportalDTO) {
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
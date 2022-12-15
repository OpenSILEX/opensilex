<template>
    <opensilex-ModalForm
        v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && loadForm"
        ref="variableForm"
        modalSize="lg"
        :tutorial="true"
        component="opensilex-VariableForm"
        createTitle="VariableForm.add"
        editTitle="VariableForm.edit"
        icon="fa#vials"
        :createAction="create"
        :updateAction="update"
        :successMessage="successMessage"
        :key="key"
    ></opensilex-ModalForm>

</template>

<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import { VariablesService, VariableDetailsDTO, VariableCreationDTO, ObjectUriResponse, VariableUpdateDTO } from "opensilex-core/index";
    import ModalForm from "../../common/forms/ModalForm.vue";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";
    import DTOConverter from "../../../models/DTOConverter";

    @Component
    export default class VariableCreate extends Vue {

        $opensilex: OpenSilexVuePlugin;
        $store: any;
        service: VariablesService;
        $i18n: any;

        @Prop()
        editMode;

        loadForm: boolean = false;

        key: number = 1;

        get user() {
            return this.$store.state.user;
        }

        get credentials() {
            return this.$store.state.credentials;
        }

        @Ref("variableForm") readonly variableForm!: ModalForm;

        created() {
            this.service = this.$opensilex.getService("opensilex.VariablesService");
        }

        showCreateForm() {
            this.refresh();
            this.loadForm = true;
            this.$nextTick(() => {
              this.variableForm.showCreateForm();
            });
        }

        showEditForm(form,linkedDataNb) {
          form.linked_data_nb = linkedDataNb;
          this.refresh();
            this.loadForm = true;
            this.$nextTick(() => {
                // the function extractURIFromResourceProperties transforms each nested object into uri
                let formCopy: VariableDetailsDTO = DTOConverter.extractURIFromResourceProperties(form);
                this.variableForm.showEditForm(formCopy);
            });
        }


        private refresh(){
            // update the key force VueJs to re-render the component properly
            this.key++;
        }

        create(variable) {

            if(variable.dataType && variable.dataType.uri){
                variable.dataType = variable.dataType.uri
            }
            this.service.createVariable(variable).then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                let message = this.$i18n.t("VariableForm.variable") + " " + variable.name + " " + this.$i18n.t("component.common.success.creation-success-message");
                this.$opensilex.showSuccessToast(message);
                variable.uri =  http.response.result.toString();
                this.$emit("onCreate",variable);
            }).catch((error) => {
              if (error.status == 409) {
                    this.$opensilex.errorHandler(error,"Variable "+variable.uri+" : "+this.$i18n.t("VariableForm.already-exist"));
                } else {
                  if (error.response.result) {
                      this.$opensilex.errorHandler(error, error.response.result.message);
                  }else {
                      this.$opensilex.errorHandler(error);
                  }
              }
            });
        }

        successMessage(variable : VariableCreationDTO){
            return this.$i18n.t("VariableView.name") + " " + variable.name;
        }

        update(variable) {

            this.service.updateVariable(variable).then(() => {
                let message = this.$i18n.t("VariableForm.variable") + " " + variable.name + " " + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);

                this.$emit("onUpdate", variable);
            }).catch((error) => {
                if (error.response.result) {
                    this.$opensilex.errorHandler(error, error.response.result.message);
                }else {
                    this.$opensilex.errorHandler(error);
                }
            });
        }
    }
</script>

<style scoped lang="scss">
</style>

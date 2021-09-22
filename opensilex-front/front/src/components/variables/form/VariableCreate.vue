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
    // @ts-ignore
    import { VariablesService, VariableGetDTO, VariableCreationDTO, ObjectUriResponse, VariableUpdateDTO } from "opensilex-core/index";
    import ModalForm from "../../common/forms/ModalForm.vue";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import HttpResponse, { OpenSilexResponse } from "../../../lib/HttpResponse";

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
                this.variableForm.showEditForm(form);
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
                    this.$opensilex.errorHandler(error,error.response.result.message);
                }
            });
        }

        successMessage(variable : VariableCreationDTO){
            return this.$i18n.t("VariableView.name") + " " + variable.name;
        }

        static formatVariableBeforeUpdate(variable) : VariableUpdateDTO{
            if(! variable){
                return undefined;
            }
            let formattedVariable = JSON.parse(JSON.stringify(variable));

            if(formattedVariable.datatype && formattedVariable.datatype.uri){
                formattedVariable.datatype = formattedVariable.datatype.uri
            }
            if(formattedVariable.entity && formattedVariable.entity.uri){
                formattedVariable.entity = formattedVariable.entity.uri;
            }
            if(formattedVariable.characteristic && formattedVariable.characteristic.uri){
                formattedVariable.characteristic = formattedVariable.characteristic.uri;
            }
            if(formattedVariable.method && formattedVariable.method.uri){
                formattedVariable.method = formattedVariable.method.uri;
            }
            if(formattedVariable.unit && formattedVariable.unit.uri){
                formattedVariable.unit = formattedVariable.unit.uri;
            }
            if(formattedVariable.species && formattedVariable.species.uri){
                formattedVariable.species = formattedVariable.species.uri;
            }

            return formattedVariable;
        }

        update(variable) {
            let formattedVariable = VariableCreate.formatVariableBeforeUpdate(variable);

            this.service.updateVariable(formattedVariable).then(() => {
                let message = this.$i18n.t("VariableForm.variable") + " " + variable.name + " " + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);

                this.$emit("onUpdate", variable);
            }).catch((error) => {
                this.$opensilex.errorHandler(error,error.response.result.message);
            });
        }
    }
</script>

<style scoped lang="scss">
</style>

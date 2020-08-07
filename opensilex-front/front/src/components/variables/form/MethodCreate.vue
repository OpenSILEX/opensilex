<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="MethodForm.add"
            editTitle="MethodForm.edit"
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
    import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
    import {VariablesService} from "opensilex-core/api/variables.service";
    import {EntityCreationDTO} from "opensilex-core/model/entityCreationDTO";
    import {MethodCreationDTO} from "opensilex-core/model/methodCreationDTO";

    @Component
    export default class MethodCreate extends Vue {

        steps = [
            {component: "opensilex-MethodForm"}
            ,{component : "opensilex-MethodExternalReferencesForm"}
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

        showEditForm() {
            this.wizardRef.showEditForm();
        }

        $opensilex: any;

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        getEmptyForm() : MethodCreationDTO {
            return {
                uri: null,
                name: null,
                comment: null,
                type: null,
                exactMatch: [],
                narrower: [],
                closeMatch: [],
                broader: []
            };
        }

        create(form){
            if(form.type){
                form.type = form.type.uri;
            }
            return this.service
                .createMethod(form)
                .then((http: HttpResponse<OpenSilexResponse<any>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("MethodForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
                    this.$opensilex.showSuccessToast(message);
                    this.$emit("onCreate", form);
                })
                .catch(error => {
                    if (error.status == 409) {
                        this.$opensilex.errorHandler(
                            error,
                            this.$i18n.t("component.project.errors.project-already-exists")
                        );
                    } else {
                        this.$opensilex.errorHandler(error);
                    }
                });
        }

        update(form){

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
    MethodForm:
        name: The method
        add: Add a method
        edit: Edit a method
        name-placeholder: Image analysis
fr:
    MethodForm:
        name: La méthode
        add: Créer une méthode
        edit: Éditer une méthode
        name-placeholder: Analyse d'image
</i18n>
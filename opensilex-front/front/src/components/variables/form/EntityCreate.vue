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
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
    import {VariablesService} from "opensilex-core/api/variables.service";
    import {EntityCreationDTO} from "opensilex-core/model/entityCreationDTO";

    @Component
    export default class EntityCreate extends Vue {

        steps = [
            {component: "opensilex-EntityForm"}
            ,{component : "opensilex-EntityExternalReferencesForm"}
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

        getEmptyForm(): EntityCreationDTO {
            return {
                uri: null,
                label: null,
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
                .createEntity(form)
                .then((http: HttpResponse<OpenSilexResponse<any>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("EntityForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
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
    EntityForm:
        name: The entity
        add: Add an entity
        edit: Edit an entity
        name-placeholder: Plant
fr:
    EntityForm:
        name: L'entité
        add: Créer une entité
        edit: Éditer une entité
        name-placeholder: Plante
</i18n>
<template>
    <opensilex-WizardForm
            ref="wizardRef"
            :steps="steps"
            createTitle="UnitForm.add"
            editTitle="UnitForm.edit"
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
    import {UnitCreationDTO} from "opensilex-core/model/unitCreationDTO";
    import {ObjectUriResponse} from "opensilex-core/model/objectUriResponse";

    @Component
    export default class UnitCreate extends Vue {

        steps = [
            {component: "opensilex-UnitForm"}
            ,{component : "opensilex-UnitExternalReferencesForm"}
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

        getEmptyForm(): UnitCreationDTO {
            return {
                uri: null,
                label: null,
                comment: null,
                symbol: null,
                alternativeSymbol: null,
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
                .createUnit(form)
                .then((http: HttpResponse<OpenSilexResponse<ObjectUriResponse>>) => {
                    form.uri = http.response.result;
                    let message = this.$i18n.t("UnitForm.name") + " " + form.uri + " " + this.$i18n.t("component.common.success.creation-success-message");
                    this.$opensilex.showSuccessToast(message);
                    this.$emit("onCreate", form);
                })
                .catch(error => {
                    if (error.status == 409) {
                        console.error("Unit already exists", error);
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
    UnitForm:
        name: The unit
        add: Add a unit
        edit: Edit a unit
        symbol: Symbol
        alternative-symbol: Alternative symbol
        name-placeholder: Kilogramm per hectare
        symbol-placeholder: kg ha-1
        alternative-symbol-placeholder: kg/ha
fr:
    UnitForm:
        name: L'unité
        add: Créer une unité
        edit: Éditer une unité
        symbol: Symbole
        alternative-symbol: Symbole alternatif
        name-placeholder: Kilogramme par hectare
        symbol-placeholder: kg ha-1
        alternative-symbol-placeholder: kg/ha
</i18n>
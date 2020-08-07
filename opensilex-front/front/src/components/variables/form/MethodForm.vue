<template>
    <ValidationObserver ref="methodValidatorRef">
        <!-- URI -->

        <opensilex-UriForm
                :uri.sync="form.uri"
                label="component.common.uri"
                :editMode="editMode"
                :generated.sync="uriGenerated"
                :required="true"
        ></opensilex-UriForm>
        <!-- Name -->
        <div class="row">
            <div class="col-lg-6">
                <opensilex-InputForm
                        :value.sync="form.name"
                        label="component.common.name"
                        type="text"
                        :required="true"
                        placeholder="MethodForm.name-placeholder"
                ></opensilex-InputForm>
            </div>

            <div class="col-lg-6">
                <!-- Class -->
                <opensilex-SelectForm
                        label="component.common.type"
                        :selected.sync="form.type"
                        :multiple="false"
                        :options="classList"
                        placeholder="VariableForm.class-placeholder"
                ></opensilex-SelectForm>
            </div>
        </div>

        <opensilex-TextAreaForm
                :value.sync="form.comment"
                label="component.common.description">
        </opensilex-TextAreaForm>
    </ValidationObserver>
</template>


<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
    import {MethodCreationDTO} from "opensilex-core/model/methodCreationDTO";
    import {OntologyService} from "opensilex-core/api/ontology.service";
    import HttpResponse, {
        OpenSilexResponse
    } from "opensilex-security/HttpResponse";
    import {VariablesService} from "opensilex-core/api/variables.service";

    @Component
    export default class MethodForm extends Vue {
        $opensilex: any;

        title = "";
        uriGenerated = true;
        editMode = false;

        errorMsg: String = "";

        @PropSync("form")
        methodDto: MethodCreationDTO;

        classList: Array<any> = [];
        service: OntologyService;

        created() {
            this.service = this.$opensilex.getService("opensilex.OntologyService");
            this.loadClasses();
        }

        loadClasses() {
            return this.service
                .getSubClassesOf("http://www.opensilex.org/vocabulary/oeso#Method",false)
                .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
                    this.classList = this.$opensilex.buildTreeListOptions(http.response.result);
                    this.$opensilex.setOntologyClasses(http.response.result);
                })
                .catch(this.$opensilex.errorHandler);
        }

        handleErrorMessage(errorMsg: string) {
            this.errorMsg = errorMsg;
        }

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("methodValidatorRef") readonly methodValidatorRef!: any;

        reset() {
            this.uriGenerated = true;
            return this.methodValidatorRef.reset();
        }

        validate() {
            return this.methodValidatorRef.validate();
        }
    }
</script>

<style scoped lang="scss">
</style>
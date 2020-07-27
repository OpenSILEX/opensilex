K
<template>
    <ValidationObserver ref="unitValidatorRef">
        <opensilex-UriForm
                :uri.sync="form.uri"
                label="component.common.uri"
                :editMode="editMode"
                :generated.sync="uriGenerated"
                :required="true"
        ></opensilex-UriForm>

        <div class="row">
            <!-- Name -->
            <div class="col-lg-4">
                <opensilex-InputForm
                        :value.sync="form.label"
                        label="component.common.name"
                        type="text"
                        :required="true"
                        placeholder="UnitForm.name-placeholder"
                ></opensilex-InputForm>
            </div>

            <!-- symbol -->
            <div class="col-lg-3">
                <opensilex-InputForm
                        :value.sync="form.symbol"
                        label="UnitForm.symbol"
                        type="text"
                        placeholder="UnitForm.symbol-placeholder"
                ></opensilex-InputForm>
            </div>

            <!-- alternative symbol -->
            <div class="col-lg-3">
                <opensilex-InputForm
                        :value.sync="form.alternativeSymbol"
                        label="UnitForm.alternative-symbol"
                        placeholder="UnitForm.alternative-symbol-placeholder"
                        type="text"
                        :required="false"
                ></opensilex-InputForm>
            </div>
        </div>

        <div class="row">
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

        <div class="row">
            <div class="col-lg-6">
                <opensilex-TextAreaForm
                        :value.sync="form.comment"
                        label="component.common.description"
                ></opensilex-TextAreaForm>
            </div>
        </div>

    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
    import {OntologyService} from "opensilex-core/api/ontology.service";
    import HttpResponse, {
        OpenSilexResponse
    } from "opensilex-security/HttpResponse";
    import {UnitCreationDTO} from "opensilex-core/model/unitCreationDTO";
    import {VariablesService} from "opensilex-core/api/variables.service";

    @Component
    export default class UnitForm extends Vue {
        $opensilex: any;

        title = "";
        uriGenerated = true;
        editMode = false;

        errorMsg: String = "";

        @PropSync("form")
        unitDto: UnitCreationDTO;

        classList: Array<any> = [];
        service: OntologyService;

        created() {
            this.service = this.$opensilex.getService("opensilex.OntologyService");
            this.loadClasses();
        }

        loadClasses() {
            return this.service
                .getSubClassesOf("http://www.opensilex.org/vocabulary/oeso#Unit",true)
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
        @Ref("unitValidatorRef") readonly unitValidatorRef!: any;

        reset() {
            this.uriGenerated = true;
            return this.unitValidatorRef.reset();
        }

        validate() {
            return this.unitValidatorRef.validate();
        }
    }
</script>

<style scoped lang="scss">
</style>
<template>
    <ValidationObserver ref="entityValidatorRef">
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
                        placeholder="EntityForm.name-placeholder"
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
    import {Component, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
    import {OntologyService} from "opensilex-core/api/ontology.service";
    import HttpResponse, {
        OpenSilexResponse
    } from "opensilex-security/HttpResponse";
    import {EntityCreationDTO} from "opensilex-core/model/entityCreationDTO";

    @Component
    export default class EntityForm extends Vue {
        $opensilex: any;

        title = "";
        uriGenerated = true;
        editMode = false;

        errorMsg: String = "";

        @PropSync("form")
        entityDto: EntityCreationDTO;

        classList: Array<any> = [];
        service: OntologyService;

        created() {
            this.service = this.$opensilex.getService("opensilex.OntologyService");
            this.loadClasses();
        }

        loadClasses() {
            return this.service
                .getSubClassesOf("http://www.opensilex.org/vocabulary/oeso#Entity",true)
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
        @Ref("entityValidatorRef") readonly entityValidatorRef!: any;

        reset() {
            this.uriGenerated = true;
            return this.entityValidatorRef.reset();
        }

        validate() {
            return this.entityValidatorRef.validate();
        }
    }
</script>

<template>
    <ValidationObserver ref="methodValidatorRef">
        <!-- URI -->

        <opensilex-UriForm
                :uri.sync="form.uri"
                label="component.variable.method-uri"
                helpMessage="component.common.method-uri.help"
                :editMode="editMode"
                :generated.sync="uriGenerated"
                :required=true
        ></opensilex-UriForm>
        <!-- Name -->
        <div class="row">
            <div class="col-lg-6">
                <opensilex-InputForm
                        :value.sync="form.label"
                        label="component.variable.method-name"
                        type="text"
                        :required=true
                ></opensilex-InputForm>
            </div>
            <div class="col-lg-6">
                <opensilex-FormInputLabelHelper
                        label=component.variable.method-class
                        helpMessage="component.variable.method-class-help">
                </opensilex-FormInputLabelHelper>

                <multiselect
                        :limit="1"
                        :closeOnSelect=true
                        :placeholder="$t('component.variable.class-placeholder')"
                        v-model="form.type"
                        :options="classList"
                        :custom-label="treeDto => treeDto.name"
                        deselectLabel="You must select one element"
                        track-by="uri"
                        :allow-empty=true
                        :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                />
            </div>
        </div>

        <opensilex-TextAreaForm
                :value.sync="form.comment"
                label="component.variable.comment"
        ></opensilex-TextAreaForm>

    </ValidationObserver>
</template>


<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
    import {MethodCreationDTO} from "opensilex-core/model/methodCreationDTO";
    import {OntologyService} from "opensilex-core/api/ontology.service";
    import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";

    @Component
    export default class MethodForm extends Vue {

        $opensilex: any;

        title = "";
        uriGenerated = true;
        editMode = false;

        errorMsg: String = "";

        @PropSync("form")
        methodDto: MethodCreationDTO;

        classList: Array<ResourceTreeDTO> = [];

        created() {

            let ontologyService: OntologyService = this.$opensilex.getService("opensilex.OntologyService");
            let classUri: string = "http://www.opensilex.org/vocabulary/oeso#Method";

            ontologyService.getSubClassesOf(classUri, true)
                .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
                    for (let i = 0; i < http.response.result.length; i++) {
                        let dto: ResourceTreeDTO = http.response.result[i];
                        this.classList.push(dto);

                        if (dto.children) {
                            dto.children.forEach(subDto => this.classList.push(subDto));
                        }
                    }
                }).catch(this.$opensilex.errorHandler);
        }


        handleErrorMessage(errorMsg: string) {
            this.errorMsg = errorMsg;
        }

        @Ref("modalRef") readonly modalRef!: any;
        @Ref("methodValidatorRef") readonly methodValidatorRef!: any;

        selectedClass: ResourceTreeDTO = null;

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
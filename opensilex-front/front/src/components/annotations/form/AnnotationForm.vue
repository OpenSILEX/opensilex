<template>
    <ValidationObserver ref="validatorRef">

        <div class="row">
            <div class="col">

                <opensilex-UriForm
                        :uri.sync="form.uri"
                        label="component.common.uri"
                        :editMode="editMode"
                        :generated.sync="uriGenerated"
                        :required="true"
                ></opensilex-UriForm>
            </div>
        </div>

        <div class="row">
            <div class="col">
                <opensilex-SelectForm
                        label="Annotation.motivation"
                        :required="true"
                        :multiple="false"
                        :disabled="viewMode"
                        :selected.sync="form.motivation"
                        :options="motivations"
                        noResultsText="Annotation.no-motivation"
                        helpMessage="Annotation.motivation-help"
                        placeholder="Annotation.motivation-placeholder"
                ></opensilex-SelectForm>
            </div>
        </div>

        <div class="row">
            <div class="col">
                <!-- bodyValue -->
                <opensilex-TextAreaForm
                        :value.sync="form.bodyValue"
                        :required="true"
                        label="Annotation.body-value">
                </opensilex-TextAreaForm>
            </div>
        </div>

    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, Prop, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
    import {AnnotationsService} from "opensilex-core/api/annotations.service";
    import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";
    import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
    import {currentConfig} from "vee-validate/dist/types/config";

    @Component
    export default class AnnotationForm extends Vue {

        @Ref("validatorRef") readonly validatorRef!: any;

        $opensilex: OpenSilexVuePlugin;
        $service: AnnotationsService

        uriGenerated = true;
        editMode = false;
        viewMode = false;

        errorMsg: String = "";

        motivations = [];

        @Prop({
            default: () => AnnotationForm.getEmptyForm()
        })
        form;

        private langUnwatcher;
        mounted() {
            this.langUnwatcher = this.$store.watch(
                () => this.$store.getters.language,
                () => {
                    this.loadMotivations();
                }
            );
        }

        created(){
            this.$service = this.$opensilex.getService("opensilex.AnnotationsService");
            this.loadMotivations();
        }

        loadMotivations() {
            this.$service.searchMotivations(undefined, undefined, undefined, undefined)
                .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                    if (http && http.response) {
                        this.motivations = [];
                        http.response.result.forEach(motivationDto => {
                            this.motivations.push({label: motivationDto.name, id: motivationDto.uri})
                        })
                    }
                }).catch(this.$opensilex.errorHandler);
        }

        static getEmptyForm() {
            return {
                uri: undefined,
                description: undefined,
                type: undefined,
                beginning: undefined,
                end: undefined,
                concernedItems: []
            };
        }

        getEmptyForm() {
            return AnnotationForm.getEmptyForm();
        }

        reset() {
            this.uriGenerated = true;
            return this.validatorRef.reset();
        }

        validate() {
            return this.validatorRef.validate();
        }
    }
</script>

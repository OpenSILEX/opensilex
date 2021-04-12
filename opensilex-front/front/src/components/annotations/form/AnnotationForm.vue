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
                        helpMessage="component.common.uri-help-message"
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
                        :itemLoadingMethod="loadMotivation"
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
                        :value.sync="form.description"
                        :required="true"
                        label="Annotation.description">
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
    import {AnnotationCreationDTO} from "opensilex-core/model/annotationCreationDTO";
    import {MotivationGetDTO} from "opensilex-core/model/motivationGetDTO";

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
                    this.searchMotivations();
                }
            );
        }

        created(){
            this.$service = this.$opensilex.getService("opensilex.AnnotationsService");
            this.searchMotivations();
        }

        searchMotivations() {
            this.$service.searchMotivations(undefined, ["name=asc"], undefined, undefined)
                .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                    if (http && http.response) {
                        this.motivations = [];
                        http.response.result.forEach(motivationDto => {
                            this.motivations.push({label: motivationDto.name, id: motivationDto.uri})
                        })
                    }
                }).catch(this.$opensilex.errorHandler);
        }

        loadMotivation(motivations: Array<any>): Array<any>{

            if(! motivations || motivations.length == 0){
                return undefined;
            }
            // in edit mode, the loaded motivation is an object composed of uri and name
            if(motivations[0].uri){
                let motivation = [{label: this.form.motivation.name, id: this.form.motivation.uri}];
                this.form.motivation = this.form.motivation.uri;
                return motivation;
            }

            return [this.motivations.find(motivation => motivation.id == motivations[0])];
        }

        static getEmptyForm(): AnnotationCreationDTO {
            return {
                uri: undefined,
                motivation: undefined,
                targets: [],
                description: undefined
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

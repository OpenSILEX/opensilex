<template>
    <ValidationObserver ref="validatorRef">

        <!-- URI -->
        <opensilex-UriForm
                :uri.sync="form.uri"
                label="component.variable.uri"
                helpMessage="component.common.uri.help-message"
                :editMode="editMode"
                :generated.sync="uriGenerated"
        ></opensilex-UriForm>

        <div class="row">
            <div class="col-lg-6">
                <!-- Entity -->
                <b-form-group>
                    <opensilex-FormInputLabelHelper label=component.variable.entity
                                                    helpMessage="component.variable.entity-help">
                    </opensilex-FormInputLabelHelper>
                    <multiselect
                            :limit="1"
                            :closeOnSelect=true
                            :placeholder="$t('component.variable.form.placeholder.entity')"
                            v-model="form.entity"
                            :options="entityClassList"
                            :custom-label="dto => dto.name"
                            track-by="uri"
                            :allow-empty=true
                            :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                            @input="updateName(form)"
                    />
<!--                    <b-button @click="showEntityCreateForm"-->
<!--                              variant="primary">{{$t('component.variable.form.add.entity')}}-->
<!--                    </b-button>-->
<!--                    <opensilex-EntityForm-->
<!--                            ref="entityForm"-->
<!--                            @onCreate="callCreateEntityService">-->
<!--                    </opensilex-EntityForm>-->
                </b-form-group>
            </div>

            <div class="col-lg-6">
                <!-- Quality -->
                <b-form-group>
                    <opensilex-FormInputLabelHelper label=component.variable.quality
                                                    helpMessage="component.variable.quality-help">
                    </opensilex-FormInputLabelHelper>
                    <multiselect
                            :limit="1"
                            :closeOnSelect=true
                            :placeholder="$t('component.variable.form.placeholder.quality')"
                            v-model="form.quality"
                            :options="qualityList"
                            :custom-label="dto => dto.label"
                            track-by="uri"
                            :allow-empty=true
                            :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                            @input="updateName(form)"
                    />
                    <b-button @click="showQualityCreateForm"
                              variant="primary">{{$t('component.variable.form.add.quality')}}
                    </b-button>
                    <opensilex-QualityCreate
                            ref="qualityForm"
                            @onCreate="updateQualityList">
                    </opensilex-QualityCreate>
                </b-form-group>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-6">
                <!-- Method uri -->
                <b-form-group>
                    <opensilex-FormInputLabelHelper label=component.variable.method
                                                    helpMessage="component.variable.method-uri-help">
                    </opensilex-FormInputLabelHelper>
                    <multiselect
                            :limit="1"
                            :closeOnSelect=true
                            :placeholder="$t('component.variable.form.placeholder.method')"
                            v-model="form.method"
                            :options="methodList"
                            :custom-label="dto => dto.label"
                            :value="dto => dto.uri"
                            track-by="uri"
                            :allow-empty=true
                            :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                            @input="updateLongName(form)"

                    />
                    <b-button @click="showMethodCreateForm" variant="primary">
                        {{$t('component.variable.form.add.method')}}
                    </b-button>
                    <opensilex-MethodCreate
                            ref="methodForm"
                            @onCreate="updateMethodList">
                    >
                    </opensilex-MethodCreate>
                </b-form-group>

            </div>
            <div class="col-lg-6">
                <!-- Scale -->
                <b-form-group>
                    <opensilex-FormInputLabelHelper label=component.variable.unit
                                                    helpMessage="component.variable.unit-help">
                    </opensilex-FormInputLabelHelper>
                    <multiselect
                            :limit="1"
                            :closeOnSelect=true
                            :placeholder="$t('component.variable.form.placeholder.unit')"
                            v-model="form.unit"
                            :options="unitList"
                            :custom-label="dto => dto.label"
                            track-by="uri"
                            :allow-empty=true
                            :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                            @input="updateLongName(form)"
                    />
                    <b-button @click="showUnitCreateForm" variant="primary">
                        {{$t('component.variable.form.add.unit')}}
                    </b-button>
                    <opensilex-UnitCreate
                            ref="unitForm"
                            @onCreate="updateUnitList">
                    </opensilex-UnitCreate>
                </b-form-group>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-5">
                <!-- Name -->
                <opensilex-InputForm
                        :value.sync="form.label"
                        label="component.variable.label"
                        type="text"
                        :required="true"
                        placeholder="component.variable.label-placeholder"
                ></opensilex-InputForm>
            </div>

            <div class="col-lg-5">
                <!-- longname -->
                <opensilex-InputForm
                        :value.sync="form.longName"
                        label="component.variable.longname"
                        type="text"
                ></opensilex-InputForm>
            </div>

        </div>

        <div class="row">
            <div class="col-lg-5">
                <!-- URI -->
                <opensilex-UriForm
                        :uri.sync="form.trait.traitUri"
                        label="component.variable.trait-uri"
                        helpMessage="component.variable.trait-uri-help"
                        :editMode="editMode"
                        :generated.sync="traitClassUriGenerated"
                ></opensilex-UriForm>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-5">
                <!-- Trait label -->
                <opensilex-InputForm
                        :value.sync="form.trait.traitLabel"
                        label="component.variable.trait-name"
                        helpMessage="component.variable.trait-name-help"
                        type="text"
                        :required="uriGenerated"
                ></opensilex-InputForm>
            </div>

            <div class="col-lg-5">
                <!-- Trait class -->
                <b-form-group>
                    <opensilex-FormInputLabelHelper label=component.variable.trait-class
                                                    helpMessage="component.variable.trait-class-help">
                    </opensilex-FormInputLabelHelper>
                    <ValidationProvider :name="$t('component.variable.trait-class')" v-slot="{ errors }">

                        <multiselect
                                :limit="1"
                                :closeOnSelect=true
                                :placeholder="$t('component.variable.trait-uri-placeholder')"
                                v-model="form.trait.traitClass"
                                :options="traitClassList"
                                :custom-label="dto => dto.name"
                                track-by="uri"
                                :allow-empty=true
                                :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                        />

                        <div class="error-message alert alert-danger">{{ errors[0] }}</div>
                    </ValidationProvider>
                </b-form-group>
            </div>
        </div>

    </ValidationObserver>

</template>

<script lang="ts">

    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import {VariableCreationDTO, ResourceTreeDTO} from "opensilex-core/index";
    import {VariablesService} from "opensilex-core/api/variables.service";
    import {OntologyService} from "opensilex-core/api/ontology.service";
    import {EntityCreationDTO} from "opensilex-core/model/entityCreationDTO";
    import {EntityGetDTO} from "opensilex-core/model/entityGetDTO";
    import {QualityGetDTO} from "opensilex-core/model/qualityGetDTO";
    import {MethodGetDTO} from "opensilex-core/model/methodGetDTO";
    import {UnitGetDTO} from "opensilex-core/model/unitGetDTO";
    import {QualityCreationDTO} from "opensilex-core/model/qualityCreationDTO";
    import {UnitCreationDTO} from "opensilex-core/model/unitCreationDTO";
    import {MethodCreationDTO} from "opensilex-core/model/methodCreationDTO";

    import Vue from "vue";
    import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";


    @Component
    export default class VariableForm2 extends Vue {
        $opensilex: any;

        @Prop()
        editMode;

        @Prop({ default: true })
        uriGenerated;

        @Prop({ default: true })
        traitClassUriGenerated;

        areFielsRequired(){
            console.log(this.uriGenerated);
            return this.uriGenerated;
        }


        getConcatName(form): string {
            let label: string = form.entity ? form.entity.name : "";
            if(label.length){
                label += "_";
            }
            if (form.quality) {
                label += form.quality.label;
            }
            return label.replace(/\s+/g, '_').toLowerCase();
        }

        getConcatLongName(form): string {
            let label: string = form.method ? form.method.label : "";
            if(label.length){
                label += "_";
            }
            if (form.unit) {
                label += form.unit.label;
            }
            return label.replace(/\s+/g, '_').toLowerCase();
        }

        updateName(form) {
            form.label = this.getConcatName(form);
            form.trait.traitLabel = form.label;
            form.longName = form.label;
            if(form.longName.length){
                form.longName += "_";
            }
            form.longName += this.getConcatLongName(form);
        }

        updateLongName(form) {
            form.longName = this.getConcatName(form);
            if(form.longName.length > 0 ){
                form.longName += "_";
            }
            form.longName += this.getConcatLongName(form);
        }

        @Ref("validatorRef") readonly validatorRef!: any;

        get user() {
            return this.$store.state.user;
        }

        @PropSync("form")
        variable: VariableCreationDTO;

        reset() {
            this.traitClassUriGenerated = true;
            return this.validatorRef.reset();
        }

        validate() {
            return this.validatorRef.validate();
        }

        service: VariablesService;
        ontologyService: OntologyService;

        dimensionList: Array<String> = ["volume", "surface", "time", "distance"];

        qualityList: Array<QualityGetDTO> = [];
        methodList: Array<MethodGetDTO> = [];
        unitList: Array<UnitGetDTO> = [];

        traitClassList: Array<ResourceTreeDTO> = [];
        entityClassList: Array<ResourceTreeDTO> = [];
        unitClassList: Array<ResourceTreeDTO> = [];

        traitClassUri: string = "http://www.opensilex.org/vocabulary/oeso#Trait";
        entityClassUri: string = "http://www.opensilex.org/vocabulary/oeso#Entity";

        @Ref("qualityForm") readonly qualityForm!: any;
        @Ref("methodForm") readonly methodForm!: any;
        @Ref("unitForm") readonly unitForm!: any;

        showQualityCreateForm() {
            this.qualityForm.showCreateForm();
        }

        updateQualityList(dto : QualityCreationDTO){
            this.qualityList.push(dto);
        }

        showMethodCreateForm() {
            this.methodForm.showCreateForm();
        }

        updateMethodList(dto : MethodCreationDTO){
            this.methodList.push(dto);
        }

        showUnitCreateForm() {
            this.unitForm.showCreateForm();
        }

        updateUnitList(dto : UnitCreationDTO){
            this.unitList.push(dto);
        }

        created() {
            this.service = this.$opensilex.getService("opensilex.VariablesService");
            this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");

            this.loadSubClasses(this.entityClassUri, this.entityClassList);
            this.loadSubClasses(this.traitClassUri, this.traitClassList);

            // load qualities, method and units
            this.loadObjects(
                this.service.searchQualities(undefined, undefined, 0, 100),
                this.qualityList
            );

            this.loadObjects(
                this.service.searchMethods(undefined, undefined, 0, 100),
                this.methodList
            );

            this.loadObjects(
                this.service.searchUnits(undefined, undefined, 0, 100),
                this.unitList);

        }

        loadSubClasses(classUri: string, classList: Array<ResourceTreeDTO>) {
            this.ontologyService.getSubClassesOf(classUri, true)
                .then((http: HttpResponse<OpenSilexResponse<Array<ResourceTreeDTO>>>) => {
                    for (let i = 0; i < http.response.result.length; i++) {
                        let dto: ResourceTreeDTO = http.response.result[i];
                        classList.push(dto);

                        if (dto.children) {
                            dto.children.forEach(subDto => classList.push(subDto));
                        }
                    }
                }).catch(this.$opensilex.errorHandler);
        }

        loadObjects(promise: Promise<HttpResponse<OpenSilexResponse<Array<any>>>>, objectList: Array<any>) {
            promise.then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                http.response.result.forEach(res => objectList.push(res));
            }).catch(this.$opensilex.errorHandler);
        }

        // callCreateQualityService(dto: QualityCreationDTO, done) {
        //     done(
        //         this.service.createQuality(dto)
        //             .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        //                 dto.uri = http.response.result;
        //                 this.qualityList.push(dto);
        //                 this.qualityForm.hideForm();
        //             }).catch(this.$opensilex.errorHandler)
        //     );
        // }

        // callCreateScaleService(dto: UnitCreationDTO, done) {
        //     done(
        //         this.service.createUnit(dto)
        //             .then((http: HttpResponse<OpenSilexResponse<any>>) => {
        //                 dto.uri = http.response.result;
        //                 this.unitList.push(dto);
        //                 this.unitForm.hideForm();
        //             }).catch(this.$opensilex.errorHandler)
        //     );
        // }
    }
</script>
<style scoped lang="scss">
</style>

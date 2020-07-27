<template>
    <ValidationObserver ref="validatorRef">
        <!-- URI -->
        <opensilex-UriForm
                :uri.sync="form.uri"
                label="component.common.uri"
                :editMode="editMode"
                :generated.sync="uriGenerated"
        ></opensilex-UriForm>

        <div class="row">
            <div class="col-lg-6">
                <!-- Entity -->
                <opensilex-SelectForm
                        label="Variables.entity"
                        :selected.sync="variable.entity"
                        :multiple="false"
                        required="true"
                        :searchMethod="searchEntities"
                        :itemLoadingMethod="loadEntity"
                        placeholder="VariableForm.entity-placeholder"
                        :conversionMethod="objectToSelectNode"
                        noResultsText="VariableForm1.no-entity"
                        @select="updateName(variable)"
                        :actionHandler="showEntityCreateForm"
                ></opensilex-SelectForm>
                <opensilex-EntityCreate ref="entityForm"></opensilex-EntityCreate>
            </div>

            <div class="col-lg-6">
                <!-- Quality -->
                <opensilex-SelectForm
                        label="Variables.quality"
                        :selected.sync="form.quality"
                        :multiple="false"
                        required="true"
                        :searchMethod="searchQualities"
                        :itemLoadingMethod="loadQuality"
                        placeholder="VariableForm.quality-placeholder"
                        :conversionMethod="objectToSelectNode"
                        noResultsText="VariableForm1.no-quality"
                        @select="updateName(form)"
                        :actionHandler="showQualityCreateForm"
                ></opensilex-SelectForm>
                <opensilex-QualityCreate ref="qualityForm"></opensilex-QualityCreate>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-6">
                <!-- Name -->
                <opensilex-InputForm
                        :value.sync="form.name"
                        label="component.common.name"
                        type="text"
                        :required="true"
                ></opensilex-InputForm>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-6">
                <!-- Method -->
                <opensilex-SelectForm
                        label="Variables.method"
                        :selected.sync="form.method"
                        :multiple="false"
                        required="true"
                        :searchMethod="searchMethods"
                        :itemLoadingMethod="loadMethod"
                        placeholder="VariableForm.method-placeholder"
                        :conversionMethod="objectToSelectNode"
                        @select="updateName(form)"
                        :actionHandler="showMethodCreateForm"
                        noResultsText="VariableForm1.no-method"
                ></opensilex-SelectForm>
                <opensilex-MethodCreate ref="methodForm"></opensilex-MethodCreate>
            </div>

            <!-- Unit -->
            <div class="col-lg-6">
                <opensilex-SelectForm
                        label="Variables.unit"
                        :selected.sync="form.unit"
                        :multiple="false"
                        required="true"
                        :searchMethod="searchUnits"
                        :itemLoadingMethod="loadUnit"
                        :conversionMethod="objectToSelectNode"
                        placeholder="VariableForm.unit-placeholder"
                        @select="updateName(form)"
                        :actionHandler="showUnitCreateForm"
                        noResultsText="VariableForm1.no-unit"
                ></opensilex-SelectForm>
                <opensilex-UnitCreate ref="unitForm"></opensilex-UnitCreate>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-6">
                <!-- longname -->
                <opensilex-InputForm
                        :value.sync="form.longName"
                        label="VariableForm.longName"
                        type="text"
                ></opensilex-InputForm>
            </div>
        </div>

        <!-- description -->
        <opensilex-TextAreaForm :value.sync="form.comment"
                                label="component.common.description"></opensilex-TextAreaForm>
    </ValidationObserver>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import {VariablesService} from "opensilex-core/api/variables.service";

    import Vue from "vue";
    import HttpResponse, {
        OpenSilexResponse
    } from "opensilex-security/HttpResponse";
    import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";

    @Component
    export default class VariableForm1 extends Vue {
        $opensilex: any;

        @Prop()
        editMode: boolean;

        @Prop({default: true})
        uriGenerated: boolean;

        @PropSync("form")
        variable;

        getLabel(dto: any, dtoList: Array<NamedResourceDTO>): string{
            if(! dto){
                return "";
            }
            if(dto.uri){
                return dto.name.replace(/\s+/g, "_");
            }
            let returnedDto: NamedResourceDTO = dtoList.find(dtoElem => dtoElem.uri == dto)
            if(returnedDto){
                return returnedDto.name.replace(/\s+/g, "_");
            }
            return "";
        }

        updateName(form) {

            let nameParts: string[] = [];

            nameParts.push(this.getLabel(form.entity,this.entities));
            nameParts.push(this.getLabel(form.quality,this.qualities));
            form.name = nameParts.join("_");

            nameParts.push(this.getLabel(form.method,this.methods));
            nameParts.push(this.getLabel(form.unit,this.units));
            form.longName = nameParts.join("_");
        }


        @Ref("validatorRef") readonly validatorRef!: any;

        get user() {
            return this.$store.state.user;
        }

        reset() {
            return this.validatorRef.reset();
        }

        validate() {
            return this.validatorRef.validate();
        }

        service: VariablesService;

        @Ref("entityForm") readonly entityForm!: any;
        @Ref("qualityForm") readonly qualityForm!: any;
        @Ref("methodForm") readonly methodForm!: any;
        @Ref("unitForm") readonly unitForm!: any;

        entities: Array<NamedResourceDTO> = [];
        units: Array<NamedResourceDTO> = [];
        methods: Array<NamedResourceDTO> = [];
        qualities: Array<NamedResourceDTO> = [];

        showEntityCreateForm() {
            this.entityForm.showCreateForm();
        }

        showQualityCreateForm() {
            this.qualityForm.showCreateForm();
        }

        showMethodCreateForm() {
            this.methodForm.showCreateForm();
        }

        showUnitCreateForm() {
            this.unitForm.showCreateForm();
        }

        created() {
            this.service = this.$opensilex.getService("opensilex.VariablesService");
        }

        searchEntities(name): Promise<Array<NamedResourceDTO>> {
            return this.service.searchEntities(name, ["name=asc"], 0, 10)
                .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                    this.entities = http.response.result;
                    return this.entities;
                });
        }

        loadEntity(uri) {
            if(this.variable.entity.uri){
                return [this.variable.entity];
            }
            return [this.entities.find(dto => dto.uri = uri)];
        }

        searchQualities(name): Promise<Array<NamedResourceDTO>> {
            return this.service
                .searchQualities(name, ["name=asc"], 0, 10)
                .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                    this.qualities = http.response.result;
                    return this.qualities;
                });
        }

        loadQuality(uri) {
            if(this.variable.quality.uri){
                return [this.variable.quality];
            }
            return [this.qualities.find(dto => dto.uri = uri)];
        }

        searchMethods(name): Promise<Array<NamedResourceDTO>> {
            return this.service
                .searchMethods(name, ["name=asc"], 0, 10)
                .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                    this.methods = http.response.result;
                    return this.methods;
                });
        }

        loadMethod(uri) {
            if(this.variable.method.uri){
                return [this.variable.method];
            }
            return [this.methods.find(dto => dto.uri = uri)];
        }

        searchUnits(name): Promise<Array<NamedResourceDTO>> {
            return this.service
                .searchUnits(name, ["name=asc"], 0, 10)
                .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                    this.units = http.response.result;
                    return this.units;
                });
        }

        loadUnit(uri) {
            if(this.variable.unit.uri){
                return [this.variable.unit];
            }
            return [this.units.find(dto => dto.uri = uri)];
        }

        objectToSelectNode(dto: NamedResourceDTO) {
            if(dto){
                return {id: dto.uri, label: dto.name};
            }
            return null;
        }
    }
</script>
<style scoped lang="scss">
</style>

<i18n>
fr:
    VariableForm1:
        no-entity:  Aucune entité correspondante
        no-quality: Aucune qualité correspondante
        no-method:  Aucune méthode correspondante
        no-unit:    Aucune unité correspondante
en:
    VariableForm1:
        no-entity: No entity found
        no-quality: No quality found
        no-method:  No method found
        no-unit:    No unit found
</i18n>

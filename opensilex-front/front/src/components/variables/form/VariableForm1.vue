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
                    ref="entitySelectForm"
                    label="Variables.entity"
                    :selected.sync="variable.entity"
                    :multiple="false"
                    :required="true"
                    :searchMethod="searchEntities"
                    :itemLoadingMethod="loadEntity"
                    placeholder="VariableForm.entity-placeholder"
                    :conversionMethod="objectToSelectNode"
                    noResultsText="VariableForm1.no-entity"
                    @select="updateName(variable)"
                    :actionHandler="showEntityCreateForm"
                ></opensilex-SelectForm>
                <opensilex-EntityCreate
                    ref="entityForm"
                    @onCreate="setLoadedEntity">
                </opensilex-EntityCreate>
            </div>

            <div class="col-lg-6">
                <!-- Quality -->
                <opensilex-SelectForm
                    ref="qualitySelectForm"
                    label="Variables.quality"
                    :selected.sync="form.quality"
                    :multiple="false"
                    :required="true"
                    :searchMethod="searchQualities"
                    :itemLoadingMethod="loadQuality"
                    placeholder="VariableForm.quality-placeholder"
                    :conversionMethod="objectToSelectNode"
                    noResultsText="VariableForm1.no-quality"
                    @select="updateName(form)"
                    :actionHandler="showQualityCreateForm"
                ></opensilex-SelectForm>
                <opensilex-QualityCreate
                    ref="qualityForm"
                    @onCreate="setLoadedQuality">
                </opensilex-QualityCreate>
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
                    ref="methodSelectForm"
                    label="Variables.method"
                    :selected.sync="form.method"
                    :multiple="false"
                    :required="true"
                    :searchMethod="searchMethods"
                    :itemLoadingMethod="loadMethod"
                    placeholder="VariableForm.method-placeholder"
                    :conversionMethod="objectToSelectNode"
                    @select="updateName(form)"
                    :actionHandler="showMethodCreateForm"
                    noResultsText="VariableForm1.no-method"
                ></opensilex-SelectForm>
                <opensilex-MethodCreate
                    ref="methodForm"
                    @onCreate="setLoadedMethod">
                </opensilex-MethodCreate>
            </div>

            <!-- Unit -->
            <div class="col-lg-6">
                <opensilex-SelectForm
                    ref="unitSelectForm"
                    label="Variables.unit"
                    :selected.sync="form.unit"
                    :multiple="false"
                    :required="true"
                    :searchMethod="searchUnits"
                    :itemLoadingMethod="loadUnit"
                    :conversionMethod="objectToSelectNode"
                    placeholder="VariableForm.unit-placeholder"
                    @select="updateName(form)"
                    :actionHandler="showUnitCreateForm"
                    noResultsText="VariableForm1.no-unit"
                ></opensilex-SelectForm>
                <opensilex-UnitCreate
                    ref="unitForm"
                    @onCreate="setLoadedUnit">
                </opensilex-UnitCreate>
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

        <div class="row">
            <div class="col-lg-6">
                <!-- synonym -->
                <opensilex-InputForm
                    :value.sync="form.synonym"
                    label="VariableForm.synonym"
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
import HttpResponse, {OpenSilexResponse} from "opensilex-security/HttpResponse";
import {NamedResourceDTO} from "opensilex-core/model/namedResourceDTO";
import {EntityCreationDTO} from "opensilex-core/model/entityCreationDTO";
import {QualityCreationDTO} from "opensilex-core/model/qualityCreationDTO";
import {MethodCreationDTO} from "opensilex-core/model/methodCreationDTO";
import {UnitCreationDTO} from "opensilex-core/model/unitCreationDTO";

@Component
export default class VariableForm1 extends Vue {
    $opensilex: any;

    @Prop()
    editMode: boolean;

    @Prop({default: true})
    uriGenerated: boolean;

    @PropSync("form")
    variable;

    getLabel(dto: any, dtoList: Array<NamedResourceDTO>): string {
        if (!dto) {
            return "";
        }
        if (dto.uri) {
            return dto.name.replace(/\s+/g, "_");
        }
        let returnedDto: NamedResourceDTO = dtoList.find(dtoElem => dtoElem.uri == dto)
        if (returnedDto) {
            return returnedDto.name.replace(/\s+/g, "_");
        }
        return "";
    }

    updateName(form) {

        let nameParts: string[] = [];

        nameParts.push(this.getLabel(form.entity, this.loadedEntities));
        nameParts.push(this.getLabel(form.quality, this.loadedQualities));
        form.name = nameParts.join("_");

        nameParts.push(this.getLabel(form.method, this.loadedMethods));
        nameParts.push(this.getLabel(form.unit, this.loadedUnits));
        form.longName = nameParts.join("_");
    }


    @Ref("validatorRef") readonly validatorRef!: any;

    reset() {
        this.loadedEntities = [];
        this.loadedQualities = [];
        this.loadedMethods = [];
        this.loadedUnits = [];
        this.uriGenerated = true;
        this.validatorRef.reset();
    }

    validate() {
        return this.validatorRef.validate();
    }

    service: VariablesService;

    @Ref("entitySelectForm") entitySelectForm!: any;
    @Ref("qualitySelectForm") qualitySelectForm!: any;
    @Ref("methodSelectForm") methodSelectForm!: any;
    @Ref("unitSelectForm") unitSelectForm!: any;

    @Ref("entityForm") readonly entityForm!: any;
    @Ref("qualityForm") readonly qualityForm!: any;
    @Ref("methodForm") readonly methodForm!: any;
    @Ref("unitForm") readonly unitForm!: any;

    loadedEntities: Array<NamedResourceDTO> = [];
    loadedUnits: Array<NamedResourceDTO> = [];
    loadedMethods: Array<NamedResourceDTO> = [];
    loadedQualities: Array<NamedResourceDTO> = [];

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

    searchEntities(name: string): Promise<Array<NamedResourceDTO>> {
        return this.service.searchEntities(name, ["name=asc"], 0, 10)
            .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                this.loadedEntities = http.response.result;
                return http.response.result;
            });
    }

    loadEntity(entities: Array<any>) {
        if(! entities || entities.length !== 1){
            return undefined;
        }
        // in edit mode, the loaded entity is an object composed of uri and name
        if(entities[0].uri){
            return [this.variable.entity];
        }
        return [this.loadedEntities.find(dto => dto.uri == entities[0])];
    }

    setLoadedEntity(created: EntityCreationDTO) {
        this.loadedEntities = [{uri: created.uri, name: created.name}];
        this.entitySelectForm.select({id: created.uri});
    }

    searchQualities(name: string): Promise<Array<NamedResourceDTO>> {
        return this.service
            .searchQualities(name, ["name=asc"], 0, 10)
            .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                this.loadedQualities = http.response.result;
                return http.response.result;
            });
    }

    loadQuality(qualities: Array<any>) {
        if(! qualities || qualities.length !== 1){
            return undefined;
        }
        // in edit mode, the loaded quality is an object composed of uri and name
        if(qualities[0].uri){
            return [this.variable.quality];
        }
        return [this.loadedQualities.find(dto => dto.uri == qualities[0])];
    }

    setLoadedQuality(created: QualityCreationDTO) {
        this.loadedQualities = [{uri: created.uri, name: created.name}];
        this.qualitySelectForm.select({id: created.uri});
    }

    searchMethods(name: string): Promise<Array<NamedResourceDTO>> {
        return this.service
            .searchMethods(name, ["name=asc"], 0, 10)
            .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                this.loadedMethods = http.response.result;
                return http.response.result;
            });
    }

    loadMethod(methods: Array<any>) {
        if(! methods || methods.length !== 1){
            return undefined;
        }
        // in edit mode, the loaded method is an object composed of uri and name
        if(methods[0].uri){
            return [this.variable.method];
        }
        return [this.loadedMethods.find(dto => dto.uri == methods[0])];
    }

    setLoadedMethod(created: MethodCreationDTO) {
        this.loadedMethods = [{uri: created.uri, name: created.name}];
        this.methodSelectForm.select({id: created.uri});
    }

    searchUnits(name: string): Promise<Array<NamedResourceDTO>> {
        return this.service
            .searchUnits(name, ["name=asc"], 0, 10)
            .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                this.loadedUnits = http.response.result;
                return http.response.result;
            });
    }

    loadUnit(units: Array<any>) {
        if(! units || units.length !== 1){
            return undefined;
        }
        // in edit mode, the loaded unit is an object composed of uri and name
        if(units[0].uri){
            return [this.variable.unit];
        }
        return [this.loadedUnits.find(dto => dto.uri == units[0])];
    }

    setLoadedUnit(created: UnitCreationDTO) {
        this.loadedUnits = [{uri: created.uri, name: created.name}];
        this.unitSelectForm.select({id: created.uri});
    }

    objectToSelectNode(dto: NamedResourceDTO) {
        if (dto) {
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
        no-entity: Aucune entité correspondante
        no-quality: Aucune qualité correspondante
        no-method: Aucune méthode correspondante
        no-unit: Aucune unité correspondante
en:
    VariableForm1:
        no-entity: No entity found
        no-quality: No quality found
        no-method: No method found
        no-unit: No unit found
</i18n>

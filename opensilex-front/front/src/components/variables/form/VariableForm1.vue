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
                    helpMessage="VariableForm.entity-help"
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
                    :selected.sync="variable.quality"
                    :multiple="false"
                    :required="true"
                    :searchMethod="searchQualities"
                    :itemLoadingMethod="loadQuality"
                    placeholder="VariableForm.quality-placeholder"
                    :conversionMethod="objectToSelectNode"
                    noResultsText="VariableForm1.no-quality"
                    helpMessage="VariableForm.quality-help"
                    @select="updateName(variable)"
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
                <opensilex-Button
                    :label="$t('VariableForm1.trait-button')"
                    :title="$t('VariableForm1.trait-button-placeholder')"
                    @click="showTraitForm()"
                    :small="false"
                    icon="fa#globe-americas"
                    variant="outline-primary"
                ></opensilex-Button>
            </div>

            <opensilex-WizardForm
                ref="traitForm"
                :steps="traitSteps"
                createTitle="Declare variable trait"
                editTitle="Edit variable trait"
                icon="fa#vials"
                modalSize="full"
                :static="false"
                :initForm="getEmptyTraitForm"
                :createAction="updateVariableTrait"
                :updateAction="updateVariableTrait"
            >
            </opensilex-WizardForm>
        </div>

        <hr/>

        <div class="row">
            <div class="col-lg-6">
                <!-- Method -->
                <opensilex-SelectForm
                    ref="methodSelectForm"
                    label="Variables.method"
                    :selected.sync="variable.method"
                    :multiple="false"
                    :required="false"
                    :searchMethod="searchMethods"
                    :itemLoadingMethod="loadMethod"
                    placeholder="VariableForm.method-placeholder"
                    :conversionMethod="objectToSelectNode"
                    helpMessage="VariableForm.method-help"
                    @select="updateName(variable)"
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
                    :selected.sync="variable.unit"
                    :multiple="false"
                    :required="true"
                    :searchMethod="searchUnits"
                    :itemLoadingMethod="loadUnit"
                    :conversionMethod="objectToSelectNode"
                    placeholder="VariableForm.unit-placeholder"
                    @select="updateName(variable)"
                    :actionHandler="showUnitCreateForm"
                    noResultsText="VariableForm1.no-unit"
                ></opensilex-SelectForm>
                <opensilex-UnitCreate
                    ref="unitForm"
                    @onCreate="setLoadedUnit">
                </opensilex-UnitCreate>
            </div>
        </div>

        <hr/>

        <div class="row">
            <div class="col-lg-6">
                <!-- Name -->
                <opensilex-InputForm
                    :value.sync="variable.name"
                    label="component.common.name"
                    type="text"
                    :required="true"
                ></opensilex-InputForm>
            </div>
            <div class="col-lg-6">
                <!-- longname -->
                <opensilex-InputForm
                    :value.sync="variable.longName"
                    label="VariableForm.longName"
                    type="text"
                ></opensilex-InputForm>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-6">
                <opensilex-SelectForm
                    label="OntologyPropertyForm.data-type"
                    :required="false"
                    :selected.sync="variable.dataType"
                    :options="datatypes"
                    :itemLoadingMethod="loadDataType"
                    helpMessage="VariableForm1.datatype-help"
                    placeholder="VariableForm1.datatype-placeholder"
                ></opensilex-SelectForm>
            </div>
        </div>

        <div class="row">
            <div class="col-lg-6">
                <!-- time-interval -->
                <opensilex-SelectForm
                    label="VariableForm.time-interval"
                    :selected.sync="form.timeInterval"
                    :multiple="false"
                    :options="periodList"
                    placeholder="VariableForm.time-interval-placeholder"
                    helpMessage="VariableForm.time-interval-help"
                ></opensilex-SelectForm>
            </div>

            <div class="col-lg-6">
                <!-- sample/distance-interval -->
                <opensilex-SelectForm
                    label="VariableForm.sampling-interval"
                    :selected.sync="form.samplingInterval"
                    :multiple="false"
                    :options="sampleList"
                    placeholder="VariableForm.sampling-interval-placeholder"
                    helpMessage="VariableForm.sampling-interval-help"
                ></opensilex-SelectForm>
            </div>
        </div>

        <div class="row">
            <div class="col-xl-12">
                <!-- description -->
                <opensilex-TextAreaForm :value.sync="variable.comment" label="component.common.description"></opensilex-TextAreaForm>
            </div>
        </div>




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
import ModalForm from "../../common/forms/ModalForm.vue";

@Component
export default class VariableForm1 extends Vue {
    $opensilex: any;

    @Prop()
    editMode: boolean;

    @Prop({default: true})
    uriGenerated: boolean;

    @PropSync("form")
    variable;

    service: VariablesService;

    @Ref("entitySelectForm") entitySelectForm!: any;
    @Ref("qualitySelectForm") qualitySelectForm!: any;
    @Ref("methodSelectForm") methodSelectForm!: any;
    @Ref("unitSelectForm") unitSelectForm!: any;

    @Ref("entityForm") readonly entityForm!: any;
    @Ref("qualityForm") readonly qualityForm!: any;
    @Ref("methodForm") readonly methodForm!: any;
    @Ref("unitForm") readonly unitForm!: any;

    @Ref("traitForm") readonly traitForm!: ModalForm;

    loadedEntities: Array<NamedResourceDTO> = [];
    loadedUnits: Array<NamedResourceDTO> = [];
    loadedMethods: Array<NamedResourceDTO> = [];
    loadedQualities: Array<NamedResourceDTO> = [];

    traitSteps = [
        {component: "opensilex-TraitForm"}
    ]

    periodList: Array<any> = [];
    sampleList: Array<any> = [];

    @Ref("validatorRef") readonly validatorRef!: any;

    created() {
        this.service = this.$opensilex.getService("opensilex.VariablesService");

        for(let period of ["millisecond","second","minute","hour","day","week","month","unique"]){
            this.periodList.push({
                id: this.$i18n.t("VariableForm.dimension-values." +period),
                label: this.$i18n.t("VariableForm.dimension-values." + period)
            })
        }

        for(let sample of ["mm","cm","m","km","day","field","region"]){
            this.sampleList.push({
                id: this.$i18n.t("VariableForm.dimension-values." +sample),
                label: this.$i18n.t("VariableForm.dimension-values." + sample)
            })
        }
    }

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

        let buildLabel = this.getLabel(form.method, this.loadedMethods);
        if(buildLabel.length > 0 ){
            nameParts.push(buildLabel);
        }
        buildLabel = this.getLabel(form.unit, this.loadedUnits);
        if(buildLabel.length > 0 ){
            nameParts.push(buildLabel);
        }

        form.longName = nameParts.join("_");
    }

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

    showTraitForm(){
        if(this.editMode){
            this.traitForm.showEditForm(this.getEmptyTraitForm());
        }else{
            this.traitForm.showCreateForm();
        }
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

    loadDataType(dataTypeUri: string){
        if(! dataTypeUri){
            return undefined;
        }
        let dataType =  this.$opensilex.getDatatype(dataTypeUri);
        return [{
            id: dataType.uri,
            label: dataType.labelKey.charAt(0).toUpperCase() + dataType.labelKey.slice(1)
        }];
    }

    getEmptyTraitForm(){
        return {
            traitUri: this.variable.traitUri,
            traitName: this.variable.traitName
        };
    }

    updateVariableTrait(form){
        let uriFilled = (form.traitUri && form.traitUri.length > 0);
        let nameFilled = (form.traitName && form.traitName.length > 0);

        // is both name and uri are filled or empty, then update current variable
        if(uriFilled == nameFilled){
            this.variable.traitUri = form.traitUri;
            this.variable.traitName = form.traitName;
        }
    }

    get datatypes() {
        let datatypeOptions = [];
        for (let i in this.$opensilex.datatypes) {
            let label: any = this.$t(this.$opensilex.datatypes[i].labelKey);
            datatypeOptions.push({
                id: this.$opensilex.datatypes[i].uri,
                label: label.charAt(0).toUpperCase() + label.slice(1)
            });
        }
        datatypeOptions.sort((a, b) => {
            if(a.label < b.label){
                return -1;
            }
            if(a.label > b.label){
                return 1;
            }
            return 0;
        });

        return datatypeOptions;
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
        trait-button: Trait
        trait-button-placeholder: Ajouter un trait existant pour cette variable
        datatype-help: Format des données enregistrées pour cette variable
        datatype-placeholder: Selectionnez un type de donnée
en:
    VariableForm1:
        no-entity: No entity found
        no-quality: No quality found
        no-method: No method found
        no-unit: No unit found
        trait-button: Trait
        trait-button-placeholder: Add an existing variable trait
        datatype-help: Format of data recorded for this variable
        datatype-placeholder: Select a datatype
</i18n>

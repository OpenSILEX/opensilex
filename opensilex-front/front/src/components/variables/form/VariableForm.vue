<template>
    <div id="v-step-global">
        <ValidationObserver ref="validatorRef">
            
            <opensilex-Tutorial
                ref="variableTutorial"
                :steps="tutorialSteps"
                @onSkip="continueFormEditing()"
                @onFinish="continueFormEditing()"
                :editMode="editMode"
            ></opensilex-Tutorial>

            <!-- URI -->
            <opensilex-UriForm
                :uri.sync="form.uri"
                label="component.common.uri"
                :editMode="editMode"
                :generated.sync="uriGenerated"
            ></opensilex-UriForm>

            <div class="row">
                
                <!-- Entity -->
                <div class="col-lg-6" id="v-step-entity">
                    <opensilex-EntitySelector
                        ref="entitySelector"
                        label="VariableView.entity"
                        placeholder="VariableForm.entity-placeholder"
                        helpMessage="VariableForm.entity-help"
                        noResultsText="VariableForm.no-entity"
                        :selected.sync="form.entity"
                        :multiple="false"
                        :required="true"
                        :actionHandler="editMode ? undefined : showEntityCreateForm"
                        :searchMethod="searchEntities"
                        @select="updateEntity"   
                        :itemLoadingMethod="loadEntity"
                        :conversionMethod="objectToSelectNode"
                        :disabled="false"
                        @loadMoreItems="loadMoreItems(entitySelector)"
                    ></opensilex-EntitySelector>
                    <opensilex-AgroportalEntityForm
                        ref="entityForm"
                        @onCreate="setLoadedEntity">
                    </opensilex-AgroportalEntityForm>
                </div>

                <!-- Entity of interest -->
                <div class="col-lg-6" id="v-step-interestEntity">
                    <opensilex-InterestEntitySelector
                        ref="interestEntitySelector"
                        label="VariableForm.interestEntity-label"
                        placeholder="VariableForm.interestEntity-placeholder"
                        :selected.sync="form.entity_of_interest"
                        :multiple="false"
                        :required="false"
                        :actionHandler="editMode ? undefined : showInterestEntityCreateForm"
                        helpMessage="VariableForm.interestEntity-help" 
                        :searchMethod="searchInterestEntities"
                        :itemLoadingMethod="loadInterestEntity"
                        :conversionMethod="objectToSelectNode"
                        noResultsText="VariableForm.no-interestEntity"
                        :disabled="false"
                        @loadMoreItems="loadMoreItems(interestEntitySelector)"
                    ></opensilex-InterestEntitySelector>
                    <opensilex-AgroportalEntityOfInterestForm
                        ref="interestEntityForm"
                        @onCreate="setLoadedInterestEntity">
                    </opensilex-AgroportalEntityOfInterestForm>
                </div>

                <!-- Characteristic -->
                <div class="col-lg-6" id="v-step-characteristic"> 
                    <opensilex-CharacteristicSelector
                        ref="characteristicSelector"
                        label="VariableView.characteristic"
                        placeholder="VariableForm.characteristic-placeholder"
                        :selected.sync="form.characteristic"
                        :multiple="false"
                        :required="true"
                        @select="updateCharacteristic"
                        :actionHandler="editMode ? undefined : showCharacteristicCreateForm"
                        helpMessage="VariableForm.interestEntity-help"
                        :searchMethod="searchCharacteristics"
                        :itemLoadingMethod="loadCharacteristic"
                        :conversionMethod="objectToSelectNode"
                        noResultsText="VariableForm.no-characteristic"
                        :disabled="false"
                        @loadMoreItems="loadMoreItems(characteristicSelector)"
                    ></opensilex-CharacteristicSelector>
                    <opensilex-AgroportalCharacteristicForm
                        ref="characteristicForm"
                        @onCreate="setLoadedCharacteristic">
                    </opensilex-AgroportalCharacteristicForm>
                </div>

                <!-- Species -->
                <div class="col-lg-6" id="v-step-species">
                    <opensilex-SpeciesSelector
                        v-if="!isGermplasmMenuExcluded"
                        label="SpeciesSelector.select-multiple"
                        placeholder="SpeciesSelector.select-multiple-placeholder"
                        :multiple="true"
                        :selected.sync="form.species"
                    ></opensilex-SpeciesSelector>
                </div>

                <!-- Method -->
                <div class="col-lg-6" id="v-step-method">
                    <opensilex-MethodSelector
                        ref="methodSelector"
                        label="VariableView.method"
                        placeholder="VariableForm.method-placeholder"
                        :multiple="false"
                        :required="true"
                        :selected.sync="form.method"
                        helpMessage="VariableForm.method-help"
                        noResultsText="VariableForm.no-method"
                        :actionHandler="editMode ? undefined : showMethodCreateForm"
                        @select="updateMethod"
                        :searchMethod="searchMethods"
                        :itemLoadingMethod="loadMethod"
                        :conversionMethod="objectToSelectNode"
                        :disabled="false"
                        @loadMoreItems="loadMoreItems(methodSelector)"
                    ></opensilex-MethodSelector>
                    <opensilex-AgroportalMethodForm
                        ref="methodForm"
                        @onCreate="setLoadedMethod">
                    </opensilex-AgroportalMethodForm>
                </div>

                <!-- Trait button -->
                <div class="col-lg-6" id="traitButton">
                    <opensilex-Button
                        label="VariableForm.trait-button"
                        helpMessage="VariableForm.trait-button-help"
                        @click="showTraitForm()"
                        :small="false"
                        icon="fa#globe-americas"
                        class="greenThemeColor"
                    ></opensilex-Button>
                </div>

                <opensilex-WizardForm
                    ref="traitForm"
                    :steps="traitSteps"
                    createTitle="VariableForm.trait-form-create-title"
                    editTitle="VariableForm.trait-form-edit-title"
                    icon="fa#vials"
                    modalSize="full"
                    :static="false"
                    :initForm="getEmptyTraitForm"
                    :createAction="updateVariableTrait"
                    :updateAction="updateVariableTrait"
                ></opensilex-WizardForm>

                <!-- Unit -->
                <div class="col-lg-6" id="v-step-unit">
                    <opensilex-UnitSelector
                        ref="unitSelector"
                        label="VariableView.unit"
                        placeholder="VariableForm.unit-placeholder"
                        :multiple="false"
                        :required="true"
                        :selected.sync="form.unit"
                        @select="updateUnit"
                        helpMessage="VariableForm.unit-help"
                        :actionHandler="editMode ? undefined : showUnitCreateForm" 
                        :searchMethod="searchUnits"
                        :itemLoadingMethod="loadUnit"
                        :conversionMethod="objectToSelectNode"
                        noResultsText="VariableForm.no-unit"
                        :disabled="false"
                        @loadMoreItems="loadMoreItems(unitSelector)"
                    ></opensilex-UnitSelector>
                    <opensilex-AgroportalUnitForm
                        ref="unitForm"
                        @onCreate="setLoadedUnit">
                    </opensilex-AgroportalUnitForm>
                </div>
            </div>

            <hr/>

            <div class="row">
                <!-- Name -->
                <div class="col-lg-6" id="v-step-name">
                    <opensilex-InputForm
                        :value.sync="form.name"
                        label="component.common.name"
                        type="text"
                        :required="true"
                    ></opensilex-InputForm>
                </div>

                <!-- AltName -->
                <div class="col-lg-6" id="v-step-alt">
                    <opensilex-InputForm
                        :value.sync="form.alternative_name"
                        label="VariableForm.altName"
                        type="text"
                    ></opensilex-InputForm>
                </div>

                <!-- DataType -->
                <div class="col-lg-6" id="v-step-datatype">
                    <opensilex-VariableDataTypeSelector
                        label="OntologyPropertyForm.data-type"
                        placeholder="VariableForm.datatype-placeholder"
                        :required="true"
                        :selected.sync="form.datatype"
                        helpMessage="VariableForm.datatype-help"
                        :itemLoadingMethod="loadDataType"
                        :disabled="hasLinkedData"
                        :options="datatypesNodes"
                    >
                    </opensilex-VariableDataTypeSelector>                   
                </div>

                <!-- Time-interval -->
                <div class="col-lg-6" id="v-step-time-interval">
                    <opensilex-VariableTimeIntervalSelector
                        label="VariableForm.time-interval"
                        :timeinterval.sync="form.time_interval"
                    >
                    </opensilex-VariableTimeIntervalSelector>
                </div>

                <!-- div d'occupation d'espace permettant de mieux positionner le prochain composant -->
                <div class="col-lg-6"></div>

                <!-- sample/distance-interval -->
                <div class="col-lg-6" id="v-step-sampling-interval">
                    <opensilex-FormSelector
                        label="VariableForm.sampling-interval"
                        :selected.sync="form.sampling_interval"
                        :multiple="false"
                        :options="sampleList"
                        placeholder="VariableForm.sampling-interval-placeholder"
                        helpMessage="VariableForm.sampling-interval-help"
                    ></opensilex-FormSelector>
                </div>

                <!-- description -->
                <div class="col-xl-12" id="v-step-description">
                    <opensilex-TextAreaForm
                        :value.sync="form.description"
                        label="component.common.description"
                        @keydown.native.enter.stop
                    >
                    </opensilex-TextAreaForm>
                </div>
            </div>
        </ValidationObserver>
    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import Tutorial from "../../common/views/Tutorial.vue";
import {
  CharacteristicCreationDTO,
  EntityCreationDTO,
  InterestEntityCreationDTO,
  MethodCreationDTO,
  NamedResourceDTO,
  UnitCreationDTO,
  VariableDatatypeDTO,
  VariablesService
} from "opensilex-core/index";
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import {DataService} from "opensilex-core/api/data.service";
import {VariableCreationDTO} from "opensilex-core/model/variableCreationDTO";
import {BaseExternalReferencesForm} from "../../common/external-references/ExternalReferencesTypes";
import {EntityGetDTO} from "opensilex-core/model/entityGetDTO";
import {InterestEntityGetDTO} from "opensilex-core/model/interestEntityGetDTO";
import {CharacteristicGetDTO} from "opensilex-core/model/characteristicGetDTO";
import {MethodGetDTO} from "opensilex-core/model/methodGetDTO";
import {UnitGetDTO} from "opensilex-core/model/unitGetDTO";
import VueI18n from "vue-i18n";
import FormSelector from "../../common/forms/FormSelector.vue";

@Component
export default class VariableForm extends Vue {
    $opensilex: any;
    $store: any;
    pageSize = 10;
    $i18n: VueI18n;

    @Prop()
    editMode: boolean;

    @Prop({default: true})
    uriGenerated: boolean;

    @Prop({
        default: () => {return VariableForm.getEmptyForm();},
    })
    form;

    savedVariable: any = {};

    service: VariablesService;
    dataService: DataService;

    @Ref("variableTutorial") readonly variableTutorial!: Tutorial;

    @Ref("entitySelector") entitySelector!: FormSelector;
    @Ref("interestEntitySelector") interestEntitySelector!: any;
    @Ref("characteristicSelector") characteristicSelector!: any;
    @Ref("methodSelector") methodSelector!: any;
    @Ref("unitSelector") unitSelector!: any;

    @Ref("entityForm") readonly entityForm!: BaseExternalReferencesForm;
    @Ref("interestEntityForm") readonly interestEntityForm!: BaseExternalReferencesForm;
    @Ref("characteristicForm") readonly characteristicForm!: BaseExternalReferencesForm;
    @Ref("methodForm") readonly methodForm!: BaseExternalReferencesForm;
    @Ref("unitForm") readonly unitForm!: BaseExternalReferencesForm;

    @Ref("traitForm") readonly traitForm!: any;

    get isGermplasmMenuExcluded() {
        return this.$opensilex.getConfig().menuExclusions.includes("germplasm");
    }

    traitSteps = [
        {component: "opensilex-TraitForm"}
    ]

    datatypes: Array<VariableDatatypeDTO> = [];
    datatypesNodes: Array<any> = [];
    periodList: Array<any> = [];
    sampleList: Array<any> = [];

    @Ref("validatorRef") readonly validatorRef!: any;

    created() {
        this.service = this.$opensilex.getService("opensilex.VariablesService");
        this.dataService = this.$opensilex.getService("opensilex-core.DataService");

        for(let period of ["millisecond","second","minute","hour","day","week","month","year","unique"]){
            this.periodList.push({
                id: this.$i18n.t("VariableForm.dimension-values." +period),
                label: this.$i18n.t("VariableForm.dimension-values." + period)
            })
        }

        for(let sample of ["mm","cm","m","km","field","region"]){
            this.sampleList.push({
                id: this.$i18n.t("VariableForm.dimension-values." +sample),
                label: this.$i18n.t("VariableForm.dimension-values." + sample)
            })
        }

        this.loadDatatypes();
    }

    reset() {
        this.uriGenerated = true;
        this.validatorRef.reset();

        if(this.variableTutorial && ! this.editMode){
            this.variableTutorial.stop();
        }
    }

    validate() {
        return this.validatorRef.validate();
    }

    getLabel(dto: any, dtoList): string {
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

    selectedEntityName;
    selectedCharacteristicName;
    selectedMethodName;
    selectedUnitName;

    updateEntity(entity){
        this.selectedEntityName = entity.label;
        this.updateName();
    }

    updateCharacteristic(characteristic){
        this.selectedCharacteristicName = characteristic.label;
        this.updateName();
    }

    updateMethod(method){
        this.selectedMethodName = method.label;
        this.updateName();
    }

    updateUnit(unit){
        this.selectedUnitName = unit.label;
        this.updateName();
    }

    updateName() {
        if(!this.editMode){
            let form = this.form;
            let nameParts: string[] = [];

            if(this.selectedEntityName && this.selectedEntityName.length > 0 ){
                let name = this.selectedEntityName.split(' ');
                nameParts.push(name[0]);
            }
            if(this.selectedCharacteristicName && this.selectedCharacteristicName.length > 0 ){
                nameParts.push(this.selectedCharacteristicName);
            }
            if(nameParts.length){
                form.alternative_name = nameParts.join("_");
            }

            if(this.selectedMethodName && this.selectedMethodName.length > 0 ){
                nameParts.push(this.selectedMethodName);
            }
            if(this.selectedUnitName && this.selectedUnitName.length > 0 ){
                nameParts.push(this.selectedUnitName);
            }
            if(nameParts.length){
                form.name = nameParts.join("_");
            }
        }
    }

    showEntityCreateForm() {
        this.entityForm.showCreateForm();
    }

    showInterestEntityCreateForm(){
        this.interestEntityForm.showCreateForm();
    }

    showCharacteristicCreateForm() {
        this.characteristicForm.showCreateForm();
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

    static getEmptyForm() {
      return {
        uri: undefined,
        alternative_name: undefined,
        name: undefined,
        entity: undefined,
        entity_of_interest: undefined,
        characteristic: undefined,
        description: undefined,
        time_interval: undefined,
        sampling_interval: undefined,
        datatype: undefined,
        trait: undefined,
        trait_name: undefined,
        method: undefined,
        unit: undefined,
        exact_match: [],
        close_match: [],
        broad_match: [],
        narrow_match: [],
        species: undefined,
        linked_data_nb: 0
      };
    }

    getEmptyForm(): VariableCreationDTO {
        return VariableForm.getEmptyForm();
    }

    searchEntities(name: string, page, pageSize){
        return this.service.searchEntities(name, ["name=asc"], page, pageSize)
            .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                return http;
            });
    }

    loadEntity(uris: Array<any>) {

        if (!uris || uris.length !== 1) {
            return undefined;
        }

        // in edit mode, the loaded entity is an object composed of uri and name
        if (uris[0].uri) {
            return [this.form.entity];
        }
        return this.service.getEntity( uris[0]).then(http =>
           [http.response.result]
        );
    }

    setLoadedEntity(created: EntityCreationDTO) {
        this.form.entity = created.uri;
        this.entitySelector.select({id: created.uri, label: created.name});
    }

    searchInterestEntities(name: string, page, pageSize){
        return this.service.searchInterestEntity(name, ["name=asc"], page, pageSize)
            .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                return http;
            });
    }

    loadInterestEntity(uris: Array<any>) {

        if (!uris || uris.length !== 1) {
            return undefined;
        }

        // in edit mode, the loaded entity is an object composed of uri and name
        if (uris[0].uri) {
            return [this.form.entity_of_interest];
        }
        return this.service.getInterestEntity( uris[0]).then(http =>
           [http.response.result]
        );
    }    

    setLoadedInterestEntity(created: InterestEntityCreationDTO) {
        this.form.entity_of_interest = created.uri;
        this.interestEntitySelector.select({id: created.uri, label: created.name});
    }
    
    searchCharacteristics(name: string, page, pageSize){
        return this.service
            .searchCharacteristics(name, ["name=asc"], page, pageSize)
            .then((http: HttpResponse<OpenSilexResponse<Array<NamedResourceDTO>>>) => {
                return http;
            });
    }

    loadCharacteristic(uris: Array<any>) {
        if (!uris || uris.length !== 1) {
            return undefined;
        }

        // in edit mode, the loaded characteristic is an object composed of uri and name
        if (uris[0].uri) {
            return [this.form.characteristic];
        }
        return this.service.getCharacteristic(uris[0]).then(http =>
            [http.response.result]
        );
    }

    setLoadedCharacteristic(created: CharacteristicCreationDTO) {
        this.form.characteristic = created.uri;
        this.characteristicSelector.select({id: created.uri, label: created.name});
    }

    searchMethods(name: string, page, pageSize){
        return this.service
            .searchMethods(name, ["name=asc"], page, pageSize)
            .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                return http;
            });
    }

    loadMethod(uris: Array<any>) {
        if (!uris || uris.length !== 1) {
            return undefined;
        }

        // in edit mode, the loaded characteristic is an object composed of uri and name
        if (uris[0].uri) {
            return [this.form.method];
        }
        return this.service.getMethod(uris[0]).then(http =>
            [http.response.result]
        );
    }

    setLoadedMethod(created: MethodCreationDTO) {
        this.form.method = created.uri;
        this.methodSelector.select({id: created.uri, label: created.name});
    }

    searchUnits(name: string ,page, pageSize){
        return this.service
            .searchUnits(name, ["name=asc"], page, pageSize)
            .then((http: HttpResponse<OpenSilexResponse<Array<any>>>) => {
                return http;
            });
    }

    loadUnit(uris: Array<any>) {
        if(! uris || uris.length !== 1){
            return undefined;
        }
        // in edit mode, the loaded unit is an object composed of uri and name
        if(uris[0].uri){
            return [this.form.unit];
        }
        return this.service.getUnit(uris[0]).then(http =>
            [http.response.result]
        );
    }

    setLoadedUnit(created: UnitCreationDTO) {
        this.form.unit = created.uri;
        this.unitSelector.select({id: created.uri, label: created.name});
    }

    objectToSelectNode(dto) {
        if (dto) {
            return {id: dto.uri, label: dto.name};
        }
        return null;
    }

    getEmptyTraitForm(){
        return {
            trait: this.form.trait,
            trait_name: this.form.trait_name
        };
    }
    updateVariableTrait(form){
        let uriFilled = (form.trait && form.trait.length > 0);
        let nameFilled = (form.trait_name && form.trait_name.length > 0);

        // is both name and uri are filled or empty, then update current variable
        if(uriFilled == nameFilled){
            this.form.trait = form.trait;
            this.form.trait_name = form.trait_name;
        }
    }

    loadDatatypes(){

        if(this.datatypes.length == 0){
            this.service.getDatatypes().then((http: HttpResponse<OpenSilexResponse<Array<VariableDatatypeDTO>>>) => {
                this.datatypes = http.response.result;
                this.updateDatatypeNodes();
            });
        }else{
            this.updateDatatypeNodes();
        }
    }

    updateDatatypeNodes(){
        this.datatypesNodes = [];
        for (let dto of this.datatypes) {
            let label: any = this.$t(dto.name);
            this.datatypesNodes.push({
                id: dto.uri,
                label: label.charAt(0).toUpperCase() + label.slice(1)
            });
        }
    }

    loadDataType(dataTypeUri: string){
        if(! dataTypeUri){
            return undefined;
        }
        let dataType = this.datatypesNodes.find(datatypeNode => datatypeNode.id == dataTypeUri);
        return [dataType];
    }



    private langUnwatcher;
    mounted() {
        this.langUnwatcher = this.$store.watch(
            () => this.$store.getters.language,
            () => this.loadDatatypes()
        );
    }

  get hasLinkedData() {
    if(! this.form && this.form.linked_data_nb){
      return true;
    }else{
      return this.form.linked_data_nb > 0;
    }

  }

  beforeDestroy() {
        this.langUnwatcher();
    }

    tutorial() {
        this.savedVariable = JSON.parse(JSON.stringify(this.form));
        this.variableTutorial.start();
    }

    continueFormEditing(){
        if(this.savedVariable){
            this.form = JSON.parse(JSON.stringify(this.savedVariable));
        }
    }

    get tutorialSteps(): any[] {
        return [
            {
                target: "#v-step-global",
                header: { title: this.$i18n.t("VariableView.title")},
                content: this.$i18n.t("VariableForm.tutorial.global"),
                params: {placement: "bottom"},
            },
            {
                target: "#v-step-entity",
                header: { title: this.$i18n.t("VariableView.entity")},
                content: this.$i18n.t("VariableForm.tutorial.entity"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-entity",
                header: { title: this.$i18n.t("VariableView.entity")},
                content: this.$i18n.t("VariableForm.tutorial.entity-check"),
                params: {placement: "right"},
            },
            {
                target: "#v-step-interestEntity",
                header: { title: this.$i18n.t("VariableView.entityOfInterest")},
                content: this.$i18n.t("VariableForm.tutorial.entityOfInterest"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-interestEntity",
                header: { title: this.$i18n.t("VariableView.entityOfInterest")},
                content: this.$i18n.t("VariableForm.tutorial.entityOfInterest-check"),
                params: {placement: "right"},
            },
            {
                target: "#v-step-characteristic",
                header: { title: this.$i18n.t("VariableView.characteristic")},
                content: this.$i18n.t("VariableForm.tutorial.characteristic"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-characteristic",
                header: { title: this.$i18n.t("VariableView.characteristic")},
                content: this.$i18n.t("VariableForm.tutorial.characteristic-check"),
                params: {placement: "right"},
            },
            {
                target: "#v-step-method",
                header: { title: this.$i18n.t("VariableView.method")},
                content: this.$i18n.t("VariableForm.tutorial.method"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-method",
                header: { title: this.$i18n.t("VariableView.method")},
                content: this.$i18n.t("VariableForm.tutorial.method-check"),
                params: {placement: "right"},
            },
            {
                target: "#v-step-unit",
                header: { title: this.$i18n.t("VariableView.unit")},
                content: this.$i18n.t("VariableForm.tutorial.unit"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-name",
                header: { title: this.$i18n.t("component.common.name")},
                content: this.$i18n.t("VariableForm.tutorial.name"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-alt",
                header: { title: this.$i18n.t("VariableForm.altName")},
                content: this.$i18n.t("VariableForm.tutorial.altName"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-species",
                header: { title: this.$i18n.t("component.experiment.species")},
                content: this.$i18n.t("VariableForm.tutorial.species"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-datatype",
                header: { title: this.$i18n.t("OntologyPropertyForm.data-type")},
                content: this.$i18n.t("VariableForm.tutorial.datatype"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-time-interval",
                header: { title: this.$i18n.t("VariableForm.time-interval")},
                content: this.$i18n.t("VariableForm.tutorial.time-interval"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-sampling-interval",
                header: { title: this.$i18n.t("VariableForm.sampling-interval")},
                content: this.$i18n.t("VariableForm.tutorial.sampling-interval"),
                params: {placement: "left"},
            },
            {
                target: "#v-step-description",
                header: { title: this.$i18n.t("component.common.description")},
                content: this.$i18n.t("VariableForm.tutorial.description"),
                params: {placement: "left"},
            },
        ];
    }
}
</script>

<style scoped>
    #traitButton {
        padding-top: 23px;
    }
</style>

<i18n>
en:
    VariableForm:
        variable: The variable
        add: Add variable
        edit: Edit variable
        altName: Alternative name
        entity-help: "Observed entity or event. e.g : Leaf, canopy, wind"
        entity-placeholder: Search and select an entity
        interestEntity-label: Entity of interest
        interestEntity-help: "Optional, must be provided if its different from the observed entity. It's the entity level that is characterised. e.g : plot, plant, area, genotype..."
        interestEntity-placeholder: Search and select an observation level
        characteristic-help: "Define what is measured/observed. e.g: temperature, infection level, weight, area"
        characteristic-placeholder: Search and select a characteristic
        method-placeholder: Search and select a method
        method-help : How it was measured. If you don't want to specify a method, select the standard method.
        unit-help: "Scale for ordinal variable (such as good, medium, bad...). e.g : kg/m2"
        unit-placeholder: Search and select an unit
        time-interval: Time interval
        time-interval-placeholder: Select an interval
        time-interval-help: Define the time between two data recording
        sampling-interval: Sample interval
        sampling-interval-placeholder: Select an interval
        sampling-interval-help: Granularity of sampling
        synonym: Synonym
        trait-name: Trait name
        trait-name-help: Variable trait name (Describe the trait name if a trait uri has been specified)
        trait-name-placeholder:  Number of grains per square meter
        trait-uri: trait uri
        trait-uri-help: Variable trait unique identifier (Can be used to link to an existing trait for interoperability)
        trait-uri-placeholder: http://purl.obolibrary.org/obo/WTO_0000171
        class-placeholder: Select a type
        no-entity: Unknown entity. Add one with the + button.
        no-interestEntity: Unknown entity of interest. Add one with the + button.
        no-characteristic: Unknown characteristic. Add one with the + button.
        no-method: Unknown method. Add one with the + button.
        no-unit: Unknown unit. Add one with the + button.
        trait-form-create-title: Add trait
        trait-form-edit-title: Edit trait
        trait-button: Trait already existing in an ontology
        trait-button-help: Add a trait (entity and characteristic) already existing in an ontology
        datatype-help: Format of data recorded for this variable. (Can't be updated while they are some data linked to this variable).
        datatype-placeholder: Select a datatype
        dimension-values:
            unique: Unique measurement
            millisecond : Millisecond
            second: Second
            minute: Minutes
            hour: Hour
            day: Day
            week: Week
            month: Month
            year: Year
            mm: Millimeter
            cm: Centimeter
            m: Meter
            km: Kilometer
            field: Field
            region: Region
        already-exist: the variable already exist
        tutorial:
            global: "Create a variable : Before creating a new variable, make sur you check the existing ones in order to avoid duplicates. For example 'grain yield at harvest'."
            entity: "Select the entity that is the object of the observation/measurement. Here 'Grain'."
            entity-check: "If the entity is not already present in the list you can add it. Double check if there is no other spelling - seed, crop, etc."
            entityOfInterest: "Select the entity of interest that is the object of the observation/measurement."
            entityOfInterest-check: "If the entity of interest is not already present in the list you can add it. Double check if there is no other spelling."
            characteristic: "Select the measured characteristic. Here 'Yield' "
            characteristic-check: "If the characteristic is not in the list you can add it. Double check if it is not already present under another name."
            method: "Select the method that is associated with this variable. In our case this is a yield sensor onboard the harvester."
            method-check: "If the method is not present you can add it. Don't neglect the description as it is especially important for methods."
            unit: "Select the unit in which the variable is measured. What should I do if the unit is different from what I have measured ? I can select kg/ha, but my measurements are in t/ha.
                1 - I convert the measurements I have into the appropriate unit.
                2 - I declare a new Unit. This is highly advised to not create too many units and prefer convert into the existing units."
            name: "Precise the variable name. By default this field is auto filled according the entity and characteristic name, but it can be filled manually."
            altName: "Precise the alternative variable name if it exist. By default this field is auto filled according the entity, characteristic, method and unit names, but this field can be filled manually."
            time-interval: "Precise the time interval which associated with this variable. Here we obtained the grain yield each month."
            sampling-interval: "Precise the sample interval which is associated with this variable. Here we obtained the grain yield by harvesting experimental microplot (10m * 2.5m)."
            datatype: "Precise the data type. Here we are using decimal numbers."
            description: "Finalize the variable with some text description of it."
            species: "Select the species that is associated with this variable. Here rice."
        example:
            entity: "Seed"
            characteristic: "Yield"
            method: "Harvest yield sensor"
            unit: "Kilogram per hectare"
            name: "grain_yield"
            altName: "grain_yield_harvest_yield_sensor_kilogram_per_hectare"
            time-interval: "Month"
            sampling-interval: "Meter"
            datatype: "http://www.w3.org/2001/XMLSchema#decimal"
            description: "Grain yield obtained after harvesting an experimental microplot"
            species: "Rice"
fr:
    VariableForm:
        variable: La variable
        add: Ajouter une variable
        edit: Éditer une variable
        altName: Nom alternatif
        entity-help: "Entité observée ou évènement sur lequel porte la mesure/l'observation. ex : Feuille, canopée, vent"
        entity-placeholder: Rechercher et sélectionner une entité
        interestEntity-label: Entité d'intérêt
        interestEntity-help: "Optionnelle, doit être spécifiée si différente de l'entité observée. C'est le niveau d'entité qui est caractérisé. ex : parcelle, plante, zone, génotype..."
        interestEntity-placeholder: Rechercher et sélectionner un niveau d'observation
        characteristic-help: "Ce qui est mesurée/observé. ex : Température, taux d'infection, masse, surface"
        characteristic-placeholder: Rechercher et sélectionner une caractéristique
        method-help: Définir comment la mesure/l'observation a été effectuée. Si vous ne voulez pas spécifier de méthode, veuillez sélectionner la méthode standard.
        method-placeholder: Rechercher et sélectionner une méthode
        unit-help: "Echelle de la variable ordinale (tel que bon, moyen, mauvais...). ex: kg/m2"
        unit-placeholder: Rechercher et sélectionner une unité
        time-interval: Intervalle de temps
        time-interval-placeholder: Sélectionner un intervalle
        time-interval-help: Durée entre deux enregistrements de données
        sampling-interval: Échantillonnage
        sampling-interval-placeholder: Sélectionner un intervalle
        sampling-interval-help: Granularité de l'échantillonage
        synonym : Synonyme
        trait-name: Nom du trait
        trait-name-help: Nom du trait (si une URI décrivant un trait a été saisie)
        trait-name-placeholder:  Nombre de grains par mètre carré
        trait-uri: URI du trait
        trait-uri-help: Identifiant unique d'un trait (Peut être utilisé pour lier cette variable avec l'URI d'un trait existant)
        trait-uri-placeholder: http://purl.obolibrary.org/obo/WTO_0000171
        class-placeholder: Sélectionner un type
        no-entity: Entité inconnue. L'ajouter avec le bouton +.
        no-interestEntity: Entité d'intérêt inconnue. L'ajouter avec le bouton +.
        no-characteristic: Caractéristique inconnue. L'ajouter avec le bouton +.
        no-method: Méthode inconnue. L'ajouter avec le bouton +.
        no-unit: Unité inconnue. L'ajouter avec le bouton +.
        trait-form-create-title: Ajouter un trait
        trait-form-edit-title: Éditer un trait
        trait-button: Trait existant déjà dans une ontologie
        trait-button-help: Ajouter un trait (entité et caractéristique) existant déjà dans une ontologie
        datatype-help: Format des données enregistrées pour cette variable. (Ne peut être mis à jour si des données sont liées à cette variable).
        datatype-placeholder: Sélectionner un type de donnée
        dimension-values:
            unique: Enregistrement unique
            millisecond : Milliseconde
            second: Seconde
            minute: Minute
            hour: Heure
            day: Jour
            week: Semaine
            month: Mois
            year: Année
            mm: Millimètre
            cm: Centimètre
            m: Mètre
            km: Kilomètre
            field: Champ
            region: Région
        already-exist: la variable existe déjà
        tutorial:
            global: "Création de variable : Avant de créer une variable, soyez bien sûr d'avoir vérifié la liste existante pour ne pas introduire de doublon. Par exemple 'Rendement du grain à la récolte'."
            entity: "Sélectionner l'entité sur laquelle la variable est mesurée/observée. Ici le 'grain'."
            entity-check: "Si l'entité n'est pas dans la liste, vous pouvez la créer. Vérifier toutefois des orthographes alternatives - seed, crop, etc."
            entityOfInterest: "Sélectionner l'entité d'intérêt sur laquelle la variable est mesurée/observée."
            entityOfInterest-check: "Si l'entité d'intérêt n'est pas dans la liste, vous pouvez la créer. Vérifier toutefois des orthographes alternatives."
            characteristic: "Sélectionner la caractéristique mesurée. Ici 'rendement'."
            characteristic-check: "Si la caractéristique n'est pas dans la liste, vous pouvez l'ajouter. Vérifier encore une fois que la caractéristique n'est pas présente sous un autre nom."
            method: " Sélectionner la méthode qui vous a permis de réaliser cette variable. Dans notre cas, un capteur embarqué à bord de la moissoneuse-batteuse."
            method-check: "Si la méthode n'est pas présente, vous pouvez l'ajouter. Ne pas oublier de bien renseigner la description, c'est particulièrement important pour la méthode."
            unit: "Sélectionner l'unité dans laquelle est exprimée la variable. Que faire si l'unité proposée ne correspond pas à ma mesure ? On me propose kg/ha, mais j'ai des mesures en t/ha ?
                1 - Je convertie ma variable dans la bonne unité.
                2 - Je crée une nouvelle unité. Il vaut mieux limiter la création de multiples unités, privilégier la conversion."

            name: "Renseigner le nom de cette variable. Par défault ce champ est rempli automatiquement en fonction de l'entité et de la caractéristique, mais il peut être rempli manuellement."
            altName: "Renseigner le nom alternatif de cette variable si il existe. Par défault ce champ est rempli automatiquement en fonction de l'entité, de la caractéristique, de la méthode et de l'unité, mais il peut être rempli manuellement."
            time-interval: "Renseigner le pas-de-temps qui a permis d'obtenir cette variable. Ici le rendement est mesuré chaque mois."
            sampling-interval: "Renseigner l'échantillonnage qui a permis d'obtenir cette variable. Ici on a obtenu le rendement sur une microparcelle expérimentale de taille standard (2.5m*10m)."
            datatype: "Renseigner le type de données. Ici nous avons des nombre décimaux."
            description: "Finaliser la variable avec une description textuelle de la variable."
            species: "Sélectionner l'espèce associée à la variable variable. Ici le 'riz'."
        example:
            entity: "Grain"
            characteristic: "Rendement"
            method: "Capteur de rendement de la moissoneuse-batteuse"
            unit: "Kilogramme par hectare"
            name: "rendement_grain"
            altName: "rendement_grain_capteur_rendement_moissoneuse_batteuse_kilogramme_par_hectare"
            time-interval: "Mois"
            sampling-interval: "Mètre"
            datatype: "http://www.w3.org/2001/XMLSchema#decimal"
            description: "Rendement du grain obtenu après récolte d'une microparcelle expérimentale."
            species: "Riz"
</i18n>

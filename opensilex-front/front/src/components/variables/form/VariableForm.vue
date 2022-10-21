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
                <div class="col-lg-6" id="v-step-entity">
                    <!-- Entity -->
                    <opensilex-SelectForm
                        ref="entitySelectForm"
                        label="VariableView.entity"
                        :selected.sync="form.entity"
                        :multiple="false"
                        :required="true"
                        :searchMethod="searchEntities"
                        :itemLoadingMethod="loadEntity"
                        placeholder="VariableForm.entity-placeholder"
                        :conversionMethod="objectToSelectNode"
                        noResultsText="VariableForm.no-entity"
                        helpMessage="VariableForm.entity-help"
                        @select="updateEntity"
                        :actionHandler="showEntityCreateForm"
                    ></opensilex-SelectForm>
                    <opensilex-EntityCreate
                        ref="entityForm"
                        @onCreate="setLoadedEntity">
                    </opensilex-EntityCreate>
                </div>

                <!-- Entity of interest -->
                <div class="col-lg-6" id="v-step-interestEntity">
                    <opensilex-SelectForm
                        ref="interestEntitySelectForm"
                        label="VariableForm.interestEntity-label"
                        :selected.sync="form.entity_of_interest"
                        :multiple="false"
                        :required="false"
                        :searchMethod="searchInterestEntities"
                        :itemLoadingMethod="loadInterestEntity"
                        placeholder="VariableForm.interestEntity-placeholder"
                        :conversionMethod="objectToSelectNode"
                        noResultsText="VariableForm.no-interestEntity"
                        helpMessage="VariableForm.interestEntity-help"
                        :actionHandler="showInterestEntityCreateForm"
                    ></opensilex-SelectForm>
                    <opensilex-InterestEntityCreate
                        ref="interestEntityForm"
                        @onCreate="setLoadedInterestEntity">
                    </opensilex-InterestEntityCreate>
                </div>

                <!-- Characteristic -->
                <div class="col-lg-6" id="v-step-characteristic">                    
                    <opensilex-SelectForm
                        ref="characteristicSelectForm"
                        label="VariableView.characteristic"
                        :selected.sync="form.characteristic"
                        :multiple="false"
                        :required="true"
                        :searchMethod="searchCharacteristics"
                        :itemLoadingMethod="loadCharacteristic"
                        placeholder="VariableForm.characteristic-placeholder"
                        :conversionMethod="objectToSelectNode"
                        noResultsText="VariableForm.no-characteristic"
                        helpMessage="VariableForm.characteristic-help"
                        @select="updateCharacteristic"
                        :actionHandler="showCharacteristicCreateForm"
                    ></opensilex-SelectForm>
                    <opensilex-CharacteristicModalForm
                        ref="characteristicForm"
                        @onCreate="setLoadedCharacteristic">
                    </opensilex-CharacteristicModalForm>
                </div>

                <!-- Species -->
                <div class="col-lg-6" id="v-step-species">
                    <opensilex-SpeciesSelector
                        v-if="!isGermplasmMenuExcluded"
                        label="SpeciesSelector.select-multiple"
                        placeholder="SpeciesSelector.select-multiple-placeholder"
                        :multiple="true"
                        :species.sync="form.species"
                    ></opensilex-SpeciesSelector>
                </div>

                <!-- Method -->
                <div class="col-lg-6" id="v-step-method">
                    <opensilex-SelectForm
                        ref="methodSelectForm"
                        label="VariableView.method"
                        :selected.sync="form.method"
                        :multiple="false"
                        :required="true"
                        :searchMethod="searchMethods"
                        :itemLoadingMethod="loadMethod"
                        placeholder="VariableForm.method-placeholder"
                        :conversionMethod="objectToSelectNode"
                        helpMessage="VariableForm.method-help"
                        @select="updateMethod"
                        :actionHandler="showMethodCreateForm"
                        noResultsText="VariableForm.no-method"
                    ></opensilex-SelectForm>
                    <opensilex-MethodCreate
                        ref="methodForm"
                        @onCreate="setLoadedMethod">
                    </opensilex-MethodCreate>
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
                    <opensilex-SelectForm
                        ref="unitSelectForm"
                        label="VariableView.unit"
                        :selected.sync="form.unit"
                        :multiple="false"
                        :required="true"
                        :searchMethod="searchUnits"
                        :itemLoadingMethod="loadUnit"
                        :conversionMethod="objectToSelectNode"
                        helpMessage="VariableForm.unit-help"
                        placeholder="VariableForm.unit-placeholder"
                        @select="updateUnit"
                        :actionHandler="showUnitCreateForm"
                        noResultsText="VariableForm.no-unit"
                    ></opensilex-SelectForm>
                    <opensilex-UnitCreate
                        ref="unitForm"
                        @onCreate="setLoadedUnit">
                    </opensilex-UnitCreate>
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

                <!-- altName -->
                <div class="col-lg-6" id="v-step-alt">
                    <opensilex-InputForm
                        :value.sync="form.alternative_name"
                        label="VariableForm.altName"
                        type="text"
                    ></opensilex-InputForm>
                </div>

                <!-- DataType -->
                <div class="col-lg-6" id="v-step-datatype">
                    <opensilex-SelectForm
                        label="OntologyPropertyForm.data-type"
                        :required="true"
                        :disabled="hasLinkedData"
                        :selected.sync="form.datatype"
                        :options="datatypesNodes"
                        :itemLoadingMethod="loadDataType"
                        helpMessage="VariableForm.datatype-help"
                        placeholder="VariableForm.datatype-placeholder"
                    ></opensilex-SelectForm>
                </div>

                <!-- time-interval -->
                <div class="col-lg-6" id="v-step-time-interval">
                    <opensilex-SelectForm
                        label="VariableForm.time-interval"
                        :selected.sync="form.time_interval"
                        :multiple="false"
                        :options="periodList"
                        placeholder="VariableForm.time-interval-placeholder"
                        helpMessage="VariableForm.time-interval-help"
                    ></opensilex-SelectForm>
                </div>

                <!-- div d'occupation d'espace permettant de mieux positionner le prochain composant -->
                <div class="col-lg-6"></div>

                <!-- sample/distance-interval -->
                <div class="col-lg-6" id="v-step-sampling-interval">
                    <opensilex-SelectForm
                        label="VariableForm.sampling-interval"
                        :selected.sync="form.sampling_interval"
                        :multiple="false"
                        :options="sampleList"
                        placeholder="VariableForm.sampling-interval-placeholder"
                        helpMessage="VariableForm.sampling-interval-help"
                    ></opensilex-SelectForm>
                </div>

                <!-- description -->
                <div class="col-xl-12" id="v-step-description">
                    <opensilex-TextAreaForm :value.sync="form.description" label="component.common.description">
                    </opensilex-TextAreaForm>
                </div>
                
                <!-- variables groups-->
                <!-- <div class="col-xl-12">
                    <opensilex-GroupVariablesTable
                        ref="groupVariablesTable"
                        :variablesGroupArray="variablesGroupArray"
                    ></opensilex-GroupVariablesTable>
                </div> -->
            </div>
        </ValidationObserver>
    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import ModalForm from "../../common/forms/ModalForm.vue";
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
import SelectForm from "../../common/forms/SelectForm.vue";

@Component
export default class VariableForm extends Vue {
    $opensilex: any;
    $store: any;

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

    @Ref("entitySelectForm") entitySelectForm!: SelectForm;
    @Ref("interestEntitySelectForm") interestEntitySelectForm!: any;
    @Ref("characteristicSelectForm") characteristicSelectForm!: any;
    @Ref("methodSelectForm") methodSelectForm!: any;
    @Ref("unitSelectForm") unitSelectForm!: any;

    @Ref("entityForm") readonly entityForm!: any;
    @Ref("interestEntityForm") readonly interestEntityForm!: any;
    @Ref("characteristicForm") readonly characteristicForm!: any;
    @Ref("methodForm") readonly methodForm!: any;
    @Ref("unitForm") readonly unitForm!: any;

    @Ref("traitForm") readonly traitForm!: ModalForm;

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

        for(let period of ["millisecond","second","minute","hour","day","week","month","unique"]){
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

    // variablesGroupArray = [];
    // setVariablesGroups(form) {
    //     this.variablesGroupArray = [];
    //     if (form.relations != null) {  
    //         form.variablesGroup.forEach(variablesGroup => {
    //             if(variablesGroup.uri != null){
    //                 this.variablesGroupArray.push(variablesGroup);
    //             }
    //         })        
    //     }
    // }

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
//        variablesGroup: []
      };
    }

    getEmptyForm() {
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
        this.entitySelectForm.select({id: created.uri, label: created.name});
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
        this.interestEntitySelectForm.select({id: created.uri, label: created.name});
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
        this.characteristicSelectForm.select({id: created.uri, label: created.name});
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
        this.methodSelectForm.select({id: created.uri, label: created.name});
    }

    searchUnits(name: string ,page, pageSize){
        return this.service
            .searchUnits(name, ["name=asc"], page,pageSize)
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
        this.unitSelectForm.select({id: created.uri, label: created.name});
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

        this.form.entity = { uri: this.$i18n.t("VariableForm.example.entity"),  name: this.$i18n.t("VariableForm.example.entity")};
        this.form.characteristic = { uri: this.$i18n.t("VariableForm.example.characteristic"),  name: this.$i18n.t("VariableForm.example.characteristic")};
        this.form.method = { uri: this.$i18n.t("VariableForm.example.method"),  name: this.$i18n.t("VariableForm.example.method")};
        this.form.unit = { uri: this.$i18n.t("VariableForm.example.unit"),  name: this.$i18n.t("VariableForm.example.unit")};

        this.form.name = this.$i18n.t("VariableForm.example.name");
        this.form.alternative_name = this.$i18n.t("VariableForm.example.altName");
        this.form.dataType = this.$i18n.t("VariableForm.example.datatype");
        this.form.time_interval =  this.$i18n.t("VariableForm.example.time-interval");
        this.form.sampling_interval =  this.$i18n.t("VariableForm.example.sampling-interval");
        this.form.comment =  this.$i18n.t("VariableForm.example.description");

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
        interestEntity-label: Observation level
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
        interestEntity-label: Niveau d'observation
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

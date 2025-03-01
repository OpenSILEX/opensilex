<template>
    <div class="container-fluid">
        <opensilex-PageHeader
            icon="fa#vials"
            title="VariableView.title"
            description="VariableView.description"
            class="detail-element-header"
        ></opensilex-PageHeader>
        <div>
            <opensilex-PageActions>
                <div>
                    <b-tabs content-class="mt-3" :value=elementIndex @input="updateType">
                        <b-tab :title="$t('component.menu.variables')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.entity')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.entityOfInterest')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.characteristic')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.method')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.unit')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.groupVariable')" @click="refreshSelected"></b-tab>
                    </b-tabs>
                </div>
                </opensilex-PageActions>
                <div class="col-lg-5">
                    <opensilex-PageActions
                    >
                        <opensilex-HelpButton
                            @click="helpModal.show()"
                            label="component.common.help-button"
                            class="helpButton"
                        ></opensilex-HelpButton>
                        
                        <b-modal ref="helpModal" size="xl" hide-header hide-footer>
                            <opensilex-VariableHelp v-if="elementType != 'VariableGroup'" @hideBtnIsClicked="hide()"></opensilex-VariableHelp>
                            <opensilex-GroupVariablesHelp v-else @hideBtnIsClicked="hide()"></opensilex-GroupVariablesHelp>
                        </b-modal>

                        <opensilex-CreateButton
                            v-show="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                            @click="showCreateForm"
                            :label="buttonTitle"
                            class="createButton"
                        ></opensilex-CreateButton>

                    </opensilex-PageActions>
                </div>
            <opensilex-PageContent
                v-if="loadVariableList()" >
                <template v-slot>
                    <opensilex-VariableList
                        ref="variableList"
                        @onEdit="showVariableEditForm" 
                        @onInteroperability="showVariableReferences"
                        @onDelete="deleteVariable"
                        @onReset="refreshRouteName"
                    ></opensilex-VariableList>
                </template>
            </opensilex-PageContent>

            <opensilex-VariableCreate
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                ref="variableCreate"
                @onCreate="showVariableDetails"
                @onUpdate="showVariableDetails"
            ></opensilex-VariableCreate>

            <!-- Create form -->
          <opensilex-AgroportalEntityForm
            ref="entityForm"
            @onCreate="refresh($event.uri)"
            @onUpdate="refresh($event.uri)"
          ></opensilex-AgroportalEntityForm>
          <opensilex-AgroportalEntityOfInterestForm
            ref="interestEntityForm"
            @onCreate="refresh($event.uri)"
            @onUpdate="refresh($event.uri)"
          ></opensilex-AgroportalEntityOfInterestForm>
          <opensilex-AgroportalMethodForm
            ref="methodForm"
            @onCreate="refresh($event.uri)"
            @onUpdate="refresh($event.uri)"
          ></opensilex-AgroportalMethodForm>
          <opensilex-AgroportalCharacteristicForm
            ref="characteristicForm"
            @onCreate="refresh($event.uri)"
            @onUpdate="refresh($event.uri)"
          ></opensilex-AgroportalCharacteristicForm>
          <opensilex-AgroportalUnitForm
            ref="unitForm"
            @onCreate="refresh($event.uri)"
            @onUpdate="refresh($event.uri)"
          ></opensilex-AgroportalUnitForm>

            <opensilex-ModalForm
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID) && loadGroupForm"
                ref="groupVariablesForm"
                modalSize="lg"
                :tutorial="false"
                @onCreate="refresh()"
                @onUpdate="refresh()"
                component="opensilex-GroupVariablesForm"
                createTitle="GroupVariablesForm.add"
                editTitle="GroupVariablesForm.edit"
            ></opensilex-ModalForm>

            <opensilex-ExternalReferencesModalForm
                ref="skosReferences"
                :references.sync="selected"
                @onUpdate="updateReferences"
            ></opensilex-ExternalReferencesModalForm>

            <div class="row">
                <div class="col-md-6">
                    <!-- Element list -->
                    <opensilex-VariableStructureList
                        v-if="! loadVariableList() "
                        ref="variableStructureList"
                        @onSelect="updateSelected"
                        @onDelete="updateSelected"
                        @onEdit="showEditForm"
                        :_type.sync="this.elementType"
                    ></opensilex-VariableStructureList>
                </div>

                <div class="col-md-6">
                    <!-- Element details page -->
                    <opensilex-VariableStructureDetails v-show="!loadVariableList() && useGenericDetailsPage()" :selected="selected"></opensilex-VariableStructureDetails>
                    <!-- Unit specialized details page -->
                    <opensilex-UnitDetails v-show="!loadVariableList() && !useGenericDetailsPage() && !groupVariablesPage()" :selected="selected"></opensilex-UnitDetails>

                    <opensilex-Card
                        v-show="!groupVariablesPage() && !loadVariableList()" label="component.skos.ontologies-references-label" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-ExternalReferencesDetails :skosReferences="selected"></opensilex-ExternalReferencesDetails>
                        </template>
                    </opensilex-Card>

                    <opensilex-Card v-if="!loadVariableList() && useGenericDetailsPage() && groupVariablesPage() && selected.variables != undefined" :label="$t('VariableView.type')" :selected="selected" icon="fa#vials">
                        <template v-slot:body>

                            <opensilex-TableView
                            v-if="groupVariablesPage() && selected.variables.length !== 0"
                            :items="selected.variables"
                            :fields="relationsFields"
                            :globalFilterField="true"
                            sortBy="name">

                                <template v-slot:cell(name)="{data}">
                                    <opensilex-UriLink
                                    :uri="data.item.uri"
                                    :value="data.item.name"
                                    :to="{path: '/variable/details/'+ encodeURIComponent(data.item.uri)}">
                                    </opensilex-UriLink>
                                </template> 

                            </opensilex-TableView>

                            <p v-else><strong>{{$t("VariableView.no-var-provided")}}</strong></p>

                        </template>
                    </opensilex-Card>

                    <opensilex-DocumentTabList
                        v-if="selected && selected.uri"
                        v-show="! loadVariableList() && useGenericDetailsPage() && (documentMethodPage() || groupVariablesPage())"  
                        :selected="selected"
                        :uri="[selected.uri]"
                        :search=false
                    ></opensilex-DocumentTabList>

                </div>
            </div>
        </div>

    </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import VariableStructureList from "./views/VariableStructureList.vue";
import VariableCreate from "./form/VariableCreate.vue";
import VariableList from "./VariableList.vue";
import ExternalReferencesModalForm from "../common/external-references/ExternalReferencesModalForm.vue";
import { VariablesService, DataService, VariableUpdateDTO } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import ModalForm from "../common/forms/ModalForm.vue";
import DTOConverter from "../../models/DTOConverter";
import {VariablesGroupCreationDTO} from "opensilex-core/model/variablesGroupCreationDTO";
import {VariablesGroupUpdateDTO} from "opensilex-core/model/variablesGroupUpdateDTO";
import GroupVariablesForm from "../groupVariable/GroupVariablesForm.vue";
import {VariablesGroupGetDTO} from "opensilex-core/model/variablesGroupGetDTO";
import {NamedResourceDTOVariableModel} from "opensilex-core/model/namedResourceDTOVariableModel";

interface GroupUpdateDtoAndVariableModels {
  updateDto: VariablesGroupUpdateDTO,
  variableModels: Array<NamedResourceDTOVariableModel>
}
import {BaseExternalReferencesForm} from "../common/external-references/ExternalReferencesTypes";

@Component
export default class VariablesView extends Vue {

    $opensilex: OpenSilexVuePlugin
    $store: any;
    $route: any;
    $router: any;
    $i18n: any;
    service: VariablesService;
    dataService: DataService;

    elementIndex: number = 0;
    elementType: string = VariablesView.VARIABLE_TYPE;

    static VARIABLE_TYPE: string = "Variable";
    static ENTITY_TYPE: string = "Entity";
    static INTEREST_ENTITY_TYPE: string = "InterestEntity";
    static CHARACTERISTIC_TYPE: string = "Characteristic";
    static METHOD_TYPE: string = "Method";
    static UNIT_TYPE:  string = "Unit";
    static GROUP_VARIABLE_TYPE: string = "VariableGroup";

    static elementTypes = [
        VariablesView.VARIABLE_TYPE,
        VariablesView.ENTITY_TYPE,
        VariablesView.INTEREST_ENTITY_TYPE,
        VariablesView.CHARACTERISTIC_TYPE,
        VariablesView.METHOD_TYPE,
        VariablesView.UNIT_TYPE,
        VariablesView.GROUP_VARIABLE_TYPE
    ]

    @Ref("variableCreate") readonly variableCreate!: VariableCreate;
    @Ref("entityForm") readonly entityForm!: BaseExternalReferencesForm;
    @Ref("interestEntityForm") readonly interestEntityForm!: BaseExternalReferencesForm;
    @Ref("characteristicForm") readonly characteristicForm!: BaseExternalReferencesForm;
    @Ref("methodForm") readonly methodForm!: BaseExternalReferencesForm;
    @Ref("unitForm") readonly unitForm!: BaseExternalReferencesForm;

    /**
     * Lazy loading of modal group form, this ensures to not load nested variable selected which trigger an API call
     */
    loadGroupForm: boolean = false;
    @Ref("groupVariablesForm") readonly groupVariablesForm!: ModalForm<GroupVariablesForm, VariablesGroupCreationDTO, VariablesGroupUpdateDTO>

    @Ref("skosReferences") skosReferences!: ExternalReferencesModalForm;

    @Ref("variableList") readonly variableList!: VariableList;
    @Ref("variableStructureList") readonly variableStructureList!: VariableStructureList;

    @Ref("helpModal") readonly helpModal!: any;

    buttonTitle;

    relationsFields: any[] = [
      {
        key: "name",
        label: "component.common.name",
        sortable: true,
      }
    ];

    created(){
        this.service = this.$opensilex.getService("opensilex-core.VariablesService");
        this.dataService = this.$opensilex.getService("opensilex-core.DataService");

        // load variable list by default
        let query: any = this.$route.query;
        if(query && query.elementType){
            let requestedTypeIdx = VariablesView.elementTypes.findIndex(elem => elem == decodeURIComponent(query.elementType));
            this.updateType(requestedTypeIdx);
        }else{
            let variableIdx = VariablesView.elementTypes.findIndex(elem => elem == VariablesView.VARIABLE_TYPE);
            this.updateType(variableIdx);
        }
    }

    private refreshRouteName(){
        if(this.$route.query.name) {
            this.$route.query.name = undefined;
            this.$opensilex.updateURLParameter("name", "");
        }
    }

    private updateType(tabIndex): void {
        if(tabIndex >= 0 && tabIndex < VariablesView.elementTypes.length){

            this.elementIndex = tabIndex;
            this.elementType = VariablesView.elementTypes[this.elementIndex];
            this.$opensilex.updateURLParameter("elementType",this.elementType);
            this.buttonTitle = "VariableView."+this.getButtonTitleTranslationSubKey();

            if(this.elementType == VariablesView.VARIABLE_TYPE){
                this.$opensilex.updateURLParameter("selected","");
                if (this.variableList) {
                    this.variableList.refresh();
                }
            }else{
                
                if(this.variableStructureList){
                    // update entity list with the new elementType value
                    this.$nextTick(() => {
                        this.$opensilex.updateURLParameter("name", "");
                        this.$opensilex.updateURLParameter("selected", "");
                        this.variableStructureList.refresh(true);
                    });
                }
            }
        }
    }

    private loadVariableList() : boolean {
        return this.elementType === VariablesView.VARIABLE_TYPE;
    }

    private useGenericDetailsPage() : boolean{
        return this.elementType != VariablesView.UNIT_TYPE;
    }

    private documentMethodPage() : boolean{
        return this.elementType == VariablesView.METHOD_TYPE;
    }

    private groupVariablesPage() : boolean{
        return this.elementType == 'VariableGroup';
    }

    selected = {
        uri: undefined,
        name: undefined,
        comment : undefined,
        type : undefined,
        typeLabel : undefined,
        exact_match: [],
        close_match: [],
        broad_match: [],
        narrow_match: []
    }

    private refreshSelected(){
        this.selected = {
            uri: undefined,
            name: undefined,
            comment : undefined,
            type : undefined,
            typeLabel : undefined,
            exact_match: [],
            close_match: [],
            broad_match: [],
            narrow_match: []
        }
    }

    get user() {
        return this.$store.state.user;
    }

    get credentials() {
        return this.$store.state.credentials;
    }

    updateSelected(newSelection) {
        this.selected = newSelection;
    }

    refresh(uri? : string) {
        this.variableStructureList.refresh(false, uri);
    }

    private getForm(): BaseExternalReferencesForm {
        switch (this.elementType) {
            case VariablesView.VARIABLE_TYPE : {
                return this.variableCreate;
            }
            case VariablesView.ENTITY_TYPE : {
                return this.entityForm;
            }
            case VariablesView.INTEREST_ENTITY_TYPE : {
                return this.interestEntityForm;
            }
            case VariablesView.CHARACTERISTIC_TYPE : {
                return this.characteristicForm;
            }
            case VariablesView.METHOD_TYPE: {
                return this.methodForm;
            }
            case VariablesView.UNIT_TYPE: {
                return this.unitForm;
            }
            case VariablesView.GROUP_VARIABLE_TYPE: {
                return this.groupVariablesForm;
            }
            default : {
                return this.variableCreate;
            }
        }
    }

    private getButtonTitleTranslationSubKey(): string {
        switch (this.elementType) {
            case VariablesView.VARIABLE_TYPE : {
                return "add-variable";
            }
            case VariablesView.ENTITY_TYPE : {
                return "add-entity";
            }
            case VariablesView.INTEREST_ENTITY_TYPE : {
                return "add-entityOfInterest";
            }
            case VariablesView.CHARACTERISTIC_TYPE : {
                return "add-characteristic";
            }
            case VariablesView.METHOD_TYPE: {
                return "add-method";
            }
            case VariablesView.UNIT_TYPE: {
                return "add-unit";
            }
            case VariablesView.GROUP_VARIABLE_TYPE: {
                return "add-groupVariable"
            }
            default : {
                return "add-variable";
            }
        }
    }


    showCreateForm(){

        // ensure that the group form component is loaded
        if(this.elementType == VariablesView.GROUP_VARIABLE_TYPE){
            this.loadGroupForm = true;
            this.$nextTick(() => {
                this.getForm().showCreateForm();
            })
        }else{
            this.getForm().showCreateForm();
        }
    }

    formatVariablesGroup(dto : VariablesGroupGetDTO) : GroupUpdateDtoAndVariableModels{
        let copy = JSON.parse(JSON.stringify(dto));
        let variables_uris : Array<String> = [];
        let new_variable_list : Array<NamedResourceDTOVariableModel>= [];
      if (copy.variables) {
        for(let variable of copy.variables){
          let variableUri = this.$opensilex.getLongUri(variable.uri);
          variables_uris.push(variableUri);
          variable.uri = variableUri;
          new_variable_list.push(variable);
        }
        copy.variables = variables_uris;
      }
      return {updateDto : (copy as VariablesGroupUpdateDTO), variableModels : new_variable_list};
    }


    showEditForm(dto : any){
        if (this.elementType == VariablesView.GROUP_VARIABLE_TYPE) {
            // ensure that the group form component is loaded
            this.loadGroupForm = true;
            this.$nextTick(() => {

              let updateDtoAndExtractedVariables = this.formatVariablesGroup(dto);
              this.groupVariablesForm.setSelectorsToFirstTimeOpenAndSetLabels(updateDtoAndExtractedVariables.variableModels);
              this.groupVariablesForm.showEditForm(updateDtoAndExtractedVariables.updateDto);
            });
        }
        else{
            this.getForm().showEditForm(dto);
        }
    }

    getCountDataPromise(variable: string){
      // Check if there exist at least one data linked to the variable
      return this.dataService.countData(
          undefined,
          undefined,
          undefined,
          undefined,
          [variable],
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          1,
          undefined,
      );
    }

    showVariableEditForm(uri: string) {

        this.getCountDataPromise(uri).then(countResult => {
            if (countResult && countResult.response) {
                this.service.getVariable(uri).then((getResult: HttpResponse<OpenSilexResponse>) => {
                    if (getResult && getResult.response) {
                        this.variableCreate.showEditForm(getResult.response.result,countResult.response.result);
                    }
                });
            }
        })
    }

    updateReferences(variable){

        let formattedVariable: VariableUpdateDTO = DTOConverter.extractURIFromResourceProperties(variable);

        this.service.updateVariable(formattedVariable).then(() => {
            let message = this.$i18n.t("VariableView.name") + " " + formattedVariable.name + " " + this.$i18n.t("component.common.success.update-success-message");
            this.$opensilex.showSuccessToast(message);
        }).catch(this.$opensilex.errorHandler);
    }

    deleteVariable(uri: string) {
            this.getCountDataPromise(uri)
            .then((http: HttpResponse<OpenSilexResponse<number>>) => {
                let count = http.response.result;
                if(count > 0){
                    this.$opensilex.showErrorToast(count + " "+this.$i18n.t("VariableView.associated-data-error"));
                }else{
                    this.service.deleteVariable(uri).then(() => {
                        this.variableList.refresh();
                        let message = this.$i18n.t("VariableView.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
                        this.$opensilex.showSuccessToast(message);
                    }).catch(this.$opensilex.errorHandler);
                }
            });

    }

    showVariableDetails(variable) {
        this.$store.commit("storeReturnPage", this.$router);
        this.$router.push({path: "/variable/details/" + encodeURIComponent(variable.uri)});
    }

    showVariableReferences(uri: string){
        this.service.getVariable(uri).then((http: HttpResponse<OpenSilexResponse>) => {
            this.selected = http.response.result;
            this.skosReferences.show();
        }).catch(this.$opensilex.errorHandler);
    }

    hide() {
        this.helpModal.hide();
    }
}
</script>

<style scoped lang="scss">

.createButton, .helpButton{
  margin-bottom: 5px;
  margin-top: -10px;
  margin-left: -10px;
  margin-right: 15px;
}
.helpButton {
  margin-left: -15px;
  color: #00A28C;
  font-size: 1.2em;
  border: none
}
  
.helpButton:hover {
  background-color: #00A28C;
  color: #f1f1f1
}
</style>

<i18n>
en:
    VariableView:
        name: The variable
        title: Variables
        type: Variable
        description : Manage and configure variables, entities, characteristics, methods and units
        add-variable: Add variable
        entity: Entity
        add-entity: Add entity
        entity-placeholder: Plant
        entityOfInterest: Entity of interest
        add-entityOfInterest: Add observation level
        entityOfInterest-placeholder: Canopy
        characteristic: Characteristic
        add-characteristic: Add characteristic
        characteristic-placeholder: Height
        method: Method
        add-method: Add method
        method-placeholder: Image analysis
        unit: "Unit/Scale"
        add-unit: Add unit
        groupVariable: Group of variables
        groupVariableAssociated: Group of associated variables
        add-groupVariable: Add a group of variables
        no-var-provided: No variable provided
        associated-data-error: Data are associated with this variable

fr:
    VariableView:
        name: La variable
        title: Variables
        type: Variable
        description : Gérer et configurer les variables, entités, charactéristiques, méthodes et unités
        add-variable: Ajouter une variable
        entity: Entité
        add-entity: Ajouter une entité
        entity-placeholder: Plante
        entityOfInterest: Entité d'intérêt
        add-entityOfInterest: Ajouter un niveau d'observation
        entityOfInterest-placeholder: Canopée
        characteristic: Caractéristique
        add-characteristic: Ajouter une caractéristique
        characteristic-placeholder: Hauteur
        method: Méthode
        add-method: Ajouter une méthode
        method-placeholder: Analyse d'image
        unit: "Unité/Echelle"
        add-unit: Ajouter une unité
        groupVariable: Groupe de variables
        groupVariableAssociated: Groupe de variables associées
        add-groupVariable: Ajouter un groupe de variables
        no-var-provided: Aucune variable associée
        associated-data-error: Données sont associées à cette variable

</i18n>


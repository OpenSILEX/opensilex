<template>
    <div class="container-fluid">
        <opensilex-PageHeader
            icon="fa#vials"
            title="VariableView.title"
            description="VariableView.description"
        ></opensilex-PageHeader>

        <div>
            <opensilex-PageActions>
                <div>
                    <b-tabs content-class="mt-3" :value=elementIndex @input="updateType">
                        <b-tab :title="$t('component.menu.variables')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.entity')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.characteristic')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.method')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.unit')" @click="refreshSelected"></b-tab>
                        <b-tab :title="$t('VariableView.groupVariable')" @click="refreshSelected"></b-tab> <!--Ajout de Hamza-->
                    </b-tabs>
                </div>

                <div class="col-lg-5">
                    <opensilex-HelpButton
                            @click="helpModal.show()"
                            label="component.common.help-button"
                    ></opensilex-HelpButton>
                    <b-modal ref="helpModal" size="xl" hide-header ok-only>
                        <opensilex-VariableHelp v-if="elementType != 'VariableGroup'"></opensilex-VariableHelp>
                        <opensilex-GroupVariablesHelp v-else></opensilex-GroupVariablesHelp>
                    </b-modal>

                    <opensilex-CreateButton
                        v-show="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        @click="showCreateForm"
                        :label="buttonTitle"
                    ></opensilex-CreateButton>
                </div>
            </opensilex-PageActions>
            <opensilex-PageContent
                v-if="loadVariableList()" >
                <template v-slot>
                    <opensilex-VariableList
                        ref="variableList"
                        @onEdit="showVariableEditForm" 
                        @onInteroperability="showVariableReferences"
                        @onDelete="deleteVariable"
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
            <opensilex-EntityCreate
                ref="entityForm" @onCreate="refresh($event.uri)" @onUpdate="refresh($event.uri)"
            ></opensilex-EntityCreate>

            <opensilex-CharacteristicModalForm
                ref="characteristicForm" @onCreate="refresh($event.uri)" @onUpdate="refresh($event.uri)"
            ></opensilex-CharacteristicModalForm>

            <opensilex-MethodCreate
                ref="methodForm" @onCreate="refresh($event.uri)" @onUpdate="refresh($event.uri)"
            ></opensilex-MethodCreate>

            <opensilex-UnitCreate
                ref="unitForm" @onCreate="refresh($event.uri)" @onUpdate="refresh($event.uri)"
            ></opensilex-UnitCreate>

            <opensilex-ModalForm
                v-if="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                ref="groupVariablesForm"
                modalSize="lg"
                :tutorial="false"
                :createAction="createVariablesGroup"
                :updateAction="updateVariablesGroup"
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

                    <opensilex-Card v-if="!loadVariableList() && useGenericDetailsPage() && groupVariablesPage() && selected.variables != undefined" :label="$t('VariableView.type')" :selected="selected">
                        <template v-slot:body>

                            <opensilex-TableView
                            v-if="groupVariablesPage() && selected.variables.length !== 0"
                            :items="selected.variables"
                            :fields="relationsFields"
                            :globalFilterField="true">

                                <template v-slot:cell(uri)="{data}">
                                    <opensilex-UriLink
                                    :uri="data.item.uri"
                                    :value="data.item.name"
                                    :to="{path: '/variable/details/'+ encodeURIComponent(data.item.uri)}">
                                    </opensilex-UriLink>
                                </template> 

                            </opensilex-TableView>

                            <p v-else><strong>{{$t("GroupVariablesDetails.no-var-provided")}}</strong></p>

                        </template>
                    </opensilex-Card>

                    <opensilex-DocumentTabList
                        v-if="selected && selected.uri"
                        v-show="! loadVariableList() && useGenericDetailsPage() && documentMethodPage()" 
                        :selected="selected"
                        :uri="selected.uri"
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
import EntityCreate from "./form/EntityCreate.vue";
import UnitCreate from "./form/UnitCreate.vue";
import VariableCreate from "./form/VariableCreate.vue";
import VariableList from "./VariableList.vue";
import ExternalReferencesModalForm from "../common/external-references/ExternalReferencesModalForm.vue";

import { VariablesService, DataService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";


@Component
export default class VariablesView extends Vue {

    $opensilex: OpenSilexVuePlugin
    $store: any;
    $route: any;
    $router: any;
    $i18n: any;
    service: VariablesService;
    dataService: DataService;


    static VARIABLE_TYPE: string = "Variable";
    static ENTITY_TYPE: string = "Entity";
    static CHARACTERISTIC_TYPE: string = "Characteristic";
    static METHOD_TYPE: string = "Method";
    static UNIT_TYPE:  string = "Unit";
    static GROUP_VARIABLE_TYPE: string = "VariableGroup";


    elementIndex: number = 0;
    elementType: string = '';

    static elementTypes = [
        // VariablesView.VARIABLE_TYPE,
        // VariablesView.ENTITY_TYPE,
        // VariablesView.CHARACTERISTIC_TYPE,
        // VariablesView.METHOD_TYPE,
        // VariablesView.UNIT_TYPE,
        // VariablesView.GROUP_VARIABLE_TYPE
    ]
    
    @Ref("variableCreate") readonly variableCreate!: VariableCreate;
    @Ref("entityForm") readonly entityForm!: EntityCreate;
    @Ref("characteristicForm") readonly characteristicForm!: any;
    @Ref("methodForm") readonly methodForm!: any;
    @Ref("unitForm") readonly unitForm!: UnitCreate;
    @Ref("groupVariablesForm") readonly groupVariablesForm!: any;

    @Ref("skosReferences") skosReferences!: ExternalReferencesModalForm;

    @Ref("variableList") readonly variableList!: VariableList;
    @Ref("variableStructureList") readonly variableStructureList!: VariableStructureList;

    @Ref("helpModal") readonly helpModal!: any;

    buttonTitle;

constructor() {
    super()
    this.elementType = VariablesView.VARIABLE_TYPE
    VariablesView.elementTypes = [
        VariablesView.VARIABLE_TYPE,
        VariablesView.ENTITY_TYPE,
        VariablesView.CHARACTERISTIC_TYPE,
        VariablesView.METHOD_TYPE,
        VariablesView.UNIT_TYPE,
        VariablesView.GROUP_VARIABLE_TYPE
    ]
}
    relationsFields: any[] = [
      {
        key: "uri",
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
        this.variableStructureList.refresh(false,uri);
    }

    private getForm() {
        switch (this.elementType) {
            case VariablesView.VARIABLE_TYPE : {
                return this.variableCreate;
            }
            case VariablesView.ENTITY_TYPE : {
                return this.entityForm;
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
        this.getForm().showCreateForm();
    }

    formatVariablesGroup(dto : any) {
        let copy = JSON.parse(JSON.stringify(dto));
        if (copy.variables) {
            copy.variables = copy.variables.map(variable => variable.uri);
        }
        return copy;
    }

    showEditForm(dto : any){
        if (this.elementType == VariablesView.GROUP_VARIABLE_TYPE) {
            let formatVariableGroup = this.formatVariablesGroup(dto);
            this.getForm().showEditForm(formatVariableGroup);
        }
        else{
            this.getForm().showEditForm(dto);
        }
    }

    getCountDataPromise(uri){
      return this.dataService.countData(
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          [uri],
          undefined,
          undefined,
          undefined,
          undefined);
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

        let formattedVariable = VariableCreate.formatVariableBeforeUpdate(variable);

        this.service.updateVariable(formattedVariable).then(() => {
            let message = this.$i18n.t("VariableView.name") + " " + formattedVariable.name + " " + this.$i18n.t("component.common.success.update-success-message");
            this.$opensilex.showSuccessToast(message);
        }).catch(this.$opensilex.errorHandler);
    }

    createVariablesGroup(form){
        return this.service.createVariablesGroup(form)
            .then((http: HttpResponse<OpenSilexResponse<any>>) => {
            let message = this.$i18n.t(form.name) + this.$i18n.t("component.common.success.creation-success-message");
            this.$opensilex.showSuccessToast(message);
            let uri = http.response.result;
            this.refresh(uri);
      })
      .catch(error => {
        if (error.status == 409) {
          console.error("Variables group already exists", error);
          this.$opensilex.errorHandler(error, "Variables group already exists");
        } else {
          this.$opensilex.errorHandler(error);
        }
      });
    }

    updateVariablesGroup(form){
        return this.service.updateVariablesGroup(form)
            .then((http: HttpResponse<OpenSilexResponse<any>>) => {
                let message = this.$i18n.t(form.name) + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);
                let uri = http.response.result;
                this.refresh(uri);
        })
        .catch(this.$opensilex.errorHandler);
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

}
</script>

<style scoped lang="scss">
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
        characteristic: Characteristic
        add-characteristic: Add characteristic
        method: Method
        add-method: Add method
        unit: "Unit/Level"
        add-unit: Add unit
        groupVariable: "Group of variables"
        add-groupVariable: Add a group of variables

fr:
    VariableView:
        name: La variable
        title: Variables
        type: Variable
        description : Gérer et configurer les variables, entités, charactéristiques, méthodes et unités
        add-variable: Ajouter une variable
        entity: Entité
        add-entity: Ajouter une entité
        characteristic: Caractéristique
        add-characteristic: Ajouter une caractéristique
        method: Méthode
        add-method: Ajouter une méthode
        unit: "Unité/Niveau"
        add-unit: Ajouter une unité
        groupVariable: "Groupe de variables"
        add-groupVariable: Ajouter un groupe de variables

</i18n>


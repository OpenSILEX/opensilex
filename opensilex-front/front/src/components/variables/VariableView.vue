<template>
    <div class="container-fluid">
        <opensilex-PageHeader
            icon="ik#ik-globe"
            title="VariableView.title"
            description="VariableView.description"
        ></opensilex-PageHeader>

        <div>
            <div class="row">
                <div class="col-lg-5">
                    <b-tabs content-class="mt-3" :value=elementIndex @input="updateType">
                        <b-tab :title="$t('component.menu.variables')"></b-tab>
                        <b-tab :title="$t('VariableView.entity')" ></b-tab>
                        <b-tab :title="$t('VariableView.quality')"></b-tab>
                        <b-tab :title="$t('VariableView.method')" ></b-tab>
                        <b-tab :title="$t('VariableView.unit')" ></b-tab>
                    </b-tabs>
                </div>
                <div class="col-lg-5">
                    <opensilex-CreateButton
                        v-show="user.hasCredential(credentials.CREDENTIAL_VARIABLE_MODIFICATION_ID)"
                        @click="showCreateForm"
                        :label="buttonTitle"
                    ></opensilex-CreateButton>
                </div>
            </div>

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

            <opensilex-QualityCreate
                ref="qualityForm" @onCreate="refresh($event.uri)" @onUpdate="refresh($event.uri)"
            ></opensilex-QualityCreate>

            <opensilex-MethodCreate
                ref="methodForm" @onCreate="refresh($event.uri)" @onUpdate="refresh($event.uri)"
            ></opensilex-MethodCreate>

            <opensilex-UnitCreate
                ref="unitForm" @onCreate="refresh($event.uri)" @onUpdate="refresh($event.uri)"
            ></opensilex-UnitCreate>

            <opensilex-ExternalReferencesModalForm
                ref="skosReferences"
                :references.sync="selected"
                @onUpdate="updateReferences"
            ></opensilex-ExternalReferencesModalForm>

            <div class="row">
                <div class="col-md-6">
                    <!-- Element list -->
                    <opensilex-EntityList
                        v-if="! loadVariableList() "
                        ref="entityTree"
                        @onSelect="updateSelected"
                        @onEdit="showEditForm"
                        :_type.sync="this.elementType"
                    ></opensilex-EntityList>
                </div>

                <div class="col-md-6">
                    <!-- Element details page -->
                    <opensilex-EntityDetails v-show="! loadVariableList() && useGenericDetailsPage()" :selected="selected"></opensilex-EntityDetails>
                    <!-- Unit specialized details page -->
                    <opensilex-UnitDetails v-show="! loadVariableList() && ! useGenericDetailsPage()" :selected="selected"></opensilex-UnitDetails>

                    <opensilex-Card
                        v-show="! loadVariableList()" label="component.skos.ontologies-references-label" icon="ik#ik-clipboard">
                        <template v-slot:body>
                            <opensilex-ExternalReferencesDetails :skosReferences="selected"></opensilex-ExternalReferencesDetails>
                        </template>
                    </opensilex-Card>
                </div>
            </div>
        </div>

    </div>
</template>

<script lang="ts">
import {Component, Ref} from "vue-property-decorator";
import Vue from "vue";
import EntityList from "./views/EntityList.vue";
import EntityCreate from "./form/EntityCreate.vue";
import UnitCreate from "./form/UnitCreate.vue";
import VariableCreate from "./form/VariableCreate.vue";
import QualityCreate from "./form/QualityCreate.vue";
import MethodCreate from "./form/MethodCreate.vue";
import VariableList from "./VariableList.vue";
import ExternalReferencesModalForm from "../common/external-references/ExternalReferencesModalForm.vue";
import { VariablesService } from "opensilex-core/index";
import HttpResponse, { OpenSilexResponse } from "../../lib/HttpResponse";

@Component
export default class VariableView extends Vue {

    $opensilex: any;
    $store: any;
    $route: any;
    $router: any;
    service: VariablesService;

    elementIndex: number = 0;
    elementType: string = VariableView.VARIABLE_TYPE;

    static VARIABLE_TYPE: string = "Variable";
    static ENTITY_TYPE: string = "Entity";
    static QUALITY_TYPE: string = "Quality";
    static METHOD_TYPE: string = "Method";
    static UNIT_TYPE:  string = "Unit";

    static elementTypes = [
        VariableView.VARIABLE_TYPE,
        VariableView.ENTITY_TYPE,
        VariableView.QUALITY_TYPE,
        VariableView.METHOD_TYPE,
        VariableView.UNIT_TYPE
    ]

    @Ref("variableCreate") readonly variableCreate!: VariableCreate;

    @Ref("entityForm") readonly entityForm!: EntityCreate;
    @Ref("qualityForm") readonly qualityForm!: any;
    @Ref("methodForm") readonly methodForm!: any;
    @Ref("unitForm") readonly unitForm!: UnitCreate;

    @Ref("skosReferences") skosReferences!: ExternalReferencesModalForm;

    @Ref("variableList") readonly variableList!: VariableList;
    @Ref("entityTree") readonly entityTree!: EntityList;

    buttonTitle;

    created(){
        this.service = this.$opensilex.getService("opensilex-core.VariablesService");

        // load variable list by default
        let query: any = this.$route.query;
        if(query && query.elementType){
            let requestedTypeIdx = VariableView.elementTypes.findIndex(elem => elem == decodeURIComponent(query.elementType));
            this.updateType(requestedTypeIdx);
        }else{
            let variableIdx = VariableView.elementTypes.findIndex(elem => elem == VariableView.VARIABLE_TYPE);
            this.updateType(variableIdx);
        }
    }

    private updateType(tabIndex) {

        if(tabIndex >= 0 && tabIndex < VariableView.elementTypes.length){

            this.elementIndex = tabIndex;
            this.elementType = VariableView.elementTypes[this.elementIndex];
            this.$opensilex.updateURLParameter("elementType",this.elementType);
            this.buttonTitle = "VariableView."+this.getButtonTitleTranslationSubKey();

            if(this.elementType == VariableView.VARIABLE_TYPE){
                this.$opensilex.updateURLParameter("selected","");
                if (this.variableList) {
                    this.variableList.refresh();
                }
            }else{
                if(this.entityTree){
                    // update entity list with the new elementType value
                    this.$nextTick(() => {
                        this.$opensilex.updateURLParameter("name", "");
                        this.entityTree.refresh();
                    });
                }
            }
        }
    }

    private loadVariableList() : boolean {
        return this.elementType === VariableView.VARIABLE_TYPE;
    }

    private useGenericDetailsPage() : boolean{
        return this.elementType != VariableView.UNIT_TYPE;
    }

    selected = {
        uri: undefined,
        name: undefined,
        comment : undefined,
        type : undefined,
        typeLabel : undefined,
        exactMatch: [],
        closeMatch: [],
        broader: [],
        narrower: []
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
        this.entityTree.refresh(uri);
    }

    private getForm() {
        switch (this.elementType) {
            case VariableView.VARIABLE_TYPE : {
                return this.variableCreate;
            }
            case VariableView.ENTITY_TYPE : {
                return this.entityForm;
            }
            case VariableView.QUALITY_TYPE : {
                return this.qualityForm;
            }
            case VariableView.METHOD_TYPE: {
                return this.methodForm;
            }
            case VariableView.UNIT_TYPE: {
                return this.unitForm;
            }
            default : {
                return this.variableCreate;
            }
        }
    }

    private getButtonTitleTranslationSubKey(): string {
        switch (this.elementType) {
            case VariableView.VARIABLE_TYPE : {
                return "add-variable";
            }
            case VariableView.ENTITY_TYPE : {
                return "add-entity";
            }
            case VariableView.QUALITY_TYPE : {
                return "add-quality";
            }
            case VariableView.METHOD_TYPE: {
                return "add-method";
            }
            case VariableView.UNIT_TYPE: {
                return "add-unit";
            }
            default : {
                return "add-variable";
            }
        }
    }



    showCreateForm(){
        this.getForm().showCreateForm();
    }

    showEditForm(dto : any){
        this.getForm().showEditForm(dto);
    }

    showVariableEditForm(uri : string){
        this.service.getVariable(uri).then((http: HttpResponse<OpenSilexResponse>) => {
            this.variableCreate.showEditForm(http.response.result);
        });
    }

    updateReferences(variable){

        let formattedVariable = VariableCreate.formatVariableBeforeUpdate(variable);

        this.service.updateVariable(formattedVariable).then(() => {
            let message = this.$i18n.t("VariableView.name") + " " + formattedVariable.uri + " " + this.$i18n.t("component.common.success.update-success-message");
            this.$opensilex.showSuccessToast(message);
            this.skosReferences.hide();
            this.showVariableDetails(formattedVariable.uri);
        }).catch(this.$opensilex.errorHandler);
    }

    deleteVariable(uri: string) {
        this.service.deleteVariable(uri).then(() => {
            this.variableList.refresh();
            let message = this.$i18n.t("VariableView.name") + " " + uri + " " + this.$i18n.t("component.common.success.delete-success-message");
            this.$opensilex.showSuccessToast(message);
        }).catch(this.$opensilex.errorHandler);
    }

    showVariableDetails(uri: any) {
        this.$store.commit("storeReturnPage", this.$router);
        this.$router.push({path: "/variable/" + encodeURIComponent(uri)});
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
        description : Manage and configure variables, entity, quality, method and units
        add-variable: Add variable
        entity: Entity
        add-entity: Add entity
        quality: Characteristic
        add-quality: Add characteristic
        method: Method
        add-method: Add method
        unit: "Unit/Level"
        add-unit: Add unit

fr:
    VariableView:
        name: La variable
        title: Variables
        description : Gérer et configurer les variables, entités, qualités, méthodes et unités
        add-variable: Ajouter une variable
        entity: Entité
        add-entity: Ajouter une entité
        quality: Caractéristique
        add-quality: Ajouter une caractéristique
        method: Méthode
        add-method: Ajouter une méthode
        unit: "Unité/Niveau"
        add-unit: Ajouter une unité

</i18n>


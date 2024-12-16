<template>
    <b-form v-if="form.name_translations">
        <opensilex-InputForm
            :value.sync="form.uri"
            label="component.common.uri"
            type="text"
            rules="url"
            :disabled="editMode"
            :required="true"
        ></opensilex-InputForm>

        <hr/>

        <div class="row">

            <div class="col-lg-6">
                <b-form-group :label="$t('OntologyPropertyForm.propertyType')">
                    <b-form-radio
                        v-model="form.rdf_type"
                        name="propertyType"
                        id="datatypeRadio"
                        :value="OWL.DATATYPE_PROPERTY_URI"
                        :disabled="editMode"
                    >{{$t("OntologyPropertyForm.dataProperty")}}
                    </b-form-radio>
                    <b-form-radio
                        v-model="form.rdf_type"
                        name="propertyType"
                        :value="OWL.OBJECT_PROPERTY_URI"
                        :disabled="editMode"
                    >{{$t("OntologyPropertyForm.objectProperty")}}
                    </b-form-radio>
                    <b-form-radio
                        v-model="form.rdf_type"
                        name="inheritedType"
                        :value="null"
                        :disabled="editMode"
                    >{{$t("OntologyPropertyForm.inheritedType")}}
                    </b-form-radio>
                </b-form-group>
            </div>
            <div class="col-lg-6">
                <opensilex-FormSelector
                    v-if="form.rdf_type == OWL.DATATYPE_PROPERTY_URI"
                    label="OntologyPropertyForm.data-type"
                    :required="true"
                    :selected.sync="form.range"
                    :options="dataTypes"
                    helpMessage="OntologyPropertyForm.dataProperty-help"
                ></opensilex-FormSelector>

                <opensilex-FormSelector
                    v-if="form.rdf_type == OWL.OBJECT_PROPERTY_URI"
                    label="OntologyPropertyForm.object-type"
                    :required="true"
                    :selected.sync="form.range"
                    :options="objectTypes"
                    helpMessage="OntologyPropertyForm.objectProperty-help"
                ></opensilex-FormSelector>

                <opensilex-FormSelector
                    v-if="form.rdf_type == null"
                    label="component.common.parent"
                    :required="true"
                    :selected.sync="form.parent"
                    :options="availableParents"
                    helpMessage="OntologyPropertyForm.parent-help"
                ></opensilex-FormSelector>

                <opensilex-TypeForm
                    :type.sync="form.domain"
                    :baseType="this.domain"
                    ignoreRoot="false"
                    label="OntologyPropertyForm.domain"
                    helpMessage="OntologyPropertyForm.domain-help"
                ></opensilex-TypeForm>
            </div>

        </div>


        <hr/>
        <opensilex-InputForm
            :value.sync="form.name_translations.en"
            label="OntologyPropertyForm.labelEN"
            type="text"
            :required.sync="enLangRequired"
        ></opensilex-InputForm>

        <opensilex-TextAreaForm
            :value.sync="form.comment_translations.en"
            label="OntologyPropertyForm.commentEN"
            :required="false"
            @keydown.native.enter.stop
        ></opensilex-TextAreaForm>

        <opensilex-InputForm
            :value.sync="form.name_translations.fr"
            label="OntologyPropertyForm.labelFR"
            type="text"
            :required.sync="otherLangRequired"
        ></opensilex-InputForm>

        <opensilex-TextAreaForm
            :value.sync="form.comment_translations.fr"
            label="OntologyPropertyForm.commentFR"
            :required="false"
            @keydown.native.enter.stop
        ></opensilex-TextAreaForm>

    </b-form>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import OWL from "../../ontologies/OWL";
// @ts-ignore
import HttpResponse, {OpenSilexResponse} from "opensilex-core/HttpResponse";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {OntologyService} from "opensilex-core/api/ontology.service";

@Component
export default class OntologyPropertyForm extends Vue {
    $opensilex: OpenSilexVuePlugin;
    $store: any;
    OWL = OWL;
    service: OntologyService;

    get lang() {
        return this.$store.getters.language;
    }

    enLangRequired: boolean = this.$store.getters.language == "en";
    otherLangRequired: boolean = this.$store.getters.language != "en";

    created() {
        this.service = this.$opensilex.getService("opensilex.OntologyService");
    }


    @Prop()
    editMode;

    @Prop({
        default: () => {
            return {
                uri: null,
                rdf_type: OWL.DATATYPE_PROPERTY_URI,
                parent: null,
                name_translations: {},
                comment_translations: {},
                domain: null,
                range: null
            };
        }
    })
    form;

    getEmptyForm() {
        return {
            uri: null,
            rdf_type: OWL.DATATYPE_PROPERTY_URI,
            parent: null,
            name_translations: {},
            comment_translations: {},
            domain: null,
            range: null
        };
    }

    get dataTypes(): Array<{id: string, label: string}> {

        let types: Array<{id: string, label: string}> = [];

        this.$opensilex.datatypes.forEach(type => {
            let label: any = this.$t(type.label_key);
            types.push({
                id: type.uri,
                label: label.charAt(0).toUpperCase() + label.slice(1)
            });
        });

        this.sortTypesByLabel(types);
        return types;
    }

    get objectTypes() : Array<{id: string, label: string}> {
        let types: Array<{id: string, label: string}> = [];
        this.$opensilex.objectTypes.forEach(type => {
            // try to get translated name
            let translatedLabel: string = type.rdf_type.name_translations[this.$store.getters.language];

            // if no translation found, then use default name
            if(! translatedLabel || translatedLabel.length == 0){
                translatedLabel = type.rdf_type.name;
            }
            types.push({
                id: type.uri,
                label: translatedLabel
            });
        });

        this.sortTypesByLabel(types);
        return types;
    }

    sortTypesByLabel(types: Array<{id: string, label: string}>): void{
        types.sort((a, b) => {
            let comparison = 0;
            if (a.label > b.label) {
                comparison = 1;
            } else if (a.label < b.label) {
                comparison = -1;
            }
            return comparison;
        });
    }

    availableParents = [];
    parentByURI = {};

    setParentPropertiesTree(nodes) {
        this.parentByURI = {};
        this.availableParents = this.loadNodesRecursivly(nodes);
    }

    private loadNodesRecursivly(nodes) {
        let parents = [];
        for (let i in nodes) {
            let node = nodes[i];
            let selectItem: any = {
                id: node.data.uri,
                label: node.title
            };
            this.parentByURI[node.data.uri] = node.data;
            if (node.children && node.children.length > 0) {
                selectItem.children = this.loadNodesRecursivly(node.children);
            }
            parents.push(selectItem);
        }

        return parents;
    }

    private domain = null;

    setDomain(domain: string) {
        this.domain = domain;
    }

    private computeFormToSend(form) {
        let sentForm = {
            uri: form.uri,
            rdf_type: form.rdf_type,
            parent: form.parent,
            name_translations: form.name_translations,
            comment_translations: form.comment_translations,
            domain: form.domain,
            range: form.range
        };

        if (sentForm.rdf_type == null) {
            let parentType = this.parentByURI[form.parent];
            sentForm.rdf_type = parentType.rdf_type;
        } else {
            sentForm.parent = null;
        }

        return sentForm;
    }

    create(form) {
        return this.service.createProperty(this.computeFormToSend(form))
            .then((http: HttpResponse<OpenSilexResponse<any>>) => {
                let uri = http.response.result;
                let message = this.$i18n.t("OntologyPropertyView.the-property") + " " + uri + this.$i18n.t("component.common.success.creation-success-message");
                this.$opensilex.showSuccessToast(message);
            })
            .catch(error => {
                if (error.status == 409) {
                    console.error("Property already exists", error);
                    this.$opensilex.errorHandler(
                        error,
                        this.$t("OntologyPropertyForm.property-already-exists")
                    );
                } else {
                    this.$opensilex.errorHandler(error);
                }
            });
    }

    update(form) {
        return this.service.updateProperty(this.computeFormToSend(form))
            .then((http: HttpResponse<OpenSilexResponse<any>>) => {
                let uri = http.response.result;
                let message = this.$i18n.t("OntologyPropertyView.the-property") + " " + uri + this.$i18n.t("component.common.success.update-success-message");
                this.$opensilex.showSuccessToast(message);
            })
            .catch(this.$opensilex.errorHandler);
    }
}
</script>

<style scoped lang="scss">
</style>


<i18n>
en:
    OntologyPropertyForm:
        propertyType: Property Type
        dataProperty: Data property
        objectProperty: Object property
        inheritedType: Type inherited from parent
        data-type: Data type
        dataProperty-help: 'Property which relate resource (e.g. device,scientific object, facility) to literal data (integer,decimal,date,string,etc)'
        object-type: Object class
        objectProperty-help: 'Property which relate resource (e.g. device,scientific object, facility) to other resource (e.g. device,scientific object, facility)'
        parent-help: 'Parent'
        labelEN: English name
        labelFR: French name
        commentEN: English description
        commentFR: French description
        property-already-exists: 'Property with same URI already exists'
        domain: Domain
        domain-help: 'Type concerned by the property. The property can be linked to the domain and on all domain descendant'

fr:
    OntologyPropertyForm:
        propertyType: Type de propriété
        dataProperty: Propriété litérale
        objectProperty: Relation vers un objet
        inheritedType: Type hérité du parent
        data-type: Type de donnée
        dataProperty-help: 'Propriété associant une valeur (nombre,date,chaîne de caractères, etc) à une ressource(ex: équipement, object scientifique, évenement) '
        object-type: Classe d'objet
        objectProperty-help: 'Propriété liant une ressource(ex: équipement, object scientifique, évenement) à une autre ressource'
        parent-help: 'Parent'
        labelEN: Nom anglais
        labelFR: Nom français
        commentEN: Description anglaise
        commentFR: Description française
        property-already-exists: Une propriété existe déjà avec la même URI
        domain: Domaine
        domain-help: 'Type concerné par la propriété. La propriété peut être liée au domaine choisi et à tous les descendants du domaine'

</i18n>
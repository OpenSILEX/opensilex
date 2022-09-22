<template>
    <div class="row">
        <div class="col-md-6">
            <b-card>
                <div class="button-zone">

                    <opensilex-CreateButton
                        v-if="user.isAdmin()"
                        @click="showCreateForm()"
                        label="OntologyPropertyView.add"
                        class="createButton"
                    ></opensilex-CreateButton>

                    <opensilex-ModalForm
                        ref="propertyForm"
                        component="opensilex-OntologyPropertyForm"
                        createTitle="OntologyPropertyView.add"
                        editTitle="OntologyPropertyView.update"
                        :initForm="initForm"
                        @onCreate="refresh()"
                        @onUpdate="refresh()"
                        modalSize="lg"
                        successMessage="OntologyPropertyView.the-property"
                        :icon="icon"
                        :title="propertiesTitle"
                        :description="propertiesDescription"
                    ></opensilex-ModalForm>
                </div>

                <opensilex-StringFilter
                    :filter.sync="nameFilter"
                    @update="updateFilter()"
                    placeholder="OntologyPropertyView.search"
                ></opensilex-StringFilter>

                <opensilex-OntologyPropertyTreeView
                    ref="propertiesTree"
                    :domain="rdfType"
                    @selectionChange="selected = $event"
                    @editProperty="showEditForm($event)"
                    @createChildProperty="showCreateForm($event)"
                    @deleteProperty="deleteProperty($event)"
                ></opensilex-OntologyPropertyTreeView>
            </b-card>
        </div>
        <div class="col-md-6">
            <opensilex-OntologyPropertyDetail :selected.sync="selected"/>
        </div>
    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {OntologyService, RDFPropertyDTO, RDFPropertyGetDTO, ResourceTreeDTO} from "opensilex-core/index";
import OWL from "../../ontologies/OWL";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import OntologyPropertyTreeView from "./OntologyPropertyTreeView.vue";
import {Store} from "vuex";
import OntologyPropertyForm from "./OntologyPropertyForm.vue";

@Component
export default class OntologyPropertyView extends Vue {

  $opensilex: OpenSilexVuePlugin;
  $store: Store<any>;

  private nameFilter: string = "";

  /**
     * Return the current connected user
     */
    get user() {
        return this.$store.state.user;
    }

    @Prop()
    rdfType;

    @Prop()
    title;

    @Prop()
    icon;

    @Prop()
    description;

    selected: RDFPropertyGetDTO = null;

    @Ref("propertyForm") readonly propertyForm!: any;
    @Ref("propertiesTree") readonly propertiesTree!: OntologyPropertyTreeView;

    ontologyService: OntologyService;

    created() {
        this.ontologyService = this.$opensilex.getService("opensilex-core.OntologyService");
    }

    initForm(form: RDFPropertyDTO) {
        form.parent = this.parentURI;
        if (OWL.hasParent(form.parent)) {
            form.rdf_type = null;
        } else if (OWL.isDatatypeProperty(form.rdf_type)) {
            form.rdf_type = OWL.DATATYPE_PROPERTY_URI;
        } else if (OWL.isObjectTypeProperty(form.rdf_type)) {
            form.rdf_type = OWL.OBJECT_PROPERTY_URI;
        }
        form.domain = this.rdfType;
    }

    parentURI;

    showCreateForm(parentURI?) {
        this.parentURI = parentURI;
        let propertyFormComponent = this.propertyForm.getFormRef();
        propertyFormComponent.setParentPropertiesTree(
            this.propertiesTree.getTree()
        );
        propertyFormComponent.setDomain(this.rdfType);

        this.propertyForm.showCreateForm();
    }

    showEditForm(data) {
        this.ontologyService.getProperty(data.uri, data.rdf_type, this.rdfType).then(http => {
            let propertyFormComponent: OntologyPropertyForm = this.propertyForm.getFormRef();
            propertyFormComponent.setParentPropertiesTree(
                this.propertiesTree.getTree()
            );
            propertyFormComponent.setDomain(this.rdfType);
            let form = http.response.result;
            if (OWL.hasParent(form.parent)) {
                form.rdf_type = null;
            } else if (OWL.isDatatypeProperty(form.rdf_type)) {
                form.rdf_type = OWL.DATATYPE_PROPERTY_URI;
                form.range = this.$opensilex.getDatatype(form.range).uri;
            } else if (OWL.isObjectTypeProperty(form.rdf_type)) {
                form.rdf_type = OWL.OBJECT_PROPERTY_URI;
                form.range = this.$opensilex.getObjectType(form.range).uri;
            }
            this.propertyForm.showEditForm(form);
        });
    }

    deleteProperty(propertyDto: ResourceTreeDTO) {
        this.ontologyService.deleteProperty(propertyDto.uri, propertyDto.rdf_type).then(() => {
            let message = this.$i18n.t("OntologyPropertyView.the-property") + " " + propertyDto.uri + this.$i18n.t("component.common.success.delete-success-message");
            this.$opensilex.showSuccessToast(message);
            this.refresh();
        }).catch(this.$opensilex.errorHandler);
    }

    refresh() {
        this.propertiesTree.refresh(this.nameFilter);
    }

    updateFilter() {
        this.refresh();
    }
}
</script>

<style scoped lang="scss">
.createButton{
  margin-bottom: -10px;
  margin-top: -10px
}
</style>


<i18n>
en:
    OntologyPropertyView:
        add: Create property
        update: Update property
        the-property: The property
        search: Search and select a property
fr:
    OntologyPropertyView:
        add: Créer une propriété
        update: Mettre à jour la propriété
        the-property: La propriété
        search: Rechercher et sélectioner une propriété
</i18n>

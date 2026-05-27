<template>
    <div class="row">

        <!-- List and create button -->
        <div class="col-md-6">
            <b-card>
                <div class="button-zone">

                    <opensilex-CreateButton
                        v-if="user.isAdmin()"
                        @click="showCreateForm()"
                        label="OntologyClassView.add"
                        class="createButton">
                    </opensilex-CreateButton>

                    <opensilex-ModalForm
                        ref="classForm"
                        component="opensilex-OntologyClassForm"
                        createTitle="OntologyClassView.add"
                        editTitle="OntologyClassView.update"
                        :initForm="initForm"
                        @onCreate="refresh()"
                        @onUpdate="refresh()"
                        modalSize="lg"
                        successMessage="OntologyClassView.the-type"
                        :icon="icon"
                    ></opensilex-ModalForm>
                </div>

                <opensilex-StringFilter
                    :filter.sync="nameFilter"
                    @update="updateFilter()"
                    placeholder="OntologyClassView.search"
                    :debounce="300"
                    :lazy="false"
                ></opensilex-StringFilter>

                <opensilex-OntologyClassTreeView
                    ref="classesTree"
                    :rdfType="rdfType"
                    @selectionChange="selected = $event"
                    @editClass="showEditForm($event)"
                    @createChildClass="showCreateForm($event)"
                    @deleteRDFType="deleteRDFType($event)"
                    class="scrollable-container"
                ></opensilex-OntologyClassTreeView>
            </b-card>
        </div>

        <!-- Détails of selected element from list  -->
        <div class="col-md-6 ">
            <div>
                <opensilex-OntologyClassDetail
                    :rdfType="rdfType"
                    :selected="selected"
                    @onDetailChange="refresh()"
                />
            </div>
        </div>
    </div>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import OntologyClassTreeView from "./OntologyClassTreeView.vue";
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin";
import {Store} from "vuex";
import {VueJsOntologyExtensionService} from "../../lib";

@Component
export default class OntologyClassView extends Vue {

    $opensilex: OpenSilexVuePlugin;
    $store: Store<any>;
    vueJsOntologyService: VueJsOntologyExtensionService;

    private nameFilter: string = "";

    get user() {
        return this.$store.state.user;
    }

    @Prop()
    rdfType;

    @Prop()
    title;

    @Prop()
    icon;

    selected = null;

    @Ref("classForm") readonly classForm!: any;
    @Ref("classesTree") readonly classesTree!: OntologyClassTreeView;

    created(){
        this.vueJsOntologyService = this.$opensilex.getService("opensilex-front.VueJsOntologyExtensionService");
    }

    initForm(form) {
        form.parent = this.parentURI;
    }

    parentURI;

    showCreateForm(parentURI?) {
        this.parentURI = parentURI;
        this.classForm.getFormRef().setParentTypes(this.classesTree.getTree());
        this.classForm.showCreateForm();
    }

    showEditForm(data) {
        this.vueJsOntologyService
            .getRDFType(data.uri, this.rdfType)
            .then(http => {
                let form = http.response.result;
                this.classForm.getFormRef().setParentTypes(this.classesTree.getTree());
                this.classForm.showEditForm(form);
            }).catch(this.$opensilex.errorHandler);
    }

    deleteRDFType(data) {
        this.vueJsOntologyService
            .deleteRDFType(data.uri)
            .then(http => {
                let message = this.$i18n.t("OntologyClassView.the-type") + " " + data.name + this.$i18n.t("component.common.success.delete-success-message");
                this.$opensilex.showSuccessToast(message);

                this.selected = undefined;
                this.refresh();
            }).catch(this.$opensilex.errorHandler);
    }

    private langUnwatcher;

    mounted() {
        this.langUnwatcher = this.$store.watch(
            () => this.$store.getters.language,
            lang => {
                this.refresh();
            }
        );
    }

    beforeDestroy() {
        this.langUnwatcher();
    }

    refresh() {
        this.classesTree.refresh(this.selected, this.nameFilter);
    }


    updateFilter() {
        this.refresh();
    }

}
</script>

<style scoped lang="scss">


.header-plus {
    margin-left: 90px;
}
.createButton{
  margin-bottom: -10px;
  margin-top: -10px
}

div.sticky {
    position: -webkit-sticky; /* Safari */
    position: sticky;
    top: 0;
}
.scrollable-container {
    width: 100%;
    height: 600px;
    overflow-y: auto; /* Enables vertical scrolling */
}
</style>


<i18n>
en:
    OntologyClassView:
        the-type: The type
        add: Create type
        update: Update type
        search: Search and select a type
fr:
    OntologyClassView:
        the-type: Le type
        add: Créer un type
        update: Mettre à jour le type
        search: Rechercher et sélectioner un type

</i18n>


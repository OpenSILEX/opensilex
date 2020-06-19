<template>
    <b-modal ref="entityModalRef" size="lg" hide-footer :static="true">
        <template v-slot:modal-ok>{{$t('component.common.ok')}}</template>
        <template v-slot:modal-cancel>{{$t('component.common.cancel')}}</template>
        <template v-slot:modal-title>
            <font-awesome-icon icon="sun"/>
            {{title}}
        </template>
        <form-wizard
                @on-error="handleErrorMessage"
                ref="formWizard"
                @on-complete="onValidate"
                shape="square"
                color="#00a38d"
        >

            <h2 slot="title"></h2>
            <tab-content
                    v-bind:title="$t('component.variable.form.add.entity-description')"
                    :before-change="checkBeforeSkosStep"
            >
                <template slot="default">
                    <ValidationObserver ref="validatorRef">

                        <b-form>
                            <!-- URI -->
                            <opensilex-UriForm
                                    :uri.sync="dto.uri"
                                    label="component.variable.entity-uri"
                                    helpMessage="component.variable.entity-uri-help"
                                    :editMode="editMode"
                                    :generated.sync="uriGenerated"
                            ></opensilex-UriForm>

                            <opensilex-InputForm
                                    :value.sync="dto.label"
                                    label="component.variable.entity-name"
                                    type="text"
                                    :required=false
                            ></opensilex-InputForm>

                            <!-- class -->
                            <b-form-group>
                                <opensilex-FormInputLabelHelper label=component.variable.entity-class
                                                                helpMessage="component.variable.entity-class-help">
                                </opensilex-FormInputLabelHelper>
                                <ValidationProvider :name="$t('component.variable.entity-class')" v-slot="{ errors }">

                                    <multiselect
                                            :limit="1"
                                            :closeOnSelect=true
                                            :placeholder="$t('component.variable.class-placeholder')"
                                            v-model="selectedClass"
                                            :options="classList"
                                            :custom-label="treeDto => treeDto.name"
                                            deselectLabel="You must select one element"
                                            track-by="uri"
                                            :allow-empty=true
                                            :limitText="count => $t('component.common.multiselect.label.x-more', {count: count})"
                                    />

                                    <div class="error-message alert alert-danger">{{ errors[0] }}</div>
                                </ValidationProvider>
                            </b-form-group>

                        </b-form>
                    </ValidationObserver>
                </template>

            </tab-content>
            <!--      <tab-content v-bind:title="$t('component.common.form-wizard.external-ontologies')">-->
            <!--        <opensilex-ExternalReferencesForm :skosReferences="dto"></opensilex-ExternalReferencesForm>-->
            <!--      </tab-content>-->
        </form-wizard>
    </b-modal>
</template>


<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {EntityCreationDTO} from "opensilex-core/index";
    import {ResourceTreeDTO} from "opensilex-core/model/resourceTreeDTO";
    import {OntologyService} from "opensilex-core/api/ontology.service";

    @Component
    export default class EntityForm extends Vue {

        title = "";
        uriGenerated = true;
        editMode = false;

        errorMsg: String = "";

        classList: Array<ResourceTreeDTO> = [];

        set _classList(value: Array<ResourceTreeDTO>) {
            this.classList = value;
        }

        get _classList(): Array<ResourceTreeDTO> {
            return this.classList;
        }

        handleErrorMessage(errorMsg: string) {
            this.errorMsg = errorMsg;
        }

        $opensilex: any;

        @Ref("entityModalRef") readonly entityModalRef!: any;
        @Ref("validatorRef") readonly validatorRef!: any;

        dto: EntityCreationDTO = {
            uri: null,
            label: null,
            comment: null,
            type: null,
            exactMatch: [],
            narrower: [],
            closeMatch: [],
            broader: []
        }

        selectedClass: ResourceTreeDTO = null;


        created() {
            this.loadingWizard = false;
        }

        showCreateForm() {
            this.title = this.$t("component.variable.form.add.entity").toString();
            let entityModalRef: any = this.$refs.entityModalRef;
            entityModalRef.show();
        }

        hideForm() {
            let entityModalRef: any = this.entityModalRef;
            entityModalRef.hide();
        }

        onValidate() {
            if (this.selectedClass) {
                this.dto.type = this.selectedClass.uri
            }
            return new Promise((resolve, reject) => {

                if (this.editMode) {
                    this.$emit("onUpdate", this.dto, result => {
                        if (result instanceof Promise) {
                            result.then(resolve).catch(reject);
                        } else {
                            resolve(result);
                        }
                    });
                } else {
                    return this.$emit("onCreate", this.dto, result => {
                        if (result instanceof Promise) {
                            result.then(resolve).catch(reject);
                        } else {
                            resolve(result);
                        }
                    });
                }
            });
        }


        validateForm() {
            let validatorRef: any = this.$refs.validatorRef;
            return validatorRef.validate();
        }

        private _loadingWizard: boolean = true;

        get loadingWizard(): boolean {
            return this._loadingWizard;
        }

        set loadingWizard(value: boolean) {
            this._loadingWizard = value;
        }

        async checkBeforeSkosStep() {
            return new Promise((resolve, reject) => {
                setTimeout(() => {
                    this.validateForm().then(isValid => {
                        if (isValid) {
                            resolve(true);
                        } else {
                            this.errorMsg = 'component.common.errors.form-step-errors'
                            reject();
                        }
                    });
                }, 400);
            });
        }

    }

</script>

<style scoped lang="scss">
</style>
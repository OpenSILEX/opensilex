<template>
    <div>
        <ValidationObserver ref="validatorRef">
            <b-form>
                <p>
                    {{$t('component.skos.addTo')}}  
                    <em>
                        <strong class="text-primary">{{this.skosReferences.uri}}</strong>
                    </em>
                </p>
                <b-card bg-variant="light">
                    <div class="row">
                        <div class="col">
                            <b-form-group
                                    label="component.skos.ontologies-references-label"
                                    label-size="lg"
                                    label-class="font-weight-bold pt-0"
                                    class="mb-0"
                            >
                                <template v-slot:label>{{$t('component.skos.ontologies-references-label') }}</template>
                            </b-form-group>
                            <b-card-text>
                                <ul>
                                    <li
                                            v-for="externalOntologyRef in externalOntologiesRefs"
                                            :key="externalOntologyRef.name"
                                    >
                                        <a
                                                target="_blank"
                                                v-bind:title="externalOntologyRef.name"
                                                v-bind:href="externalOntologyRef.link"
                                                v-b-tooltip.v-info.hover.left="externalOntologyRef.description"
                                        >{{ externalOntologyRef.name }}</a>
                                    </li>
                                </ul>
                            </b-card-text>
                        </div>
                        <div class="col">
                             <opensilex-FilterField :fullWidth="true">
                                <opensilex-FormSelector
                                label="component.skos.relation"
                                helpMessage="component.skos.relation-help"
                                placeholder="component.skos.no-relation"
                                :selected.sync="currentRelation"
                                :options="options"
                                :requiredBlue="true"
                                ></opensilex-FormSelector>
                            </opensilex-FilterField>
                             <!-- URI -->
                            <opensilex-FilterField :fullWidth="true">
                            <b-form-group>
                              <div class="helperAndBlueStar"> <!-- petite triche pour faire apparaitre l'étoile en bleu -->
                                <opensilex-FormInputLabelHelper
                                        label="component.skos.uri"
                                        helpMessage="component.skos.uri-help"
                                ></opensilex-FormInputLabelHelper>
                                <pre class="blueStar"> *</pre>
                              </div>
                                <ValidationProvider
                                        :name="$t('component.skos.uri')"
                                        :rules="{
                    required: true,
                    regex: /^(http:\/\/www\.|https:\/\/www\.|http:\/\/|https:\/\/)?[a-z0-9]+([\-\.]{1}[a-z0-9]+)*\.[a-z]{2,5}(:[0-9]{1,5})?(\/.*)?$/
                  }"
                                        v-slot="{ errors }"
                                >
                    <span
                            class="error-message alert alert-danger"
                            v-if="isIncludedInRelations()"
                    >{{$t('component.skos.external-already-existing')}}</span>
                                    <b-form-input
                                            id="externalUri"
                                            v-model.trim="currentExternalUri"
                                            type="text"
                                            required
                                            :placeholder="$t('component.skos.uri-placeholder')"
                                            debounce="300"
                                    ></b-form-input>

                                    <div class="error-message alert alert-danger">{{ errors[0] }}</div>
                                </ValidationProvider>
                            </b-form-group>
                            <b-form-group label-align-sm="right">
                                <b-button
                                        @click="addRelationsToSkosReferences"
                                        class="greenThemeColor"
                                >{{$t('component.skos.add')}}
                                </b-button>
                            </b-form-group>
                            </opensilex-FilterField>
                        </div>
                    </div>
                </b-card>

                <b-form-group v-if="displayInsertButton" label-align-sm="right">
                    <b-button
                            class="float-right"
                            @click="update"
                    >{{$t("component.skos.update")}}
                    </b-button>
                </b-form-group>
            </b-form>
        </ValidationObserver>
        <div>
            <b-table v-if="relations.length !== 0"
            striped
              hover
              small
              responsive
              sort-icon-left
              bordered  
             :items="relations" 
             :fields="fields">
                <template v-slot:head(relation)="data">{{$t(data.label)}}</template>
                <template v-slot:cell(relation)="data">{{$t(data.value)}}</template>
                <template v-slot:head(relationURI)="data">{{$t(data.label)}}</template>
                <template v-slot:cell(relationURI)="data">
                    <a :href="data.value" target="_blank">{{ data.value }}</a>
                </template>
                <template v-slot:head(actions)="data">{{$t(data.label)}}</template>
                <template v-slot:cell(actions)="data">
                    <div class="text-center">
                    <b-button-group size="md">
                        <b-button
                                size="md"
                                @click="removeRelationsToSkosReferences(data.item)"
                                variant="danger"
                        >
                              <opensilex-Icon icon="fa#trash-alt" />
                        </b-button>
                    </b-button-group>
                    </div>
                </template>
            </b-table>
            <p v-else>
                <strong>{{$t('component.skos.no-external-links-provided')}}</strong>
            </p>
        </div>
    </div>
</template>

<script lang="ts">
    import {Component, Prop, PropSync, Ref} from "vue-property-decorator";
    import Vue from "vue";
    import {Skos} from "../../../models/Skos";
    import {ExternalOntologies} from "../../../models/ExternalOntologies";

    @Component
    export default class ExternalReferencesForm extends Vue {
        $opensilex: any;
        $store: any;
        $t: any;
        $i18n: any;

        currentRelation: string = "";
        currentExternalUri: string = "";

        @PropSync("references")
        skosReferences: any;

        @Ref("validatorRef") readonly validatorRef!: any;

        @Prop({default: true})
        displayInsertButton: boolean;

        @Prop({default: (() => [])})
        ontologiesToSelect: string[];

        get externalOntologiesRefs(): any[] {
            if (this.ontologiesToSelect) {
                return ExternalOntologies.getExternalOntologiesReferences(this.ontologiesToSelect);
            } else {
                return [];
            }
        }

        relationsInternal: any[] = [];

        skosRelationsMap: Map<string, string> = Skos.getSkosRelationsMap();

        options: any[] = [];

        setOptions(){
            this.options = [];
            for (let [key, value] of this.skosRelationsMap) {
                this.$set(this.options, this.options.length, {
                    id: key,
                    label: this.$t(value)
                });
            }
        }

        created() {
           this.setOptions();
        }

        private langUnwatcher;
        mounted() {
            this.langUnwatcher = this.$store.watch(
                () => this.$store.getters.language,
                () => {this.setOptions();}
            );
        }

        beforeDestroy() {
            this.langUnwatcher();
        }        

        resetForm() {
            this.currentRelation = "";
            this.currentExternalUri = "";
        }

        resetExternalUriForm() {
            this.currentExternalUri = "";
            this.$nextTick(() => this.validatorRef.reset());
        }

        fields = [
            {
                key: "relation",
                label: "component.skos.relation",
                sortable: true
            },
            {
                key: "relationURI",
                label: "component.skos.uri",
                sortable: false
            },
            {
                key: "actions",
                label: "component.common.actions"
            }
        ];

        get relations() {
            this.relationsInternal = [];
            if (this.skosReferences !== undefined) {
                for (let [key, value] of this.skosRelationsMap) {
                    this.updateRelations(key, this.skosReferences[key]);
                }
            }
            return this.relationsInternal;
        }

        updateRelations(relation: string, references: string[]) {
            if(references !== undefined){
                for (let index = 0; index < references.length; index++) {
                    const element = references[index];
                    this.addRelation(relation, element);
                }
            }
        }

        addRelation(relation: string, externalUri: string) {
            this.$set(this.relationsInternal, this.relationsInternal.length, {
                relation: this.skosRelationsMap.get(relation),
                relationURI: externalUri
            });
        }

        validateForm() {
            let validatorRef: any = this.$refs.validatorRef;
            return validatorRef.validate();
        }

        addRelationsToSkosReferences() {
            this.validateForm().then(isValid => {
                if (isValid) {
                    this.addRelationToSkosReferences();
                    return new Promise((resolve, reject) => {
                        this.$emit("onAdd", this.skosReferences, result => {
                            if (result instanceof Promise) {
                                result.then(resolve).catch(reject);
                            } else {
                                resolve(result);
                            }
                        });
                    });
                }
            });
        }

        addRelationToSkosReferences() {
            let isIncludedInRelations = this.isIncludedInRelations();
            if (!isIncludedInRelations) {
                this.skosReferences[this.currentRelation].push(this.currentExternalUri);
                this.resetExternalUriForm();
            }
        }

        isIncludedInRelations(): boolean {
            if (
                this.currentExternalUri == undefined ||
                this.currentExternalUri == "" ||
                this.currentExternalUri.length == 0
            ) {
                return false;
            }
            let includedInRelations = false;
            for (let [key, value] of this.skosRelationsMap) {
                if (this.skosReferences[key].includes(this.currentExternalUri)) {
                    includedInRelations = true;
                    break;
                }
            }
            return includedInRelations;
        }

        removeRelationsToSkosReferences(row: any) {
            for (let [key, value] of this.skosRelationsMap) {
                this.skosReferences[key] = this.skosReferences[key].filter(function (
                    value,
                    index,
                    arr
                ) {
                    return value != row.relationURI;
                });
            }
            return new Promise((resolve, reject) => {
                        this.$emit("onDelete", this.skosReferences, result => {
                            if (result instanceof Promise) {
                                result.then(resolve).catch(reject);
                            } else {
                                resolve(result);
                            }
                        });
                    });
        }

        async update() {
            return new Promise((resolve, reject) => {
                this.$emit("onUpdate", this.skosReferences, result => {
                    if (result instanceof Promise) {
                        result.then(resolve).catch(reject);
                    } else {
                        resolve(result);
                    }
                });
            });
        }
    }
</script>

<style scoped lang="scss">
    a {
        color: #007bff;
    }

    .helperAndBlueStar {
      display: flex;
    }
    .blueStar {
      color: #007bff;
    }
</style>
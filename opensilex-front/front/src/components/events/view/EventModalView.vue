<template>

    <b-modal
            ref="modalRef"
            :size="modalSize"
            :static="static"
            no-close-on-backdrop
            no-close-on-esc
            ok-only
            :title="$t('Event.event')"
    >
      <div class="card-body" v-if="event">
            <opensilex-UriView :uri="event.uri"></opensilex-UriView>
            <opensilex-TypeView :type="event.rdf_type" :typeLabel="event.rdf_type_name"></opensilex-TypeView>
            <opensilex-TextView label="component.common.description" :value="event.description"></opensilex-TextView><br>

            <opensilex-StringView label="Event.start"  v-if="event.start" :value="new Date(event.start).toLocaleString()">
            </opensilex-StringView>

            <opensilex-StringView label="Event.end" v-if="event.end" :value="new Date(event.end).toLocaleString()">
            </opensilex-StringView>

            <opensilex-StringView 
                label="Event.publisher" 
                :value="displayPublisher(event.publisher)">
            </opensilex-StringView>

            <opensilex-StringView label="Event.datePublication" :value="event.publication_date">
            </opensilex-StringView>

            <opensilex-StringView label="Event.lastUpdateDate" :value="event.last_updated_date">
            </opensilex-StringView>

          <opensilex-StringView class="overflow-auto" style="height: 100px" label="Event.targets" :uri="event.targets">
                <br>
                <div :key="targetURI" v-for="(targetURI) in event.targets">
                <opensilex-UriLink
                    :uri="targetURI"
                    :value="uriLabels[targetURI]"
                    :to="{
                        path: uriPaths[targetURI]
                    }"

                ></opensilex-UriLink>
                </div>

          </opensilex-StringView>

        </div>

        <div v-if="event && isMove()">
            <opensilex-MoveView
              :event="event"
              :positionsUriLabels="positionsUriLabels"
              :positionsUriPaths="positionsUriPaths"
            ></opensilex-MoveView>
        </div>

        <div v-if="hasRelations(event)" class="card-body">
            <br>
            <p><b>  {{$t('Event.specific-properties')}} ({{event.rdf_type_name}})</b></p>
            <hr/>

            <div :key="index" v-for="(relation, index) in event.relations">
                <opensilex-UriView
                    :uri="relation.value"
                    :value="specificPropertiesLabels[relation.value] ? specificPropertiesLabels[relation.value] : relation.value"
                    :title="getPropertyName(relation.property)"
                    v-bind:to="specificPropertiesPaths[relation.value] ? { path: specificPropertiesPaths[relation.value] } : null"
                    customClass="specificProperties"
                ></opensilex-UriView>

            </div>
        </div>

        <opensilex-DocumentTabList
            :modificationCredentialId="credentials.CREDENTIAL_DOCUMENT_MODIFICATION_ID"
            :uri="event.uri"
            :search=false
        ></opensilex-DocumentTabList>

        <template v-slot:modal-footer>
            <button
                type="button"
                class="btn greenThemeColor"
                v-on:click="hide(false)"
            >
                {{ $t('component.common.ok') }}
            </button>
        </template>


    </b-modal>
</template>

<script lang="ts">
import {Component, Prop, Ref} from "vue-property-decorator";
import Vue from "vue";
import {VueJsOntologyExtensionService, VueRDFTypeDTO} from "../../../lib";
import HttpResponse, {OpenSilexResponse} from "../../../lib/HttpResponse";
import {EventDetailsDTO, MoveDetailsDTO} from 'opensilex-core/index';
import {UserGetDTO} from "../../../../../../opensilex-security/front/src/lib";
import {OntologyService} from "opensilex-core/api/ontology.service";
import OpenSilexVuePlugin from "../../../models/OpenSilexVuePlugin";


    @Component
    export default class EventModalView extends Vue {

        @Ref("modalRef") readonly modalRef!: any;

        @Prop({default: "lg"})
        modalSize;

        /**
         * Renders the content of the component in-place in the DOM, rather than portalling it to be appended to the body element
         */
        @Prop({default: true})
        static: boolean;

        type: string;

        baseEventType: string;

        $opensilex: OpenSilexVuePlugin;

        $vueJsOntologyService: VueJsOntologyExtensionService;

        ontologyService: OntologyService;

        uriLabels: {[key : string] : string} = {};

        uriPaths: {[key : string] : string} = {};

        specificPropertiesPaths: {[key : string] : string} = {};

        specificPropertiesLabels: {[key : string] : string} = {};

        positionsUriLabels: {[key : string] : string} = {};

        positionsUriPaths: {[key : string] : string} = {};

        event: EventDetailsDTO = {};

        eventPropertyByUri: Map<string, VueRDFTypeDTO> = new Map();

        get credentials() {
          return this.$store.state.credentials;
        }

        static getEmptyForm(): EventDetailsDTO {
            return {
                uri: undefined,
                rdf_type: undefined,
                rdf_type_name: undefined,
                relations: [],
                start: undefined,
                end: undefined,
                targets: [],
                description: undefined,
                publisher: undefined,
                is_instant: true
            };
        }

        created() {
            this.baseEventType = this.$opensilex.Oeev.EVENT_TYPE_URI;
            this.$vueJsOntologyService = this.$opensilex.getService("opensilex.VueJsOntologyExtensionService");
            this.ontologyService = this.$opensilex.getService("opensilex.OntologyService");
            this.buildPropertyMap();
        }

        buildPropertyMap() {

            if(! this.type || this.$opensilex.Oeev.checkURIs(this.type,this.$opensilex.Oeev.EVENT_TYPE_URI)) {
                return;
            }

            this.$vueJsOntologyService.getRDFTypeProperties(this.type, this.baseEventType)
                .then((http: HttpResponse<OpenSilexResponse<VueRDFTypeDTO>>) => {
                    let typeModel = http.response.result;

                    this.eventPropertyByUri = new Map();
                    typeModel.data_properties.forEach(property => {
                        this.eventPropertyByUri.set(property.uri, property);
                    });
                    typeModel.object_properties.forEach(property => {
                        this.eventPropertyByUri.set(property.uri, property);
                    });
                }).catch(this.$opensilex.errorHandler);

        }

      /**
       * Fetches Event details using uri, loads the labels for inner targets and relations, then shows the modal
       *
       * @param promiseParam , not typed because this param is a bit wierd, it gets used alongside the getEventPromise whose param is also untyped
       * @param getEventPromise the getEvent service
       *
       * @return An EventViewCalculatableProps with the props values, or undefined if an error was caught and handled
       * the caller must verify not undefined.
       */
      async show(
        getEventPromiseHttpResult: HttpResponse<OpenSilexResponse<EventDetailsDTO>>
      ) {
        //Set the result to a const to be used for other get requests
        //We only set this.event at the end because of display delay problems
        const event: EventDetailsDTO = getEventPromiseHttpResult.response.result;

        // Check and load target names
        if (event.targets && event.targets.length > 0) {
          //Get labels and super-types at same time as the requests are independent
          const [targetLabels, targetTypes] = await Promise.all([
            this.ontologyService.getURILabelsList(event.targets),
            this.ontologyService.getURITypes(event.targets),
          ]);

          //Fill uriLabels data
          for (const element of targetLabels.response.result) {
            this.uriLabels[element.uri] = element.name;
          }

          // create paths
          for (const element of targetTypes.response.result) {
            this.uriPaths[element.uri] = this.$opensilex.getTargetPath(
              element.uri,
              null,
              this.$opensilex.getPathFromUriTypes(element.rdf_types)
            );
          }
        }

        // Check and load specific properties names and paths
        if (event.relations && event.relations.length > 0) {
          const relationsURIs = event.relations.map(relation => relation.value);

          const [specificPropertyLabels, specificPropertyTypes] = await Promise.all([
            this.ontologyService.getURILabelsList(relationsURIs),
            this.ontologyService.getURITypes(relationsURIs),
          ]);

          for (const element of specificPropertyLabels.response.result) {
            this.specificPropertiesLabels[element.uri] = element.name;
          }

          for (const element of specificPropertyTypes.response.result) {
            this.specificPropertiesPaths[element.uri] = this.$opensilex.getTargetPath(
              element.uri,
              null,
              this.$opensilex.getPathFromUriTypes(element.rdf_types)
            );
          }
        }

        if (this.isMove(event) && (event as MoveDetailsDTO).targets_positions) {

          try {
            // Retrieve position target names from move
            const targetUris = (event as MoveDetailsDTO).targets_positions.map((positionObject: any) => positionObject.target);

            const [labelsResponse, typesResponse] = await Promise.all([
              this.ontologyService.getURILabelsList(targetUris),
              this.ontologyService.getURITypes(targetUris),
            ]);

            for (let element of labelsResponse.response.result) {
              this.$set(this.positionsUriLabels, element.uri, element.name);
            }

            // Creation of paths for move position targets types
            for (let element of typesResponse.response.result) {
              const responsePath = this.$opensilex.getTargetPath(
                element.uri,
                null,
                this.$opensilex.getPathFromUriTypes(element.rdf_types)
              );
              this.$set(this.positionsUriPaths, element.uri, responsePath);
            }
          } catch (error) {
            console.error("Error processing move positions:", error);
          }
        }

        this.event = getEventPromiseHttpResult.response.result;
        this.type = event.rdf_type;

        // Trigger DOM update and show modal
        this.buildPropertyMap();
        this.$nextTick(() => {
          this.buildPropertyMap();
          this.modalRef.show();
        });
      }

      getPropertyName(propertyUri: string) {

            if (!propertyUri) {
                return undefined;
            }

            let property = this.eventPropertyByUri.get(propertyUri);
            if (property) {
                return property.name;
            }
            return property;
        }

        hide() {
            this.modalRef.hide();
        }

        isMove(event?: EventDetailsDTO): boolean {
            const eventToUse: EventDetailsDTO = event ? event : this.event;
            if (!eventToUse.rdf_type) {
                return false;
            }

          return this.$opensilex.Oeev.checkURIs(eventToUse.rdf_type,this.$opensilex.Oeev.MOVE_TYPE_URI)
        }

        hasRelations(event): boolean {
            return event && event.relations && event.relations.length > 0;
        }

        displayPublisher(publisher: UserGetDTO) {
            if(!publisher || !publisher.uri) {
                return undefined;
            } else {
                return publisher.first_name && publisher.last_name ? publisher.first_name + ' ' + publisher.last_name : publisher.uri
            }
        }
    }

</script>

<style scoped lang="scss">
    .icon-title {
        margin-right: 5px;
    }

    ::v-deep .full-screen-modal-form > .modal-dialog {
        max-width: 95%;
    }
</style>;

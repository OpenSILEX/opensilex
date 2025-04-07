<template>
  <span class="person_contact" v-if="personContact">
      <b-button
          id="name"
          @click="showPopup"
          variant="link">
        {{ displayableName }}
      </b-button>

      <a v-if="personHasMail" :href="mailToUrl">
        <button
            class="mail-button"
            :title="$t('component.person.mail-redirection')"
        >
          <opensilex-Icon icon="ik#ik-mail"/>
        </button>
      </a>
         &nbsp;
      <button
          v-else-if="uri"
          v-on:click.prevent.stop="copyURI(uri)"
          class="mail-button"
          :title="$t('component.copyToClipboard.copyUri')"
      >
        <opensilex-Icon icon="ik#ik-copy"/>
      </button>

    <b-modal
        ref="popup"
        :title="first_name+' '+last_name"
        hide-footer
        centered>

      <div class="champ-popup">
        <p class="title-popup"> URI : </p>
        <p>  {{ uri }} </p>
      </div>

      <div class="champ-popup" v-if="orcid">
        <p class="title-popup">{{ $t("component.person.orcid") }} :</p>
        <opensilex-UriLink
            :url="orcid"
            :value="orcid"
            class="personOrcid"
        />
      </div>

      <div class="champ-popup" v-if="mail">
        <p class="title-popup">{{ $t("component.person.email") }} :</p>
        <p>  {{ mail }} </p>
      </div>

      <div class="champ-popup" v-if="affiliation">
        <p class="title-popup">{{ $t("component.person.affiliation") }} :</p>
        <p>  {{ affiliation }} </p>
      </div>

      <div class="champ-popup" v-if="phone">
        <p class="title-popup">{{ $t("component.person.phone_number") }} :</p>
        <p>  {{ phone }} </p>
      </div>

    </b-modal>
  </span>
</template>

<script lang="ts">
import {Component, Prop} from "vue-property-decorator"
import copy from "copy-to-clipboard"
import Vue from "vue"
import OpenSilexVuePlugin from "../../models/OpenSilexVuePlugin"
import {PersonDTO} from "opensilex-security/index";
import {AccountGetDTO} from "opensilex-security/model/accountGetDTO";

@Component
export default class PersonContact extends Vue {
  $opensilex: OpenSilexVuePlugin

  @Prop({ required: true })
  personContact!: PersonDTO | AccountGetDTO

  @Prop()
  customDisplayableName: String

  private isAccountDTO(person: any): person is AccountGetDTO {
    return 'person_first_name' in person || 'person_last_name' in person;
  }

  private isPersonDTO(person: any): person is PersonDTO {
    return 'first_name' in person || 'last_name' in person;
  }

  get displayableName(): String {
    return this.customDisplayableName ?
        this.customDisplayableName :
        this.first_name + " " + this.last_name
  }

  get mailToUrl() {
    return this.mail ? "mailto:" + this.mail : null
  }

  get uri(): string {
    return this.personContact.uri
  }

  get first_name(): string {
    if (this.isPersonDTO(this.personContact)) {
      return this.personContact.first_name || "";
    }
    if (this.isAccountDTO(this.personContact)) {
      return this.personContact.person_first_name || "";
    }
    return "";
  }

  get last_name(): string {
    if (this.isPersonDTO(this.personContact)) {
      return this.personContact.last_name || "Contact";
    }
    if (this.isAccountDTO(this.personContact)) {
      return this.personContact.person_last_name || "Contact";
    }
    return "Contact";
  }

  get orcid(): string {
    if (this.isPersonDTO(this.personContact)) {
      return this.personContact.orcid || "";
    }
    return "";
  }

  get mail(): string {
    return this.personContact.email
  }

  get personHasMail(): boolean {
    return this.mail != null && this.mail != ""
  }

  get affiliation(): string {
    if (this.isPersonDTO(this.personContact)){
      return this.personContact.affiliation || "";
    }
    return "";
  }

  get phone(): string {
    if (this.isPersonDTO(this.personContact)) {
      return this.personContact.phone_number || "";
    }
    return "";
  }

  showPopup() {
    let modalRef: any = this.$refs.popup;
    modalRef.show();
  }

  copyURI(address) {
    copy(address);
    this.$opensilex.showSuccessToast(
        address.startsWith("http://") || address.startsWith("https://")
            ? this.$t("component.common.url-copy") + ": " + address
            : this.$t("component.common.uri-copy") + ": " + address
    );
  }
}
</script>

<style scoped lang="scss">
#name {
  padding: 0;
  color: #2E7FFD;
}

.mail-button {
  text-decoration: none !important;
  background-color: transparent !important;
}

.person_contact {
  display: inline-flex;
  max-width: 400px;
  padding-right: 30px;
  position: relative;
}

.person_contact .mail-button {
  display: none;
  border: 1px solid #d8dde5;
  border-radius: 5px;
  color: #212121;
  padding: 3px 5px 0;
  position: absolute;
  right: 0;
  top: -3px;
}

.person_contact:hover .mail-button {
  display: inline;
}

.person_contact:hover {
  color: #212121;
  text-decoration: underline;
}

.champ-popup {
  display: flex;
}

.title-popup {
  font-weight: bold;
  padding-right: 1%;
}

.champ-popup a:hover {
  text-decoration: underline;
}
</style>

<i18n>
en:
  Annotation:
    publisher: Publisher
fr:
  Annotation:
    publisher: Publieur
</i18n>

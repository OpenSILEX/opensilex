<template>
  <span class="person_contact" v-if="personContact">
    <!-- Nom cliquable -> ouvre la popup -->
    <button id="name" type="button" class="linklike" @click="showPopup">
      {{ displayableName }}
    </button>

    <!-- Bouton mail si disponible -->
    <a v-if="personHasMail" :href="mailToUrl">
      <button
        class="mail-button"
        :title="t('component.person.mail-redirection')"
        @click.stop
      >
        <opensilex-Icon icon="bi#bi-envelope" />
      </button>
    </a>

    <!-- Sinon copie d’URI -->
    <button
      v-else-if="uri"
      class="mail-button"
      :title="t('component.copyToClipboard.copyUri')"
      @click.stop.prevent="copyURI(uri)"
    >
      <opensilex-Icon icon="bi#bi-clipboard" />
    </button>

    <!-- Pop -->
    <teleport to="body">
      <div v-if="showModal" class="pc-modal-backdrop" @click.self="closePopup">
        <div class="pc-modal">
          <div class="pc-modal-header">
            <strong>{{ first_name }} {{ last_name }}</strong>
            <button class="pc-modal-close" @click="closePopup" aria-label="Close">x</button>
          </div>

          <div class="pc-modal-body">
            <div class="champ-popup">
              <p class="title-popup">URI :</p>
              <p>{{ uri }}</p>
            </div>

            <div class="champ-popup" v-if="orcid">
              <p class="title-popup">{{ t('component.person.orcid') }} :</p>
              <opensilex-UriLink :url="orcid" :value="orcid" class="personOrcid" />
            </div>

            <div class="champ-popup" v-if="mail">
              <p class="title-popup">{{ t('component.person.email') }} :</p>
              <p>{{ mail }}</p>
            </div>

            <div class="champ-popup" v-if="affiliation">
              <p class="title-popup">{{ t('component.person.affiliation') }} :</p>
              <p>{{ affiliation }}</p>
            </div>

            <div class="champ-popup" v-if="phone">
              <p class="title-popup">{{ t('component.person.phone_number') }} :</p>
              <p>{{ phone }}</p>
            </div>
          </div>
        </div>
      </div>
    </teleport>
  </span>
</template>

<script setup lang="ts">
import { computed, inject, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import copy from 'copy-to-clipboard'
import type { OpenSilexVuePlugin } from '@/models/OpenSilexVuePlugin'
import type { PersonDTO } from 'opensilex-security'
import type { AccountGetDTO } from 'opensilex-security/model/accountGetDTO'

// Props
const props = defineProps<{
  personContact: PersonDTO | AccountGetDTO
  customDisplayableName?: string
}>()

const { t } = useI18n()
const opensilex = inject<OpenSilexVuePlugin>('$opensilex')!

// Helpers de type
function isAccountDTO(person: any): person is AccountGetDTO {
  return 'person_first_name' in person || 'person_last_name' in person
}
function isPersonDTO(person: any): person is PersonDTO {
  return 'first_name' in person || 'last_name' in person
}

// Modal state
const showModal = ref(false)

// Computed fields
const displayableName = computed(() => {
  if (props.customDisplayableName) return props.customDisplayableName
  return `${first_name.value} ${last_name.value}`.trim()
})

const uri = computed(() => props.personContact?.uri ?? '')

const first_name = computed(() => {
  const p: any = props.personContact
  return p?.first_name ?? p?.person_first_name ?? ''
})

const last_name = computed(() => {
  const p: any = props.personContact
  return p?.last_name ?? p?.person_last_name ?? 'Contact'
})


const orcid = computed(() => (isPersonDTO(props.personContact) ? (props.personContact.orcid || '') : ''))
const mail = computed(() => (props.personContact as any)?.email || '')
const personHasMail = computed(() => !!mail.value && mail.value !== '')
const mailToUrl = computed(() => (personHasMail.value ? `mailto:${mail.value}` : null))

const affiliation = computed(() => (isPersonDTO(props.personContact) ? (props.personContact.affiliation || '') : ''))
const phone = computed(() => (isPersonDTO(props.personContact) ? (props.personContact.phone_number || '') : ''))

// Actions
function showPopup () { showModal.value = true }
function closePopup () { showModal.value = false }

function copyURI (address: string) {
  if (!address) return
  copy(address)
  const msg = address.startsWith('http://') || address.startsWith('https://')
    ? `${t('component.common.url-copy')}: ${address}`
    : `${t('component.common.uri-copy')}: ${address}`
  opensilex.showSuccessToast(msg)
}
</script>

<style scoped lang="scss">
#name,
.linklike {
  padding: 0;
  background: none;
  border: 0;
  color: #2E7FFD;
  cursor: pointer;
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

.person_contact:hover .mail-button { display: inline; }
.person_contact:hover { color: #212121; text-decoration: underline; }

.champ-popup { display: flex; }
.title-popup { font-weight: bold; padding-right: 1%; }
.champ-popup a:hover { text-decoration: underline; }

/* Modal minimaliste */
.pc-modal-backdrop {
  position: fixed; inset: 0;
  background: rgba(0,0,0,.45);
  display: flex; align-items: center; justify-content: center;
  z-index: 1050;
}
.pc-modal {
  background: white; border-radius: 8px;
  width: min(520px, 92vw);
  box-shadow: 0 10px 30px rgba(0,0,0,.2);
}
.pc-modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid #eee;
}
.pc-modal-close {
  background: transparent; border: none; font-size: 20px; line-height: 1; cursor: pointer;
}
.pc-modal-body { padding: 16px; }
</style>

<i18n>
en:
  component:
    person:
      mail-redirection: "Send a mail"
      orcid: "ORCID"
      email: "Email"
      affiliation: "Affiliation"
      phone_number: "Phone number"
    common:
      url-copy: "URL copied"
      uri-copy: "URI copied"
fr:
  component:
    person:
      mail-redirection: "Envoyer un email"
      orcid: "ORCID"
      email: "Email"
      affiliation: "Affiliation"
      phone_number: "Numéro de téléphone"
    common:
      url-copy: "URL copiée"
      uri-copy: "URI copiée"
</i18n>

<template>
  <div v-if="choice?.candidates?.length" class="mb-2">
    <div class="small fw-semibold">{{ title }}</div>
    <ul class="small mb-0">
      <li v-for="(candidate, index) in choice.candidates" :key="index">
        <strong>{{ candidate.label }}</strong>
        <span class="text-body-secondary ms-1">{{ percent(candidate.score) }}</span>
        <ul class="mb-0">
          <li v-for="(evidence, position) in candidate.evidence ?? []" :key="position">
            <span :class="(evidence.weight ?? 0) < 0 ? 'text-danger' : 'text-success'">{{ signed(evidence.weight) }}</span>
            {{ reportText($opensilex, evidence.reason) }}
          </li>
        </ul>
      </li>
    </ul>
    <p v-for="(note, index) in choice.notes ?? []" :key="'note' + index" class="small text-warning-emphasis mb-0">
      {{ reportText($opensilex, note) }}
    </p>
  </div>
</template>

<script setup lang="ts">
import { inject } from 'vue'
import type { ChoiceDTO } from '../lib'
import type { OpenSilexPlugin } from '../opensilexPlugin'
import { percent, reportText, signed } from '../reasons'

//#region Public
defineProps<{
  choice?: ChoiceDTO
  /** What the choice is about, already translated. */
  title: string
}>()
//#endregion

//#region Private
//#region Plugins & services
const $opensilex = inject<OpenSilexPlugin>('$opensilex')!
//#endregion
//#endregion
</script>

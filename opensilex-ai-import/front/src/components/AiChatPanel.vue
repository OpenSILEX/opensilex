<template>
  <div class="card h-100">
    <div class="card-header d-flex align-items-center justify-content-between">
      <span class="fw-semibold">{{ $t('AiImport.chat.title') }}</span>
    </div>

    <div ref="scroller" class="card-body ai-import-chat__scroller">
      <p v-if="!messages.length" class="text-body-secondary mb-0">
        {{ $t('AiImport.chat.empty') }}
      </p>

      <template v-for="(message, index) in messages" :key="index">
        <opensilex-ai-import-AiChatMessage :message="message" />
        <!-- The confirmation sits next to the words that explain it, not in a panel elsewhere. -->
        <opensilex-ai-import-CreationProposalCard
          v-if="message.proposal_id && proposalsById[message.proposal_id]"
          :proposal="proposalsById[message.proposal_id]"
          :session-id="sessionId"
          @applied="$emit('applied', $event)"
          @cancelled="$emit('proposalCancelled')"
        />
      </template>

      <div v-if="busy" class="d-flex align-items-center gap-2 text-body-secondary">
        <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        <span>{{ $t('AiImport.chat.thinking') }}</span>
      </div>
    </div>

    <div class="card-footer">
      <form @submit.prevent="submit">
        <div class="d-flex gap-2 align-items-end">
          <textarea
            v-model="draft"
            class="form-control"
            rows="2"
            :placeholder="$t('AiImport.chat.placeholder')"
            :disabled="busy"
            @keydown.enter.exact.prevent="submit"
          ></textarea>
          <n-button
            type="primary"
            attr-type="submit"
            :disabled="busy || !draft.trim()"
            :loading="busy"
          >
            {{ $t('AiImport.chat.send') }}
          </n-button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

/**
 * The conversation. Sending is the parent's job: it owns the session and the report, which a reply
 * can change.
 */
const props = defineProps<{
  messages: Array<{ role: string; content: string; lookups?: string[]; proposal_id?: string }>
  busy: boolean
  sessionId: string
  /**
   * The drafts of this conversation, keyed by identifier. Only the one a message points at is
   * rendered, so an old turn keeps showing what was decided on it.
   */
  proposalsById: { [id: string]: any }
}>()

const emit = defineEmits<{
  (event: 'ask', question: string): void
  (event: 'applied', payload: { target: string; report: any }): void
  (event: 'proposalCancelled'): void
}>()

const draft = ref('')
const scroller = ref<HTMLElement | null>(null)

function submit() {
  const question = draft.value.trim()
  if (!question || props.busy) {
    return
  }
  draft.value = ''
  emit('ask', question)
}

// Enter submits, so a multi-line answer needs shift+enter; keep the view pinned to the latest turn.
watch(
  () => [props.messages.length, props.busy],
  async () => {
    await nextTick()
    if (scroller.value) {
      scroller.value.scrollTop = scroller.value.scrollHeight
    }
  }
)
</script>

<style scoped>
.ai-import-chat__scroller {
  overflow-y: auto;
  /* The panel sits beside the report, and both should scroll independently rather than the page. */
  max-height: 60vh;
  min-height: 18rem;
}
</style>

<template>
  <div class="ai-import-message" :class="`ai-import-message--${message.role}`">
    <div class="ai-import-message__author">
      {{ message.role === 'user' ? $t('AiImport.chat.you') : $t('AiImport.chat.assistant') }}
    </div>

    <!--
      A message of the module's own — the model unreachable, the lookup limit hit — carries a
      translation key instead of prose, so it reads in the user's language and not the model's.
    -->
    <div v-if="message.content_key" class="ai-import-message__body ai-import-message__body--notice">
      {{ $t(message.content_key) }}
    </div>
    <div v-else-if="message.role === 'assistant'" class="ai-import-message__body" v-html="rendered"></div>
    <div v-else class="ai-import-message__body ai-import-message__body--plain">{{ message.content }}</div>

    <details v-if="message.lookups && message.lookups.length" class="ai-import-message__lookups">
      <summary>{{ $t('AiImport.chat.lookups') }} ({{ message.lookups.length }})</summary>
      <ul>
        <li v-for="(lookup, index) in message.lookups" :key="index"><code>{{ lookup }}</code></li>
      </ul>
    </details>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'

/**
 * One turn of the conversation.
 *
 * The assistant answers in markdown, so it is rendered. A user message is not: it is shown exactly
 * as typed, since rendering someone's own input as markup would be surprising and pointless.
 */
const props = defineProps<{
  message: {
    role: string
    content: string
    /** Set when the module wrote the message rather than the assistant. */
    content_key?: string
    lookups?: string[]
  }
}>()

const rendered = computed(() => {
  const content = props.message.content
  if (!content) {
    return ''
  }
  return marked.parse(content, { async: false, breaks: true }) as string
})
</script>

<style scoped>
.ai-import-message {
  margin-bottom: 1rem;
}

/* A notice is the module speaking, not the assistant: set apart, and never rendered as markdown. */
.ai-import-message__body--notice {
  border-left: 3px solid var(--bs-warning, #ffc107);
  padding-left: 0.6rem;
  color: var(--bs-secondary-color, #6c757d);
}

.ai-import-message__author {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  opacity: 0.6;
  margin-bottom: 0.25rem;
}

.ai-import-message__body {
  padding: 0.6rem 0.85rem;
  border-radius: 0.5rem;
  background-color: rgba(127, 127, 127, 0.08);
  overflow-wrap: break-word;
}

.ai-import-message--user .ai-import-message__body {
  background-color: rgba(13, 110, 253, 0.09);
}

.ai-import-message__body--plain {
  white-space: pre-wrap;
}

/* The assistant is told to keep tables small, but a wide one must not stretch the panel. */
.ai-import-message__body :deep(table) {
  display: block;
  overflow-x: auto;
  max-width: 100%;
  font-size: 0.875rem;
}

.ai-import-message__body :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-import-message__body :deep(pre) {
  overflow-x: auto;
  padding: 0.5rem;
  border-radius: 0.25rem;
  background-color: rgba(127, 127, 127, 0.12);
}

.ai-import-message__lookups {
  margin-top: 0.35rem;
  font-size: 0.8rem;
  opacity: 0.75;
}

.ai-import-message__lookups ul {
  margin: 0.25rem 0 0;
  padding-left: 1.1rem;
}
</style>

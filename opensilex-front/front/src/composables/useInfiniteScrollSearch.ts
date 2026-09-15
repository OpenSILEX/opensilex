import { computed, ref, shallowRef } from 'vue'

export interface UseInfiniteScrollSearchOptions<TRaw, TOption, TKey> {
  /** Page size, or a function resolving it fresh on every call (e.g. reading a prop). */
  pageSize: number | (() => number)
  debounceMs?: number
  /**
   * Renvoie la page demandée. `hasNext` est facultatif : s'il est fourni (l'API le connaît),
   * il fait autorité pour savoir s'il reste des pages. Sinon on retombe sur une heuristique.
   */
  fetchPage: (query: string, pageIndex: number, pageSize: number) => Promise<{ result: TRaw[]; total: number; hasNext?: boolean }>
  mapItem: (raw: TRaw) => TOption
  getOptionKey: (option: TOption) => TKey
  onError?: (error: unknown) => void
  /** Runs only on reset (page-0) loads, e.g. to re-inject already-selected options missing from the new page. */
  afterReset?: (newOptions: TOption[]) => TOption[]
}

/**
 * Infinite-scroll pagination state machine: tracks page/loading/hasMore, de-dupes and appends
 * pages, debounces search-triggered resets, and guards against out-of-order responses.
 */
export default function useInfiniteScrollSearch<TRaw, TOption, TKey>(
  options: UseInfiniteScrollSearchOptions<TRaw, TOption, TKey>
) {
  const debounceMs = options.debounceMs ?? 250

  const items = shallowRef<TOption[]>([])
  const searchText = ref('')
  const currentPage = ref(0)
  const isLoading = ref(false)
  const hasMoreResults = ref(true)
  const totalCount = ref(0)
  const loadedCount = ref(0)

  // Local to this call, not module-scoped: two component instances using this composable
  // must never share a counter/timer, or one instance's request could invalidate another's.
  let searchTimer: number | null = null
  let requestId = 0

  const displayedCount = computed(() => {
    if (totalCount.value <= 0) return items.value.length
    return Math.min(loadedCount.value, totalCount.value)
  })

  function resolvePageSize(): number {
    return typeof options.pageSize === 'function' ? options.pageSize() : options.pageSize
  }

  async function load(reset: boolean) {
    if (isLoading.value) return
    if (!hasMoreResults.value && !reset) return

    const currentRequestId = ++requestId
    isLoading.value = true

    try {
      if (reset) {
        currentPage.value = 0
        hasMoreResults.value = true
        items.value = []
        totalCount.value = 0
        loadedCount.value = 0
      }

      const pageSize = resolvePageSize()
      const { result, total, hasNext } = await options.fetchPage(searchText.value.trim(), currentPage.value, pageSize)

      // Ignore a stale response if a newer request has since started (e.g. fast typing).
      if (currentRequestId !== requestId) return

      totalCount.value = total
      loadedCount.value = reset ? result.length : loadedCount.value + result.length

      const mapped = result.map(options.mapItem)
      const existingKeys = new Set(items.value.map(options.getOptionKey))
      const newItems = mapped.filter(option => !existingKeys.has(options.getOptionKey(option)))

      const nextItems = reset ? newItems : [...items.value, ...newItems]
      items.value = reset && options.afterReset ? options.afterReset(nextItems) : nextItems

      // pageSize <= 0 is a "load everything at once" request: never ask for a next page.
      // Sinon on préfère le hasNext de l'API ; l'heuristique ne sert que s'il est absent
      // (?? et non || : un hasNext explicitement false doit être respecté).
      hasMoreResults.value =
        pageSize > 0 &&
        (hasNext ?? (
          totalCount.value > 0 &&
          loadedCount.value < totalCount.value &&
          result.length === pageSize
        ))

      if (result.length > 0) {
        currentPage.value += 1
      }
    } catch (error) {
      options.onError?.(error)
    } finally {
      if (currentRequestId === requestId) {
        isLoading.value = false
      }
    }
  }

  function search(query: string) {
    searchText.value = query ?? ''

    if (searchTimer) {
      window.clearTimeout(searchTimer)
    }

    searchTimer = window.setTimeout(() => {
      load(true)
    }, debounceMs)
  }

  function reload() {
    return load(true)
  }

  function loadMore() {
    return load(false)
  }

  function onScroll(event: Event) {
    const target = event.target as HTMLElement | null
    if (!target) return

    const bottomThreshold = 80
    const position = target.scrollTop + target.clientHeight
    const nearBottom = position >= target.scrollHeight - bottomThreshold

    if (nearBottom && hasMoreResults.value && !isLoading.value) {
      loadMore()
    }
  }

  function dispose() {
    if (searchTimer) {
      window.clearTimeout(searchTimer)
    }
  }

  return {
    options: items,
    isLoading,
    hasMoreResults,
    totalCount,
    loadedCount,
    displayedCount,
    searchText,
    search,
    reload,
    loadMore,
    onScroll,
    dispose
  }
}

#!/usr/bin/env bash
# Mechanical check of the OpenSILEX conventions on .vue files.
#
# Usage:
#   check-vue-conventions.sh [file.vue ...]
#   without arguments: the .vue files changed against HEAD, plus the untracked ones (git).
#
# Output: one line per finding, `file:line: LEVEL [rule] message`.
#   ERROR = breaks an explicit convention; WARN = needs a look (often a real problem).
# Exit code: 1 when there is at least one ERROR, 0 otherwise.
#
# This script does not replace a review: it sees neither dead code, nor the reuse of the
# common/ building blocks, nor a wrapped FormSelector, nor hard-coded text.

set -u

files=()
if [ "$#" -gt 0 ]; then
  files=("$@")
else
  while IFS= read -r f; do
    [ -n "$f" ] && files+=("$f")
  done < <( { git diff --name-only HEAD -- '*.vue'; git ls-files --others --exclude-standard -- '*.vue'; } 2>/dev/null | sort -u )
fi

if [ "${#files[@]}" -eq 0 ]; then
  echo "No .vue file to check." >&2
  exit 0
fi

errors=0

report() { # file line level rule message
  printf '%s:%s: %s [%s] %s\n' "$1" "$2" "$3" "$4" "$5"
  [ "$3" = "ERROR" ] && errors=$((errors + 1))
}

# grep_rule file level rule message regex [regex-to-exclude]
grep_rule() {
  local f="$1" level="$2" rule="$3" msg="$4" re="$5" exclude="${6:-}"
  local line
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    if [ -n "$exclude" ] && printf '%s' "${line#*:}" | grep -qE -- "$exclude"; then continue; fi
    report "$f" "${line%%:*}" "$level" "$rule" "$msg"
  done < <(grep -nE -- "$re" "$f" 2>/dev/null)
}

for f in "${files[@]}"; do
  [ -f "$f" ] || { echo "$f: not found" >&2; continue; }

  # 1. Block order: template → script setup → style → i18n
  order=$(grep -nE '^<(template|script|style|i18n)([ >]|$)' "$f" | sed -E 's/^([0-9]+):<([a-z0-9]+).*/\2/' | tr '\n' ' ' | sed 's/ $//')
  case "$order" in
    "template script"|"template script style"|"template script i18n"|"template script style i18n") ;;
    *) report "$f" 1 ERROR block-order "block order \"$order\"; expected: template script style i18n" ;;
  esac

  # 2. <script setup lang="ts">
  if ! grep -qE '^<script setup lang="ts">' "$f"; then
    first_script=$(grep -nE '^<script' "$f" | head -1 | cut -d: -f1)
    report "$f" "${first_script:-1}" ERROR script-setup 'expected <script setup lang="ts">'
  fi

  # 3. <style scoped lang="scss">
  grep_rule "$f" ERROR style-scoped 'expected <style scoped lang="scss">' '^<style' '^<style scoped lang="scss">'

  # 4. defineEmits: named-tuple syntax, not call overloads, not an array of strings
  grep_rule "$f" ERROR emits-syntax "defineEmits with call-overload syntax: write  name: [payload: Type]" "\(e: *['\"]"
  grep_rule "$f" ERROR emits-untyped "untyped defineEmits: write defineEmits<{ name: [payload: Type] }>()" "defineEmits\(\["

  # 5. Template refs: useTemplateRef<InstanceType<typeof X>>('name')
  grep_rule "$f" ERROR template-ref "template ref as ref(null): use useTemplateRef<InstanceType<typeof X>>('name')" "\bref(<[^>]*>)?\(null\)"
  grep_rule "$f" WARN template-ref "ref<InstanceType<…>>: a template ref is written with useTemplateRef" "\bref<InstanceType"
  grep_rule "$f" WARN template-ref-untyped "useTemplateRef without a type parameter" "useTemplateRef\(['\"]"

  # 6. any
  grep_rule "$f" ERROR no-any "type any: use the generated DTO/service, a naive-ui type or a local interface" "(:[[:space:]]*any\b|<any>|<any\[\]>|\bas any\b|\bany\[\]|Array<any>|Record<[^>]*,[[:space:]]*any>|\(any\))" "^[[:space:]]*(//|\*)"

  # 7. Vue 2 / bootstrap-vue leftovers
  grep_rule "$f" ERROR vue2 "Vue 2 / bootstrap-vue leftover" '(<b-[a-z]|vue-property-decorator|@Component|@Prop\(|export default \{|this\.\$)'
  grep_rule "$f" WARN vue2-emit "\$emit in the script: use the function returned by defineEmits" '\$emit\(' '(^[[:space:]]*@|="|#)'

  # 8. Bootstrap components (the d-flex, m*-, p*-, text-* utilities are fine)
  grep_rule "$f" ERROR bootstrap "Bootstrap component: replace it with the Naive UI equivalent (see references/bootstrap-to-naive.md)" "class=\"([^\"]* )?(row|col(-[a-z0-9]+)*|btn(-[a-z-]+)?|card(-[a-z]+)?|form-group|form-control|form-select|form-check|input-group|dropdown(-[a-z]+)?|table|list-group(-item)?|nav|nav-tabs|nav-item|nav-link|alert(-[a-z]+)?|badge|modal(-[a-z]+)?|collapse|container(-fluid)?)( [^\"]*)?\""
  grep_rule "$f" ERROR bootstrap "Bootstrap JS attribute (data-bs-*): use the Naive UI component" "data-bs-"

  # 9. //#region Public / Private regions and place of defineExpose
  pub=$(grep -nE '//[[:space:]]*#region[[:space:]]+Public' "$f" | head -1 | cut -d: -f1)
  priv=$(grep -nE '//[[:space:]]*#region[[:space:]]+Private' "$f" | head -1 | cut -d: -f1)
  script_len=$(awk '/^<script/{s=1;next} /^<\/script>/{s=0} s' "$f" | wc -l)
  # The Public region is required only when the component has a public API.
  has_public_api=$(grep -cE '\b(defineProps|defineEmits|defineModel)\b' "$f")
  if [ -z "$priv" ] || { [ -z "$pub" ] && [ "$has_public_api" -gt 0 ]; }; then
    # Below 25 script lines the whole script fits on one screen, so missing regions are tolerated.
    if [ "$script_len" -gt 25 ]; then
      report "$f" 1 ERROR regions "//#region Public and //#region Private missing (script of $script_len lines)"
    fi
  else
    [ -z "$pub" ] || [ "$pub" -lt "$priv" ] || report "$f" "$priv" ERROR regions "the Private region comes before the Public region"
    while IFS=: read -r ln _; do
      [ "$ln" -lt "$priv" ] || report "$f" "$ln" ERROR regions "defineProps/defineEmits/defineModel outside the Public region"
    done < <(grep -nE '\b(defineProps|defineEmits|defineModel)\b' "$f")
    expose=$(grep -nE '^[[:space:]]*defineExpose\(' "$f" | head -1 | cut -d: -f1)
    last_end=$(grep -nE '//[[:space:]]*#endregion' "$f" | tail -1 | cut -d: -f1)
    if [ -n "$expose" ] && [ -n "$last_end" ] && [ "$expose" -lt "$last_end" ]; then
      report "$f" "$expose" ERROR regions "defineExpose() must come after the Private region is closed"
    fi
    # Order of the Private sub-regions
    awk -v F="$f" -v P="$priv" '
      function rank(t) {
        t = tolower(t)
        if (t ~ /plugin|service|inject|composable/) return 1
        if (t ~ /template/) return 2
        if (t ~ /data|computed|state|constant/) return 3
        if (t ~ /hook|watch|lifecycle/) return 4
        if (t ~ /event|handler/) return 5
        if (t ~ /method|function/) return 6
        return 0
      }
      NR > P && /\/\/[ \t]*#region/ {
        title = $0; sub(/.*#region[ \t]*/, "", title)
        r = rank(title)
        if (r > 0 && r < last) printf "%s:%d: WARN [region-order] sub-region \"%s\" after a higher-ranked sub-region (order: Plugins & services → Template refs → Data & Computed → Hooks & watchers → Event handlers → Methods)\n", F, NR, title
        if (r > 0) last = r
      }' "$f"
  fi

  # 10. Miscellaneous
  grep_rule "$f" WARN console "console.* left in the component" "console\.(log|debug)\("
  grep_rule "$f" WARN function-type "Function type too broad: type the signature" ":[[:space:]]*Function\b"
done

if [ "$errors" -gt 0 ]; then
  echo "— $errors error(s)." >&2
  exit 1
fi
exit 0

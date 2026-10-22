#!/usr/bin/env bash
# Contrôle mécanique des conventions OpenSILEX sur des fichiers .vue.
#
# Usage :
#   check-vue-conventions.sh [fichier.vue ...]
#   sans argument : les .vue modifiés ou nouveaux par rapport à HEAD (git).
#
# Sortie : une ligne par constat, `fichier:ligne: NIVEAU [règle] message`.
#   ERROR = viole une convention explicite ; WARN = à examiner (souvent un vrai problème).
# Code de retour : 1 s'il y a au moins une ERROR, 0 sinon.
#
# Ce script ne remplace pas la revue : il ne voit ni le code mort, ni la réutilisation
# des briques common/, ni le double wrapping d'un FormSelector, ni le texte en dur.

set -u

if [ "$#" -gt 0 ]; then
  files=("$@")
else
  mapfile -t files < <( { git diff --name-only HEAD -- '*.vue'; git ls-files --others --exclude-standard -- '*.vue'; } 2>/dev/null | sort -u )
fi

if [ "${#files[@]}" -eq 0 ]; then
  echo "Aucun fichier .vue à contrôler." >&2
  exit 0
fi

errors=0

report() { # fichier ligne niveau règle message
  printf '%s:%s: %s [%s] %s\n' "$1" "$2" "$3" "$4" "$5"
  [ "$3" = "ERROR" ] && errors=$((errors + 1))
}

# grep_rule fichier niveau règle message regex [regex-à-exclure]
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
  [ -f "$f" ] || { echo "$f: introuvable" >&2; continue; }

  # 1. Ordre des blocs : template → script setup → style → i18n
  order=$(grep -nE '^<(template|script|style|i18n)([ >]|$)' "$f" | sed -E 's/^([0-9]+):<([a-z0-9]+).*/\2/' | tr '\n' ' ' | sed 's/ $//')
  case "$order" in
    "template script"|"template script style"|"template script i18n"|"template script style i18n") ;;
    *) report "$f" 1 ERROR block-order "ordre des blocs « $order » ; attendu : template script style i18n" ;;
  esac

  # 2. <script setup lang="ts">
  if ! grep -qE '^<script setup lang="ts">' "$f"; then
    first_script=$(grep -nE '^<script' "$f" | head -1 | cut -d: -f1)
    report "$f" "${first_script:-1}" ERROR script-setup 'attendu <script setup lang="ts">'
  fi

  # 3. <style scoped lang="scss">
  grep_rule "$f" ERROR style-scoped 'attendu <style scoped lang="scss">' '^<style' '^<style scoped lang="scss">'

  # 4. defineEmits : syntaxe tableau nommé, pas overload d'appel, pas tableau de chaînes
  grep_rule "$f" ERROR emits-syntax "defineEmits en syntaxe d'overload : écrire  nom: [payload: Type]" "\(e: *['\"]"
  grep_rule "$f" ERROR emits-untyped "defineEmits non typé : écrire defineEmits<{ nom: [payload: Type] }>()" "defineEmits\(\["

  # 5. Template refs : useTemplateRef<InstanceType<typeof X>>('nom')
  grep_rule "$f" ERROR template-ref "ref de gabarit en ref(null) : utiliser useTemplateRef<InstanceType<typeof X>>('nom')" "\bref(<[^>]*>)?\(null\)"
  grep_rule "$f" WARN template-ref "ref<InstanceType<…>> : une ref de gabarit s'écrit avec useTemplateRef" "\bref<InstanceType"
  grep_rule "$f" WARN template-ref-untyped "useTemplateRef sans paramètre de type" "useTemplateRef\(['\"]"

  # 6. any
  grep_rule "$f" ERROR no-any "type any : utiliser le DTO/service généré, un type naive-ui ou une interface locale" "(:[[:space:]]*any\b|<any>|<any\[\]>|\bas any\b|\bany\[\]|Array<any>|Record<[^>]*,[[:space:]]*any>|\(any\))" "^[[:space:]]*(//|\*)"

  # 7. Vestiges Vue 2 / bootstrap-vue
  grep_rule "$f" ERROR vue2 "vestige Vue 2 / bootstrap-vue" '(<b-[a-z]|vue-property-decorator|@Component|@Prop\(|export default \{|this\.\$)'
  grep_rule "$f" WARN vue2-emit "\$emit dans le script : passer par la fonction issue de defineEmits" '\$emit\(' '(^[[:space:]]*@|="|#)'

  # 8. Composants Bootstrap (les utilitaires d-flex, m*-, p*-, text-* sont tolérés)
  grep_rule "$f" ERROR bootstrap "composant Bootstrap : remplacer par l'équivalent Naive UI (voir references/bootstrap-to-naive.md)" "class=\"([^\"]* )?(row|col(-[a-z0-9]+)*|btn(-[a-z-]+)?|card(-[a-z]+)?|form-group|form-control|form-select|form-check|input-group|dropdown(-[a-z]+)?|table|list-group(-item)?|nav|nav-tabs|nav-item|nav-link|alert(-[a-z]+)?|badge|modal(-[a-z]+)?|collapse|container(-fluid)?)( [^\"]*)?\""
  grep_rule "$f" ERROR bootstrap "attribut Bootstrap JS (data-bs-*) : utiliser le composant Naive UI" "data-bs-"

  # 9. Régions //#region Public / Private et place de defineExpose
  pub=$(grep -nE '//[[:space:]]*#region[[:space:]]+Public' "$f" | head -1 | cut -d: -f1)
  priv=$(grep -nE '//[[:space:]]*#region[[:space:]]+Private' "$f" | head -1 | cut -d: -f1)
  script_len=$(awk '/^<script/{s=1;next} /^<\/script>/{s=0} s' "$f" | wc -l)
  # La région Public n'est exigée que si le composant a une API publique.
  has_public_api=$(grep -cE '\b(defineProps|defineEmits|defineModel)\b' "$f")
  if [ -z "$priv" ] || { [ -z "$pub" ] && [ "$has_public_api" -gt 0 ]; }; then
    if [ "$script_len" -gt 25 ]; then
      report "$f" 1 ERROR regions "régions //#region Public et //#region Private absentes (script de $script_len lignes)"
    fi
  else
    [ -z "$pub" ] || [ "$pub" -lt "$priv" ] || report "$f" "$priv" ERROR regions "la région Private précède la région Public"
    while IFS=: read -r ln _; do
      [ "$ln" -lt "$priv" ] || report "$f" "$ln" ERROR regions "defineProps/defineEmits/defineModel hors de la région Public"
    done < <(grep -nE '\b(defineProps|defineEmits|defineModel)\b' "$f")
    expose=$(grep -nE '^[[:space:]]*defineExpose\(' "$f" | head -1 | cut -d: -f1)
    last_end=$(grep -nE '//[[:space:]]*#endregion' "$f" | tail -1 | cut -d: -f1)
    if [ -n "$expose" ] && [ -n "$last_end" ] && [ "$expose" -lt "$last_end" ]; then
      report "$f" "$expose" ERROR regions "defineExpose() doit venir après la fermeture de la région Private"
    fi
    # Ordre des sous-régions de Private
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
        if (r > 0 && r < last) printf "%s:%d: WARN [region-order] sous-région « %s » après une sous-région de rang supérieur (ordre : Plugins & services → Template refs → Data & Computed → Hooks/watchers → Event handlers → Methods)\n", F, NR, title
        if (r > 0) last = r
      }' "$f"
  fi

  # 10. Divers
  grep_rule "$f" WARN console "console.* laissé dans le composant" "console\.(log|debug)\("
  grep_rule "$f" WARN function-type "type Function trop large : typer la signature" ":[[:space:]]*Function\b"
done

if [ "$errors" -gt 0 ]; then
  echo "— $errors erreur(s)." >&2
  exit 1
fi
exit 0

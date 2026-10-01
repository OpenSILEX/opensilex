/**
 * Which characters of a suggestion differ from what the file wrote — so "Chardonay" shown against
 * "Chardonnay" can put the missing n in bold.
 *
 * Seeing the difference is what makes a confirmation quick and safe: the user checks one letter
 * instead of rereading two words. The comparison ignores what the matching itself ignores — case,
 * accents, the kind of separator — so only differences that count are highlighted.
 */
export interface DiffSegment {
  text: string
  changed: boolean
}

/** Case, accents and separators set aside, as the server's normalisation does. */
function comparable(c: string): string {
  const plain = c.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()
  return /[\s_\-.]/.test(plain) ? ' ' : plain
}

/**
 * @return the characters of `suggestion`, grouped into runs that the file's value shares
 *         (`changed: false`) and runs it does not (`changed: true`)
 */
export function diffAgainst(fileValue: string, suggestion: string): DiffSegment[] {
  const a = Array.from(fileValue ?? '')
  const b = Array.from(suggestion ?? '')
  // Longest common subsequence, on names a few dozen characters long: a small table is enough.
  const table: number[][] = Array.from({ length: a.length + 1 }, () => new Array(b.length + 1).fill(0))
  for (let i = a.length - 1; i >= 0; i--) {
    for (let j = b.length - 1; j >= 0; j--) {
      table[i][j] = comparable(a[i]) === comparable(b[j])
        ? table[i + 1][j + 1] + 1
        : Math.max(table[i + 1][j], table[i][j + 1])
    }
  }

  const segments: DiffSegment[] = []
  const push = (char: string, changed: boolean) => {
    const last = segments[segments.length - 1]
    if (last && last.changed === changed) {
      last.text += char
    } else {
      segments.push({ text: char, changed })
    }
  }
  let i = 0
  let j = 0
  while (j < b.length) {
    if (i < a.length && comparable(a[i]) === comparable(b[j])) {
      push(b[j], false)
      i++
      j++
    } else if (i < a.length && table[i + 1][j] >= table[i][j + 1]) {
      i++ // a character only the file has: nothing to show on the suggestion's side
    } else {
      push(b[j], true)
      j++
    }
  }
  return segments
}

# Migrating a Vue 2 component to Vue 3

A migration is not a bug-for-bug copy: the goal is a component that follows the conventions of `SKILL.md` and does what
the old one really did, nothing more. Each step below says why it matters.

## Steps

1. **Read the original, from a branch that still holds the Vue 2 front.** Run
   `git show <vue2-branch>:<path>`. Check first that the branch is still on Vue 2
   (`git show <vue2-branch>:package.json` declares `vue` 2), because the branch that was Vue 2 yesterday may have been
   migrated since. Then list what the component **actually uses**: filters, columns, actions, events, props. This list
   is what the review compares against at the end.
2. **Drop dead code instead of porting it** (section below). Porting it copies bugs and hides what the component does.
3. **Rewrite** with the conventions of `SKILL.md` and the Vue 3 building blocks. For the markup, use
   `bootstrap-to-naive.md`; for a list page, `list-page.md`.
4. **Verify** as described in `SKILL.md` ("Verify"). During a migration, the **whole file** must comply with the
   checker, not only the lines written: a half-converted file is harder to review than either version.
5. **Say what was dropped on purpose**, so that the user can object.

## Dead code

Remove what the old component carried along:

- unused props, refs, imports, computed values or methods;
- filter fields without a matching UI control;
- props passed to a child that does not declare them, often inherited from the old `opensilex-ModalForm`
  (`initForm`, `successMessage`, `modalSize`);
- commented-out code and `console.log`;
- dead Vue 2 syntax (`.sync`, `this.$refs`, `$listeners`).

## Review of a migration

On top of the manual grid of `review-checklist.md`, compare with the original (step 1): each filter, column, action and
event of the old component is either **ported** or **deliberately removed** as dead code, and the review says which.
Also check that:

- no unused prop, event, ref, import, computed value or method remains;
- no field of the filter object lacks a UI control that changes it, unless it comes from the route or a prop (and is then
  commented);
- no commented-out code was copied from the old component.

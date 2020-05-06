
# How to rebase this own branch from master branch

Why rebase ?

- keep a clean history;
- correct merge errors;
- facilitate collaborative work;
- facilitate mergers on branches which require a very long development.

master : branch named ``master`` from ``remote`` (Ex : ``origin``)
own_branch : branch named ``master`` from ``origin`` repository

```bash
git checkout {own_branch}
git rebase {remote}/master
```

Example :

```bash
git checkout dev-fix
git rebase origin/master
```

# How to reset this own master branch from origin master branch

git update-ref refs/heads/master origin/master
git reset --hard master
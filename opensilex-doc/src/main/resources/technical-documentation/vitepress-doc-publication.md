# Vitepress documentation online publication

## checking for errors

If you just have edited a markdown file, it is a good idea to check if it will pass build step or not. To check for error go to `functional-specifications` or `technical-documentation` folder wit `cd` command, then run `npm run docs:build`.

or run it from your IDE :
- **functional specifications** :
```shell
cd ../functional-specifications
npm install
npm run docs:build
```
- **technical documentation** :
```shell
npm install
npm run docs:build
```

## modifyng home page

The entry point (home page) of each documentation is the `index.md` file in `functional-specifications` or `technical-documentation` folder.

You can edit it to add links, modify description or else.

## modifyng config

The config of each vitepress site is the `.vitepress/config.ts` file in `functional-specifications` or `technical-documentation` folder.
# Vitepress documentation online publication

## checking for errors after editing a markdown file

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

You can edit it to add links, modify the description or else. Be aware that in case of publishing documentation on another domain, hardcoded links in the home pages will need to be modified.

## modifying config

The config of each vitepress site is the `.vitepress/config.ts` file in `functional-specifications` or `technical-documentation` folder.

Here you can change parameters about sidebar, search system, generated titles and layout...

## publishing pipeline

The **BUILD - GITLAB PAGES DOCS** section of the .gitlab-ci.yml file contains the configuration for building and publishing the documentation.

### index.html

an index.html file is generated in the `public` folder of the gitlab pages repository to redirect to the functional specifications page.

### theme/index.ts

functional specifications and technical documentation have the same basic vitepress theme. The `.vitepress/theme/index.ts`
of both documentation only contains a configuration that modifies navigation's url when this url targets a file in the other documentation.
This is done to avoid broken links when the documentation is published on gitlab pages.
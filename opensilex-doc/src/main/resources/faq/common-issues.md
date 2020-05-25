# Building errors:
## Can't find node module
If you get an error while building the front part, it could be due to finding library issues. For example, you get this error "Error: Cannot find module 'ajv'" whereas you can find it in the "node-modules" folder.
  
To resolve this issue, try this :
- In *opensilex-dev/.node/node*, run this command: 

```
node yarn/dist/bin/yarn.js cache clean 
```

- In opensilex-dev-tools, run the file ResetNodeModules.java  
This will delete every node module and yarn.lock files from all the application modules
  
- Build the whole project
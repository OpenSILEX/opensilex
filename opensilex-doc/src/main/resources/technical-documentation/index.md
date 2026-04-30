---
layout: doc

sidebar: true
---

# OpenSILEX technical documentation

**databases, architecture and more**

*To navigate among concepts use the left sidebar or hit CTRL+K and search among indexed titles*

*Following links could be interesting if you want to develop for or try OpenSILEX*

<div class="actions-container">
  <a href="https://opensilex.org/sandbox/app/" class="action-link action-primary">OpenSILEX sandbox</a>
  <a href="https://www.opensilex.org/" class="action-link action-primary">Our site</a>
  <a href="/index.html" target="_self" class="action-link action-secondary">functional specifications</a>
  <a href="https://opensilex.pages-forge.inrae.fr/opensilex-dev/maven-report/" class="action-link action-secondary">javadoc</a>
  <a href="https://opensilex.pages-forge.inrae.fr/opensilex-dev/jacoco-aggregate/maven-report/index.html" class="action-link action-secondary">code coverage</a>
  <a href="https://opensilex.pages-forge.inrae.fr/opensilex-dev/maven-report/dependencies.html" class="action-link action-secondary">dependencies</a>
</div>

---

## Documentation helper

- If you are working on a specific feature, the `Opensilex core` section may contain the relevant information.
- If you are looking for global information about the project, the `Architecture` section may be more relevant.
- If you are working on frontend development, the `Opensilex front` section may contain the relevant information.
- If you are working on backend development and databases, the `Opensilex nosql`, `Opensilex sparql` and `Databases` sections may contain the relevant information.

## Version

This documentation corresponds to Opensilex developement version. It could differ from the version you are using.

<style scoped>
.actions-container {
  margin: 2rem 0;
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.action-link {
  padding: 0.75rem 1.5rem;
  border-radius: 6px;
  text-decoration: none;
  font-weight: 500;
  display: inline-block;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.action-link:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.action-primary {
  background: #646cff;
  color: white;
}

.action-secondary {
  background: transparent;
  border: 1px solid #646cff;
  color: #646cff;
}
</style>
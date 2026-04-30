---
layout: doc

sidebar: true
---

# OpenSILEX specification

**rules and definition of core concepts**

*To navigate among concepts use the left sidebar or hit CTRL+K and search among indexed titles*

*Following links could be interesting if you want to develop for or try OpenSILEX*

<div class="actions-container">
  <a href="https://opensilex.org/sandbox/app/" class="action-link action-primary">OpenSILEX sandbox</a>
  <a href="https://www.opensilex.org/" class="action-link action-primary">Our site</a>
  <a href="/opensilex-dev/technical-documentation/" target="_self" class="action-link action-secondary">technical documentation</a>
  <a href="https://opensilex.pages-forge.inrae.fr/opensilex-dev/maven-report/" class="action-link action-secondary">javadoc</a>
  <a href="https://opensilex.pages-forge.inrae.fr/opensilex-dev/maven-report/jacoco-aggregate/index.html" class="action-link action-secondary">code coverage</a>
  <a href="https://opensilex.pages-forge.inrae.fr/opensilex-dev/maven-report/dependencies.html" class="action-link action-secondary">dependencies</a>
</div>

## Features

<div class="features-grid">
  <div class="feature-card">
    <div class="feature-icon">🏢</div>
    <h3 class="feature-title">Scientific organization</h3>
    <ul class="feature-list">
      <li>Organizations</li>
      <li>Projects</li>
      <li>Experiments</li>
      <li>Facilities</li>
      <li>Devices</li>
      <li>Sites</li>
      <li>Persons</li>
    </ul>
  </div>
  
  <div class="feature-card">
    <div class="feature-icon">🧬</div>
    <h3 class="feature-title">Scientific information</h3>
    <ul class="feature-list">
      <li>Scientific Objects</li>
      <li>Variables</li>
      <li>Germplasm</li>
    </ul>
  </div>
  
  <div class="feature-card">
    <div class="feature-icon">📊</div>
    <h3 class="feature-title">Data & provenance</h3>
    <ul class="feature-list">
      <li>Datasets</li>
      <li>Data Files</li>
      <li>Provenance</li>
      <li>Events</li>
      <li>Documents</li>
    </ul>
  </div>
  
  <div class="feature-card">
    <div class="feature-icon">👥</div>
    <h3 class="feature-title">Administration</h3>
    <ul class="feature-list">
      <li>Accounts</li>
      <li>Groups</li>
      <li>Profiles</li>
    </ul>
  </div>
</div>

---

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

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
  margin: 2rem 0;
}

.feature-card {
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  padding: 1.5rem;
  background: var(--vp-c-bg-soft);
}

.feature-icon {
  font-size: 2rem;
  margin-bottom: 0.5rem;
}

.feature-title {
  margin: 0.5rem 0;
  color: var(--vp-c-brand);
}

.feature-list {
  margin: 1rem 0;
  padding-left: 1.5rem;
  color: var(--vp-c-text-2);
}
</style>
package org.opensilex.core.variable.dal;

import org.opensilex.core.ontology.Oeso;
import org.opensilex.sparql.annotations.SPARQLProperty;
import org.opensilex.sparql.annotations.SPARQLResource;

import java.net.URI;

@SPARQLResource(
        ontology = Oeso.class,
        resource = "Logo",
        graph = VariableModel.GRAPH
)

public class LogoModel {

        @SPARQLProperty(
                ontology = Oeso.class,
                property = "hasOnLocal"
        )
        private Boolean onLocal;

        @SPARQLProperty(
                ontology = Oeso.class,
                property = "hasOnShared"
        )
        private Boolean onShared;

        public LogoModel() {

        }

        public Boolean getOnLocal() {
            return onLocal;
        }

        public void setOnLocal(Boolean onLocal) {
            this.onLocal = onLocal;
        }

        public Boolean getOnShared() {
            return onShared;
        }

        public void setOnShared(Boolean onShared) {
        this.onShared = onShared;
    }
}

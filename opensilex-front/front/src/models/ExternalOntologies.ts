export class ExternalOntologies {

    static AGROPORTAL: string = "http://agroportal.lirmm.fr/";
    static AGROVOC: string = "http://agrovoc.uniroma2.it/agrovoc/agrovoc/en/";
    static BIOPORTAL: string = "https://ncbo.bioontology.org/";
    static CROP_ONTOLOGY: string = "https://www.cropontology.org/";
    static PLANT_ONTOLOGY: string = "https://www.ebi.ac.uk/ols/ontologies/po";
    static PLANTEOME: string = "http://planteome.org/";
    static UNIT_OF_MEASUREMENT: string = "http://www.ontobee.org/ontology/UO";
    static UNIT_OF_MEASURE: string = "http://www.ontology-of-units-of-measure.org/page/om-2";
    static QUDT: string = "http://www.qudt.org/release2/qudt-catalog.html#vocabs";
    static XSD: string = "http://books.xmlschemata.org/relaxng/relax-CHP-19.html"

    static externalOntologies: Map<string, any> = new Map([

        [ExternalOntologies.AGROPORTAL, {
            link: "http://agroportal.lirmm.fr/",
            name: "AGROPORTAL",
            description: "component.ontology.externalOntologies.agroportal"
        }],
        [ExternalOntologies.AGROVOC, {
            link: "http://agrovoc.uniroma2.it/agrovoc/agrovoc/en/",
            name: "AGROVOC",
            description: "component.ontology.externalOntologies.agrovoc"
        }],
        [ExternalOntologies.BIOPORTAL, {
            link: "http://bioportal.bioontology.org/",
            name: "BioPortal",
            description: "component.ontology.externalOntologies.bioportal"
        }],

        [ExternalOntologies.CROP_ONTOLOGY, {
            link: "https://www.cropontology.org/",
            name: "Crop Ontology",
            description: "component.ontology.externalOntologies.crop"
        }],

        [ExternalOntologies.PLANT_ONTOLOGY, {
            link: "https://www.ebi.ac.uk/ols/ontologies/po",
            name: "Plant Ontology",
            description: "component.ontology.externalOntologies.plant"
        }],

        [ExternalOntologies.PLANTEOME, {
            link: "http://planteome.org/",
            name: "Planteome",
            description: "component.ontology.externalOntologies.planteome"
        }],

        [ExternalOntologies.UNIT_OF_MEASUREMENT, {
            link: "http://www.ontobee.org/ontology/UO",
            name: "Units of measurement ontology (UO)",
            description: "component.ontology.externalOntologies.uo"
        }],

        [ExternalOntologies.UNIT_OF_MEASURE, {
            link: "http://www.ontology-of-units-of-measure.org/page/om-2",
            name: "Units of Measure (OM)",
            description: "component.ontology.externalOntologies.om"
        }],

        [ExternalOntologies.QUDT, {
            link: "https://www.qudt.org/2.1/catalog/qudt-catalog.html#vocabs",
            name: "QUDT Ontologies (QUDT)",
            description: "component.ontology.externalOntologies.qudt"
        }],

        [ExternalOntologies.XSD, {
            link: "http://books.xmlschemata.org/relaxng/relax-CHP-19.html",
            name: "XML/XSD Datatype Schemas",
            description: "component.ontology.externalOntologies.xsd"
        }]
    ]);


    static getExternalOntologiesReferences(ontologies: string[]): any[] {

        if (ontologies == undefined || ontologies.length == 0) {
            return Array.from(this.externalOntologies.values());
        }

        let refs = [];
        ontologies.forEach(refKey => {
            let ref = this.externalOntologies.get(refKey);
            if (ref != undefined) {
                refs.push(ref);
            }
        });
        return refs;
    }
}

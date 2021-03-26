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
            description: `AgroPortal project is based on five driving agronomic use cases which participate in the design 
            and orientation of the platform. AgroPortal already offers a robust and stable reference repository highly valuable for the agronomic domain.`
        }],
        [ExternalOntologies.AGROVOC, {
            link: "http://agrovoc.uniroma2.it/agrovoc/agrovoc/en/",
            name: "AGROVOC",
            description: `AGROVOC is a controlled vocabulary covering all areas of interest of the Food and
        Agriculture Organization (FAO) of the United Nations, including food, nutrition, agriculture, forestry,
        fisheries, scientific and common names of animals and plants, environment, biological notions, techniques
        of plant cultivation and more.. It is published by FAO and edited by a community of experts.`
        }],
        [ExternalOntologies.BIOPORTAL, {
            link: "http://bioportal.bioontology.org/",
            name: "BioPortal",
            description: `The goal of the National Center for Biomedical Ontology is to
        support biomedical researchers in their knowledge-intensive work, by providing
        online tools and a Web portal enabling them to access, review, and integrate
        disparate ontological resources in all aspects of biomedical investigation
        and clinical practice. A major focus of our work involves the use of
        biomedical ontologies to aid in the management and analysis of data derived
        from complex experiments.`
        }],

        [ExternalOntologies.CROP_ONTOLOGY, {
            link: "https://www.cropontology.org/",
            name: "Crop Ontology",
            description: `The Crop Ontology (CO) current objective is to compile validated concepts along with
      their inter-relationships on anatomy, structure and phenotype of Crops, on trait measurement and
      methods as well as on Germplasm with the multi-crop passport terms. The concepts of the CO are being
      used to curate agronomic databases and describe the data.`
        }],

        [ExternalOntologies.PLANT_ONTOLOGY, {
            link: "https://www.ebi.ac.uk/ols/ontologies/po",
            name: "Plant Ontology",
            description: `The Plant Ontology is a structured vocabulary and database
      resource that links plant anatomy, morphology and growth and development to plant
       genomics data.`
        }],

        [ExternalOntologies.PLANTEOME, {
            link: "http://planteome.org/",
            name: "Planteome",
            description: `Research engine project brings an integrated approach of adopting
      common annotation standards and a set of reference ontologies for Plants.`
        }],

        [ExternalOntologies.UNIT_OF_MEASUREMENT, {
            link: "http://www.ontobee.org/ontology/UO",
            name: "Units of measurement ontology (UO)",
            description: `Metrical units for use in conjunction with the Phenotype And Trait
      Ontology (PATO)`
        }],

        [ExternalOntologies.UNIT_OF_MEASURE, {
            link: "http://www.ontology-of-units-of-measure.org/page/om-2",
            name: "Units of Measure (OM)",
            description: `The Ontology of units of Measure (OM) 2.0 models concepts and
      relations important to scientific research. It has a strong focus on units,
      quantities, measurements, and dimensions.`
        }],

        [ExternalOntologies.QUDT, {
            link: "http://www.qudt.org/release2/qudt-catalog.html#vocabs",
            name: "QUDT Ontologies (QUDT)",
            description: `Quantities, Units, Dimensions and Data Types models are based on dimensional analysis expressed in the
      OWL Web Ontology Language (OWL). The dimensional approach relates each unit
      to a system of base units using numeric factors and a vector of exponents
      defined over a set of fundamental dimensions.`
        }],

        [ExternalOntologies.XSD, {
            link: "http://books.xmlschemata.org/relaxng/relax-CHP-19.html",
            name: "XML/XSD Datatype Schemas",
            description: `Discover XML schema languages.`
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

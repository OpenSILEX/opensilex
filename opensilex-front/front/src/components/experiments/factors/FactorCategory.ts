export class FactorCategory {

    static otherLabel: string = "component.factor.select.other";
    static otherId: string = "other";

    static factorCategories: any = {
        "fieldManagement": "component.factor.select.fieldManagement",
        "lightManagement": "component.factor.select.lightManagement",
        "waterManagement": "component.factor.select.waterManagement",
        "chemical": "component.factor.select.chemical",
        "bioticStress": "component.factor.select.bioticStress",
        "abioticStress": "component.factor.select.abioticStress",
        "temperature": "component.factor.select.temperature",
        "soil": "component.factor.select.soil",
        "nutrient": "component.factor.select.nutrient",
        "atmospheric": "component.factor.select.atmospheric",
    }
    static getFactorCategories(): any {
        return this.factorCategories;
    }

    static getFactorCategoryLabel(categoryId: string): string {
        if (categoryId == null) {
            return this.otherLabel;
        }
        return this.factorCategories[categoryId];
    }
}



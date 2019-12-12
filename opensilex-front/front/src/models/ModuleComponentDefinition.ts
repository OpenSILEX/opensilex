export class ModuleComponentDefinition {

  private module: string;

  private id: string;

  public constructor(module, name) {
    this.module = module;
    this.id = this.module + "." + name;
  }

  public getId(): string {
    return this.id;
  }

  public getModule(): string {
    return this.module;
  }

  public static fromString(component): ModuleComponentDefinition {
    let componentParts = component.split(".");
    if (componentParts.length >= 2) {
      return new ModuleComponentDefinition(componentParts[0], componentParts[componentParts.length - 1]);

    } else {
      throw new Error("Invalid component definition: " + component);
    }
  }
}

export class ModuleComponentDefinition {

  private module: string;

  private name: string;

  public constructor(module, name) {
    this.module = module;
    this.name = name;
  }

  public getId(): string {
    return this.module + "#" + this.name;
  }

  public getModule(): string {
    return this.module;
  }

  public getName(): string {
    return this.name;
  }

  public static fromString(component): ModuleComponentDefinition {
    let componentParts = component.split("#");
    if (componentParts.length == 2) {
      return new ModuleComponentDefinition(componentParts[0], componentParts[1]);

    } else {
      throw new Error("Invalid component definition: " + component);
    }
  }
}

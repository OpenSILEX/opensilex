/*
 * *****************************************************************************
 *                         TableDataLine.ts
 * OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
 * Copyright © INRAE 2025.
 * Last Modification: 21/07/2025 09:49
 * Contact: yvan.roux@inrae.fr, anne.tireau@inrae.fr, pascal.neveu@inrae.fr,
 * *****************************************************************************
 */

export default class GermplasmTableDataRow {
  private data: any = {};

  /**
   * Flag to indicate if the line is for an update operation, i.e if the uri of the germplasm already exists.
   */
  private hasError: boolean = false;
  private isValidated: boolean = false;

  public constructor(data: any = {}) {
    this.data = data;
  }

  public getData(): any {
    return this.data;
  }

  public setCheckingStatus(checkingStatus: string): void {
    this.data.checkingStatus = checkingStatus;
  }

  public setInsertionStatus(insertionStatus: string): void {
    this.data.insertionStatus = insertionStatus;
  }

  public setIsUpdate(isUpdate: boolean): void {
    if ( !this.hasError && !this.isValidated ) {
      this.data.status = isUpdate ? 'UPDATE' : '';
    }
  }

  public setHasError(): void {
    this.hasError = true
    this.isValidated = false;
    this.data.status = 'NOK';
  }

  public setisValidated(): void {
    this.isValidated = true;
    this.hasError = false;
    this.data.status = 'OK';
  }

}
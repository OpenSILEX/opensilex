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
  private errors: Array<string> = null;
  private isValidated: boolean = false;

  public constructor(index: number, data: any = getEmptyData() ) {
    this.data = data;
    this.data.status = this.data.status || '';
    this.data.index = index;
  }

  public getData(): any {
    return this.data;
  }

  public hasError(): boolean {
    return this.errors && this.errors.length > 0;
  }

  public getErrors(): Array<string> {
    return this.errors;
  }

  public setCheckingStatus(checkingStatus: string): void {
    this.data.checkingStatus = checkingStatus;
  }

  public setInsertionStatus(insertionStatus: string): void {
    this.data.insertionStatus = insertionStatus;
  }

  public setIsUpdate(isUpdate: boolean): void {
    if ( !this.hasError() && !this.isValidated ) {
      this.data.status = isUpdate ? 'UPDATE' : '';
    }
  }

  public setErrors(errors: Array<string>): void {
    this.errors = errors
    this.isValidated = false;
    this.data.status = 'NOK';
  }

  /**
   * call this method when the row has been validated by the API, after being checked or upserted.
   */
  public setIsValidated(): void {
    this.isValidated = true;
    this.errors = null;
    this.data.status = 'OK';
  }

}

function getEmptyData(): any {
  return {
    uri: '',
    name: '',
    accessionNumber: '',
    taxon: '',
    status: '',
    checkingStatus: '',
    insertionStatus: '',
    index: 0
  };
}
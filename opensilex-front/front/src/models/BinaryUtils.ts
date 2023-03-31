
function stringToUint8Array(str: string) : Uint8Array {
    return new TextEncoder().encode(str);
}

/**
 * return true if the index th bit of the byte array is equal to 1, false else
 * @param byteArray
 * @param index
 * @see https://stackoverflow.com/questions/72944998/how-to-read-a-single-bit-from-a-buffer-in-typescript
 * @see https://medium.com/@parkerjmed/practical-bit-manipulation-in-javascript-bfd9ef6d6c30
 */
function getUint8ArrayBit(byteArray: Uint8Array, index: number): boolean {

    // first get the correspondit uint8/number from the byteArray
    // since an uint8 store 8bits, we need to compute (index / 8) to determine the index inside the array
    // index >> 3 is equivalent to index / 8, but use bitwise operators
    let byteIdxInArray: number = index >> 3
    let byte: number = byteArray[byteIdxInArray];

    // Now find the desired bit inside the byte
    // To perform this, we compute the modulo of index and 8
    // index & 7 is equivalent to index / 8, but use bitwise operators
    // Here
    let bitIdxInByte = (index & 7)-1

    // Then test if the specified bit inside the byte is 1 or 0
    return ((byte >> bitIdxInByte) & 1) == 1
}

const BinaryUtils = {
    getUint8ArrayBit,
    stringToUint8Array
}

export default BinaryUtils
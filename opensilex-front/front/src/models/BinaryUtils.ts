/**
 *
 * @return a {@link Uint8Array} containing each byte (uint8) of the binary decoded string.
 *
 * This method is expecting a non-null/or empty string encoded with Base64
 * @param base64EncodedStr a Base64 encoded String
 *
 * @see https://mozilla.org/en-US/docs/Glossary/Base64
 * @see  https://developer.mozilla.org/en-US/docs/Web/API/atob
 * @see  https://stackoverflow.com/questions/21797299/convert-base64-string-to-arraybuffer
 */
function base64StringToUint8Array(base64EncodedStr: string) : Uint8Array {

    if(! base64EncodedStr || base64EncodedStr.length == 0){
        return new Uint8Array([]);
    }

    // Transform Base64 str to binary str
    // Then convert it to an Uint8Array
    return Uint8Array.from(atob(base64EncodedStr), c => c.charCodeAt(0));
}

/**
 * return true if the index th bit of the byte array is equal to 1, false else
 * @param byteArray an array of uint8
 * @param index
 * @see https://stackoverflow.com/questions/72944998/how-to-read-a-single-bit-from-a-buffer-in-typescript
 * @see https://medium.com/@parkerjmed/practical-bit-manipulation-in-javascript-bfd9ef6d6c30
 */
function getUint8ArrayBit(byteArray: Uint8Array, index: number): boolean {

    if(byteArray.length == 0){
        return false;
    }

    // first get the correspondit uint8/number from the byteArray
    // since an uint8 store 8bits, we need to compute (index / 8) to determine the index inside the array
    // index >> 3 is equivalent to index / 8, but use bitwise operators
    let byteIdxInArray: number = index >> 3
    let byte: number = byteArray[byteIdxInArray];

    // Now find the desired bit inside the byte
    // To perform this, we compute the modulo of index and 8
    // index & 7 is equivalent to index / 8, but use bitwise operators
    let bitIdxInByte = (index & 7)

    console.log(
        "Byte Idx: ",byteIdxInArray,
        " byte: ", byte,
        " bitIdx: ", bitIdxInByte,
        " value: ", ((byte >> bitIdxInByte) & 1) == 1
    );

    // Then test if the specified bit inside the byte is 1 or 0
    return ((byte >> bitIdxInByte) & 1) == 1
}



const BinaryUtils = {
    getArrayBit: getUint8ArrayBit,
    base64ToArray: base64StringToUint8Array
}

export default BinaryUtils
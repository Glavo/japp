/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.glavo.japp.boot.decompressor.zstd;

import org.glavo.japp.util.UnsafeUtil;

import static org.glavo.japp.boot.decompressor.zstd.BitInputStream.peekBits;
import static org.glavo.japp.boot.decompressor.zstd.Constants.*;
import static org.glavo.japp.boot.decompressor.zstd.Util.verify;
import static org.glavo.japp.util.UnsafeUtil.ARRAY_BYTE_BASE_OFFSET;

final class FiniteStateEntropy {
    public static final int MAX_SYMBOL = 255;
    public static final int MAX_TABLE_LOG = 12;
    public static final int MIN_TABLE_LOG = 5;

    private FiniteStateEntropy() {
    }

    public static int decompress(FiniteStateEntropy.Table table, final Object inputBase, final long inputAddress, final long inputLimit, byte[] outputBuffer) {
        final Object outputBase = outputBuffer;
        final long outputAddress = ARRAY_BYTE_BASE_OFFSET;
        final long outputLimit = outputAddress + outputBuffer.length;

        long input = inputAddress;
        long output = outputAddress;

        // initialize bit stream
        BitInputStream.Initializer initializer = new BitInputStream.Initializer(inputBase, input, inputLimit);
        initializer.initialize();
        int bitsConsumed = initializer.getBitsConsumed();
        long currentAddress = initializer.getCurrentAddress();
        long bits = initializer.getBits();

        // initialize first FSE stream
        int state1 = (int) peekBits(bitsConsumed, bits, table.log2Size);
        bitsConsumed += table.log2Size;

        BitInputStream.Loader loader = new BitInputStream.Loader(inputBase, input, currentAddress, bits, bitsConsumed);
        loader.load();
        bits = loader.getBits();
        bitsConsumed = loader.getBitsConsumed();
        currentAddress = loader.getCurrentAddress();

        // initialize second FSE stream
        int state2 = (int) peekBits(bitsConsumed, bits, table.log2Size);
        bitsConsumed += table.log2Size;

        loader = new BitInputStream.Loader(inputBase, input, currentAddress, bits, bitsConsumed);
        loader.load();
        bits = loader.getBits();
        bitsConsumed = loader.getBitsConsumed();
        currentAddress = loader.getCurrentAddress();

        byte[] symbols = table.symbol;
        byte[] numbersOfBits = table.numberOfBits;
        int[] newStates = table.newState;

        // decode 4 symbols per loop
        while (output <= outputLimit - 4) {
            int numberOfBits;

            UnsafeUtil.putByte(outputBase, output, symbols[state1]);
            numberOfBits = numbersOfBits[state1];
            state1 = (int) (newStates[state1] + peekBits(bitsConsumed, bits, numberOfBits));
            bitsConsumed += numberOfBits;

            UnsafeUtil.putByte(outputBase, output + 1, symbols[state2]);
            numberOfBits = numbersOfBits[state2];
            state2 = (int) (newStates[state2] + peekBits(bitsConsumed, bits, numberOfBits));
            bitsConsumed += numberOfBits;

            UnsafeUtil.putByte(outputBase, output + 2, symbols[state1]);
            numberOfBits = numbersOfBits[state1];
            state1 = (int) (newStates[state1] + peekBits(bitsConsumed, bits, numberOfBits));
            bitsConsumed += numberOfBits;

            UnsafeUtil.putByte(outputBase, output + 3, symbols[state2]);
            numberOfBits = numbersOfBits[state2];
            state2 = (int) (newStates[state2] + peekBits(bitsConsumed, bits, numberOfBits));
            bitsConsumed += numberOfBits;

            output += SIZE_OF_INT;

            loader = new BitInputStream.Loader(inputBase, input, currentAddress, bits, bitsConsumed);
            boolean done = loader.load();
            bitsConsumed = loader.getBitsConsumed();
            bits = loader.getBits();
            currentAddress = loader.getCurrentAddress();
            if (done) {
                break;
            }
        }

        while (true) {
            verify(output <= outputLimit - 2, input, "Output buffer is too small");
            UnsafeUtil.putByte(outputBase, output++, symbols[state1]);
            int numberOfBits = numbersOfBits[state1];
            state1 = (int) (newStates[state1] + peekBits(bitsConsumed, bits, numberOfBits));
            bitsConsumed += numberOfBits;

            loader = new BitInputStream.Loader(inputBase, input, currentAddress, bits, bitsConsumed);
            loader.load();
            bitsConsumed = loader.getBitsConsumed();
            bits = loader.getBits();
            currentAddress = loader.getCurrentAddress();

            if (loader.isOverflow()) {
                UnsafeUtil.putByte(outputBase, output++, symbols[state2]);
                break;
            }

            verify(output <= outputLimit - 2, input, "Output buffer is too small");
            UnsafeUtil.putByte(outputBase, output++, symbols[state2]);
            int numberOfBits1 = numbersOfBits[state2];
            state2 = (int) (newStates[state2] + peekBits(bitsConsumed, bits, numberOfBits1));
            bitsConsumed += numberOfBits1;

            loader = new BitInputStream.Loader(inputBase, input, currentAddress, bits, bitsConsumed);
            loader.load();
            bitsConsumed = loader.getBitsConsumed();
            bits = loader.getBits();
            currentAddress = loader.getCurrentAddress();

            if (loader.isOverflow()) {
                UnsafeUtil.putByte(outputBase, output++, symbols[state1]);
                break;
            }
        }

        return (int) (output - outputAddress);
    }

    public static final class Table {
        int log2Size;
        final int[] newState;
        final byte[] symbol;
        final byte[] numberOfBits;

        public Table(int log2Capacity) {
            int capacity = 1 << log2Capacity;
            newState = new int[capacity];
            symbol = new byte[capacity];
            numberOfBits = new byte[capacity];
        }

        public Table(int log2Size, int[] newState, byte[] symbol, byte[] numberOfBits) {
            int size = 1 << log2Size;
            if (newState.length != size || symbol.length != size || numberOfBits.length != size) {
                throw new IllegalArgumentException("Expected arrays to match provided size");
            }

            this.log2Size = log2Size;
            this.newState = newState;
            this.symbol = symbol;
            this.numberOfBits = numberOfBits;
        }
    }
}

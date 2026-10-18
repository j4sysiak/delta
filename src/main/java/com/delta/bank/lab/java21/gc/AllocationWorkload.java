package com.delta.bank.lab.java21.gc;

import java.util.ArrayList;
import java.util.List;

public class AllocationWorkload {

    private static final int ROUNDS = 30;
    private static final int ARRAYS_PER_ROUND = 20_000;
    private static final int ARRAY_SIZE_BYTES = 1_024;

    public static void main(String[] args) {
        long checksum = 0;
        long arraysAllocated = 0;

        for (int round = 0; round < ROUNDS; round++) {
            List<byte[]> batch = new ArrayList<>(ARRAYS_PER_ROUND);

            for (int index = 0; index < ARRAYS_PER_ROUND; index++) {
                var bytes = new byte[ARRAY_SIZE_BYTES];
                bytes[0] = (byte) (round + index);
                batch.add(bytes);
            }

            for (var bytes : batch) {
                checksum += Byte.toUnsignedInt(bytes[0]);
            }

            arraysAllocated += batch.size();
        }

        System.out.println("Rounds: " + ROUNDS);
        System.out.println("Arrays allocated: " + arraysAllocated);
        System.out.println(STR."Approximate array payload: \{arraysAllocated * ARRAY_SIZE_BYTES / (1024 * 1024)} MiB");
        System.out.println(STR."Checksum: \{checksum}");
    }
}
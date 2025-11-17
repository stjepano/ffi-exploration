package dev.stjepano.ffi;

import java.util.ArrayList;
import java.util.List;

public class Main {

    static void main() {
        IO.println("Hello World!");
        LibTest.init();
        LibTest.printHello();
        LibTest.printText("This is text from java!!!!");

        long t0 = System.nanoTime();

        List<String> allString = new ArrayList<>(1_000_000);
        for (int i = 0; i < 1_000_000; i++) {
            try (LibTest.NativeText nativeText = LibTest.getText()) {
                allString.add(nativeText.getString());
            }
        }

        long t1 = System.nanoTime();
        long nanos = t1 - t0;
        double seconds = ((double) nanos) / 1_000_000_000.0;
        System.out.printf("It took %f seconds!\n", seconds);
        System.out.println("There are " + allString.size() + " strings");

        allString.clear();
        t0 = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) {
            allString.add(LibTest.getTextNonAlloc());
        }
        t1 = System.nanoTime();
        nanos = t1 - t0;
        seconds = ((double) nanos) / 1_000_000_000.0;
        System.out.printf("It took %f seconds!\n", seconds);
        System.out.println("There are " + allString.size() + " strings");

        allString.clear();
        t0 = System.nanoTime();
        LibTest.getTextNonAllocMillion(allString);
        t1 = System.nanoTime();
        nanos = t1 - t0;
        seconds = ((double) nanos) / 1_000_000_000.0;
        System.out.printf("It took %f seconds!\n", seconds);
        System.out.println("There are " + allString.size() + " strings");
    }
}

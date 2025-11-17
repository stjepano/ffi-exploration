package dev.stjepano.ffi;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;

public class LibTest {

    private static Linker linker;
    private static SymbolLookup symbolLookup;

    private static MethodHandle hPrintHello;
    private static MethodHandle hPrintText;
    private static MethodHandle hGetText;
    private static MethodHandle hGetTextNonAlloc;
    private static MethodHandle hFreeText;


    private static MethodHandle findFunction(String functionName, FunctionDescriptor functionDescriptor) {
        return linker.downcallHandle(
                symbolLookup.find(functionName).orElseThrow(() -> new NoSuchElementException("Function " + functionName + " not found in library")),
                functionDescriptor);
    }

    public static void init() {
        linker = Linker.nativeLinker();
        symbolLookup = SymbolLookup.libraryLookup(Path.of("csrc/build/libtest.so"), Arena.global());
        hPrintHello = findFunction("PrintHello", FunctionDescriptor.ofVoid());
        hPrintText = findFunction("PrintText", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
        hGetText = findFunction("GetText", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
        hGetTextNonAlloc = findFunction("GetTextNonAlloc", FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS));
        hFreeText = findFunction("FreeText", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
    }

    public static void printHello() {
        try {
            hPrintHello.invokeExact();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void printText(String text) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ctext = arena.allocateFrom(text);
            hPrintText.invokeExact(ctext);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static NativeText getText() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment outLengthPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment cstrPtr = (MemorySegment) hGetText.invokeExact(outLengthPtr);
            int outLength = outLengthPtr.get(ValueLayout.JAVA_INT, 0);
            return new NativeText(cstrPtr.reinterpret(outLength + 1));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTextNonAlloc() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bufferPtr = arena.allocate(512);
            MemorySegment outLengthPtr = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment retBufferPtr = (MemorySegment) hGetTextNonAlloc.invokeExact(bufferPtr, 512L, outLengthPtr);
            return retBufferPtr.reinterpret(512).getString(0);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void getTextNonAllocMillion(List<String> outList) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment bufferPtr = arena.allocate(512);
            MemorySegment outLengthPtr = arena.allocate(ValueLayout.JAVA_INT);
            for (int i = 0; i < 1_000_000; i++ ) {
                MemorySegment retBufferPtr = (MemorySegment) hGetTextNonAlloc.invokeExact(bufferPtr, 512L, outLengthPtr);
                outList.add(retBufferPtr.reinterpret(512).getString(0));
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static void freeText(MemorySegment segment) {
        try {
            hFreeText.invokeExact(segment);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static class NativeText implements AutoCloseable {
        private final MemorySegment memorySegment;

        public NativeText(MemorySegment memorySegment) {
            this.memorySegment = memorySegment;
        }

        public String getString() {
            return this.memorySegment.getString(0);
        }

        @Override
        public void close() {
            freeText(this.memorySegment);
        }
    }

}

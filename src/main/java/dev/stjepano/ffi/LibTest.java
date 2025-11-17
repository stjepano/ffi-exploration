package dev.stjepano.ffi;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

public class LibTest {

    private static Linker linker;
    private static SymbolLookup symbolLookup;

    private static MethodHandle hPrintHello;
    private static MethodHandle hPrintText;
    private static MethodHandle hGetText;
    private static MethodHandle hGetTextNonAlloc;
    private static MethodHandle hFreeText;
    private static MethodHandle hCallbackFn;


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
        hCallbackFn = findFunction("CallbackFn", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
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

    public static int callbackFn(Function<String, Integer> handler) {
        // This is the most complicated piece of code here, not very efficient
        // Should avoid frequent callbacks from C into Java.

        // 1. Describe the callback layout
        FunctionDescriptor nativeHandlerDesc = FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS);
        MethodHandle nativeMethodHandle;
        try {

            // 2. Bind a callback method of an anonymous object and get the handle to the method
            nativeMethodHandle = MethodHandles.lookup().bind(new Object() {
                @SuppressWarnings("unused")
                public int callback(MemorySegment strPtr) {
                    return handler.apply(strPtr.reinterpret(Integer.MAX_VALUE).getString(0, StandardCharsets.US_ASCII));
                }
            }, "callback", nativeHandlerDesc.toMethodType());
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        try (Arena arena = Arena.ofConfined()) {
            // 3. Stub an upcal using nativeMethodHandle, we need to pass in the arena so Java can do proper allocations
            MemorySegment upcallFnPtr = linker.upcallStub(nativeMethodHandle, nativeHandlerDesc, arena);
            // 4. Pass in the resulting memory segment (from 3.) into the native method
            return (int) hCallbackFn.invokeExact(upcallFnPtr);
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

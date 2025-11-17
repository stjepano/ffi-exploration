# FFI exploration

Just exploring new Java FFI for calling C native functions.

## Running

You need to have cmake (minimum 3.20) on your system.

```shell
./gradlew run
```

Note this will build both Java and native library.


## Performance tests

I made some "back of the envelope" performance tests.

* Typically Java FFI function call will be cca 2x slower than C to .so call
* If you are not careful and use arenas, copying etc... in a loop, you will easily end up 100x times slower than C to .so call
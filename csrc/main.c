#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

#include "library.h"


int main(int ArgC, char** ArgV) {
    int OutLength;

    char** List = (char**) malloc(sizeof(char*) * 1000000);
    memset(List, 0, sizeof(char*) * 1000000);
    int ListIndex = 0;
    assert(List);

    struct timespec T0, T1;

    clock_gettime(CLOCK_MONOTONIC, &T0);
    for (int i = 0; i < 1000000; i++) {
        char* Buffer = GetText(&OutLength);
        if (Buffer) {
            // NOTE: this is incorrect program but fine for perf counting
            List[ListIndex++] = Buffer;
            FreeText(Buffer);
        }
    }
    clock_gettime(CLOCK_MONOTONIC, &T1);

    int64_t Seconds = T1.tv_sec - T0.tv_sec;
    int64_t Nanos = T1.tv_nsec - T0.tv_nsec;
    double Elapsed = ((double) Seconds) + ((double) Nanos) / 1000000000.0;
    int Count = 0;
    for (int i = 0; i < 1000000; i++) {
        if (List[i]) {
            Count++;
        }
    }
    printf("It took %f seconds\n", Elapsed);
    printf("There are %d string in list\n", Count);

    // Test 2: GetTextNonAlloc with single buffer (like Java Test 3)
    memset(List, 0, sizeof(char*) * 1000000);
    ListIndex = 0;

    char Buffer[512];  // Single buffer, reused

    clock_gettime(CLOCK_MONOTONIC, &T0);
    for (int i = 0; i < 1000000; i++) {
        GetTextNonAlloc(Buffer, sizeof(Buffer), &OutLength);
        // To match Java behavior (storing strings), we need to copy
        List[ListIndex++] = strdup(Buffer);
    }
    clock_gettime(CLOCK_MONOTONIC, &T1);

    Seconds = T1.tv_sec - T0.tv_sec;
    Nanos = T1.tv_nsec - T0.tv_nsec;
    Elapsed = ((double) Seconds) + ((double) Nanos) / 1000000000.0;
    Count = 0;
    for (int i = 0; i < 1000000; i++) {
        if (List[i]) {
            Count++;
            free(List[i]);  // Clean up strdup'd strings
        }
    }
    printf("Test 2 (GetTextNonAlloc single buffer): %f seconds\n", Elapsed);
    printf("There are %d strings in list\n", Count);

    free(List);

    return 0;
}
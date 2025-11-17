#include "library.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

void PrintHello() {
    printf("Hello World from native library\n");
    fflush(stdout);
}

void PrintText(const char* Text) {
    if (!Text) {
        printf("There is no text\n");
    } else {
        printf("The text is '%s'\n", Text);
    }
    fflush(stdout);
}

char* GetText(int* OutLength) {
    char* buffer = malloc(512);
    if (buffer) {
        snprintf(buffer, 512, "This is natively allocated string, should use FreeText to free!");
        *OutLength = (int) strlen(buffer);
    }
    return buffer;
}

char* GetTextNonAlloc(char* buffer, size_t buffersz, int* OutLength) {
    if (buffer) {
        snprintf(buffer, buffersz, "this is natively printed string, don't use FreeText ...");
        *OutLength = (int) strlen(buffer);
    }
    return buffer;
}

void FreeText(char* Text) {
    free(Text);
}

int CallbackFn(int(*callback)(const char *Str)) {
    return callback("You have been called from C native function. How do you feel, you punk?");
}

void DoSomething(const something *Something) {
    int res = printf(Something->Fmt, Something->Value1, Something->Value2);
    fflush(stdout);
    if (Something->Callback) {
        Something->Callback(res);
    }
}


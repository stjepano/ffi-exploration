#include "library.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

void PrintHello() {
    printf("Hello World from native library\n");
}

void PrintText(const char* Text) {
    if (!Text) {
        printf("There is no text\n");
    } else {
        printf("The text is '%s'\n", Text);
    }
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


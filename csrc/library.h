#pragma once

#include <stdlib.h>

void PrintHello();

void PrintText(const char* Text);

char* GetText(int* OutLength);

char* GetTextNonAlloc(char* buffer, size_t buffersz, int* OutLength);

void FreeText(char* Text);

int CallbackFn(int (*callback)(const char* Str));

typedef struct {
    int Value1;
    int Value2;
    const char* Fmt;
    void (*Callback)(int);
} something;

void DoSomething(const something* Something);

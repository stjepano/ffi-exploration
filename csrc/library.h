#pragma once

#include <stdlib.h>

void PrintHello();

void PrintText(const char* Text);

char* GetText(int* OutLength);

char* GetTextNonAlloc(char* buffer, size_t buffersz, int* OutLength);

void FreeText(char* Text);

int CallbackFn(int (*callback)(const char* Str));

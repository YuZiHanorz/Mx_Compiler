#include <stdio.h>
#include <stdlib.h>
#include <string.h>

void lib_print(char* str) {
    printf("%s", str + 8);
}

void lib_println(char* str) {
    printf("%s\n", str + 8);
}

char* lib_getString() {
    char buffer[1024 * 64];
    scanf("%s", buffer);
    long length = strlen(buffer);
    char* ptr = malloc(8 + length);
    *((long *)ptr) = length;
    strcpy(ptr + 8, buffer);
    return ptr;
}

long lib_getInt() {
    long num;
    scanf("%ld", &num);
    return num;
}

char* lib_toString(long num) {
    char* ptr = malloc(8 + 24);
    *((long*)ptr) = sprintf(ptr + 8, "%ld", num);
    return ptr;
}

long lib_stringLength(char* ptr) {
    return *((long*)ptr);
}

char* lib_stringSubstring(char* ptr, long left, long right) {
    long length = right - left + 1;
    char* ret = malloc(8 + length + 1);
    *((long*)ret) = length;
    for (int i = 0; i < length; ++i)
        ret[8 + i] = ptr[8 + left + i];
    ret[8 + length] = '\0';
    return ret;
}

long lib_stringParseInt(char* ptr) {
    long num = 0;
    int neg = 0;
    ptr += 8;
    if (*ptr == '-') {
        neg = 1;
        ptr++;
    }
    while ('0' <= *ptr && *ptr <= '9') {
        num = num * 10 + (*ptr - '0');
        ptr++;
    }
    return neg ? -num : num;
}

long lib_stringOrd(char* ptr, long pos) {
    return ptr[8 + pos];
}

char* lib_stringConcat(char* ptr1, char* ptr2) {
    long len1 = *((long*)ptr1);
    long len2 = *((long*)ptr2);
    char* ptr = malloc(8 + len1 + len2 + 1);
    *((long*)ptr) = len1 + len2;
    for (int i = 0; i < len1; ++i)
        ptr[8 + i] = ptr1[8 + i];
    for (int i = 0; i < len2; ++i)
        ptr[8 + len1 + i] = ptr2[8 + i];
    ptr[8 + len1 + len2] = '\0';
    return ptr;
}

long lib_stringCmp(char* ptr1, char* ptr2) {
    return strcmp(ptr1 + 8, ptr2 + 8);
}
int lib_mod10000(int a){
    return a % 10000;
}

extern int lib_init();

int main() {
    return lib_init();
}

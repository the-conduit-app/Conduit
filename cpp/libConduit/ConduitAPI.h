#pragma once

#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*ConduitTokenCallback)(const char* text, void* userData);

void* conduit_create(size_t maxGenTokens);
void conduit_destroy(void* conduit);

void* conduit_create_session(void* conduit, const char* modelPath, const char *modelSha);
void conduit_destroy_session(void* conduit, void* session);

int conduit_generate(void* session, const char* prompt, ConduitTokenCallback callback, void* userData);
void conduit_abort_generation(void* session);

#ifdef __cplusplus
}
#endif

#pragma once

#ifdef __cplusplus
extern "C" {
#endif

void macwindow_hide_title(void* window);
void macwindow_style_titlebar(void* window);

/* Pinch zoom relay */
typedef void (*MagnificationCallback)(double magnification);
void macwindow_set_magnification_callback(void* window, MagnificationCallback callback);

#ifdef __cplusplus
}
#endif


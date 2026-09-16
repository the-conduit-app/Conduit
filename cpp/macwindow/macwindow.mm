#import <Cocoa/Cocoa.h>
#import "macwindow.h"

void macwindow_hide_title(void* window) {
    NSWindow* nsWindow = (__bridge NSWindow*)window;

    dispatch_async(dispatch_get_main_queue(), ^{
        [nsWindow setTitleVisibility:NSWindowTitleHidden];
    });
}

void macwindow_style_titlebar(void* window) {
    NSWindow* nsWindow = (__bridge NSWindow*)window;

    dispatch_async(dispatch_get_main_queue(), ^{
        [nsWindow setTitleVisibility:NSWindowTitleHidden];
        [nsWindow setTitlebarAppearsTransparent:YES];
        [nsWindow setBackgroundColor:[NSColor clearColor]];
        [nsWindow setStyleMask:
            [nsWindow styleMask] | NSWindowStyleMaskFullSizeContentView
        ];
    });
}

static id magnificationMonitor = nil;

void macwindow_set_magnification_callback(void* window, MagnificationCallback callback) {
    NSWindow* nsWindow = (__bridge NSWindow*)window;

    if (magnificationMonitor != nil) {
        [NSEvent removeMonitor:magnificationMonitor];
        magnificationMonitor = nil;
    }

    if (callback == NULL) {
        return;
    }

    magnificationMonitor = [NSEvent addLocalMonitorForEventsMatchingMask:
        NSEventMaskMagnify
        handler:^NSEvent* (NSEvent* event) {

            if (event.window == nsWindow) {
                callback((double)[event magnification]);
            }

            return event;
        }
    ];
}

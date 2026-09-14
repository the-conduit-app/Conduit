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

// void macwindow_style_titlebar(void* window) {
//     NSWindow* nsWindow = (__bridge NSWindow*)window;

//     dispatch_async(dispatch_get_main_queue(), ^{
//         [nsWindow setTitleVisibility:NSWindowTitleHidden];
//         [nsWindow setTitlebarAppearsTransparent:YES];
//     });
// }

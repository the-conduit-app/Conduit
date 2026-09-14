// Usage:
// Class Conduit: Create it with a string trigger. Then you simply send sequences into it, (stored
// as indiv strings") and emitted in the same sequence they arrived after a period of testing.
//
// The caller can  pop strings from the conduit (same order), but only after conduit makes sure
// the trigger string will not next be output.
//
// Mainly: to safeguard against models that respond with trailing ChatML markers.
//  E.g. Gemma4 returns "...response... <|im_end|>" before eog
//
// Note: Changed to u32 string to accommodate UTF8 sequence chunks.
#pragma once

#include "Portal.h"

#include <string>
#include <string_view>
#include <optional>

class Portal {
private:
    std::u32string _window;
    std::u32string _trigger;
    bool _isPortalClosed;

public:
    explicit Portal(const std::u32string &trigger): _trigger(trigger), _isPortalClosed(false) {};

    bool isClosed();
    void closeInput();
    bool push(std::u32string_view seq);

    std::optional<char32_t> pop();
    std::u32string popAvailable();
};


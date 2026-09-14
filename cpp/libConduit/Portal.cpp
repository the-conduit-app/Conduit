/* Portal class to watch for trigger strings in the 
 * LLM response before emitting them
 */
#include "Portal.h"

#include <string>
#include <string_view>
#include <optional>

//// DEBUG INCLUDES
#include <unistd.h>
#include "UtfCodec.h"

void Portal::closeInput() {
    _isPortalClosed = true;
}

bool Portal::isClosed() {
    return _isPortalClosed;
}

bool Portal::push(std::u32string_view seq) {
    if (_isPortalClosed) return false;
    _window.append(seq);
    return true;
}

// One char at a time
std::optional<char32_t> Portal::pop() {
    if (_window.starts_with(_trigger)) {

        _isPortalClosed = true;
        return std::nullopt;
    }

    if (!_isPortalClosed && _window.length() < _trigger.length())
        return std::nullopt;

    if (!_window.empty()) {
        char32_t c = _window.front();
        _window.erase(0, 1);
        return c;
    }

    return std::nullopt;
}

// All available chars
std::u32string Portal::popAvailable() {
    std::u32string result;

    while (auto cp = pop()) {
        result.push_back(*cp);
    }

    return result;
}

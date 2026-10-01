// ChatGpt Jul 26, 2025, 11:20a
// UTF8-UTF32 codec to interact with utf8 llama from utf32 env
//
#include "UtfCodec.h"

#include <cstdio>

namespace {
    bool isContinuation(unsigned char b) {
        return (b & 0xC0) == 0x80;
    }
    void logUtf8Error(const char* message) {
        fprintf(stderr, "[UTF8 ERROR] %s\n", message);
    }
} // anonymous namespace

std::string UtfCodec::toUtf8(std::u32string_view utf32) {
    std::string result;

    for (char32_t cp : utf32)
        result += toUtf8(cp);

    return result;
}

// translate a single character codepoint
std::string UtfCodec::toUtf8(char32_t cp) {
    std::string result;

    // Unicode scalar values exclude the UTF-16 surrogate range.
    if (cp <= 0x7F) {
        result.push_back(static_cast<char>(cp));
    }
    else if (cp <= 0x7FF) {
        result.push_back(static_cast<char>(0xC0 | (cp >> 6)));
        result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    }
    else if (cp >= 0xD800 && cp <= 0xDFFF) {
        logUtf8Error("Invalid Unicode surrogate code point.");
        result = "\xEF\xBF\xBD"; // U+FFFD
    }
    else if (cp <= 0xFFFF) {
        result.push_back(static_cast<char>(0xE0 | (cp >> 12)));
        result.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
        result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    }
    else if (cp <= 0x10FFFF) {
        result.push_back(static_cast<char>(0xF0 | (cp >> 18)));
        result.push_back(static_cast<char>(0x80 | ((cp >> 12) & 0x3F)));
        result.push_back(static_cast<char>(0x80 | ((cp >> 6) & 0x3F)));
        result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
    }
    else {
        logUtf8Error("Invalid Unicode code point.");
        result = "\xEF\xBF\xBD"; // U+FFFD
    }

    return result;
}

std::u32string UtfCodec::fromUtf8(std::string_view utf8) {
    std::u32string result;

    size_t i = 0;

    while (i < utf8.size()) {
        unsigned char b0 = static_cast<unsigned char>(utf8[i]);

        if ((b0 & 0x80) == 0) {
            // 1-byte sequence
            result.push_back(b0);
            ++i;
        }
        else if ((b0 & 0xE0) == 0xC0) {
            // 2-byte sequence
            if (i + 1 >= utf8.size()) {
                logUtf8Error("Incomplete 2-byte UTF-8 sequence.");
                break;
            }

            unsigned char b1 =
                static_cast<unsigned char>(utf8[i + 1]);

            if (!isContinuation(b1)) {
                logUtf8Error("Invalid UTF-8 continuation byte.");
                break;
            }

            char32_t cp = ((b0 & 0x1F) << 6) | (b1 & 0x3F);
            result.push_back(cp);
            i += 2;
        }
        else if ((b0 & 0xF0) == 0xE0) {
            // 3-byte sequence
            if (i + 2 >= utf8.size()) {
                logUtf8Error("Incomplete 3-byte UTF-8 sequence.");
                break;
            }

            unsigned char b1 = static_cast<unsigned char>(utf8[i + 1]);
            unsigned char b2 = static_cast<unsigned char>(utf8[i + 2]);

            if (!isContinuation(b1) || !isContinuation(b2)) {
                logUtf8Error("Invalid UTF-8 continuation byte.");
                break;
            }

            char32_t cp = ((b0 & 0x0F) << 12) | ((b1 & 0x3F) << 6) | (b2 & 0x3F);
            result.push_back(cp);
            i += 3;
        }
        else if ((b0 & 0xF8) == 0xF0) {
            // 4-byte sequence
            if (i + 3 >= utf8.size()) {
                logUtf8Error("Incomplete 4-byte UTF-8 sequence.");
                break;
            }

            unsigned char b1 = static_cast<unsigned char>(utf8[i + 1]);
            unsigned char b2 = static_cast<unsigned char>(utf8[i + 2]);
            unsigned char b3 = static_cast<unsigned char>(utf8[i + 3]);

            if (!isContinuation(b1) || !isContinuation(b2) || !isContinuation(b3)) {
                logUtf8Error("Invalid UTF-8 continuation byte.");
                break;
            }

            char32_t cp = ((b0 & 0x07) << 18) | ((b1 & 0x3F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F);
            result.push_back(cp);
            i += 4;
        }
        else {
            logUtf8Error("Invalid UTF-8 leading byte.");
            break;
        }
    }

    return result;
}

std::u32string UtfCodec::fromUtf8Streaming(
    std::string& pending,
    std::string_view chunk) {

    pending.append(chunk);

    size_t completeBytes = pending.size();
    size_t i = 0;

    while (i < completeBytes) {
        unsigned char b0 = static_cast<unsigned char>(pending[i]);

        size_t needed;

        if ((b0 & 0x80) == 0)
            needed = 1;
        else if ((b0 & 0xE0) == 0xC0)
            needed = 2;
        else if ((b0 & 0xF0) == 0xE0)
            needed = 3;
        else if ((b0 & 0xF8) == 0xF0)
            needed = 4;
        else {
            logUtf8Error("Invalid UTF-8 leading byte.");
            ++i;
            continue;
        }

        if (i + needed > completeBytes) {
            // This is normal for tokenized LLM output.
            // Keep the incomplete sequence for the next token.
            completeBytes = i;
            break;
        }

        bool valid = true;

        for (size_t j = 1; j < needed; ++j) {
            if (!isContinuation(static_cast<unsigned char>(pending[i + j]))) {
                valid = false;
                break;
            }
        }

        if (!valid) {
            logUtf8Error("Invalid UTF-8 continuation byte.");
            ++i;
            continue;
        }

        i += needed;
    }

    std::u32string result;
    if (completeBytes > 0) {
        result = fromUtf8(std::string_view(pending.data(), completeBytes));
        pending.erase(0, completeBytes);
    }
    return result;
}

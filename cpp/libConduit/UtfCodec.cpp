// ChatGpt Jul 26, 2025, 11:20a
// UTF8-UTF32 codec to interact with utf8 llama from utf32 env
//
#include "UtfCodec.h"

#include <stdexcept>

namespace {
    bool isContinuation(unsigned char b) {
        return (b & 0xC0) == 0x80;
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

    if (cp <= 0x7F) {
        result.push_back(static_cast<char>(cp));
    }
    else if (cp <= 0x7FF) {
        result.push_back(static_cast<char>(0xC0 | (cp >> 6)));
        result.push_back(static_cast<char>(0x80 | (cp & 0x3F)));
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
        throw std::runtime_error("Invalid Unicode code point.");
    }

    return result;
}

std::u32string UtfCodec::fromUtf8(std::string_view utf8) {
    std::u32string result;

    size_t i = 0; while (i < utf8.size()) {
        unsigned char b0 = static_cast<unsigned char>(utf8[i]);

        if ((b0 & 0x80) == 0) {                    // 1-byte sequence: 0xxxxxxx
            result.push_back(b0);
            ++i;
        }
        else if ((b0 & 0xE0) == 0xC0) {            // 2-byte sequence: 110xxxxx 10xxxxxx
            if (i + 1 >= utf8.size())
                throw std::runtime_error("Incomplete UTF-8 sequence.");

            unsigned char b1 = static_cast<unsigned char>(utf8[i + 1]);

            if (!isContinuation(b1))
                throw std::runtime_error("Invalid UTF-8 continuation byte.");

            char32_t cp =
                ((b0 & 0x1F) << 6) |
                ( b1 & 0x3F);

            result.push_back(cp);
            i += 2;
        }
        else if ((b0 & 0xF0) == 0xE0) {            // 3-byte sequence: 1110xxxx 10xxxxxx 10xxxxxx
            if (i + 2 >= utf8.size())
                throw std::runtime_error("Incomplete UTF-8 sequence.");

            unsigned char b1 = static_cast<unsigned char>(utf8[i + 1]);
            unsigned char b2 = static_cast<unsigned char>(utf8[i + 2]);

            if (!isContinuation(b1) || !isContinuation(b2))
                throw std::runtime_error("Invalid UTF-8 continuation byte.");

            char32_t cp =
                ((b0 & 0x0F) << 12) |
                ((b1 & 0x3F) << 6) |
                ( b2 & 0x3F);

            result.push_back(cp);
            i += 3;
        }
        else if ((b0 & 0xF8) == 0xF0) {            // 4-byte sequence: 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx
            if (i + 3 >= utf8.size())
                throw std::runtime_error("Incomplete UTF-8 sequence.");

            unsigned char b1 = static_cast<unsigned char>(utf8[i + 1]);
            unsigned char b2 = static_cast<unsigned char>(utf8[i + 2]);
            unsigned char b3 = static_cast<unsigned char>(utf8[i + 3]);

            if (!isContinuation(b1) ||
                !isContinuation(b2) ||
                !isContinuation(b3))
                throw std::runtime_error("Invalid UTF-8 continuation byte.");

            char32_t cp =
                ((b0 & 0x07) << 18) |
                ((b1 & 0x3F) << 12) |
                ((b2 & 0x3F) << 6)  |
                ( b3 & 0x3F);

            result.push_back(cp);
            i += 4;
        }
        else {
            throw std::runtime_error("Invalid UTF-8 leading byte.");
        }
    }

    return result;
}


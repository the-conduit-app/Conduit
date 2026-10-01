#pragma once

#include <string>
#include <string_view>

class UtfCodec {
public:
    static std::u32string fromUtf8(std::string_view utf8);
    static std::u32string fromUtf8Streaming(std::string& pending, std::string_view chunk);
        
    static std::string toUtf8(std::u32string_view utf32);
    static std::string toUtf8(char32_t codePoint);
};

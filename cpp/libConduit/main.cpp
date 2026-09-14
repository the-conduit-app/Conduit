#include <iostream>
#include <fstream>
#include <thread>
#include <mutex>
#include "Conduit.h"

std::mutex outputMutex;

struct OutputBuffer {
    std::ofstream file;
    std::mutex mutex;
    explicit OutputBuffer(const std::string& path) : file(path) { }
};

void tokenCallback(const char* text, void* userData)
{
    auto* out = static_cast<OutputBuffer*>(userData);

    std::lock_guard<std::mutex> lock(out->mutex);
    out->file << text;
    out->file.flush();
}


void runSession(Conduit::Session* session, const char* prompt)
{
    int rc = session->generate(prompt, tokenCallback, nullptr);

    std::lock_guard<std::mutex> lock(outputMutex);
    std::cout << "\n\nReturn code: " << rc << "\n";
}

int main()
{
    Conduit conduit(2048);

    const std::string model =
        "/Users/bubba/Library/Application Support/Conduit/llm/gemma-2-9b-it-Q4_K_M.gguf";

    auto* s1 = conduit.createSession(model);
    auto* s2 = conduit.createSession(model);

    if (!s1 || !s2) {
        std::cerr << "Failed to create sessions\n";
        return 1;
    }

    OutputBuffer out1("rome.txt");
    OutputBuffer out2("beijing.txt");

    std::thread t1(
        [&] {
            s1->generate(
                "Tell me about Rome.",
                tokenCallback,
                &out1
            );
        }
    );

    std::thread t2(
        [&] {
            s2->generate(
                "Tell me about Beijing.",
                tokenCallback,
                &out2
            );
        }
    );

    t1.join();
    t2.join();

    conduit.destroySession(s1);
    conduit.destroySession(s2);

    return 0;
}

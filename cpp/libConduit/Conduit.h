#pragma once

#include <string>
#include <vector>
#include <unordered_map>
#include <mutex>

#include "llama.h"

class Conduit {
public:
    using TokenCallback = void (*)(const char* text, void*);
    class Session;

    explicit Conduit(size_t maxGenTokens = 2048);
    ~Conduit();

    // Session management
    Session* createSession(const std::string& modelPath, const std::string& modelSha);
    void destroySession(Session* session);

private:
    size_t _maxGenTokens;
    void _unregisterSession(Session* session);

    struct CachedModel {
        llama_model* model = nullptr;
        int refCount = 0;
    };

    llama_model* _acquireModel(const std::string& modelPath, const std::string& modelSha);
    void _releaseModel(llama_model* model);

    std::unordered_map<std::string, CachedModel> _modelsBySha;
    std::vector<Session*> _sessions;

    std::mutex _mutex;
};

class Conduit::Session {
    friend class Conduit;

public:
    ~Session();

    int generate(const char* prompt, TokenCallback callback, void* userData);
    void abortDecode();

private:
    Conduit& _conduit;
    int _createContext();

    llama_model* _model = nullptr;
    llama_context* _ctx = nullptr;
    llama_sampler* _sampler = nullptr;
    bool _abortCurrentDecode = false;

    Session(Conduit& conduit, const std::string& modelPath, const std::string& modelSha);

    static bool llamaAbortCallback(void *);
};


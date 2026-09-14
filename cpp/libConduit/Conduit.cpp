// Conduit to talk to local on disk llms
//
#include "UtfCodec.h"
#include "Conduit.h"
#include "Portal.h"
#include "ConduitErrorCodes.h"

#include <unistd.h>

#include <algorithm>

using namespace std;

static void llamaLogCallback(ggml_log_level level, const char* text, void*) {
    if (text == nullptr)
        return;
    if (level == GGML_LOG_LEVEL_INFO || level == GGML_LOG_LEVEL_DEBUG || level == GGML_LOG_LEVEL_CONT)
        return;

    write(2, text, strlen(text));
}

Conduit::Conduit(size_t maxGenTokens) : _maxGenTokens(maxGenTokens) {
    static std::once_flag flag;

    std::call_once(flag, [] {
        llama_log_set(llamaLogCallback, nullptr);
        llama_backend_init();
    });
}

Conduit::~Conduit() {
    for (;;) {
        Session* session;
        {
            std::lock_guard<std::mutex> lock(_mutex);
            if (_sessions.empty())
                break;
            session = _sessions.back();
        }
        delete session;
    }
    
#ifndef NDEBUG
    if (!_modelsBySha.empty()) {
        fprintf(stderr, "Conduit::~Conduit(): model cache not empty\n");
    }
#endif

    llama_backend_free();
}

llama_model* Conduit::_acquireModel(
    const std::string& modelPath,
    const std::string& modelSha) {

    std::lock_guard<std::mutex> lock(_mutex);

    // Model already loaded?
    auto it = _modelsBySha.find(modelSha);

    if (it != _modelsBySha.end()) {
        it->second.refCount++;
        return it->second.model;
    }

    // Model is not loaded, so load it from the supplied path.
    llama_model_params params = llama_model_default_params();

    params.use_mmap = false;
    params.use_mlock = false;
    params.n_gpu_layers = 0;

    llama_model* model = llama_model_load_from_file(modelPath.c_str(), params);

    if (model == nullptr)
        return nullptr;

    CachedModel entry;
    entry.model = model;
    entry.refCount = 1;

    _modelsBySha.emplace(modelSha, std::move(entry));

    return model;
}

void Conduit::_releaseModel(llama_model* model) {
    std::lock_guard<std::mutex> lock(_mutex);

    for (auto it = _modelsBySha.begin(); it != _modelsBySha.end(); ++it) {
        if (it->second.model == model) {
            it->second.refCount--;

            if (it->second.refCount == 0) {
                llama_model_free(it->second.model);
                _modelsBySha.erase(it);
            }

            return;
        }
    }
}

Conduit::Session* Conduit::createSession(const std::string& modelPath, const std::string& modelSha) {
    Session* session = new Session(*this, modelPath, modelSha);

    if (session->_model == nullptr) {
        delete session; // never registered
        return nullptr;
    }

    {
        std::lock_guard<std::mutex> lock(_mutex);
        _sessions.push_back(session);
    }
    return session;
}

void Conduit::destroySession(Session* session) {
    if (session != nullptr)
        delete session;
}

void Conduit::_unregisterSession(Session* session) {
    std::lock_guard<std::mutex> lock(_mutex);

    auto it = std::find(_sessions.begin(), _sessions.end(), session);

    if (it != _sessions.end())
        _sessions.erase(it);
}


// ----------------------------------------------------------------------

Conduit::Session::Session(Conduit& conduit, const std::string& modelPath, const std::string& modelSha) : _conduit(conduit) {
    _model = _conduit._acquireModel(modelPath, modelSha);

    _ctx = nullptr;
    _sampler = nullptr;
    _abortCurrentDecode = false;
}

bool Conduit::Session::llamaAbortCallback(void *data) {
    auto *session = static_cast<Conduit::Session *>(data);
    return session->_abortCurrentDecode;
}

Conduit::Session::~Session() {
    _conduit._unregisterSession(this);
    
    if (_sampler != nullptr)
        llama_sampler_free(_sampler);

    if (_ctx != nullptr)
        llama_free(_ctx);

    if (_model != nullptr)
        _conduit._releaseModel(_model);
}

int Conduit::Session::_createContext() {
    if (_model == nullptr)
        return ConduitError::NULL_MODEL;

    if (_sampler != nullptr) {
        llama_sampler_free(_sampler);
        _sampler = nullptr;
    }

    if (_ctx != nullptr) {
        llama_free(_ctx);
        _ctx = nullptr;
    }

    llama_context_params ctxParams = llama_context_default_params();
    ctxParams.n_ctx = 8192;     // matching llama.cpp cli
    ctxParams.abort_callback = this->llamaAbortCallback;
    ctxParams.abort_callback_data = this;

    _ctx = llama_init_from_model(_model, ctxParams);
    if (_ctx == nullptr)
        return ConduitError::NULL_SESSION_CTX;

    llama_sampler_chain_params samplerParams = llama_sampler_chain_default_params();
    _sampler = llama_sampler_chain_init(samplerParams);

    if (_sampler == nullptr) {
        llama_free(_ctx);
        _ctx = nullptr;
        return ConduitError::NULL_SESSION_CTX_IN_SAMPLER;
    }
    llama_sampler_chain_add(_sampler, llama_sampler_init_greedy());

    return ConduitError::OK;
}

void Conduit::Session::abortDecode() {
    _abortCurrentDecode = true;
}

int Conduit::Session::generate(const char* prompt, Conduit::TokenCallback callback, void* userData) {
    int rc = 0;
    _abortCurrentDecode = false;

    if (prompt == nullptr)
        return ConduitError::NULL_PROMPT;
    size_t promptLen = strlen(prompt);
    
    if (_ctx == nullptr) {
        rc = _createContext();
        if (rc != ConduitError::OK)
            return rc;
    }

    // Each generate call is idempotent (clears previous KV cache)
    llama_memory_clear(llama_get_memory(_ctx), true);

    const llama_vocab* vocab = llama_model_get_vocab(_model);

    bool addSpecial = true;
    bool parseSpecial = true;

    int required = llama_tokenize(
        vocab,
        prompt,
        promptLen,
        nullptr,
        0,
        addSpecial,
        parseSpecial);

    if (required < 0)
        required = -required;

    std::vector<llama_token> tokens(required + 1);

    const int nTokens =
        llama_tokenize(
            vocab,
            prompt,
            promptLen,
            tokens.data(),
            tokens.size(),
            addSpecial,
            parseSpecial);

    if (nTokens < 0)
        return ConduitError::TOKENIZE;

    int batchSize = llama_n_batch(_ctx);
    if (batchSize <= 0)
        return ConduitError::LLAMA_BATCH;
    
    for (int i = 0; i < nTokens; i += batchSize) {
        int n = std::min(batchSize, nTokens - i);

        llama_batch batch = llama_batch_get_one(tokens.data() + i, n);

        int ret = llama_decode(_ctx, batch);
        switch(ret) {
        case 0:
            break;
        case 2:
            return ConduitError::ABORTED;
        default:
            return ConduitError::DECODE_INIT;
        }

    }

    // The Portal transmits chunks to "the other side" and shuts down if it sees EOG or the
    // shutdown trigger ("<|im_end|>")
    Portal portal(UtfCodec::fromUtf8("<|im_end|>"));
    size_t generatedTokens = 0;

    for (;;) {
        if (_abortCurrentDecode) {
            write(2, "Returning from decode due to user abort\n", 40);
            _abortCurrentDecode = false;
            return ConduitError::ABORTED;
        }
        
        if (++generatedTokens >= _conduit._maxGenTokens) {
            portal.closeInput();
            while (auto cp = portal.pop()) {
                std::string utf8 = UtfCodec::toUtf8(*cp);
                callback(utf8.c_str(), userData);
            }
            return ConduitError::OUTPUT_MAXED;
        }
        
        llama_token next;
        next = llama_sampler_sample(_sampler, _ctx, -1);

        if (llama_vocab_is_eog(vocab, next)) {
            portal.closeInput();

            auto output = portal.popAvailable();
            if (!output.empty()) {
                std::string utf8 = UtfCodec::toUtf8(output);
                callback(utf8.c_str(), userData);
            }

            // while (auto cp = portal.pop()) {
            //     std::string utf8 = UtfCodec::toUtf8(*cp);
            //     callback(utf8.c_str(), userData);
            // }

            return ConduitError::OK;
        }

        required = llama_token_to_piece(vocab, next, nullptr, 0, 0, true);
        if (required < 0)
            required = -required;

        std::vector<char> piece(required);

        int len = llama_token_to_piece(
            vocab,
            next,
            piece.data(),
            piece.size(),
            0,
            true);

        if (len < 0)
            return ConduitError::TOKEN_TO_PIECE;

        auto utf32 = UtfCodec::fromUtf8(
            std::string_view(piece.data(), len));

        if (!portal.push(utf32))
            return ConduitError::PORTAL;

        auto output = portal.popAvailable();
        if (!output.empty()) {
            std::string utf8 = UtfCodec::toUtf8(output);
            callback(utf8.c_str(), userData);
        }
        
        // while (auto cp = portal.pop()) {
        //     std::string utf8 = UtfCodec::toUtf8(*cp);
        //     callback(utf8.c_str(), userData);
        // }
        
        if (portal.isClosed())
            return ConduitError::OK;

        llama_batch batch = llama_batch_get_one(&next, 1);

        switch(llama_decode(_ctx, batch)) {
        case 0:
            break;
        case 2:
            return ConduitError::ABORTED;
        default:
            return ConduitError::DECODE_LOOP;
        }
    }

    return ConduitError::OK;
}

// ------------------------------ API functions -----------------------------------

#include "ConduitAPI.h"

extern "C" {

    void* conduit_create(size_t maxGenTokens) {
        return new Conduit(maxGenTokens);
    }

    void conduit_destroy(void* conduit) {
        if (!conduit)
            return;

        delete static_cast<Conduit*>(conduit);
    }

    void* conduit_create_session(
        void* conduit,
        const char* modelPath,
        const char* modelSha) {

        if (!conduit || !modelPath || !modelSha)
            return nullptr;

        return static_cast<Conduit*>(conduit)->createSession(
            modelPath,
            modelSha);
    }

    void conduit_destroy_session(void* conduit, void* session) {
        if (!conduit || !session)
            return;

        static_cast<Conduit*>(conduit)->destroySession(
            static_cast<Conduit::Session*>(session));
    }

    int conduit_generate(
        void* session,
        const char* prompt,
        ConduitTokenCallback callback,
        void* userData) {

        if (!session || !prompt || !callback)
            return ConduitError::API_GENERATE;

        int result =
            static_cast<Conduit::Session*>(session)
                ->generate(prompt, callback, userData);

        return result;
    }

    void conduit_abort_generation(void* session) {
        if (!session)
            return;

        static_cast<Conduit::Session*>(session)->abortDecode();
    }

} // extern "C"

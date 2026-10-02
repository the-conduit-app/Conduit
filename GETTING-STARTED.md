# Getting Started with Conduit

**AFTER you have successfully built and installed Conduit, this is how you get started using it:**

## 1. Download a model

Conduit runs local LLMs using GGUF model files. Download the following model and place it in:

```text
$HOME/llm/
```

### Gemma

Download:

`gemma-2-9b-it-Q4_K_M.gguf`

Hugging Face: **[https://huggingface.co/bartowski/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf]**

This is the primary model used by the default Conduit configuration.

### SmolLM2 — optional

If you want to use the **Babe** expert in the Sample pack, also download:

`SmolLM2-135M-Instruct-Q4_K_M.gguf`

Hugging Face: **[https://huggingface.co/unsloth/SmolLM2-135M-Instruct-GGUF/resolve/main/SmolLM2-135M-Instruct-Q4_K_M.gguf]**

### Qwen2.5-Coder — optional

If you want to use the **Matt** expert in the Sample pack, also download:

`Qwen2.5-Coder-14B-Instruct-Q5_K_M.gguf`

Hugging Face: **[https://huggingface.co/apto-as/Qwen2.5-Coder-14B-Instruct-Q5_K_M-GGUF/resolve/main/qwen2.5-coder-14b-instruct-q5_k_m.gguf]**

You only need the optional models if you want to use those particular experts.

## 2. Start Conduit

Launch the installed **Conduit** application.

Conduit will locate the models in:

```text
$HOME/llm/
```

Approved models are identified by their SHA-256 hash, so the model file must be the expected version rather than merely having the expected filename.

## 3. Open a conversation

Conduit includes starter conversations and packs that demonstrate its branching-conversation features.

You can select a conversation and use the **TreeView** to navigate between branches.

## 4. Start chatting

Select an available expert/model and begin generating responses.

Conduit runs the model locally on your Mac; model execution is handled through the native Conduit library and `llama.cpp`.

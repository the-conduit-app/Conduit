Conduit

Conduit is a local-first desktop AI application for exploring conversations with multiple local LLM experts. Conversations are stored locally, and different experts can be used within the same conversation.

Requirements

* macOS
* Apple Silicon Mac recommended
* A compatible .gguf LLM model
* Sufficient memory for the models you choose to run

Conduit currently uses llama.cpp through its native Conduit library.

Installation

Download the latest .dmg release and drag Conduit to your Applications folder.

Use

(Your only clue: The following is the secret key that opens the portal. How?)
 - LLM: Gemma4 (gemma-2-9b-it-Q4_K_M.gguf)
 - Must be legit (have SHA 13b2a7b4115bbd0900162edcebe476da1ba1fc24e718e8b40d32f6e300f56dfe)
 
On first launch, Conduit creates its application data directory:

~/Library/Application Support/Conduit

The directory contains user data such as chats, packs, and approved-model information. Once you're in the app, try talking to Condy for more help.

Models

Place supported .gguf model files in:

~/Library/Application Support/Conduit/llm

Conduit identifies approved models by SHA-256 hash rather than filename alone. Renaming a model does not make an unapproved model valid.

The release includes the model information required for the built-in/system expert. Additional models can be used when they are included in the application’s approved-model configuration.

Conversations

Conduit represents a conversation as a tree rather than a simple linear transcript.

This allows you to:

* Continue from an earlier point.
* Create alternate branches.
* Navigate between branches using the conversation tree.
* Ask different experts about the same conversational context.

The chat view displays the path from the root to the currently selected node, while the tree view provides navigation through the complete conversation.

Packs

Packs provide predefined expert configurations and can be found under:

~/Library/Application Support/Conduit/packs

A pack can define the experts available for a particular task or workflow.

Data and Privacy

Conduit is designed around local data and local model execution.

Conversation files and application data are stored under:

~/Library/Application Support/Conduit

LLM model files are also read locally. Conduit does not require a cloud LLM service to generate responses.

Back up this directory if you want to preserve your conversations and configuration.

Troubleshooting

If Conduit cannot verify the required system model, it will remain at the startup/splash stage rather than attempting to initialize an invalid model.

If a model is not recognized:

1. Confirm that it is a .gguf model.
2. Confirm that it is an approved model.
3. Confirm that the complete model file is present in the llm directory.

For development builds, application diagnostics are available through the console/log output.

Status

Conduit is currently distributed as a macOS desktop application. The project is under active development.

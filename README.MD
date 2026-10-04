## Conduit

https://the-conduit-app.github.io

Conduit is a local-AI desktop application for exploring conversations with multiple LLM experts. Conversations are stored locally, and different experts can be used within the same conversation.

## Requirements for the Mac version:

* macOS
* Apple Silicon Mac recommended
* A compatible .gguf LLM model
* Sufficient memory for the models you choose to run

[Sep 2026 Update: You need to download the repo and build it yourself. To do so, change into the "packaging" directory and run the script ./package.sh. It may involve enabling xcode]

Conduit currently uses llama.cpp through its native Conduit library.

## Installation

[Update Sep 24: I’m afraid you have to download the source and build it yourself since I didn’t code-sign the distributed DMG.] It should be simple — just follow the steps below. After Conduit.dmg has been produced, open it and drag the Conduit app into your Applications folder.

### The gist of it is
```text
bubba@Om /tmp % git clone https://github.com/the-conduit-app/Conduit.git
Cloning into 'Conduit'...
remote: Enumerating objects: 1836, done.
remote: Counting objects: 100% (1836/1836), done.
remote: Compressing objects: 100% (560/560), done.
remote: Total 1836 (delta 988), reused 1779 (delta 932), pack-reused 0 (from 0)
Receiving objects: 100% (1836/1836), 22.67 MiB | 418.00 KiB/s, done.
Resolving deltas: 100% (988/988), done.
...

bubba@Om % cd ./Conduit/packaging 
bubba@Om packaging % ./package.sh 
+ '[' '!' -f package.sh ']'
+ cd ../cpp/libConduit
+ make install
mkdir -p "/Users/bubba/Library/Application Support/Conduit/lib"
cp -f libconduit.dylib "/Users/bubba/Library/Application Support/Conduit/lib"
codesign --force --sign - "/Users/bubba/Library/Application Support/Conduit/lib/libconduit.dylib"
/Users/bubba/Library/Application Support/Conduit/lib/libconduit.dylib: replacing existing signature

+ cd ../macwindow
+ make install
mkdir -p "/Users/bubba/Library/Application Support/Conduit/lib"
cp libmacwindow.dylib "/Users/bubba/Library/Application Support/Conduit/lib/"

+ cd ../..
+ rm -rf build/compose/binaries/main/app/Conduit.app
+ rm -f Conduit.dmg
+ ./gradlew --rerun-tasks copyNativeLibsToApp
... [more output from the command - just paste into ChatGPT/Gemini interface if there are problems]
...
BUILD SUCCESSFUL in 43s
15 actionable tasks: 15 executed
+ cd packaging
+ dmgbuild -s dmg_settings.py Conduit ../Conduit.dmg

bubba@Om packaging % ls -lh ../Conduit.dmg
-rw-r--r--@ 1 bubba  staff    68M Oct  4 12:25 ../Conduit.dmg
bubba@Om packaging % open ../Conduit.dmg 
```

## Usage

(Your only clue: The following is the secret key that opens the portal. How?)
 - LLM: Gemma4 (gemma-2-9b-it-Q4_K_M.gguf) 
 - Must be legit (have SHA 13b2a7b4115bbd0900162edcebe476da1ba1fc24e718e8b40d32f6e300f56dfe)
 - You can get it from [HuggingFace](https://huggingface.co/bartowski/gemma-2-9b-it-GGUF/resolve/main/gemma-2-9b-it-Q4_K_M.gguf)
 
On first launch, Conduit creates its application data directory:

`~/Library/Application Support/Conduit`

The directory contains user data such as chats, packs, and approved-model information. Once you're in the app, try talking to Condy for more help.

## Models

Place supported .gguf model files in:

`~/Library/Application Support/Conduit/llm`

Conduit identifies approved models by SHA-256 hash rather than filename alone. Renaming a model does not make an unapproved model valid.

The release includes the model information required for the built-in/system expert. Additional models can be used when they are included in the application’s approved-model configuration.

## Conversations

Conduit represents a conversation as a tree rather than a simple linear transcript.

This allows you to:

* Continue from an earlier point.
* Create alternate branches.
* Navigate between branches using the conversation tree.
* Ask different experts about the same conversational context.

The chat view displays the path from the root to the currently selected node, while the tree view provides navigation through the complete conversation.

## Packs

Packs provide predefined expert configurations and can be found under:

`~/Library/Application Support/Conduit/packs`

A pack can define the experts available for a particular task or workflow. Find community packs to download at https://github.com/the-conduit-app/the-conduit-app.github.io/discussions

## Data and Privacy

Conduit is designed around local data and local model execution.

Conversation files and application data are stored under:

`~/Library/Application Support/Conduit`

LLM model files are also read locally. Conduit does not require a cloud LLM service to generate responses.

Back up this directory if you want to preserve your conversations and configuration.

## Troubleshooting

If Conduit cannot verify the required system model, it will remain at the startup/splash stage rather than attempting to initialize an invalid model.

If a model is not recognized:

1. Confirm that it is a .gguf model.
2. Confirm that it is an approved model.
3. Confirm that the complete model file is present in the llm directory.

For development builds, application diagnostics are available through the console/log output.

## Status

Conduit is currently distributed as a macOS desktop application. 

## Acknowledgements

Conduit makes use of:

- Liquid by Fletch McKee
  https://github.com/FletchMcKee/liquid

- Kuiver by justdeko
  https://github.com/justdeko/kuiver


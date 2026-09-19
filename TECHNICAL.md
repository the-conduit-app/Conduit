Conduit — Technical Overview

Overview

Conduit is a Kotlin Compose Desktop application for running and interacting with local LLMs.

The application consists of three principal layers:

Compose UI
    │
    ├── ChatView / TreeView
    ├── AppState / ChatManager
    └── Packs / conversations
         │
         ▼
Kotlin application layer
         │
         ▼
Conduit native library
         │
         ▼
llama.cpp
         │
         ▼
GGUF model

The project uses Kotlin, Compose Multiplatform/Desktop, JNA, and a native C/C++ layer.

Application Startup

The basic startup sequence is:

main()
  ├── copyAssetsToFilesDir()
  ├── createConduit()
  ├── create AppState
  └── launch Compose application

Application data is stored under:

~/Library/Application Support/Conduit

Packaged native libraries are located inside the application bundle’s:

Contents/Frameworks

Development execution also supports loading the native libraries from the project/native build locations.

State Management

AppState provides application-wide state, while ChatManager owns conversation-related mutable state.

A particularly important distinction is between persistent message state and transient generation state.

Completed message text belongs to ChatMessage. Streaming response text is maintained separately by ChatManager:

textInProgress[nodeId] → current streamed text

This avoids placing rapidly changing transient Compose state inside persistent conversation nodes and prevents snapshot/state mutations from occurring in inappropriate contexts.

Generation state includes:

* currently generating node
* whether generation is active
* transient response text
* chat/version invalidation counters

Conversation Model

Conversations are trees of Node objects.

A node can contain:

* A message
* Parent/child relationships
* Conversation metadata

cursorNodeId identifies the currently selected point in the conversation.

ChatUtils.getFullHistory() derives the linear path from the root to the current cursor for display in ChatView.

TreeView exposes the complete tree and allows the user to select alternate branches.

ChatView and Branch Transitions

ChatView normally displays the current history directly.

Actual branch changes are handled using BranchTransition:

data class BranchTransition(
    val shared: List<Node>,
    val outgoing: List<Node>,
    val incoming: List<Node>
)

The transition is based on the common prefix between the old and new paths.

A critical distinction is made between:

* Extending the current conversation.
* Switching to a different branch.

Normal message generation must not be treated as a branch transition. Only transitions with an outgoing branch use the animated branch presentation.

This distinction is important because AnimatedContent changes layout height during its animation. Applying it to ordinary response generation can cause the chat column’s measured height to change unexpectedly and interfere with scrolling.

Scrolling

ChatView uses a Compose ScrollState.

The scroll range is derived from the measured content height and viewport height:

maxValue ≈ content height − viewport height

When content is still being animated or remeasured, maxValue can change. Therefore scrolling logic must account for Compose’s asynchronous composition and measurement rather than assuming that a single scroll request represents the final layout.

LLM Integration

LlmPortal provides the Kotlin-facing interface to the native Conduit library.

JNA loads:

libconduit.dylib

The native layer manages the underlying LLM sessions and delegates model execution to llama.cpp.

Models are identified by SHA-256 rather than relying solely on filenames. This prevents an arbitrary or incorrectly substituted .gguf file from being treated as an approved model merely because it has the expected filename.

The native cache is also keyed by model SHA.

Native Resources

Native resources have explicit lifecycle operations for:

* Creating the Conduit instance.
* Initializing model sessions.
* Destroying sessions.
* Destroying the Conduit instance.

Native libraries are packaged into the macOS application bundle under Contents/Frameworks.

Persistent Data

Important application data includes:

Application Support/Conduit/
├── chats/
├── packs/
├── llm/
└── .approved-models.json

Bundled assets provide initial/default content when corresponding user data is absent.

Startup is intentionally defensive: missing directories and optional data should normally be recreated or handled gracefully rather than causing the application to terminate.

Build

The project uses Gradle with a JDK toolchain appropriate to the current Kotlin/Compose build.

The release process creates a macOS application bundle and copies the native libraries into:

Conduit.app/Contents/Frameworks

The packaging/ directory contains the resources/scripts used to create the distributable macOS image.

Development Notes

A few areas deserve particular care when modifying the application:

Compose state

Avoid putting transient streaming state directly into persistent model objects. State updated from background generation should be marshalled onto the appropriate Compose/main context.

Conversation branches

Do not assume every history change is a branch change. Adding a new child to the current path is normal conversation growth and should not invoke branch-transition animation.

Native model validation

Validate model identity before initializing native LLM state. A .gguf filename alone is not sufficient to establish that the file is the expected model.

Startup data

User data under Application Support should be treated as potentially missing, incomplete, or malformed. Startup should fail only when continuing safely is not possible.

Packaging

The application depends on its native libraries being present in the expected location inside the packaged application bundle. Changes to native library names or locations should therefore be reflected in both Gradle packaging and runtime library discovery.

# ----- Configuration -----

LLAMA_DIR := $(HOME)/lib/llama.cpp
LLAMA_BUILD_DIR := $(LLAMA_DIR)/build-static
DEST_DIR := $(HOME)/Library/Application Support/Conduit/lib

CXX := g++

CXXFLAGS := \
	-std=c++20 \
	-fPIC \
	-O2 \
	-Wall \
	-Wextra \
	-I$(LLAMA_DIR)/include \
	-I$(LLAMA_DIR)/ggml/include

LDFLAGS := \
	-dynamiclib \
	$(LLAMA_BUILD_DIR)/src/libllama.a \
	$(LLAMA_BUILD_DIR)/common/libllama-common.a \
	$(LLAMA_BUILD_DIR)/common/libllama-common-base.a \
	$(LLAMA_BUILD_DIR)/ggml/src/libggml.a \
	$(LLAMA_BUILD_DIR)/ggml/src/libggml-base.a \
	$(LLAMA_BUILD_DIR)/ggml/src/libggml-cpu.a \
	$(LLAMA_BUILD_DIR)/ggml/src/ggml-blas/libggml-blas.a \
	$(LLAMA_BUILD_DIR)/ggml/src/ggml-metal/libggml-metal.a \
	-framework Metal \
	-framework Foundation \
	-framework Accelerate \
	-framework MetalKit \
	-Wl,-rpath,@loader_path

SRC := \
	Conduit.cpp \
	Portal.cpp \
	UtfCodec.cpp

TARGET_LIB := libconduit.dylib
TARGET_TEST := conduit_test

# ----- Build -----

all: $(TARGET_LIB)

test: $(TARGET_TEST)

$(TARGET_LIB): $(SRC)
	$(CXX) $(CXXFLAGS) $(SRC) $(LDFLAGS) -o $@
	codesign --force --sign - $@

$(TARGET_TEST): main.cpp $(TARGET_LIB)
	$(CXX) $(CXXFLAGS) \
		main.cpp \
		-L. \
		-lconduit \
		-Wl,-rpath,@loader_path \
		-Wl,-rpath,$(abspath $(LLAMA_BUILD_DIR)/bin) \
		-o $@

clean:
	rm -f $(TARGET_LIB) $(TARGET_TEST)

install: $(TARGET_LIB)
	mkdir -p "$(DEST_DIR)"
	cp -f $(TARGET_LIB) "$(DEST_DIR)"
	codesign --force --sign - "$(DEST_DIR)/$(TARGET_LIB)"

.PHONY: all test clean install

cmake_minimum_required(VERSION 3.22.1)
project(openpiano_audio LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

add_library(openpiano_audio SHARED
    AudioEngine.cpp
    SynthEngine.cpp
    Reverb.cpp
    Limiter.cpp
    jni_bridge.cpp
)

target_include_directories(openpiano_audio PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})
target_link_libraries(openpiano_audio PRIVATE oboe::oboe android log)

# Vendored single-header sources warn heavily; keep the project's own warnings strict.
set_source_files_properties(SynthEngine.cpp PROPERTIES COMPILE_FLAGS "-Wno-everything")
target_compile_options(openpiano_audio PRIVATE -Wall -Wextra -ffast-math)

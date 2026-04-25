cmake_minimum_required(VERSION 3.22.1)
project("smartnotes_whisper")

add_library(whisper_jni SHARED whisper_jni.cpp)

find_library(log-lib log)
target_link_libraries(whisper_jni ${log-lib})

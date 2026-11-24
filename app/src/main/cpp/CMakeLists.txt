cmake_minimum_required(VERSION 3.22.1)
project(ampy_native LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Android 15+ devices can use 16 KB pages. Align every LOAD segment to 16 KB
# so all three libraries load correctly on both 4 KB and 16 KB page devices.
function(ampy_configure_library target)
    target_compile_options(${target} PRIVATE -Wall -Wextra -fexceptions)
    target_link_options(${target} PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
    )
endfunction()

add_library(ampy_logparse SHARED log_parser.cpp)
add_library(ampy_chart SHARED chart_downsample.cpp)
add_library(ampy_stats SHARED power_stats.cpp)

ampy_configure_library(ampy_logparse)
ampy_configure_library(ampy_chart)
ampy_configure_library(ampy_stats)

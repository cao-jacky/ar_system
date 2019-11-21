cmake_minimum_required(VERSION 3.13.4)
include_directories(include)
project( DisplayImage )
find_package( OpenCV REQUIRED )
add_executable( client 
    client.cpp 
    client.hpp)
target_link_libraries( client ${OpenCV_LIBS} )
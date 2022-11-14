### Skija
[skija](https://github.com/JetBrains/skija) is an open source 2D graphics library based on Google's [skia](https://skia.org) 
which provides common APIs that work across a variety of hardware and software platforms.

### This project
This project draw a circle on to a window. This project uses [lwjgl](https://www.lwjgl.org) to create a window. Skija 
uses this window to get a `surface`. Finally, the `canvas` from that surface is passed on to the `App`. The `draw` method 
draws a circle on to the screen.

### Running the project
The `glfw` has to be executed on the main thread. You need to pass `-XstartOnFirstThread` to the jvm instance.
I'm using the apple arm64 architecture and that's why I've added the snapshot repository. You can run this code as it is
if you are using an Apple M1 computer. Otherwise, changing the architecture is easy. Add your OS name and architecture to 
this line `arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) }` to the `build.gradle.kts` file.


rootProject.name = "front"

include("front", ":common")
project(":common").projectDir = java.io.File("../common")
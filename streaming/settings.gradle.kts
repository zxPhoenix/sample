rootProject.name = "streaming"

include("front", ":common")
project(":common").projectDir = java.io.File("../common")
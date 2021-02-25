rootProject.name = "loader"

include("loader", ":common")
project(":common").projectDir = File("../common")
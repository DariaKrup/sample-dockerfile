import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.RunInDockerBuildFeature
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildFeatures.runInDocker
import jetbrains.buildServer.configs.kotlin.buildSteps.DockerCommandStep
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2024.12"

project {

    buildType(PullAndRun)
    buildType(Build)
}

object Build : BuildType({
    name = "Build and push"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dockerCommand {
            id = "DockerCommand_2"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                namesAndTags = "dkrupkinacontainerregistry.azurecr.io/build:new"
                commandArgs = "--pull"
            }
        }
        dockerCommand {
            id = "DockerCommand_3"
            commandType = push {
                namesAndTags = "dkrupkinacontainerregistry.azurecr.io/build:new"
            }
        }
        dockerCommand {
            id = "DockerCommand_1"
            commandType = build {
                source = content {
                    content = """
                        FROM dkrupkinacontainerregistry.azurecr.io/build:new
                        WORKDIR /app
                        COPY . .
                        RUN ls
                    """.trimIndent()
                }
                platform = DockerCommandStep.ImagePlatform.Linux
                namesAndTags = "dkrupkinacontainerregistry.azurecr.io/app:latest"
            }
        }
        dockerCommand {
            id = "DockerCommand"
            commandType = push {
                namesAndTags = "dkrupkinacontainerregistry.azurecr.io/app:latest"
            }
            param("dockerImage.platform", "linux")
            param("dockerfile.path", "Dockerfile")
        }
        script {
            name = "Pull and run"
            id = "Pull_and_run"
            enabled = false
            scriptContent = "ls /"
            dockerImage = "dkrupkinacontainerregistry.azurecr.io/app:latest"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerPull = true
        }
    }

    triggers {
        vcs {
        }
    }

    features {
        perfmon {
        }
        dockerSupport {
            cleanupPushedImages = true
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_132"
            }
        }
        runInDocker {
            dockerImage = "ubuntu:latest"
        }
    }
})

object PullAndRun : BuildType({
    name = "Pull and run"

    steps {
        script {
            id = "simpleRunner"
            enabled = false
            scriptContent = "ls /"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerPull = true
        }
        script {
            id = "simpleRunner_1"
            scriptContent = """
                ls /
                echo ${'$'}deep
            """.trimIndent()
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_132"
            }
        }
        runInDocker {
            dockerImage = "dkrupkinacontainerregistry.azurecr.io/app:latest"
            dockerImagePlatform = RunInDockerBuildFeature.ImagePlatform.Linux
            dockerPull = true
            dockerRunParameters = """-e "deep=purple""""
        }
    }

    dependencies {
        snapshot(Build) {
        }
    }
})

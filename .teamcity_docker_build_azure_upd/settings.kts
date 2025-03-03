import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.buildFeatures.perfmon
import jetbrains.buildServer.configs.kotlin.buildSteps.DockerCommandStep
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.dockerCommand
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.projectFeatures.dockerRegistry
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

    buildType(Pull)
    buildType(Build)

    features {
        dockerRegistry {
            id = "PROJECT_EXT_3"
            name = "Docker Registry (Azure)"
            url = "https://dkrupkinacontainerregistry.azurecr.io"
            userName = "17c5afa7-698f-4afd-b518-0db1fb4f0984"
            password = "credentialsJSON:842d6ce7-1568-41d9-beac-88eb7cac08ef"
        }
        dockerRegistry {
            id = "PROJECT_EXT_4"
            name = "Docker Registry"
            userName = "dariakrup"
            password = "credentialsJSON:b84176df-21c9-49a2-87c1-6b48efdc2f8d"
        }
    }
}

object Build : BuildType({
    name = "Build"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        dockerCommand {
            id = "DockerCommand"
            commandType = build {
                source = file {
                    path = "Dockerfile"
                }
                platform = DockerCommandStep.ImagePlatform.Linux
                namesAndTags = "dkrupkinacontainerregistry.azurecr.io/build:new"
                commandArgs = "--pull"
            }
        }
        dockerCommand {
            id = "DockerCommand_1"
            commandType = push {
                namesAndTags = "dkrupkinacontainerregistry.azurecr.io/build:new"
            }
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
                dockerRegistryId = "PROJECT_EXT_3"
            }
        }
    }
})

object Pull : BuildType({
    name = "Pull"

    steps {
        script {
            id = "simpleRunner"
            scriptContent = "ls ./"
            dockerImage = "dkrupkinacontainerregistry.azurecr.io/build:new"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
        }
    }

    features {
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_3,PROJECT_EXT_4"
            }
        }
    }
})

# budget-kmp

## Run

## Learnings about KMP

This repository is, at this point, my experiments for learning about KMP.

The first things I'll probably do are

1. experiment with a local server reached via applications running on another machine
   1. build a server and run it on a machine on my network
   2. build a simple Android or Web app just to test whether I can access
      server from another device on the same network
2. get me existing CLI application to build as a multi-platform target

Longer term ideas:

1. build a server around the budget data and web/android/desktop apps to use that server
2. see how much I can make native then target linux/windows/mac

### Gradle structures

Compilations

Targets

Source Sets

I'm thinking I'll probably want to move the cli target out of composeApp and into server because I'll want to put
common GUI code into composeApp/common but that code won't be needed in the CLI. Anything in composeApp/common will
automatically be a part of every target under composeApp. I don't want the CLI to include that.

Or, I could
[do this](https://kotlinlang.org/docs/multiplatform-advanced-project-structure.html#declaring-custom-source-sets)
to give myself a source set that is shared only among the GUI targets.  But, since CLI
won't be a compose app, I feel like moving it to server makes more sense.

> Or, I might just make CLI its own top-level module parallel to server and commonApp.
> Looks like this is the only option since it doesn't seem to allow multiple jvm targets in the same multi-platform
> project???

I can use a source set under shared for common JVM code.  See
[here](https://kotlinlang.org/docs/multiplatform-add-dependencies.html#dependency-on-another-multiplatform-project).

But, let's make sure basic things build before we mess around with the structure too much.

| Module     | Target  | Source Set | Dependencies | Description                                                                                |
|------------|---------|------------|--------------|--------------------------------------------------------------------------------------------|
| composeApp | android | main/test  |              | Checking whether a phone can call APIs in server                                           |
| composeApp | common  | main/test  |              | Shared front-end functionality in pure Kotlin. <br/> Binaries are produced for each target |
| composeApp | desktop | main/test  |              | Front-end for JVM budget application                                                       |
| composeApp | wasmJs  | main/test  |              | Checking whether a phone can call APIs in server                                           |
| composeApp | cli     | main/test  |              | Console Budget app                                                                         |
| server     | ktor    | main/test  |              | REST APIs for budget data                                                                  |
| shared     | android | main/test  |              | actuals for android/JVM                                                                    |
| shared     | common  | main/test  |              | Kotlin expects, etc.   <br/> Binaries are produced for each target                         |
| shared     | jvm     | main/test  |              | actuals for JVM                                                                            |
| shared     | wasmJs  | main/test  |              | actuals for WASM/JS                                                                        |

> Multiplatform dependencies are propagated down the dependsOn structure. When you add a dependency to commonMain, it
> will be automatically added to all source sets that declare dependsOn relations directly or indirectly in commonMain.

[This info](https://kotlinlang.org/docs/multiplatform-hierarchy.html#creating-additional-source-sets) or maybe
[this](https://kotlinlang.org/docs/multiplatform-hierarchy.html#manual-configuration), might help with cli.

> Kotlin doesn't currently support sharing a source set for these combinations:
>
> * Several JVM targets
> * JVM + Android targets
> * Several JS targets
>
> If you need to access platform-specific APIs from a shared native source set, IntelliJ IDEA will help you detect
> common declarations that you can use in the shared native code.

Not sure if that means I can't have two JVM targets in the same module?  I'm interpreting it to mean that two JVM
targets in the same module cannot share another source set in the same module?

I think I should be able to use libraries in `shared` to solve this kind of problem.

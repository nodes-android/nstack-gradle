![nStack Badge](https://maven-badges.herokuapp.com/maven-central/dk.nodes.nstack/translation/badge.svg)

# nstack-gradle

Gradle plugin for [nstack.io](https://nstack.io) to generate various project files such as:

+ Translation.java (Model class for using translations)
+ nstack_keys.xml (Strings resouces containing a list of all NStack keys to be used with the NStack Kotlin plugin)
+ all_translations.json (Asset file containing all our translations

### Setup

Add this to your **Project** build.gradle:
```groovy
buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }
    dependencies {
        ...
        classpath 'dk.nodes.nstack:translation:1.0.4'
    }
}
```

Add this to your **Module** build.gradle:

```groovy
apply plugin: 'dk.nstack.translation.plugin'

translation {
    appId = "<generated app id from nstack.io>"
    apiKey = "<generated app key from nstack.io>"
    acceptHeader = "da-dk" // Accept header for which langauge we are selecting
    autoRunUpdate = true // Should the gradle task auto update the translation assets/keys
}
```

### Run

Find the **generateTranslationClass** gradle task and run it. Located in :<project>/nstack.

### Run Mode

Plugin automatically will try to identify app build type based on the tasks scheduled and set `RunMode` accordingly

- `DEBUG` - when offline, plugin will get translations from assets (if available) without interrupting build process
- `RELEASE` - Exception will be thrown when building offline
- `UNDEFINED` - Default value (also set when there was an error identifying buldType). Behaves similar to `RELEASE` 
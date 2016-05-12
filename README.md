# nstack-gradle

Gradle plugin for [nstack.io](https://nstack.io) to generate various project files such as:

+ Translation.java (Model class for using translations)
+ translationstrings.xml (Strings resource, if you're into that)
+ translation.json (Asset file for offline/first run usage)

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
        classpath 'dk.nodes.nstack:translation:0.31'
    }
}
```

Add this to your **Module** build.gradle:
```groovy
apply plugin: 'dk.nstack.translation.plugin'

translation {
    appId = "<generated app id from nstack.io>"
    apiKey = "<generated app key from nstack.io>"
    acceptHeader = "<accept header e.g. da-DK>"
}
```

### Run

Find the **generateTranslationClass** gradle task and run it. Located in :<project>/Tasks/Other.
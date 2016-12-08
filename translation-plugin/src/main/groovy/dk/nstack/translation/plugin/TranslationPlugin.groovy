package dk.nstack.translation.plugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import groovy.json.JsonSlurper
import groovy.json.StringEscapeUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileVisitDetails

class TranslationPlugin implements Plugin<Project> {

    def project = null
    def pathPrefix = ""

    void apply(Project project) {
        project.setDescription("Gradle extensions for nstack.io translations")

        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)
        if (!hasApp && !hasLib) {
            throw new IllegalStateException("NSTACK GRADLE PLUGIN: 'android' or 'android-library' plugin required.")
        }

        // Add the extension object
        project.extensions.create("translation", TranslationExtension)

        project.task('generateTranslationStrings') << {
            println "Fetching: " + project.translation.acceptHeader
            this.project = project
            findPaths()
            this.project.translation.flattenSections = true
            def json = fetchJson( project )
            def languageObject = json.data."${project.translation.acceptHeader}"
            generateStringsResource(languageObject, project)
        }

        project.task('generateTranslationClass') << {
            this.project = project
            println "Fetching: " + project.translation.acceptHeader
            findPaths()
            def json = fetchJson( project )
            def languageObject = json.data."${project.translation.acceptHeader}"
            generateJavaClass(languageObject, project)
        }
    }

    /**
     * URL params from translation settings
     * @return Absolute URL to nstack
     */
    String generateContentUrl() {
        def url = this.project.translation.contentUrl + "?"

        if( this.project.translation.allLanguages && this.project.translation.flattenSections ) {
            url += "all=true&flat=true"
        }

        else if( this.project.translation.allLanguages ) {
            url += "all=true"
        }

        else if( this.project.translation.flattenSections ) {
            url += "flat=true"
        }

        return url
    }

    /**
     * Fetch json from nstack with apiKey and appId
     * @param project
     * @return JsonSlurper object
     */
    Object fetchJson( project ) {
        def inputFile = new URL(generateContentUrl())

        def jsonText = inputFile.getText(requestProperties: [
                'Accept-Language':project.translation.acceptHeader,
                'X-Application-Id':project.translation.appId,
                'X-Rest-Api-Key':project.translation.apiKey,
                'N-Meta': 'androidstudio;debug;1.0;1.0;gradleplugin'
        ]);

        generateFallbackFile(jsonText, project)

        def json = new JsonSlurper().parseText(jsonText)

        return json
    }

    /**
     * Find the path for the Translation.java file
     * This file is where we generate main class and inner classes from the JSON file from nstack
     * */
    void findPaths() {
        // Find AndroidManifest.xml
        def results = []
        def manifestFilePath = "src/main/AndroidManifest.xml"
        project.fileTree(".").visit { FileVisitDetails details ->
            if (details.file.name.contains("AndroidManifest.xml") && details.relativePath.toString().contains("src")) {
                results << details.relativePath
                println "found --> " + details.relativePath.toString()
            }
        }

        if (results.size() == 0) {
            throw new RuntimeException("No AndroidManifest.xml file found!")
        } else {
            manifestFilePath = results.first()
        }

        def manifest;

        def rootManifest = new File(manifestFilePath.toString())
        def appManifest = new File("app/" + manifestFilePath.toString())
        def domainManifest = new File("domain/" + manifestFilePath.toString())
        def pathPicked;

        if (rootManifest.exists()) {
            pathPicked = rootManifest
        } else if (appManifest.exists()) {
            pathPicked = appManifest
        } else if (domainManifest.exists()) {
            pathPicked = domainManifest
        }

        try {
            manifest = new groovy.util.XmlSlurper().parse(pathPicked)
        } catch(Exception e) {
            println e.toString()
            println "Failed opening: " + pathPicked
            return
        }

        String packageName = manifest.@package.text()

        // Find path to our Translation.class
        if( project.translation.classPath == null ) {
            def names = []
            project.fileTree(".").visit { FileVisitDetails details ->
                if (details.file.name.contains("Translation.java")) {
                    names << details.relativePath
                    println details.toString()
                    println details.path
                }
            }

            if ( names.size() == 0 ) {
                throw new RuntimeException("No Translation.class file found!")
            } else {
                project.translation.classPath = names.first()
                println "Found classPath: " + names.first()
            }
        }

        // Find package path for when generating class
        try {
            String possibleModelPath = project.translation.classPath
            possibleModelPath = possibleModelPath.replace("/", ".")
            possibleModelPath = possibleModelPath.substring(
                    possibleModelPath.indexOf(packageName),
                    possibleModelPath.indexOf(".Translation.java")
            );

            project.translation.modelPath = possibleModelPath;

            println "Found modelPath: " + possibleModelPath
        }
        catch (Exception e1) {
            throw new RuntimeException("Could not determine modelPath from: " + project.translation.classPath)
        }
    }

    /**
     * Write the json response to a file, which we can parse later as a fallback
     * */
    void generateFallbackFile( json, project ) {
        def translationsFile = new File(project.translation.assetsPath)
        def assetsPath = project.translation.assetsPath.substring(0, project.translation.assetsPath.lastIndexOf('/'))
        def assetsFolder = new File(assetsPath)

        if( ! assetsFolder.exists() ) { assetsFolder.mkdirs() }

        translationsFile.write( json.toString() )
    }

    /**
     * Generate our Translation.java file to project.translation.classPath
     * */
    void generateJavaClass( json, project ) {
        def translationsFile = new File(project.translation.classPath)

        if( ! translationsFile.exists() ) {
            println "Java class does not exist, or path is wrong: " + pathPrefix + project.translation.classPath
            println "pathPrefix: " + pathPrefix
            println "classPath: " + project.translation.classPath
            pathPrefix = 'app/'
            translationsFile = new File(pathPrefix + project.translation.classPath)
        }

        def translationClassString = "package ${project.translation.modelPath};\n\n"
        translationClassString += "/**\n" +
                " * Created by nstack.io gradle translation plugin\n" +
                " * Built from Accept Header: ${project.translation.acceptHeader} \n" +
                " * Generated: ${new Date().toString()} \n" +
                " */\n\n"
        translationClassString += "public class Translation {\n"
        json.each {
            k, v ->
                if( v instanceof String ) {
                    translationClassString += "\tpublic static String ${k} = \"${StringEscapeUtils.escapeJava(v).replace("'","\\'")}\";\n";
                }
                else if( v instanceof Object ) {
                    // Default is a reserved keyword
                    if( k == "default" ) {
                        k = "defaultSection"
                    }
                    translationClassString += generateInnerClass( k, v )
                }
        }
        translationClassString += "}\n"
        translationsFile.write( translationClassString )
    }

    /**
     *
     * @param className
     * @param data
     * @return String Inner static class with key/value strings
     */
    String generateInnerClass(className, data) {
        println "generateInnerClass from: " + data

        def innerClass = "\tpublic final static class ${className} {\n"
        data.each {
            k, v ->
                innerClass += "\t\tpublic static String ${k} = \"${StringEscapeUtils.escapeJava(v).replace("'","\\'")}\";\n";
        }
        innerClass += "\t}\n"

        return innerClass
    }

    /**
     * Write translation data to xml as a strings resource file
     * @param json Result object of JsonSlurper parsing
     * @param project Reference to project scope
     */
    void generateStringsResource( json, project ) {
        def sw = new StringWriter()
        def xml = new groovy.xml.MarkupBuilder(sw)

        println json

        // add json values to the xml builder
        xml.resources() {
            json.each {
                k, v ->
                    string(name: "${k}", formatted:"false", "${StringEscapeUtils.escapeJava(v).replace("'","\\'")}")
            }
        }

        def stringsPath = project.translation.stringsPath.substring(0, project.translation.stringsPath.lastIndexOf('/'))

        def stringsFolder = new File(stringsPath)
        if (!stringsFolder.exists()) {
            stringsFolder.mkdirs()
        }

        def stringsFile = new File(project.translation.stringsPath)
        stringsFile.write(sw.toString())
    }
}
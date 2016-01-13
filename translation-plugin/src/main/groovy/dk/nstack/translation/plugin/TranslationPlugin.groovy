package dk.nstack.translation.plugin

import com.android.build.gradle.AppPlugin
import groovy.json.JsonSlurper
import groovy.json.StringEscapeUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileVisitDetails

class TranslationPlugin implements Plugin<Project> {

    def project = null

    void apply(Project project) {
        project.setDescription("Gradle extensions for nstack.io translations")

        def hasApp = project.plugins.withType(AppPlugin)
        if (!hasApp) {
            throw new IllegalStateException("'android' plugin required.")
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

    Object fetchJson( project ) {
        def inputFile = new URL(generateContentUrl())

        def jsonText = inputFile.getText(requestProperties: [
                'Accept-Language':project.translation.acceptHeader,
                'X-Application-Id':project.translation.appId,
                'X-Rest-Api-Key':project.translation.apiKey
        ]);

        generateFallbackFile(jsonText, project)

        def json = new JsonSlurper().parseText(jsonText)

        return json
    }

    void findPaths() {
        def manifestFilePath = "src/main/AndroidManifest.xml"
        def manifest = new groovy.util.XmlSlurper().parse(manifestFilePath)
        String packageName = manifest.@package.text()

        // Find path to our Translation.class
        if( project.translation.classPath == null ) {
            def names = []
            project.fileTree(".").visit { FileVisitDetails details ->
                if (details.file.name.contains("Translation.java")) {
                    names << details.relativePath
                }
            }

            if ( names.size() == 0 ) {
                throw new RuntimeException("No Translation.class file found!")
            } else {
                project.translation.classPath = names.first()
            }
        }

        // Find package path for when generating class
        if( project.translation.modelPath == null ) {
            project.translation.modelPath = packageName + ".util.model"

            println project.translation.modelPath
        }
    }

    void generateFallbackFile( json, project ) {
        def translationsFile = new File(project.translation.assetsPath)
        def assetsPath = project.translation.assetsPath.substring(0, project.translation.assetsPath.lastIndexOf('/'))
        def assetsFolder = new File(assetsPath)

        if( ! assetsFolder.exists() ) { assetsFolder.mkdirs() }

        translationsFile.write( json.toString() )
    }

    void generateJavaClass( json, project ) {
        def translationsFile = new File(project.translation.classPath)

        if( ! translationsFile.exists() ) {
            println "Java class does not exist, or path is wrong: " + project.translation.classPath
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
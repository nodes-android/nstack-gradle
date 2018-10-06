package dk.nstack.translation.plugin

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap

import java.nio.file.Files
import java.nio.file.Paths

class AssetManager {
    public static final String FILE_NAME_ALL_TRANSLATIONS = "all_translations"
    public static final String DIRECTORY_PATH_ASSETS = "${File.separator}src${File.separator}main${File.separator}assets"

    /**
     * Creates our assets folder to be used for downloading and storing our translation files
     */

    private static void checkIfAssetsFolderExists() {
        Log.info("Creating Assets Folder")

        File file = new File(TranslationPlugin.project.projectDir, DIRECTORY_PATH_ASSETS)

        if (!file.exists()) {
            if (file.mkdirs()) {
                Log.info("Successfully Created Assets Folder")
            } else {
                Log.error("Failed Created Assets Folder")
            }
        } else {
            Log.error("Assets Folder Already Exists")
        }
    }

    /**
     * Get all translations from nStack
     */

    private static File getAllTranslationsPath() {
        String translationFileName = FILE_NAME_ALL_TRANSLATIONS + ".json"
        File directoryFile = new File(TranslationPlugin.project.projectDir, DIRECTORY_PATH_ASSETS)
        return new File(directoryFile, translationFileName)
    }

    private static LazyMap getAllTranslations() {
        // Provide the url for downloading all translations
        String url = TranslationPlugin.project.translation.contentUrl + "?all=true"

        // Get our json string from the provided url
        String jsonString = Util.getTextFromUrl(url)

        // Load translations from assets in case of error
        if (jsonString == null) {
            Log.error("Error getting from url, Getting from assets ")
            return getTranslationsFromAssets()
        } else if (jsonString.isEmpty()) {
            return new LazyMap()
        }

        // Pull our json data from that json string we get
        Log.info("From nstack: " + jsonString)
        return  new JsonSlurper().parseText(jsonString).data
    }

    static LazyMap getTranslationsFromAssets() {
        File translationPath = getAllTranslationsPath()
        Log.info("From assets: " + translationPath.text)
        return new JsonSlurper().parseText(translationPath.text)
    }

    static LazyMap saveAllTranslationsToAssets() {
        checkIfAssetsFolderExists()

        File translationPath = getAllTranslationsPath()
        LazyMap allTranslations = getAllTranslations()

        translationPath.text = JsonOutput.toJson(allTranslations)

        return allTranslations
    }
}
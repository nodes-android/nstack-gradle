package dk.nstack.translation.plugin

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

class AssetManager {
    public static final String FILE_NAME_ALL_TRANSLATIONS = "all_translations"
    public static final String DIRECTORY_PATH_ASSETS = "/src/main/assets"

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

    private static Object getAllTranslations() {
        // Provide the url for downloading all translations
        String url = TranslationPlugin.project.translation.contentUrl + "?all=true"

        // Get our json string from the provided url
        String jsonString = Util.getTextFromUrl(url)

        // Pull our json data from that json string we get
        return new JsonSlurper().parseText(jsonString).data
    }

    static Object saveAllTranslationsToAssets() {
        checkIfAssetsFolderExists()

        File translationPath = getAllTranslationsPath()
        Object allTranslations = getAllTranslations()

        translationPath.text = JsonOutput.toJson(allTranslations)

        return allTranslations
    }
}
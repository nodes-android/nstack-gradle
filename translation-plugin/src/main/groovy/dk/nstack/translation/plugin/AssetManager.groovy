package dk.nstack.translation.plugin

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.json.internal.LazyMap

class AssetManager {
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

    private static File getTranslationsPath(int index, String lang) {
        String translationFileName = "translations_${index}_${lang}.json"
        File directoryFile = new File(TranslationPlugin.project.projectDir, DIRECTORY_PATH_ASSETS)
        return new File(directoryFile, translationFileName)
    }

    private static LazyMap getTranslationsFrom(String url) {
        String jsonString = Util.getTextFromUrl(url)
        if (jsonString.isEmpty()) {
            return new LazyMap()
        }
        return new JsonSlurper().parseText(jsonString).data
    }

    static LazyMap saveAllTranslationsToAssets() {
        checkIfAssetsFolderExists()

        String url = TranslationPlugin.project.translation.contentUrl + "api/v2/localize/resources/platforms/mobile"
        String indexJson = Util.getTextFromUrl(url)
        if (indexJson.isEmpty()) {
            return new LazyMap()
        }

        LazyMap allTranslations = new LazyMap()

        ArrayList indexResults = new JsonSlurper().parseText(indexJson)
        indexResults.eachWithIndex { result, index ->
            String locale = result.language.locale
            File path = getTranslationsPath(index, locale)
            LazyMap translations = getTranslationsFrom(result.url)
            path.text = JsonOutput.toJson(translations)
            allTranslations[locale] = translations
        }

        return allTranslations
    }
}
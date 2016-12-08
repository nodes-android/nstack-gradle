package dk.nstack.translation.plugin

class TranslationExtension {
    final String contentUrl = "https://nstack.io/api/v1/translate/mobile/keys"
    String appId = ""
    String apiKey = ""
    String modelPath = null
    String classPath = null
    String stringsPath = "src/main/res/values/translationstrings.xml"
    String assetsPath = "src/main/assets/translations.json"
    String acceptHeader = 'en-US'
    boolean allLanguages = true
    boolean flattenSections = false
}
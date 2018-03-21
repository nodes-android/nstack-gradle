package dk.nstack.translation.plugin

class TranslationExtension {
    final String contentUrl = "https://nstack.io/api/v1/translate/mobile/keys"
    String appId = ""
    String apiKey = ""
    String modelPath = null
    String classPath = null
    String stringsPath = "src${File.separator}main${File.separator}res${File.separator}values${File.separator}nstack_keys.xml"
    String assetsPath = "src${File.separator}main${File.separator}assets${File.separator}translations.json"
    String acceptHeader = 'en-US'
    boolean autoRunUpdate = true
}
package dk.nstack.translation.plugin

class Util {

    static String getTextFromUrl(String url) throws IOException {
        def inputFile = new URL(url)
        try {
            String response =  inputFile.getText(requestProperties: [
                    'Accept-Language' : TranslationPlugin.project.translation.acceptHeader,
                    'X-Application-Id': TranslationPlugin.project.translation.appId,
                    'X-Rest-Api-Key'  : TranslationPlugin.project.translation.apiKey,
                    'N-Meta'          : 'androidstudio;debug;1.0;1.0;gradleplugin'
            ])
            Log.info("Translations fetched from nstack.io")
            return response
        } catch (Exception e) {
            handleError(e)
            return null
        }
    }

    static void handleError(Exception e) {
        // If DEBUG mode is enabled, proceed without exception being thrown
        if (TranslationPlugin.RUN_MODE.isDebug()) {
            Log.error("Exception was silenced when fetching translations\n\r" +
                    "Exception: " + e.message)
        } else {
            throw new Exception ("Error fetching translation while in non DEBUG Mode: " + e.toString())
        }
    }
}
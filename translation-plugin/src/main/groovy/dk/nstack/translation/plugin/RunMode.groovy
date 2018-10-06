package dk.nstack.translation.plugin


enum RunMode {
    DEBUG(true), RELEASE(false), UNDEFINED(false)

    private boolean isDebug = false

    RunMode(boolean isDebug) {
        this.isDebug = isDebug
    }

     boolean isDebug() {
        return isDebug
    }

    static RunMode parse(String taskName) {
        if (taskName.contains("Debug")) {
            return DEBUG
        } else if (taskName.contains("Release")) {
            return RELEASE
        } else {
            return UNDEFINED
        }
    }

}

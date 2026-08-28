package cc.watchneko.utils.common;

public class GrimArguments {

    public static final boolean TRANSACTION_KICKS = !Boolean.getBoolean("watchneko.disable-transaction-kick");
    public static final String API_URL = System.getProperty("watchneko.api-url", "https://api.grim.ac/v1/server/");
    public static final String PASTE_URL = System.getProperty("watchneko.paste-url", "https://paste.grim.ac/");
    public static final String PLATFORM_OVERRIDE = System.getProperty("watchneko.platform-override", "");

}

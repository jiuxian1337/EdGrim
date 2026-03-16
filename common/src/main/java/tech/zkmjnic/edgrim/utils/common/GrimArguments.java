package tech.zkmjnic.edgrim.utils.common;

public class GrimArguments {

    public static final boolean TRANSACTION_KICKS = !Boolean.getBoolean("edgrim.disable-transaction-kick");
    public static final String API_URL = System.getProperty("edgrim.api-url", "https://api.grim.ac/v1/server/");
    public static final String PASTE_URL = System.getProperty("edgrim.paste-url", "https://paste.grim.ac/");
    public static final String PLATFORM_OVERRIDE = System.getProperty("edgrim.platform-override", "");

}

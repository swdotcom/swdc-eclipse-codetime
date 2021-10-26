package com.swdc.codetime.webview;

import com.swdc.codetime.managers.ThemeModeInfoManager;

public class CssUtil {

    public static String getGlobalStyle() {
        if (new ThemeModeInfoManager().isLightMode()) {
            return getLightStyle();
        }
        return getDarkStyle();
    }

    private static String getDarkStyle() {
        return "  <style type=\"text/css\">\n" +
                "    body { background-color: #2e2e2e; color: #fafafa; font-family: 'Inter', sans-serif; }\n" +
                "  </style>\n";
    }

    private static String getLightStyle() {
        return "  <style type=\"text/css\">\n" +
                "    body { font-family: 'Inter', sans-serif; }\n" +
                "  </style>\n";
    }
}

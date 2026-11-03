package org.telegram.messenger;

/** Interface palette only: never processes user photos or media. */
public final class ClaudPalette {
    private ClaudPalette() { }
    public static int warm(int color) {
        int alpha = color >>> 24;
        if (alpha == 0) return color;
        int red = (color >>> 16) & 255, green = (color >>> 8) & 255, blue = color & 255;
        if (blue - red < 18 || blue + 5 < green || green < red) return color;
        if (blue < 90) {
            int gray = (red + green + blue) / 3;
            return (alpha << 24) | (Math.min(255, gray + 3) << 16) | (gray << 8) | Math.max(0, gray - 3);
        }
        return (alpha << 24) | ((218 * blue / 255) << 16) | ((151 * blue / 255) << 8) | (117 * blue / 255);
    }
    public static boolean isLove(String emoji) {
        if (emoji == null) return false;
        for(int i=0;i<emoji.length();) {
            int code=emoji.codePointAt(i);i+=Character.charCount(code);
            if(code==0x2764 || code==0x2763 || code>=0x1F493 && code<=0x1F49F
                    || code==0x1F5A4 || code==0x1F90D || code==0x1F90E
                    || code>=0x1FA75 && code<=0x1FA77) return true;
        }
        return false;
    }
}

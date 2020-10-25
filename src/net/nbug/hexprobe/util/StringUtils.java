package net.nbug.hexprobe.util;

/**
 * StringUtils
 * 
 * @author hexprobe <hexprobe@nbug.net>
 * 
 * @license
 * This code is hereby placed in the public domain.
 * 
 */
public class StringUtils {
	private StringUtils() {
	}
	
	public static int getPhysicalWidth(char hi) {
		if (hi <= 127) {
			if (' ' <= hi && hi <= '~') {
				return 1;
			} else {
				return 0;
			}
		} else {
			if ('｡' <= hi && hi <= 'ﾟ') {
				return 1;
			} else {
				return 2;
			}
		}
	}
	
	public static String join(String delimiter, Iterable<String> strings) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String str : strings) {
			if (first) {
				first = false;
			} else {
				sb.append(delimiter);
			}
			sb.append(str);
		}
		return sb.toString();
	}
}

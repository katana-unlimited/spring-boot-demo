package com.example.demo.utils;

/**
 * ケース変換ユーティリティ.
 *
 * @author https://so-kai-app.sakura.ne.jp/
 */
public final class CaseUtils {

	/** プライベートコンストラクタ. */
	private CaseUtils() {
	}

	/**
	 * 小文字 に変換します。
	 *
	 * @param text 変換前の文字列
	 * @return 小文字 に変換された文字列
	 */
	public static String toLowerCase(final String text) {
		return (text == null) ? null : text.toLowerCase();
	}

	/**
	 * 大文字 に変換します。
	 *
	 * @param text 変換前の文字列
	 * @return 大文字 に変換された文字列
	 */
	public static String toUpperCase(final String text) {
		return (text == null) ? null : text.toUpperCase();
	}

	/**
	 * チェインケース (ケバブケース) に変換.
	 *
	 * @param word 変換前の文字列
	 * @return チェインケース (ケバブケース) に変換された文字列
	 */
	public static String toChainCase(final String word) {
		return word == null
				? null
				: word.replaceAll("([A-Z])", "-$1").toLowerCase().replaceAll("[-_ ]+", "-").replaceAll("^-|-$", "");
	}

	/**
	 * スネークケース に変換.
	 *
	 * @param word 変換前の文字列
	 * @return スネークケース に変換された文字列
	 */
	public static String toSnakeCase(final String word) {
		return word == null
				? null
				: word.replaceAll("([A-Z])", "_$1").toLowerCase().replaceAll("[_ ]+", "_").replaceAll("^_|_$", "");
	}

	/**
	 * 先頭文字が 小文字 の キャメルケース に変換.
	 *
	 * @param word 変換前の文字列
	 * @return 先頭文字が 小文字 の キャメルケース に変換された文字列
	 */
	public static String toLowerCamelCase(final String word) {
		final String camel = toCamelCase(word);
		if (camel == null || camel.length() < 2) {
			return camel;
		} else {
			return camel.substring(0, 1).toLowerCase() + camel.substring(1);
		}
	}

	/**
	 * 先頭文字が 大文字 の キャメルケース に変換.
	 *
	 * @param word 変換前の文字列
	 * @return 先頭文字が 大文字 の キャメルケース に変換された文字列
	 */
	public static String toUpperCamelCase(final String word) {
		final String camel = toCamelCase(word);
		if (camel == null || camel.length() < 2) {
			return camel;
		} else {
			return camel.substring(0, 1).toUpperCase() + camel.substring(1);
		}
	}

	/**
	 * キャメルケース に変換する プライベート メソッド.
	 *
	 * @param word 変換前の文字列
	 * @return スネークケース に変換された文字列
	 * @see #toUpperCamelCase(String)
	 * @see #toLowerCamelCase(String)
	 */
	private static String toCamelCase(final String word) {
		if (word == null) {
			return null;
		}

		final StringBuilder sb = new StringBuilder();
		final char[] chars = word.toLowerCase().replaceAll("[_ -]+", "_").toCharArray();
		boolean flag = false;
		for (final char c : chars) {
			if (c == '_') {
				flag = true;
				continue;
			}

			if (flag) {
				sb.append(Character.toUpperCase(c));
				flag = false;
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

}
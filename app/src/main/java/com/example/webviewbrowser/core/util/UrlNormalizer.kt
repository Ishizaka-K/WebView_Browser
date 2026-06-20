package com.example.webviewbrowser.core.util

import java.net.URLEncoder
import java.util.Locale

/**
 * アドレスバー入力の正規化結果。
 */
sealed interface NormalizedTarget {
    /** http/https の URL として読み込む。 */
    data class Url(val url: String) : NormalizedTarget

    /** 検索クエリとして検索エンジン URL に変換した。 */
    data class Search(val url: String) : NormalizedTarget
}

/**
 * アドレスバー入力を URL か検索クエリに正規化する純粋ロジック。
 *
 * 判定方針:
 * - 既に http/https スキームがあれば URL。
 * - 空白を含む、またはドット区切りのホスト形でなければ検索。
 * - `host[.tld][:port][/path]` の形なら https:// を補って URL。
 */
object UrlNormalizer {

    // 仕様: http/https スキームのみ URL として扱う。
    // ftp/file/custom スキームはそのまま load せず検索にフォールバックする。
    private val schemeRegex = Regex("(?i)^https?://\\S+$")
    // 例: example.com, sub.example.co.jp, localhost:8080, 192.168.0.1/path
    private val hostLikeRegex = Regex("^[^\\s/?#]+\\.[^\\s/?#]+.*$")
    private val localhostRegex = Regex("^localhost(:\\d+)?(/.*)?$", RegexOption.IGNORE_CASE)
    private val ipv4Regex = Regex("^\\d{1,3}(\\.\\d{1,3}){3}(:\\d+)?(/.*)?$")

    /**
     * @param input ユーザー入力。
     * @param searchTemplate `%s` を含む検索 URL テンプレート。例: `https://www.google.com/search?q=%s`
     */
    fun normalize(input: String, searchTemplate: String): NormalizedTarget {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return NormalizedTarget.Search(buildSearchUrl("", searchTemplate))
        }

        // 既にスキーム付き。
        if (schemeRegex.matches(trimmed)) {
            return NormalizedTarget.Url(trimmed)
        }

        // 空白を含むものは検索とみなす。
        val hasWhitespace = trimmed.any { it.isWhitespace() }
        if (!hasWhitespace) {
            val lower = trimmed.lowercase(Locale.US)
            val isHostLike = localhostRegex.matches(lower) ||
                ipv4Regex.matches(trimmed) ||
                hostLikeRegex.matches(trimmed)
            if (isHostLike) {
                return NormalizedTarget.Url("https://$trimmed")
            }
        }

        return NormalizedTarget.Search(buildSearchUrl(trimmed, searchTemplate))
    }

    private fun buildSearchUrl(query: String, template: String): String {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return if (template.contains("%s")) {
            template.replace("%s", encoded)
        } else {
            // テンプレートが不正な場合は末尾に付与してフォールバック。
            template + encoded
        }
    }
}

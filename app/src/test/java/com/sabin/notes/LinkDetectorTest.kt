package com.sabin.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class LinkDetectorTest {
    private fun urls(s: String) = LinkDetector.find(s).map { s.substring(it.first, it.last + 1) }

    @Test fun emptyString() = assertEquals(emptyList<IntRange>(), LinkDetector.find(""))
    @Test fun noLinks() = assertEquals(emptyList<String>(), urls("just some text"))
    @Test fun oneLink() = assertEquals(listOf("https://example.com/a?b=1"), urls("see https://example.com/a?b=1 now"))
    @Test fun severalLinks() = assertEquals(listOf("http://a.com", "https://b.org"), urls("http://a.com and\nhttps://b.org"))
    @Test fun trailingPunctuation() = assertEquals(listOf("https://a.com/x"), urls("Go to https://a.com/x."))
    @Test fun trailingComma() = assertEquals(listOf("https://a.com"), urls("https://a.com, then"))
    @Test fun inParentheses() = assertEquals(listOf("https://a.com/p"), urls("(https://a.com/p)"))
    @Test fun wwwOnlyNotMatched() = assertEquals(emptyList<String>(), urls("visit www.example.com"))
    @Test fun schemeOnlyNotMatched() = assertEquals(emptyList<String>(), urls("https:// nothing"))
}

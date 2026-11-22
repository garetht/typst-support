package com.github.garetht.typstsupport.languageserver.locations

import java.util.regex.Pattern

data class Version(val major: Int, val minor: Int, val patch: Int): Comparable<Version> {
  override fun compareTo(other: Version): Int =
    compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

  fun toPathString(): String {
    return "v$major.$minor.$patch"
  }

  fun toConsoleString(): String {
    return "tinymist ${toPathString()}"
  }

  companion object {
    fun parseVersion(output: String): Version? {
      // Pattern to match "tinymist X.Y.Z" format (e.g. from `tinymist -V` output)
      val pattern =
        Pattern.compile("tinymist\\s+(\\d+)\\.(\\d+)\\.(\\d+)", Pattern.CASE_INSENSITIVE)
      val matcher = pattern.matcher(output)

      if (matcher.find()) {
        return versionOf(matcher.group(1), matcher.group(2), matcher.group(3))
      }

      // Fallback: bare "X.Y.Z" format (e.g. the LSP `InitializeResult` serverInfo.version).
      // Anchored so that unrelated numbers embedded in longer text (e.g. error
      // messages) are not mistaken for a version.
      val barePattern = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")
      val bareMatcher = barePattern.matcher(output.trim())
      if (bareMatcher.matches()) {
        return versionOf(bareMatcher.group(1), bareMatcher.group(2), bareMatcher.group(3))
      }

      return null
    }

    private fun versionOf(major: String, minor: String, patch: String): Version? = try {
      Version(major.toInt(), minor.toInt(), patch.toInt())
    } catch (e: NumberFormatException) {
      null
    }
  }
}

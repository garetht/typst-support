package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.languageserver.locations.Version

object VersionRequirement {
  // The version of tinymist that is downloaded automatically when no custom
  // binary is configured.
  val downloadVersion = Version(0, 15, 12)

  // The oldest tinymist version that the plugin accepts for custom binaries.
  val minimumVersion = Version(0, 13, 14)

  // tinymist v0.13.17 changed the preview's `--partial-rendering` flag from a
  // bare boolean flag to one that requires an explicit value (`=true`): older
  // builds reject the valued form, newer builds reject the bare form.
  val valuedPartialRenderingVersion = Version(0, 13, 17)
}

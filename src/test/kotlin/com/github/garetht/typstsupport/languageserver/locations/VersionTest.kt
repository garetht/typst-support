package com.github.garetht.typstsupport.languageserver.locations

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class VersionTest {
  @Test
  fun `test version creation and properties`() {
    val version = Version(1, 2, 3)
    assertEquals(1, version.major)
    assertEquals(2, version.minor)
    assertEquals(3, version.patch)
  }

  @Test
  fun `test toPathString`() {
    val version = Version(1, 2, 3)
    assertEquals("v1.2.3", version.toPathString())
  }

  @Test
  fun `test toConsoleString`() {
    val version = Version(1, 2, 3)
    assertEquals("tinymist v1.2.3", version.toConsoleString())
  }


  @Test
  fun `test parseVersion with valid input`() {
    val validInputs = listOf(
      "tinymist 1.2.3",
      "Tinymist 1.2.3",
      "TINYMIST 1.2.3",
      "tinymist 10.20.30",
      // Bare format from the LSP `InitializeResult` serverInfo.version
      "0.15.12",
      "v0.15.12"
    )

    validInputs.forEach { input ->
      val version = Version.parseVersion(input)
      assertNotNull(version, "Failed to parse: $input")
      when (input) {
        "tinymist 1.2.3" -> {
          assertEquals(1, version?.major)
          assertEquals(2, version?.minor)
          assertEquals(3, version?.patch)
        }

        "tinymist 10.20.30" -> {
          assertEquals(10, version?.major)
          assertEquals(20, version?.minor)
          assertEquals(30, version?.patch)
        }
      }
    }
  }

  @Test
  fun `test parseVersion with invalid input`() {
    val invalidInputs = listOf(
      "tinymist",
      "tinymist abc.def.ghi",
      "tinymist 1.2",
      "not tinysssmist 1.2.3",
      "tinymist 1.2.extra",
    )

    invalidInputs.forEach { input ->
      val version = Version.parseVersion(input)
      assertNull(version, "Should not parse: $input")
    }
  }

  @Test
  fun `test data class equality`() {
    val version1 = Version(1, 2, 3)
    val version2 = Version(1, 2, 3)
    val version3 = Version(1, 2, 4)

    assertEquals(version1, version2)
    assertNotEquals(version1, version3)
  }

  @ParameterizedTest(name = "{0} should be less than {1}")
  @MethodSource("provideLessThanVersions")
  fun `test version less than`(version1: Version, version2: Version) {
    assertTrue(version1 < version2)
    assertTrue(version1.compareTo(version2) < 0)
  }

  @ParameterizedTest(name = "{0} should be greater than {1}")
  @MethodSource("provideGreaterThanVersions")
  fun `test version greater than`(version1: Version, version2: Version) {
    assertTrue(version1 > version2)
    assertTrue(version1.compareTo(version2) > 0)
  }

  @ParameterizedTest(name = "{0} should equal {1}")
  @MethodSource("provideEqualVersions")
  fun `test version equality`(version1: Version, version2: Version) {
    assertEquals(0, version1.compareTo(version2))
    assertTrue(version1 == version2)
    assertFalse(version1 < version2)
    assertFalse(version1 > version2)
  }

  @ParameterizedTest(name = "sorted({0}) should equal {1}")
  @MethodSource("provideSortingTestCases")
  fun `test version sorting`(versions: List<Version>, expected: List<Version>) {
    assertEquals(expected, versions.sorted())
  }

  companion object {
    @JvmStatic
    fun provideLessThanVersions() = listOf(
      // Major version differences
      Arguments.of(Version(1, 0, 0), Version(2, 0, 0)),
      Arguments.of(Version(1, 9, 9), Version(2, 0, 0)),

      // Minor version differences (same major)
      Arguments.of(Version(1, 1, 0), Version(1, 2, 0)),
      Arguments.of(Version(1, 1, 9), Version(1, 2, 0)),

      // Patch version differences (same major and minor)
      Arguments.of(Version(1, 1, 1), Version(1, 1, 2)),
      Arguments.of(Version(1, 1, 0), Version(1, 1, 1)),

      // Mixed scenarios
      Arguments.of(Version(0, 9, 9), Version(1, 0, 0)),
      Arguments.of(Version(1, 0, 9), Version(1, 1, 0)),
      Arguments.of(Version(2, 1, 1), Version(2, 1, 2))
    )

    @JvmStatic
    fun provideGreaterThanVersions() = listOf(
      // Major version differences
      Arguments.of(Version(2, 0, 0), Version(1, 0, 0)),
      Arguments.of(Version(2, 0, 0), Version(1, 9, 9)),

      // Minor version differences (same major)
      Arguments.of(Version(1, 2, 0), Version(1, 1, 0)),
      Arguments.of(Version(1, 2, 0), Version(1, 1, 9)),

      // Patch version differences (same major and minor)
      Arguments.of(Version(1, 1, 2), Version(1, 1, 1)),
      Arguments.of(Version(1, 1, 1), Version(1, 1, 0)),

      // Mixed scenarios
      Arguments.of(Version(1, 0, 0), Version(0, 9, 9)),
      Arguments.of(Version(1, 1, 0), Version(1, 0, 9)),
      Arguments.of(Version(2, 1, 2), Version(2, 1, 1))
    )

    @JvmStatic
    fun provideEqualVersions() = listOf(
      Arguments.of(Version(1, 0, 0), Version(1, 0, 0)),
      Arguments.of(Version(1, 2, 3), Version(1, 2, 3)),
      Arguments.of(Version(0, 0, 0), Version(0, 0, 0)),
      Arguments.of(Version(10, 20, 30), Version(10, 20, 30))
    )

    @JvmStatic
    fun provideSortingTestCases(): Stream<Arguments> = Stream.of(
      // Simple case
      Arguments.of(
        listOf(Version(2, 0, 0), Version(1, 0, 0)),
        listOf(Version(1, 0, 0), Version(2, 0, 0))
      ),

      // Complex case with all version components
      Arguments.of(
        listOf(
          Version(2, 1, 0),
          Version(1, 2, 3),
          Version(2, 0, 1),
          Version(1, 2, 2),
          Version(1, 1, 0)
        ),
        listOf(
          Version(1, 1, 0),
          Version(1, 2, 2),
          Version(1, 2, 3),
          Version(2, 0, 1),
          Version(2, 1, 0)
        )
      ),

      // Edge case with zeros
      Arguments.of(
        listOf(
          Version(1, 0, 1),
          Version(0, 1, 0),
          Version(0, 0, 1),
          Version(1, 0, 0)
        ),
        listOf(
          Version(0, 0, 1),
          Version(0, 1, 0),
          Version(1, 0, 0),
          Version(1, 0, 1)
        )
      )
    )
  }
} 

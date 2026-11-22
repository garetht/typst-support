<p align="center">
  <img alt="Typst Support Logo" src="./assets/logo.png" width="450" />
</p>

####

<p align="center">

[![Version](https://img.shields.io/jetbrains/plugin/v/27697-typst-support.svg)](https://plugins.jetbrains.com/plugin/27697-typst-support)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/27697-typst-support.svg)](https://plugins.jetbrains.com/plugin/27697-typst-support)

</p>


<!-- Plugin description -->
<p align="center">
  <h3>An IntelliJ Plugin for Typst powered by Tinymist</h3>
</p>

## Features

### Live side-by-side preview
Changes to documents are updated in the preview window in real time.

### Reformat files
Reformat entire files with `typstfmt` or `typstyle`.

### Documentation on Hover
Hover over a symbol to view its documentation.

### Jump to Definition from Preview
Click on a part of the preview document to jump to the definition of that element in your Typst sources.

### Find Usages
List all the places a symbol is used in the project.

### Autocomplete
Suggestions for symbols as you type.

### Bring your own binary
Use a `tinymist` binary that exists on your system by pointing to its path locally.

<!-- Plugin description end -->

# Feature Demos

<h3>Live Preview</h3>

####

<p align="center">
  <img alt="Live Preview" src="./assets/live-preview.gif" width="800" />
</p>

####

<h3>Reformat files</h3>

####

<p align="center">
  <img alt="Reformat File" src="./assets/reformat-file.gif" width="800" />
</p>

####

<h3>Jump to Definition from Preview</h3>

####

<p align="center">
  <img alt="Jump to Definition" src="./assets/jump-to-definition.gif" width="800" />
</p>

####

<h3>Documentation on Hover</h3>

####

<p align="center">
  <img alt="Documentation on Hover" src="./assets/hover-documentation.gif" width="800" />
</p>

####

<h3>Find Usages</h3>

####

<p align="center">
  <img alt="Find Usages" src="./assets/find-usages.gif" width="800" />
</p>

####

<h3>Autocomplete</h3>

####

<p align="center">
  <img alt="Reformat File" src="./assets/autocomplete.gif" width="800" />
</p>

####

<h3>Bring your own binary!</h3>
Specify your local version of `tinymist` to be used with this plugin.

####

<p align="center">
  <img alt="Typst Support Logo" src="./assets/configuration.png" width="800" />
</p>

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "Typst
  Support"</kbd> >
  <kbd>Install Plugin</kbd>

## Compatible IDEs

Works in 2025.1+ IntelliJ IDEs. May not work in Rider.

## Compatible Tinymist

Custom `tinymist` binaries `0.13.14` and above are supported. Otherwise, `tinymist` `v0.15.12` is downloaded automatically.

## Feature Support

IntelliJ supports [the following LSP features out of the box](https://plugins.jetbrains.com/docs/intellij/language-server-protocol.html#supported-features). This plugin will improve as IntelliJ support for LSP features improves. In the meantime, we may be able to integrate unsupported LSP features into this plugin manually, with a view to removing them when official support arrives.

# slacky

![](resources/public/images/slacky-logo.png?raw=true)

Memes-as-a-Service for Slack. Live instance and registration at https://zomg.oliy.co.uk.

## Demo

![](resources/public/images/memes-on-slack.gif?raw=true)

## Installation

Build using nixpacks

## Examples

The generic template is:

`/meme search term | upper text | lower text`

The _search term_ can optionally include the keyword `:anim` to search for animated images, e.g.

`/meme :anim gandalf vs balrog | you shall not | pass!`

You can provide an image to use by providing the url instead:

`/meme http://path/to/image.jpg | upper text | lower text`

Some pre-defined memes are also provided, documentation will follow shortly:

`/meme create all the memes!`

## Build
[![Circle CI](https://circleci.com/gh/oliyh/slacky.svg?style=svg)](https://circleci.com/gh/oliyh/slacky)

## Development

### memecaptain gem

You will need:
- imagemagick
- [ruby](https://github.com/rbenv/rbenv)
- [bundler](https://bundler.io/)

Run `./install-memecaptain` to install the memecaptain binstub under `bin/memecaptain`.

### ClojureScript

Sources files are in `resources/src/cljs`.

Run `lein figwheel` for a live-reloading development environment. Once the homepage has been loaded
in a browser, the figwheel terminal will provide a REPL running inside the browser's Javascript engine.

Alternatively run `(dev/cljs-repl)` to obtain a fresh CLJS REPL running inside the Clojure JVM, and type `:cljs/quit` to exit it.

Run `lein cljsbuild auto dev` to automatically build during development without figwheel.
To build for production use run `lein cljsbuild once prod`.

Copyright © 2015  oliyh

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

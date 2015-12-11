# slacky

![](resources/public/images/slacky-logo.png?raw=true)

Memes-as-a-Service for Slack. Live instance and registration at https://slacky-server.herokuapp.com.

## Demo

![](resources/public/images/memes-on-slack.gif?raw=true)

## Installation

All ready for Heroku deployment.

## Examples

The generic template is:

`/meme search term | upper text | lower text`

The _search term_ can optionally include the keyword `:anim` to search for animated images, e.g.

`/meme :anim gandalf vs balrog | you shall not | pass!`

You can provide an image to use by providing the url instead:

`/meme http://path/to/image.jpg | upper text | lower text`

Some pre-defined memes are also provided, documentation will follow shortly:

`/meme create all the memes!`

Copyright © 2015  oliyh

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

# slacky

Memes-as-a-service for Slack. Live instance at https://slacky-server.herokuapp.com/api.

## Installation

All ready for Heroku deployment.

To integrate with Slack:
- Create a [Slash command](https://my.slack.com/services/new/slash-commands/) for `/meme` to point to https://slacky-server.herokuapp.com:443/api/slack/meme. Set the usage hint to be `image url or search term | upper text | lower text`.
- Create an [Incoming webhook](https://my.slack.com/services/new/incoming-webhook/) and in your Heroku dashboard set config variable `WEBHOOK-URL` to be the webhook url provided by Slack

Then simply visit a Slack channel and type /meme to get going!

Copyright © 2015  oliyh

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

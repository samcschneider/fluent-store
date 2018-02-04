# Fluent Store

Sample ecomm storefront that makes use of the Fluent APIs to create orders and show the store locator

## Overview

Provide some code examples of working with the APIs. This project will be continuously enhanced as
new API samples are needed.

## Setup

Acquire a fluent sandbox environment. You'll receive fluent credentials in your setup email.
You'll need to include settings for the following values:

/src/fr_api/network.cljs provide values {{here}}
  -> default-env :client-id     "{{your client id}}"
                       :client-secret "{{your client secret}}"
                       :username "{{your api user name}}"
                       :password "{{your api password}}"})

/resources/index.html
  -> if you wish to use the store locator widget, you'll need your API key as well as a google
maps API key.
  apiKey: '{{your api key}}'
  googleAPIKey: '{{your *google* API key}}'


To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL. 

## License

Copyright © 2017

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.

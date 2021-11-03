# huckster

A marketmaker bot based on Broxus Nova.

*huckster* helps you to place asynchronous orders in Broxus Nova orderbook using different strategies and price feeds.

## Building
* Clone *huckster* repository with the `git clone` command.
* Go to the repo directory
* Run `./gradlew build` to build the executable
* Copy the executable to your current location:
  ```bash
  mv ./build/libs/hucksterFat.jar ./huckster.jar
  ```

**Important:** You need to have `jdk1.8.0_201` installed to build the app. In case it is not the only Java instance on your PC, specify the path to jdk explicitly by adding `-D org.gradle.java.home=<path/to/jdk>` parameter to Gradlew.

## Running

To run *huckster*, use a regular java syntax:
```bash
java -jar ./huckster.jar [JOB] [PARAMS]
```

### Supported jobs
* `seller`<br/>Sell at the highest possible price
* `orderbook`<br/>Visualize existing orderbook

### Runtime parameters
#### `seller` job
* `-k, --keys <FILE>`<br/>Keys to access Broxus Nova platform
* `-s, --strategy <FILE>`<br/>Orders placement strategy configuration
* `-pad, --priceAdapter <FILE>`<br/>Price adapter configuration
* `-pau, --priceAuth <FILE>`<br/>Price adapter authentication file \[optional, adapter-dependent\]
* `-n, --notify <FILE>`<br/>Notifier configuration file [optional]

#### `orderbook` job
* `-p, --pair <PAIR>`<br/>
  Base and counter currency tickers separated by a delimiter.
  Supported delimiters: underscore(`_`), dash(`-`) and slash (`/`)
* `-k, --keys <FILE>`<br/>Keys to access Broxus Nova platform
* `-r, --refresh <SECONDS>`<br/>Orderbook refresh rate in seconds \[optional\]. Default: 10

### Supported strategies
* `simple`<br/>
  A simple strategy that takes the following parameters as inputs:
  * Source currency
  * Offsets (for parallel execution) along with volume parts
  * Execution limits
  * Volume and prices structure per target currencies

  For more details see [Simple strategy specification](examples/Simple%20strategy%20specification.md).

See [examples](examples) folder for samples of strategy configuration files.

### Supported price feeds
* `fixed`<br/>
  Goes without saying, fixed prices per different target currencies.

* `googleSheet`<br/>
  Sources price feed from a Google Sheet. Requires [OAuth 2.0 file](https://developers.google.com/identity/protocols/oauth2) downloaded from [Google Developer Account](https://console.developers.google.com/).
  
* `bitcoin.com`<br/>
  Gets price feed from Bitcoin.com exchange

See [examples](examples) folder for samples of price feed configuration files.

### Supported notifiers
* `telegram`<br/>
  Uses credentials of a Telegram bot to push notifications to private and public chats.</br>
  
See [examples](examples) folder for configuration samples.
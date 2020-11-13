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

## Running

To run *huckster*, use a regular java syntax:
```bash
java -jar ./huckster.jar [JOB] [PARAMS]
```

### Supported jobs
* `seller` - Sell at the highest possible price

### Runtime parameters
* `-k, --keys <FILE>`<br/>Keys to access Broxus Nova platform
* `-s, --strategy <FILE>`<br/>Orders placement strategy configuration
* `-pad, --priceAdapter <FILE>`<br/>Price adapter configuration
* `-pau, --priceAuth <FILE>`<br/>Price adapter authentication file \[optional\]

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
  Sources price feed from a Google Sheet. Requires OAuth 2.0 file downloaded from Google Developer Account.

See [examples](examples) folder for samples of price feed configuration files.

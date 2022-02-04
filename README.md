# Json-LD Benchmark Java implementations

A benchmark of the following JSON-LD implementations:
- [Titanium](https://github.com/filip26/titanium-json-ld): Implements the JSON-LD 1.1 spec with a [high coverage](https://w3c.github.io/json-ld-api/reports/#subj_Titanium_Java).
- [Json-LD Java](https://github.com/jsonld-java/jsonld-java): Implements the JSON-LD 1.0 spec with a [high coverage](https://json-ld.org/test-suite/reports/#subj_7). Implements part of the JSON-LD 1.1 API (still [low coverage](https://github.com/jsonld-java/jsonld-java/pull/283)).

The benchmarks use a subset of [W3C compliance test suite for JSON-LD 1.1](https://w3c.github.io/json-ld-api/tests/).
In order to compare the 2 implementations, we need the subset of test entries which pass in both implementations. Because of the low ow coverage for Json-LD Java on Json-LD 1.1 spec this subset is sometimes small.

## Running the benchmarks

The default parameters are: 5 JVM warmup iterations and 10 measurement iterations:

```
sbt jmh:run
```

You can tweak the warmup iterations and the measurament iteration parameters:

```
sbt jmh:run -iw {warmup_iterations_number} -i {measurement_iterations_number}
```

### Updating the test suite

The test suite can be found as a compress bundle in `src/test/resources/json-ld-11.org.tgz` from the original [W3C compliance test suite]((https://w3c.github.io/json-ld-api/tests/)). 
However, tests can be updated (in case W3C modifies them) using the following command (Mac OS and Linux):

```bash
bash download_suite.sh
```

## Results
The provided results are the ratio between the throughput obtained by Json-LD Java implementation / the one obtained by Titanium.

|         | Number of tests | Ratio (Json-LD Java / Titanium) |
|---------|:---------------:|:-------------------------------:|
| compact |        89       |               4.21              |
| expand  |        83       |               3.39              |
| flatten |        45       |               5.10              |
| frame   |        42       |               6.90              |
| fromRdf |        30       |               3.21              |
| toRdf   |       127       |               4.53              |
| Total   |       416       |              4.556              |

from the benchmark results the **Json-LD Java implementation is ~ 4.6 times faster in average than Titanium**. 

This difference in performance can be caused by the greater complexity of the different algorithms in Json-LD 1.1 due to the introduction of many new features. 

In the current state (02.04.2022), the Titanium library is 2x faster than in [its initial state](https://github.com/umbreak/jsonld-benchmarks/blob/c6b7ba93f88db942258008ad771728341b0f3851/README.md#results) (03.12.2020).

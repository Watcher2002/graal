# SMT-Based Canonicalization Checker: Overview

Files modified for SMT-based canonicalization checking:
- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/smt/`  
  Added as a new package containing the SMT verification infrastructure. It includes the intermediate symbolic representation, bitvector, Boolean, integer, and floating-point expression nodes, path-condition support, the IR-to-SMT translator, solver interaction through Z3, verification result types, and utility classes.

- `compiler/mx.compiler/suite.py`  
  Modified to add the Z3 dependency to the compiler suite configuration.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/debug/DebugOptions.java`  
  Modified to add a debug option used to intentionally trigger incorrect canonicalization behavior during evaluation.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/phases/common/CanonicalizerPhase.java`  
  Modified to invoke the SMT verifier before selected canonicalization replacements are committed.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/AddNode.java`  
  Modified to support controlled injection of an incorrect canonicalization result for testing.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/SubNode.java`  
  Modified to support controlled injection of an incorrect subtraction-related canonicalization result.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/MulNode.java`  
  Modified to support controlled injection of an incorrect strength-reduction result.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/NegateNode.java`  
  Modified to support controlled injection of an incorrect negation-related rewrite.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/NotNode.java`  
  Modified to support controlled injection of an incorrect bitwise-complement rewrite.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/XorNode.java`  
  Modified to support controlled injection of an incorrect XOR canonicalization result.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/LeftShiftNode.java`  
  Modified to support controlled injection of an incorrect shift-combination result.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/UnsignedRightShiftNode.java`  
  Modified to support controlled injection of an incorrect mask-related canonicalization result.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/nodes/calc/SignExtendNode.java`  
  Modified to support controlled injection of an incorrect sign-extension rewrite.

- `compiler/src/jdk.graal.compiler/src/jdk/graal/compiler/replacements/nodes/arithmetic/IntegerNegExactOverflowNode.java`  
  Modified by adding an accessor for the input value, allowing the SMT translator to inspect the node.

- `compiler/src/jdk.graal.compiler.test/src/jdk/graal/compiler/nodes/test/SmtCanonicalizationErrorTest.java`  
  Added targeted tests containing intentionally incorrect canonicalization cases used to evaluate whether the verifier detects unsound rewrites.




# SMT-Based Canonicalization Checker: Setup Guide

## 1. Clone `mx` and add it to `PATH`

`mx` is the build tool for all Graal suite projects.

```bash
git clone https://github.com/graalvm/mx.git
export PATH="$PWD/mx:$PATH"
```

## 2. Download the Z3 binary distribution

```bash
wget https://github.com/Z3Prover/z3/releases/download/z3-4.13.3/z3-4.13.3-x64-glibc-2.35.zip
unzip z3-4.13.3-x64-glibc-2.35.zip
export Z3_HOME="$PWD/z3-4.13.3-x64-glibc-2.35"
```

## 3. Export the native library path
### Linux

```bash
export LD_LIBRARY_PATH="$Z3_HOME/bin${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
```

## 4. Build the compiler suite

In the repository root run:
```bash
cd compiler
mx build
```

## 5. Enable the SMT checker

The checker is disabled by default and only activates when both:

1. JVM assertions are enabled (`-ea`), and
2. the `VerifyCanonicalizationWithSMT` option is set.

Pass both flags when running tests or invoking the compiler:

```bash
mx unittest -ea -Djdk.graal.VerifyCanonicalizationWithSMT=true --enable-native-access=jdk.graal.compiler SmtCanonicalizationErrorTest
```

Or, for a full unit test run:

```bash
mx gate --tags=test --extra-vm-argument="-ea -Djdk.graal.VerifyCanonicalizationWithSMT=true --enable-native-access=jdk.graal.compiler"
```

[![GraalVM](.github/assets/logo_320x64.svg)][website]

[![GraalVM downloads][badge-dl]][downloads] [![GraalVM docs][badge-docs]][docs] [![GraalVM on Slack][badge-slack]][slack] [![GraalVM Gate][badge-gate]][gate] [![License][badge-license]](#license) [![GraalVM on LinkedIn][badge-linkedin]][social-linkedin] [![GraalVM on X][badge-x]][social-x] [![GraalVM on Bluesky][badge-bluesky]][social-bluesky] [![GraalVM on Medium][badge-medium]][social-medium] [![GraalVM on YouTube][badge-yt]][social-youtube]

GraalVM compiles applications ahead of time into standalone binaries that start instantly, provide peak performance with no warmup, and use fewer resources.
You can use GraalVM just like any other Java Development Kit in your IDE.

The project website at [https://www.graalvm.org/][website] describes how to [get started][getting-started], how to [stay connected][community], and how to [contribute][contributors].

## Documentation

Please refer to the [GraalVM website for documentation][docs].
You can find most of the documentation sources in the [_docs/_](docs/) directory in the same hierarchy as displayed on the website.
Additional documentation including developer instructions for individual components can be found in corresponding _docs/_ sub-directories.
The documentation for the Truffle framework, for example, is in [_truffle/docs/_](truffle/docs/).
This also applies to languages, tools, and other components maintained in [related repositories](#related-repositories).
Instructions for building GraalVM from source are in [`vm/README.md`](vm/README.md).

## Get Support

* Open a [GitHub issue][issues] for reproducible bugs and concrete feature requests.
* Use [GitHub Discussions](https://github.com/oracle/graal/discussions) or join the [GraalVM Slack][slack] for questions, design discussion, and general community support.
* Read the [contribution guide](CONTRIBUTING.md) before opening a pull request.
* Report security vulnerabilities according to the [Reporting Vulnerabilities guide][reporting-vulnerabilities], not in public issues.

## Repository Structure

This source repository is the main repository for GraalVM and includes the following components:

Directory | Description
------------ | -------------
[`.github/`](.github/) | Configuration files for GitHub issues, workflows, ….
[`ci/`](ci/) | Configuration files for internal CI/CD infrastructure.
[`ci_includes/`](ci_includes/) | Configuration include files for internal CI/CD infrastructure.
[`compiler/`](compiler/) | [Graal compiler][reference-compiler], a modern, versatile compiler written in Java.
[`espresso-compiler-stub/`](espresso-compiler-stub/) | A dummy GraalJVMCICompiler implementation for Espresso.
[`espresso-shared/`](espresso-shared/) | Espresso shared code for runtime class loading.
[`espresso/`](espresso/) | [Espresso][java-on-truffle], a meta-circular Java bytecode interpreter for the GraalVM.
[`regex/`](regex/) | TRegex, a regular expression engine for other GraalVM languages.
[`sdk/`](sdk/) | [GraalVM SDK][graalvm-sdk], long-term supported APIs of GraalVM.
[`substratevm/`](substratevm/) | Framework for ahead-of-time (AOT) compilation with [Native Image][native-image].
[`sulong/`](sulong/) | [Sulong][reference-sulong], an engine for running LLVM bitcode on GraalVM.
[`tools/`](tools/) | Tools for Graal Languages implemented with the instrumentation framework.
[`truffle/`](truffle/) | [Language implementation framework][truffle] for creating languages and tools.
[`visualizer/`](visualizer/) | [Ideal Graph Visualizer (IGV)][igv], a tool for analyzing Graal compiler graphs.
[`vm/`](vm/) | Components for building GraalVM distributions.
[`wasm/`](wasm/) | [GraalWasm][reference-graalwasm], an engine for running WebAssembly programs on GraalVM.
[`web-image/`](web-image/) | [Web Image](web-image/), an experimental WebAssembly backend for Native Image.

## Related Repositories

GraalVM provides additional languages, tools, and other components developed in related repositories. These are:

Name         | Description
------------ | -------------
[GraalJS] | Implementation of JavaScript and Node.js.
[GraalPy] | Implementation of the Python language.
[graalvm-reachability-metadata] | Reachability metadata for open-source libraries.
[Native Build Tools][native-build-tools] | Build tool plugins for GraalVM Native Image.
[setup-graalvm] | GitHub Action for GraalVM.
[SimpleLanguage] | A simple example language built with the Truffle framework.
[SimpleTool] | A simple example tool built with the Truffle framework.

## Examples and Tutorials

Explore practical examples, deep-dive workshops, and language-specific demos for working with GraalVM.

Name         | Description
------------ | -------------
[GraalVM Demos][graalvm-demos] | Example applications highlighting GraalVM key features and best practices.
[GraalVM Workshops and Tutorials][graalvm-workshops] | Workshops and tutorials to help you learn and apply GraalVM tools and capabilities.
[Graal Languages - Demos and Guides][graal-languages-demos] | Demo applications and guides for GraalJS, GraalPy, GraalWasm, and other Graal Languages.

## License

GraalVM Community Edition is open source and distributed under [version 2 of the GNU General Public License with the “Classpath” Exception](LICENSE), which are the same terms as for Java. The licenses of the individual GraalVM components are generally derivative of the license of a particular language (see the table below).

Component(s) | License
------------ | -------------
[Espresso](espresso/LICENSE), [Ideal Graph Visualizer](visualizer/LICENSE), [Web Image](web-image/LICENSE) | GPL 2
[GraalVM Compiler](compiler/LICENSE.md), [SubstrateVM](substratevm/LICENSE), [Tools](tools/LICENSE), [VM](vm/LICENSE_GRAALVM_CE) | GPL 2 with Classpath Exception
[GraalVM SDK](sdk/LICENSE.md), [GraalWasm](wasm/LICENSE), [Truffle Framework](truffle/LICENSE.md), [TRegex](regex/LICENSE.md) | Universal Permissive License
[Sulong](sulong/LICENSE) | 3-clause BSD


[badge-bluesky]: https://img.shields.io/badge/-grey?logo=bluesky&logoColor=f5f5f5
[badge-dl]: https://img.shields.io/badge/download-latest-blue
[badge-docs]: https://img.shields.io/badge/docs-read-active
[badge-gate]: https://img.shields.io/github/actions/workflow/status/oracle/graal/main.yml
[badge-license]: https://img.shields.io/badge/license-GPLv2+CE-active
[badge-linkedin]: https://custom-icon-badges.demolab.com/badge/-555?logo=linkedin-white&logoColor=fff
[badge-medium]: https://img.shields.io/badge/-grey?logo=medium
[badge-slack]: https://img.shields.io/badge/Slack-join-active
[badge-x]: https://img.shields.io/badge/-grey?logo=X
[badge-yt]: https://img.shields.io/badge/-grey?logo=youtube
[community]: https://www.graalvm.org/community/
[contributors]: https://www.graalvm.org/community/contributors/
[docs]: https://www.graalvm.org/latest/docs/
[downloads]: https://www.graalvm.org/downloads/
[gate]: https://github.com/oracle/graal/actions/workflows/main.yml
[getting-started]: https://www.graalvm.org/latest/docs/getting-started/
[graal-languages-demos]: https://github.com/graalvm/graal-languages-demos/
[graaljs]: https://github.com/oracle/graaljs
[graalpy]: https://github.com/oracle/graalpython
[graalvm-demos]: https://github.com/graalvm/graalvm-demos
[graalvm-reachability-metadata]: https://github.com/oracle/graalvm-reachability-metadata
[graalvm-sdk]: https://www.graalvm.org/sdk/javadoc/
[graalvm-workshops]: https://github.com/graalvm/workshops
[igv]: https://www.graalvm.org/latest/tools/igv/
[issues]: https://github.com/oracle/graal/issues
[java-on-truffle]: https://www.graalvm.org/latest/reference-manual/java-on-truffle/
[native-build-tools]: https://github.com/graalvm/native-build-tools
[native-image]: https://www.graalvm.org/native-image/
[reference-compiler]: https://www.graalvm.org/latest/reference-manual/java/compiler/
[reference-graalwasm]: https://www.graalvm.org/latest/reference-manual/wasm/
[reference-sulong]: https://www.graalvm.org/latest/reference-manual/llvm/
[reporting-vulnerabilities]: https://www.oracle.com/corporate/security-practices/assurance/vulnerability/reporting.html
[setup-graalvm]: https://github.com/graalvm/setup-graalvm
[simplelanguage]: https://github.com/graalvm/simplelanguage
[simpletool]: https://github.com/graalvm/simpletool
[slack]: https://www.graalvm.org/slack-invitation/
[social-bluesky]: https://bsky.app/profile/graalvm.org
[social-linkedin]: https://www.linkedin.com/company/graalvm/
[social-medium]: https://medium.com/graalvm
[social-x]: https://x.com/graalvm
[social-youtube]: https://www.youtube.com/graalvm
[truffle]: https://www.graalvm.org/graalvm-as-a-platform/language-implementation-framework/
[website]: https://www.graalvm.org/

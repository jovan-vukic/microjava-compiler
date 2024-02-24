<div id="top"></div>

<!-- PROJECT [othneildrew] SHIELDS -->

<!-- PROJECT LOGO -->
<br />
<div align="center">
  <h2 align="center">MicroJava Compiler - Jovan Vukić</h2>

  <p align="center">
    This project is a compiler for the MicroJava language. It translates syntactically and semantically correct programs into MicroJava bytecode for execution on MicroJava virtual machines. It includes lexical, syntactic, and semantic analysis, as well as code generation functionalities.
    <br />
    <a href="https://github.com/jovan-vukic/microjava-compiler"><strong>Explore the project »</strong></a>
    <br />
    <br />
    <a href="https://github.com/jovan-vukic/microjava-compiler/issues">Report Bug</a>
    ·
    <a href="https://github.com/jovan-vukic/microjava-compiler/issues">Request Feature</a>
  </p>
</div>

<!-- TABLE OF CONTENTS -->
<details>
  <summary>Table of Contents</summary>
  <ol>
    <li>
      <a href="#about-the-project">About The Project</a>
    </li>
    <li>
      <a href="#getting-started">Getting Started</a>
      <ul>
        <li><a href="#prerequisites">Prerequisites</a></li>
        <li><a href="#installation">Installation</a></li>
        <li><a href="#expected-output">Expected Output</a></li>
      </ul>
    </li>
    <li><a href="#usage">Usage</a></li>
    <li><a href="#contributing">Contributing</a></li>
    <li><a href="#license">License</a></li>
    <li><a href="#contact">Contact</a></li>
    <li><a href="#acknowledgments">Acknowledgments</a></li>
  </ol>
</details>

<!-- ABOUT THE PROJECT -->
## About The Project

The aim of the project is to implement a compiler for the MicroJava programming language.
The compiler enables the translation of syntactically and semantically correct MicroJava programs into MicroJava bytecode, which runs on the MicroJava virtual machine.
Syntactically and semantically correct MicroJava programs are defined by [the language specification](docs/language%20specification.pdf). The compiler has four basic functionalities: lexical analysis, syntactic analysis, semantic analysis, and code generation.

The lexical analyzer should recognize language lexemes and return a set of tokens extracted from the source code, which are further examined within the syntactic analysis.
If a lexical error is detected during lexical analysis, an appropriate message should be printed to the output.

The syntactic analyzer is tasked with determining whether the extracted tokens from the source code of the program can form grammatically correct sentences.
After parsing syntactically correct MicroJava programs, it is necessary to inform the user about the success of parsing. If the source code has syntax errors, it is necessary to issue a suitable explanation of the detected syntax error, perform recovery, and continue parsing.

The semantic analyzer is formed based on the abstract syntax tree that resulted from syntactic analysis. Semantic analysis is conducted by implementing methods for visiting nodes of the abstract syntax tree. The tree is formed based on the grammar implemented in the previous phase. If the source code has semantic errors, an appropriate message about the detected semantic error should be displayed.

The code generator translates syntactically and semantically correct programs into executable form for the selected MicroJava VM runtime environment. Code generation is implemented in a similar way to semantic analysis, by implementing methods that visit nodes.

This project considers programs that only contain the main function (method), which do not use loops or branches, nor support the creation of classes.
Examples of programs written in this Java-like language are provided in the files [`program.mj`](test/program.mj) and [`test301.mj`](test/test301.mj).

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- GETTING STARTED -->
## Getting Started

To get a local copy up and running follow these simple steps.

### Prerequisites

List of things you need to do:

* Download and launch your favorite Java IDE.

### Installation

Setup & execution:

1. Clone the repo:
   ```sh
   git clone https://github.com/jovan-vukic/microjava-compiler.git
   ```
2. Set the Java version in the project to 1.8 for the best compatibility.
3. Mark all folders present in this repository (except `docs` and `lib`) as Sources Root folders (for some IDEs this step may not be necessary)
4. Execute the `compile` target from the `build.xml` file present in the project folder, which performs lexical, syntactic, and semantic analysis.
5. Run the [`Compiler.java`](test/rs/ac/bg/etf/pp1/Compiler.java) class, which will perform code generation and create the object file `test/test301.obj`. Before creating the object file, pass the following command-line arguments to the `Compiler.java`:
   ```sh
   test/test301.mj test/test301.obj
   ```
6. After creating the object file, execute it by running the `runTest301` or `debugTest301` targets from the `build.xml`. They utilize the MicroJava VM implementation provided in the `lib/mj-runtime-1.1.jar` file.
7. Consider changing the input value for the program `test/test301.mj`, which is located in the file `test/test301_input.txt`. By doing so or by modifying the code itself, it is possible to obtain different output values. The same test can be run for the file `test/program.mj`.

### Expected Output

```sh
9    6    6    true    b    c  -28
```

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- USAGE EXAMPLES -->
## Usage

Since the execution of lexical, syntactic, and semantic analysis, as well as code generation, relies on the use of tools in the `lib` directory, executing the `compile` target from the `build.xml` file can also be done via the command line, as shown below.

**Parser generation**

A parser is generated based on the [CUP tool](lib/cup_v10k.jar), where for each grammar production, a corresponding class is generated in the `ast` folder in the `src/rs/ac/bg/etf/pp1` folder, enabling traversal of the syntax tree in subsequent compiler phases.
The specification of the language grammar provided as input to this tool is defined in the `spec/mjparser.cup` file.

```sh
java -jar lib/cup_v10k.jar -destdir src/rs/ac/bg/etf/pp1/ -ast src.rs.ac.bg.etf.pp1.ast -parser MJParser -buildtree spec/mjparser.cup
```

After this, the `MJParser.java` class is generated in the `src/rs/ac/bg/etf/pp1` folder. It is necessary to change the package name to `rs.ac.bg.etf.pp1` in all files in the `ast` folder, as instructed in the `repackage` target in the `build.xml` file.

**Lexer generation**

By using the [JFlex tool](lib/JFlex.jar) with the lexer specification provided in the `spec/mjlexer.flex` file, a lexer class is generated into the file `YYlex.java`. Prior execution of the parser generation was necessary because it produces the `sym.java` class used in the mentioned specification.

```sh
java -jar lib/JFlex.jar -d src/rs/ac/bg/etf/pp1/ spec/mjlexer.flex
```

**Semantic analysis and code generation**

All of this is done through Java code by running the `Compiler.java` class, with `test/program.mj test/program.obj` passed to it as command-line arguments.

**Program execution**

The object file is executed using the MicroJava VM specified in the `lib\mj-runtime-1.1.jar` file, through the `Run` class.

```sh
java -cp lib\mj-runtime-1.1.jar rs.etf.pp1.mj.runtime.Run test\program.obj
```

It is also possible to disassemble the MicroJava code using the `disasm` class from the same file.

```sh
java -cp lib\mj-runtime-1.1.jar rs.etf.pp1.mj.runtime.disasm test\program.obj
```

**Expected output**
```sh
true    3
```

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Don't forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- LICENSE -->
## License

Distributed under the MIT License. See `LICENSE` for more information.

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- CONTACT -->
## Contact

Jovan - [@jovan-vukic](https://github.com/jovan-vukic)

Project Link: [https://github.com/jovan-vukic/microjava-compiler](https://github.com/jovan-vukic/microjava-compiler)

<p align="right">(<a href="#top">back to top</a>)</p>

<!-- ACKNOWLEDGMENTS -->
## Acknowledgments

This project was done as part of the course 'Compiler Construction 1' (13E114PP1) at the University of Belgrade, Faculty of Electrical Engineering.

Used resources:

* [The full specification of the project in Serbian language](./docs/project%20specification.pdf)
* [The MicroJava language specification in Serbian language](./docs/language%20specification.pdf)

<p align="right">(<a href="#top">back to top</a>)</p>

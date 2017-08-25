# Fn Java Functions Developer Kit (FDK)

This project adds support for writing functions in Java on the [Fn platform](https://github.com/fnproject/fn).


# Quick Start Tutorial

By following this step-by-step guide you will learn to create, run and deploy a simple app written in Java on Fn.

## Pre-requisites

Before you get started you will need the following things:

* The [Fn CLI](https://github.com/fnproject/cli) tool
* [Docker-ce 17.06+ installed locally](https://docs.docker.com/engine/installation/)
* A [Docker Hub](http://hub.docker.com) account

### Install the Fn CLI tool

To install the Fn CLI tool, just run the following:

```
curl -LSs https://raw.githubusercontent.com/fnproject/cli/master/install | sh
```

This will download a shell script and execute it. If the script asks for a password, that is because it invokes sudo.

### Log in to DockerHub

You will also need to be logged in to your Docker Hub account in order to deploy functions.

```shell
docker login
```

## Your first Function

### 1. Create your first Java Function:

```bash
$ mkdir hello-java-function && cd hello-java-function
$ fn init --runtime=java your_dockerhub_account/hello
Runtime: java
function boilerplate generated.
func.yaml created
```

This creates the boilerplate for a new Java Function based on Maven and Oracle Java 8. The `pom.xml` includes a dependency on the latest version of the Fn Java FDK that is useful for developing your Java functions.

Note that the `your_dockerhub_account/hello` name follows the format of a Docker image name. The Fn platform relies on docker images implementing functions and these will be deployed to a Docker registry. By default Docker Hub is used, hence the requirement for a Docker Hub account. You should replace `your_dockerhub_account` with your account name.

You can now import this project into your favourite IDE as normal.

### 2. Deep dive into your first Java Function:
We'll now take a look at what makes up our new Java Function. First, lets take a look at the `func.yaml`:

```bash
$ cat func.yaml
name: your_dockerhub_account/hello
version: 0.0.1
runtime: java
cmd: com.example.fn.HelloFunction::handleRequest
path: /hello
```

The `cmd` field determines which method is called when your funciton is invoked. In the generated Function, the `func.yaml` references `com.example.fn.HelloFunction::handleRequest`. Your functions will likely live in different classes, and this field should always point to the method to execute, with the following syntax:

```text
cmd: <fully qualified class name>::<method name>
```

For more information about the fields in `func.yaml`, refer to the [Fn platform documentation](https://github.com/fnproject/fn/blob/master/docs/function-file.md) about it.

Let's also have a brief look at the source: `src/main/java/com/example/fn/HelloFunction.java`:

```java
package com.example.fn;

public class HelloFunction {

    public String handleRequest(String input) {
        String name = (input == null || input.isEmpty()) ? "world"  : input;

        return "Hello, " + name + "!";
    }

}
```

The function takes some optional input and returns a greeting dependent on it.

### 3. Run your first Java Function:
You are now ready to run your Function locally using the Fn CLI tool.

```bash
$ fn run
Building image your_dockerhub_account/hello:0.0.1
Sending build context to Docker daemon  14.34kB
Step 1/11 : FROM maven:3.5-jdk-8-alpine as build-stage
 ---> 5435658a63ac
Step 2/11 : WORKDIR /function
 ---> 37340c5aa451

...

Step 5/11 : RUN mvn package dependency:copy-dependencies -DincludeScope=runtime -DskipTests=true -Dmdep.prependGroupId=true -DoutputDirectory=target --fail-never
---> Running in 58b3b1397ba2
[INFO] Scanning for projects...
Downloading: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-compiler-plugin/3.3/maven-compiler-plugin-3.3.pom
Downloaded: https://repo.maven.apache.org/maven2/org/apache/maven/plugins/maven-compiler-plugin/3.3/maven-compiler-plugin-3.3.pom (11 kB at 21 kB/s)

...

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 2.228 s
[INFO] Finished at: 2017-06-27T12:06:59Z
[INFO] Final Memory: 18M/143M
[INFO] ------------------------------------------------------------------------

...

Function your_dockerhub_account/hello:0.0.1 built successfully.
Hello, world!
```

The next time you run this, it will execute much quicker as your dependencies are cached. Try passing in some input this time:

```bash
$ echo -n "Universe" | fn run
...
Hello, Universe!
```

### 4. Testing your function
The Fn Java FDK includes a testing library providing useful [JUnit 4](http://junit.org/junit4/) rules to test functions. Look at the test in `src/test/java/com.example.fn/HelloFunctionTest.java`:

```java
package com.example.fn;

import com.fnproject.fn.testing.*;
import org.junit.*;

import static org.junit.Assert.*;

public class HelloFunctionTest {

    @Rule
    public final FnTestingRule testing = FnTestingRule.createDefault();

    @Test
    public void shouldReturnGreeting() {
        testing.givenEvent().enqueue();
        testing.thenRun(HelloFunction.class, "handleRequest");

        FnResult result = testing.getOnlyResult();
        assertEquals("Hello, world!", result.getBodyAsString());
    }

}
```

This test is very simple: it just enqueues an event with empty input and then runs the function, checking its output. Under the hood, the `FnTestingRule` is actually instantiating the same runtime wrapping function invocations, so that during the test your function will be invoked in exactly the same way that it would when deployed.

There is much more functionality to construct tests in the testing library. Testing functions is covered in more detail in [Testing Functions](docs/TestingFunctions.md).

### 5. Run using HTTP and the local Fn server
The previous example used `fn run` to run a function directly via docker, you can also  use the Fn server locally to test the deployment of your function and the HTTP calls to your functions.

Open another terminal and start the Fn server:

```bash
$ fn start
```

Then in your original terminal create an app:

```bash
$ fn apps create java-app
Successfully created app: java-app
```

Now deploy your Function using the `fn deploy` command. This will bump the function's version up, rebuild it, and push the image to the Docker registry, ready to be used in the function deployment. Finally it will create a route on the local Fn server, corresponding to your function.

```bash
$ fn deploy java-app
...
Bumped to version 0.0.2
Building image your_dockerhub_account/hello:0.0.2
Sending build context to Docker daemon  14.34kB

...

Successfully built bf2b7fa55520
Successfully tagged your_dockerhub_account/hello:0.0.2
Pushing to docker registry...
The push refers to a repository [docker.io/your_dockerhub_account/hello]
d641fa720e99: Pushed
9f961bc46650: Pushed
24972d67929a: Pushed
e18e511b41d6: Pushed
a8cf2f688ac8: Pushed
0.0.2: digest: sha256:9a585899aa5c705172f8a798169a86534048b55ec2f47851938103ffbe9cfba5 size: 1368
Updating route /hello using image your_dockerhub_account/hello:0.0.2...
```

Call the Function via the Fn CLI:

```bash
$ fn call java-app /hello
Hello, world!
```

You can also call the Function via curl:

```bash
$ curl http://localhost:8080/r/java-app/hello
Hello, world!
```

### 6. Something more interesting
The Fn Java FDK supports [flexible data binding](docs/DataBinding.md)  to make it easier for you to map function input and output data to Java objects.

Below is an example to of a Function that returns a POJO which will be serialized to JSON using Jackson:

```java
package com.example.fn;

public class PojoFunction {

    public static class Greeting {
        public final String name;
        public final String salutation;

        public Greeting(String salutation, String name) {
            this.salutation = salutation;
            this.name = name;
        }
    }

    public Greeting greet(String name) {
        if (name == null || name.isEmpty())
            name = "World";

        return new Greeting("Hello", name);
    }

}
```

Update your `func.yaml` to reference the new method:

```yaml
cmd: com.example.fn.PojoFunction::greet
```

Now run your new function:

```bash
$ fn run
...
{"name":"World","salutation":"Hello"}

$ echo -n Michael | fn run
...
{"name":"Michael","salutation":"Hello"}
```

## 7. Where do I go from here?

Learn more about the Fn Java FDK by reading the next tutorials in the series. Also check out the examples in the [`examples` directory](examples) for some functions demonstrating different features of the Fn Java FDK.

### Configuring your function

If you want to set up the state of your function object before the function is invoked, and to use external configuration variables that you can set up with the Fn tool, have a look at the [Function Configuration](docs/FunctionConfiguration.md) tutorial.

### Input and output bindings

You have the option of taking more control of how serialization and deserialization is performed by defining your own bindings.

See the [Data Binding](docs/DataBinding.md) tutorial for other out-of-the-box options and the [Extending Data Binding](docs/ExtendingDataBinding.md) tutorial for how to define and use your own bindings.

### Asynchronous workflows

Suppose you want to call out to some other function from yours - perhaps a function written in a different language, or even one maintained by a different team. Maybe you then want to do some processing on the result. Or even have your function interact asynchronously with a completely different system. Perhaps you also need to maintain some state for the duration of your function, but you don't want to pay for execution time while you're waiting for someone else to do their work.

If this sounds like you, then have a look at the [Cloud Threads quickstart](docs/CloudThreadsUserGuide.md).

# Contributing

*TBD*
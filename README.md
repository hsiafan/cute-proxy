
Capture Http/Https/WebSocket traffics via Http Proxy.

![screen shot](https://raw.githubusercontent.com/clearthesky/byproxy/master/images/screenshot_0.6.1.png)

## Download
[Pre Build Native Releases for macOS](https://github.com/clearthesky/byproxy/releases).

## Build By Yourself
Java8+ required To build this project. ByProxy use lombok to simplify java code, you may want to install lombok plugin to you ide, If you want to do some develop.

ByProxy use [javafx maven plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) to distribute.

Create executable java jar:

```sh
mvn jfx:jar
```

The jar file can be found at target/app/byproxy.jar. Use

```sh
java -jar target/app/byproxy.jar
```
to run the program.


Create platform native application: 

```sh
mvn jfx:native
```

you will find native applications under path target/native.


## Usage

ByProxy start http proxy and socks proxy at the same port, by default is 2080. 
Just press the start button to start the proxy, then set the application you want to capture http traffics to use the proxy.


## Https Traffics
ByProxy use mitm to capture https traffics, This need a self signed root certificate be installed and trusted.

When ByProxy start at the first time,it will create a new CA Root Certificate and private key, save to to keystore file $HOME/.ByProxy/ByProxy.p12.
Also, you can specify a another key store file you want to use. 

You need to import the CA Root Certificate into you operation system. The CA Root Cert can be got by export button.
Or, you can open one browser, and enter the address the proxy listened on, a certificate export web page will be displayed.

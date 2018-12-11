
Capture Http/Https/Http2/WebSocket traffics via Http Proxy.

![screen shot](https://raw.githubusercontent.com/hsiafan/monkey-proxy/master/images/screenshot_1.png)

## Download
[Pre Build Native Releases for macOS](https://github.com/hsiafan/monkey-proxy/releases).

## Build By Yourself
OpenJDK 11+ is required To build this project.

Create distribution and launch scripts using jlink:

```sh
mvn clean package -Pjlink
```

Create native application image with jpackager:

```sh
mvn clean package -Pjpackager
```

jpackager tool is required to generate native images, it should be include in jdk for OpenJdk 12+. For OpenJDK 11, you can download jpackager from:

```
http://download2.gluonhq.com/jpackager/11/jdk.packager-linux.zip
http://download2.gluonhq.com/jpackager/11/jdk.packager-osx.zip
http://download2.gluonhq.com/jpackager/11/jdk.packager-windows.zip
``` 

The packaged artifacts could be found in target/ directory.


## Usage

MonkeyProxy start http proxy and socks proxy at the same port, by default is 2080.
Just press the start button to start the proxy, then set the application you want to capture http traffics to use the proxy.


## Https Traffics
MonkeyProxy use mitm to capture https traffics, This need a self signed root certificate be installed and trusted.

When MonkeyProxy start at the first time,it will create a new CA Root Certificate and private key, save to to keystore file $HOME/.MonkeyProxy/MonkeyProxy.p12.
Also, you can specify a another key store file you want to use. 

You need to import the CA Root Certificate into you operation system. The CA Root Cert can be got by export button.
Or, you can open one browser, and enter the address the proxy listened on, a certificate export web page will be displayed.

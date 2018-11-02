package nablarch.fw.web.httpserver;

import nablarch.fw.web.HttpServer;
import nablarch.fw.web.HttpServerFactory;

public class HttpServerFactoryJetty6 implements HttpServerFactory {

    @Override
    public HttpServer create() {
        return new HttpServerJetty6();
    }
}

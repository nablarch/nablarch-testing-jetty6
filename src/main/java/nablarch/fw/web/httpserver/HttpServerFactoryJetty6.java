package nablarch.fw.web.httpserver;

import nablarch.fw.web.HttpServer;
import nablarch.fw.web.HttpServerFactory;

/**
 * Jetty6対応の{@link HttpServerFactory}実装クラス。
 *
 * @author Tsuyoshi Kawasaki
 */
public class HttpServerFactoryJetty6 implements HttpServerFactory {

    @Override
    public HttpServer create() {
        return new HttpServerJetty6();
    }
}

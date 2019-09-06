package nablarch.fw.web.httpserver;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.HttpServer;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.ResourceLocator;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.LocalConnector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.ResourceCollection;

import javax.servlet.Filter;
import java.io.File;
import java.util.List;

/**
 * Jetty6対応の{@link HttpServer}サブクラス。
 *
 * @author Tsuyoshi Kawasaki
 */
@Published(tag = "architect")
public class HttpServerJetty6 extends HttpServer {

    /** アプリケーションサーバの実体 */
    private Server jetty = null;

    /** 自動テスト実行用コネクター */
    private LocalConnector localConnector = null;

    /**
     * サーバを起動する。
     * <pre>
     * サーバスレッドを生成し、port()メソッドで指定されたポート番号上の
     * HTTPリクエストに対して処理を行う。
     * </pre>
     *
     * @return このオブジェクト自体
     */
    public HttpServerJetty6 start() {
        Connector conn = new SocketConnector();
        conn.setPort(getPort());
        initialize(conn);
        try {
            jetty.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    /**
     * このサーバをテストモードで起動する。
     * @return このオブジェクト自体
     */
    public HttpServerJetty6 startLocal() {
        localConnector = new LocalConnector();
        initialize(localConnector);
        try {
            jetty.start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * サーバスレッドが終了するまでカレントスレッドをwaitさせる。
     *
     * @return このオブジェクト自体
     */
    public HttpServer join() {
        try {
            jetty.join();

        } catch (InterruptedException e) {
            // カレントスレッドに割り込み要求を行ってから抜ける。
            Thread.currentThread().interrupt();
        }
        return this;
    }

    /**
     * {@inheritDoc}
     * <pre>
     * このクラスの実装では、
     * 引数のHTTPリクエストオブジェクトをHTTPメッセージにシリアライズし、
     * ローカルコネクションに送信する。
     * 内蔵アプリケーションサーバでの処理後、返信されたHTTPレスポンスメッセージを
     * HTTPレスポンスオブジェクトにパースし、この関数の戻り値として返す。
     * また、HTTPダンプ出力が有効である場合、
     * そのレスポンスボディの内容を所定のディレクトリに出力する。
     * </pre>
     */
    public HttpResponse handle(HttpRequest req, ExecutionContext unused) {
        if (localConnector == null) {
            throw new RuntimeException(
                    "this server is not running on a local connector. "
                            + "you must call startLocal() method beforehand."
            );
        }
        localConnector.reopen();

        String host = req.getHost();
        if (host == null || host.isEmpty()) {
            ((MockHttpRequest) req).setHost("127.0.0.1");
        }

        try {
            byte[] rawReq = req.toString().getBytes();
            byte[] rawRes = localConnector.getResponses(
                    new ByteArrayBuffer(rawReq), false
            ).asArray();
            HttpResponse res = HttpResponse.parse(rawRes);
            if (isHttpDumpEnabled()) {
                dumpHttpMessage(req, res);
            }
            return res;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Jettyサーバインスタンスの初期化を行う。
     *
     * @param conn このサーバがacceptするコネクタ
     */
    private void initialize(Connector conn) {
        jetty = new Server();
        jetty.setConnectors(new Connector[]{conn});
        jetty.setSendServerVersion(false);
        deploy();
    }

    /**
     * 内部サーバにWARをデプロイする。
     * <pre>
     * エントリポイントサーブレットと、
     * {@link #setWarBasePath(String)}で指定されたパス上に存在するWARをデプロイする。
     * </pre>
     */
    private void deploy() {
        WebAppContext webApp = new WebAppContext();
        webApp.setContextPath(getServletContextPath());
        webApp.setBaseResource(toResourceCollection(getWarBasePaths()));
        webApp.setClassLoader(Thread.currentThread().getContextClassLoader());
        Filter webFrontController = getWebFrontController();
        webApp.addFilter(
                new FilterHolder(webFrontController)
                , "/*"
                , org.mortbay.jetty.Handler.REQUEST
        );
        File tmpDir = getTempDirectory();
        if (tmpDir != null) {
            webApp.setTempDirectory(tmpDir);
        }
        jetty.addHandler(webApp);
    }

    /**
     * {@link ResourceLocator}のリストを{@link ResourceCollection}に変換する。
     * @param warBasePaths 変換元のリスト
     * @return 変換後の {@link ResourceCollection}
     */
    private ResourceCollection toResourceCollection(List<ResourceLocator> warBasePaths) {
        String[] realPaths = new String[warBasePaths.size()];
        for (int i = 0; i < warBasePaths.size(); i++) {
            realPaths[i] = warBasePaths.get(i).getRealPath();
        }
        try {
            return new ResourceCollection(realPaths);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "invalid warBasePath. " + warBasePaths, e);
        }
    }


}

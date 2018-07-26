package piuk.blockchain.androidcore.data.shapeshift;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;

public final class ApiInterceptor implements Interceptor {

    private static Logger log = LoggerFactory.getLogger(ApiInterceptor.class);

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long startTime = System.nanoTime();

        String requestLog = String.format(
            "Sending request of type %s to %s with headers %s",
            request.method(),
            request.url(),
            request.headers());

        if (request.method().compareToIgnoreCase("post") == 0) {
            requestLog = "\n" + requestLog + "\n" + requestBodyToString(request.body());
        }

        log.info("-------------------------------------------------------------------------------");
        log.info("Request:" + "\n" + requestLog);

        Response response = chain.proceed(request);
        long endTime = System.nanoTime();

        String responseLog = String.format(
            Locale.ENGLISH,
            "Received response from %s in %.1fms%n%s",
            response.request().url(),
            (endTime - startTime) / 1e6d,
            response.headers());

        String bodyString = response.body().string();
        if (response.code() == 200) {
            log.info("Response: " + response.code() + "\n" + responseLog + "\n" + bodyString+ "\n");
        } else {
            log.error("Response: " + response.code() + "\n" + responseLog + "\n" + bodyString+ "\n");
        }
        log.info("-------------------------------------------------------------------------------");

        return response.newBuilder()
            .body(ResponseBody.create(response.body().contentType(), bodyString))
            .build();
    }

    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    private String requestBodyToString(final RequestBody request) {
        final Buffer buffer = new Buffer();
        try {
            if (request != null) {
                request.writeTo(buffer);
                return buffer.readUtf8();
            } else {
                return "";
            }
        } catch (final IOException e) {
            return "IOException reading request body";
        } finally {
            buffer.close();
        }
    }
}
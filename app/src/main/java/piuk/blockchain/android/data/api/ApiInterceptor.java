package piuk.blockchain.android.data.api;

import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

public class ApiInterceptor implements Interceptor {

    private static final String TAG = ApiInterceptor.class.getSimpleName();

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long startTime = System.nanoTime();

        String requestLog = String.format(
                "Sending request %s with headers %s%n%s",
                request.url(),
                chain.connection(),
                request.headers());

        if (request.method().compareToIgnoreCase("post") == 0) {
            requestLog = "\n" + requestLog + "\n" + requestBodyToString(request.body());
        }

        Log.d(TAG, "Request:" + "\n" + requestLog);

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
            Log.d(TAG, "Response:" + "\n" + responseLog + "\n" + bodyString);
        } else {
            Log.e(TAG, "Response:" + "\n" + responseLog + "\n" + bodyString);
        }

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
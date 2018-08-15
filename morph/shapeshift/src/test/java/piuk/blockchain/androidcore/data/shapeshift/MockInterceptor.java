package piuk.blockchain.androidcore.data.shapeshift;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class MockInterceptor implements Interceptor {

    private static Logger log = LoggerFactory.getLogger(MockInterceptor.class);

    private LinkedList<String> responseStringList;
    private LinkedList<Integer> responseCodeList;
    private boolean ioException = false;

    @Deprecated
    public void setResponseStringList(LinkedList<String> responseStringList) {
        this.responseStringList = responseStringList;
    }

    public void setResponseString(String response) {

        this.responseStringList = new LinkedList<>();
        this.responseStringList.add(response);
    }

    @Deprecated
    public void setResponseCodeList(LinkedList<Integer> responseCodeList) {
        this.responseCodeList = responseCodeList;
    }

    public void setResponseCode(int responseCode) {

        this.responseCodeList = new LinkedList<>();
        this.responseCodeList.add(responseCode);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        if (ioException)
            throw new IOException();

        String responseString = null;

        try {
            responseString = responseStringList.getFirst();
        } catch (NoSuchElementException e) {
            log.error("Missing mock response", e);
        }

        if (responseCodeList == null || responseCodeList.size() == 0) {
            responseCodeList = new LinkedList<>();
            responseCodeList.add(200);
        }

        int responseCode = responseCodeList.getFirst();

        Response response = new Response.Builder()
                .code(responseCode)
                .message(responseString)
                .request(chain.request())
                .protocol(Protocol.HTTP_1_0)
                .body(ResponseBody.create(MediaType.parse("application/json"), responseString.getBytes()))
                .addHeader("content-type", "application/json")
                .build();

        //Reset responses
        responseStringList.removeFirst();
        responseCodeList.removeFirst();
        ioException = false;

        return response;
    }
}

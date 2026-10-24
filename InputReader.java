import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;

class InputReader {

    InputStream in;
    boolean streamEnd = false;
    private byte[] requestByte = new byte[64];
    private String requestString = new String();
    private final int MAX_REQUEST_BYTES = 256;
    boolean isClient;

    InputReader(InputStream in, boolean isClient) {
        this.in = in;
        this.isClient = isClient;
    }

    String[] ReadLine() throws IOException {
        requestString = "";
        int total = 0;
        while (!requestString.endsWith("\r\n\r\n")) {
            int len = in.read(requestByte);
            if (len <= 0) throw new EOFException();

            total += len;
            if (total > MAX_REQUEST_BYTES) throw new IOException("Request abnormally long");
            requestString += new String(requestByte, 0, len);
        }

        // remove special characters for server
        if (!isClient)
            requestString = requestString.replaceAll("(\\r\\n)+$", "");

        return requestString.split(" ");
    }
}

import java.io.IOException;
import java.io.EOFException;
import java.io.InputStream;

class InputReader {

    private InputStream in;
    private byte[] requestByte = new byte[20];
    private String requestString = new String();
    private String messDelimiter = "\r\n\r\n";
    private final int MAX_REQUEST_BYTES = 256;
    private boolean isClient;
    private String nextRequest = "";

    InputReader(InputStream in, boolean isClient) {
        this.in = in;
        this.isClient = isClient;
    }

    String[] ReadLine() throws IOException {
        int total = 0;
        int len = 0;
        int index;
        requestString = nextRequest;
        while ((index = requestString.indexOf(messDelimiter)) == -1) {
            len = in.read(requestByte);
            if (len <= 0) throw new EOFException();
            
            total += len;
            if (total > MAX_REQUEST_BYTES) throw new IOException("Request abnormally long");
            requestString += new String(requestByte, 0, len);
        }

        int end = index + messDelimiter.length(); // take accound of the delimiter
        nextRequest = end == len ? "" : requestString.substring(end);

        return requestString.substring(0, isClient ? end : index).split(" ");
    }
}

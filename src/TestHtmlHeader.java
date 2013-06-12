import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;

public class TestHtmlHeader {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		teste();
	}

	public static void teste() {
		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod("http://201.76.53.61/DSMonitor/DSMonitor");
		// GetMethod getMethod = new GetMethod("http://localhost:8180/DSMonitor/DSMonitor");
		// Provide custom retry handler is necessary
		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler(3, false));
		int statusCode = 0;
		try {
			statusCode = httpClient.executeMethod(getMethod);
		} catch (Exception e) {
			System.err.println("Exception : " + e.getMessage());
		}
		if (statusCode != HttpStatus.SC_OK) {
			System.err.println("Method failed: " + getMethod.getStatusLine());
		}
		Header[] h = getMethod.getResponseHeaders();
		System.err.println("Length : " + h.length);
		for (int i = 0; i < h.length; i++) {
			System.err.println("> : " + h[i]);
		}
		Header header = getMethod.getResponseHeader("dsfail");
		System.err.println("Header : " + header.getValue());
		if (header.getValue().startsWith("true"))
			System.out.println("---> True");
		getMethod.releaseConnection();
	}

}

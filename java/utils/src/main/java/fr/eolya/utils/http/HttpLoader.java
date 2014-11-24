/*
 * Licensed to Eolya and Dominique Bejean under one
 * or more contributor license agreements. 
 * Eolya licenses this file to you under the 
 * Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package fr.eolya.utils.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DecompressingHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import fr.eolya.utils.Utils;

// TODO: use gzip compression 


/**
 * A wrapper class for Apache HttpClient 4.x
 */
public class HttpLoader {

	//private DefaultHttpClient client;
	private HttpClient client;
	private HttpGet get;
	private HttpResponse response;

	private String proxyHost;
	private int proxyPort;
	private String proxyExclude;
	private String proxyUserName;
	private String proxyPassword;
	private int connectionTimeOut;
	private int sockeTimeOut;
	private String userAgent;
	private Map<String, String> authBasicLogin;
	private Map<String, String> cookies;
	private boolean simulateHttps = false;

	private int responseStatusCode;
	private String responseReasonPhrase;

	private String condGetLastModified;
	private String condGetETag;
	private boolean followRedirect;

	private String redirection;

	private long maxContentLength;

	private HashMap<String,String> responseHeader;

	public int errorCode;
	public String errorMessage;
	
	private InputStream stream = null;

	public static final int LOAD_ERROR = -1;
	public static final int LOAD_SUCCESS = 0;
	public static final int LOAD_PAGEUNCHANGED = 1;
	public static final int LOAD_PAGEREDIRECTED = 2;

	public HttpLoader() {
		proxyHost = null;
		//proxyExclude = null;
		proxyUserName = null;
		connectionTimeOut = 5000;
		sockeTimeOut = 20000;
		cookies = null;
		authBasicLogin = null;
		followRedirect = false;
		maxContentLength = 0;
		responseHeader = null;
		maxContentLength = 0;
		errorCode = LOAD_ERROR;
		errorMessage = "";
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public void setProxyExclude(String proxyExclude) {
		this.proxyExclude = proxyExclude;
	}

	public void setProxyUserName(String proxyUserName) {
		this.proxyUserName = proxyUserName;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	public void setConnectionTimeOutt(int connectionTimeOut) {
		this.connectionTimeOut = connectionTimeOut;
	}

	public void setSockeTimeOut(int sockeTimeOut) {
		this.sockeTimeOut = sockeTimeOut;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public void setCookies(Map<String, String> cookies) {
		this.cookies = cookies;
	}
	
	public void setBasicLogin(Map<String, String> authBasicLogin) {
		this.authBasicLogin = authBasicLogin;
	}


	public void setCondGetLastModified(String condGetLastModified) {
		this.condGetLastModified = condGetLastModified;
	}

	public void setCondget_eTag(String condGetETag) {
		this.condGetETag = condGetETag;
	}

	public String getCondGetETag() {
		return condGetETag;
	}	

	public String getCondGetLastModified() {
		return condGetLastModified;
	}	

	public void setFollowRedirect(boolean followRedirect) {
		this.followRedirect = followRedirect;
	}

	public void setMaxContentLength (long maxContentLength) {
		this.maxContentLength = maxContentLength;
	}

	public void setSimulateHttps(boolean simulate) {
		this.simulateHttps = simulate;
	}

	public int getResponseStatusCode() {
		return responseStatusCode;
	}

	public String getResponseReasonPhrase() {
		return responseReasonPhrase;
	}

	public String getRedirectionLocation() {
		return redirection;
	}

	public String getContentType() {
		return getResponseHeader("Content-Type");
	}

	public String getContentEncoding() { 
		return getResponseHeader("Content-Encoding");
	}

	public String getCharSet() {
		String contentType=getContentType();
		if (contentType.toLowerCase().indexOf("charset=")!=-1) {
			return contentType.substring(contentType.toLowerCase().indexOf("charset=")+"charset=".length(), contentType.length());
		}
		return null;

	}	

	public int getContentLength() {
		try {
			return Integer.parseInt(getResponseHeader("Content-Length"));
		} catch(Exception e) {
			return 0;
		}
	}

	public int open(String url) throws IOException {
		if (simulateHttps) url = url.replace("https://", "http://");
		if (authBasicLogin!=null) url = HttpUtils.urlAddBasicAuthentication(url, authBasicLogin.get("login"), authBasicLogin.get("password"));

		try {
			close();

			// HttpClient
			client = getHttpClient();
			if (client == null) throw new IOException("HttpClient object creation failed");

			// HttpGet
			get = getHttpGet(url);
			if (get == null) throw new IOException("HttpGet object creation failed");

			// execute
			response = client.execute(get);
			responseStatusCode = response.getStatusLine().getStatusCode();
			responseReasonPhrase = response.getStatusLine().getReasonPhrase();
			responseHeader = getResponseHeader(response);

			if(responseStatusCode < 300) {
				// no error

				if (maxContentLength>0 && getResponseHeader("Content-Length")!=null) {
					long contentLength = Long.parseLong(getResponseHeader("Content-Length"));
					if (contentLength > maxContentLength)
						throw new IOException("Too large Content-Length");
				}

				// server gave us a document
				if (getResponseHeader("ETag")!=null) {
					condGetETag = getResponseHeader("ETag");
				}

				if (getResponseHeader("Last-Modified")!=null) {
					condGetLastModified = getResponseHeader("Last-Modified");
				}
				errorCode = LOAD_SUCCESS;
			}
			else {
				if (!"".equals(condGetETag) && !"".equals(condGetLastModified) && responseStatusCode == 304) {
					//304 = Not Modified
					// server didn't give us a document, no document update
					errorCode = LOAD_PAGEUNCHANGED;
					errorMessage = String.valueOf(responseStatusCode) + " - " + responseReasonPhrase;
					//close();
					return LOAD_PAGEUNCHANGED;
				}
				else {
					if (responseStatusCode == 301 || responseStatusCode == 302 || responseStatusCode == 303 || responseStatusCode == 307) {		
						// obtain redirect target
						Header locationHeader = response.getFirstHeader("location");
						redirection = "";
						if (locationHeader != null) {
							redirection = locationHeader.getValue();
						} else {
							// The response is invalid and did not provide the new location for
							// the resource.  Report an error or possibly handle the response
							// like a 404 Not Found error.
						}

						//close();
						errorCode = LOAD_PAGEREDIRECTED;
						errorMessage = "redirection";
					} else {
						// error reading 
						errorCode = LOAD_ERROR;
						errorMessage = responseReasonPhrase;
						//String message = String.valueOf(responseStatusCode) + " - " + responseReasonPhrase;
						//close();
						//throw new IOException(message);
					}
					return errorCode;
				}
			}
		}
		catch (HttpHostConnectException e) {
			errorCode = LOAD_ERROR;
			errorMessage = "Host connection error : " + e.getMessage();
			responseStatusCode = 503;
			responseReasonPhrase = "Service Unavailable (HttpHostConnectException)";
			//if (e.getMessage()==null) e.printStackTrace();
			//throw new IOException("Unkwnown host : " + e.getMessage());
		}
		catch (UnknownHostException e) {
			errorCode = LOAD_ERROR;
			errorMessage = "Unkwnown host : " + e.getMessage();
			responseStatusCode = 503;
			responseReasonPhrase = "Service Unavailable (UnknownHostException)";
			//if (e.getMessage()==null) e.printStackTrace();
			//throw new IOException("Unkwnown host : " + e.getMessage());
		}
		catch (ConnectTimeoutException e) {
			errorCode = LOAD_ERROR;
			errorMessage = "Connect timeout : " + e.getMessage();
			responseStatusCode = 408;
			responseReasonPhrase = "Request Timeout (ConnectTimeoutException)";
			//if (e.getMessage()==null) e.printStackTrace();
			//throw new IOException("Unkwnown host : " + e.getMessage());
		}
		catch (Exception e) {
			errorCode = LOAD_ERROR;
			responseStatusCode = 503; // Service Unavailable
			responseReasonPhrase = "Service Unavailable (Exception)";
			String statusMessage = e.getMessage();
			if (statusMessage.indexOf(" - ") > 0) {
				errorMessage = statusMessage.substring(statusMessage.indexOf(" - ") + 3).trim();
			}
			else {
				errorMessage = e.getMessage();
			}
			//if (e.getMessage()==null) e.printStackTrace();
			//throw new IOException(e.getMessage());
		}
		return errorCode;
	}

	public int getHeadStatusCode(String url) {
		if (simulateHttps) url = url.replace("https://", "http://");
		if (authBasicLogin!=null) url = HttpUtils.urlAddBasicAuthentication(url, authBasicLogin.get("login"), authBasicLogin.get("password"));

		try {
			
			//this.url = url;
			URI uri = new URI(url);		

			close();

			// HttpClient
			client = getHttpClient();
			if (client == null) throw new IOException("HttpClient object creation failed");

			// HttpGet
			HttpHead head = new HttpHead(uri);
			//if (head == null) throw new IOException("HttpHead object creation failed");

			// execute
			response = client.execute(head);
			return response.getStatusLine().getStatusCode();
		}
		catch (Exception e) {
			return -1;
		}
	}

	public void close() {
		if (get!=null && response!=null) {
			// Low level resources should be released before initiating a new request
			HttpEntity entity = response.getEntity();
			if (entity != null) get.abort();
		}
	}

	public String load() {
		return getResponseBody(response);
	}

	public InputStream getStream() {
		if (stream==null) {
			try {
				HttpEntity resEntity = response.getEntity();
				//stream = new BufferedInputStream(resEntity.getContent());
				stream = resEntity.getContent();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return stream;
		
//		HttpEntity resEntity = response.getEntity();
//		if (resEntity != null)
//			try {
//				return new BufferedInputStream(resEntity.getContent());
//			} catch (IllegalStateException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		return null;
	}

	public int openRetry(String url, int maxRetry) {

		int ret = -1;
		int tryCount = 0;
		while (ret == LOAD_ERROR && tryCount < maxRetry) {
			try {
				tryCount++;
				ret = open(url);
			}
			catch (IOException e) {
				String msg = e.getMessage();
				if (tryCount == 1 && msg!=null && msg.toLowerCase().startsWith("invalid uri")) {
					url = HttpUtils.urlEncode(url);
				}
				else {
					Utils.sleep(tryCount * 1000);
				}
				//if (tryCount == maxRetry) return LOAD_ERROR;
			}
		}
		return ret;
	}

	/**
	 * It returns response body string
	 * 
	 * @param response
	 * @return
	 */
	private String getResponseBody(HttpResponse response) {
		HttpEntity resEntity = null;
		if (response != null) resEntity = response.getEntity();
		if (resEntity != null) {
			try {
				return EntityUtils.toString(resEntity);
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "";
	}

	/**
	 * @param 
	 * @return
	 */
	private HttpClient getHttpClient() {
		try {
			// ClientConnectionManager
			SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy() {
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}
			}, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
			registry.register(new Scheme("https", 443, sf));

			ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);

			// Params
			HttpParams httpParams = getHttpParams();

			// DefaultHttpClient
			DefaultHttpClient httpClient = new DefaultHttpClient(ccm, httpParams);
			
			// Proxy
			if (StringUtils.isNotEmpty(proxyHost)) {
				if (StringUtils.isNotEmpty(proxyUserName) && StringUtils.isNotEmpty(proxyPassword)) {
					httpClient.getCredentialsProvider().setCredentials(
						    new AuthScope(proxyHost,Integer.valueOf(proxyPort)),
						    new UsernamePasswordCredentials(proxyUserName, proxyPassword));
				}
				HttpHost proxy = new HttpHost(proxyHost,Integer.valueOf(proxyPort));
				httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,proxy);
			} else {
				httpClient.getParams().removeParameter(ConnRoutePNames.DEFAULT_PROXY);
			}

			// Cookies
			if (cookies!=null) {
				CookieStore cookieStore = new BasicCookieStore(); 
				Iterator<Entry<String, String>> it = cookies.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> pairs = (Map.Entry<String, String>)it.next();
					BasicClientCookie cookie = new BasicClientCookie(pairs.getKey(), pairs.getValue());
					//cookie.setDomain("your domain");
					cookie.setPath("/");
					cookieStore.addCookie(cookie); 
				}
				httpClient.setCookieStore(cookieStore); 		
			}

			return new DecompressingHttpClient(httpClient);
		} catch (Exception e) {
			return new DecompressingHttpClient(new DefaultHttpClient());
		}
	} 

	private HttpGet getHttpGet(String url) {
		if (simulateHttps) url = url.replace("https://", "http://");
		URI uri;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e1) {
			e1.printStackTrace();
			return null;
		}
		HttpGet httpGet = new HttpGet(uri);
		// Conditional Get
		if (!StringUtils.isEmpty(condGetLastModified) && !StringUtils.isEmpty(condGetETag)) {
			httpGet.addHeader(new BasicHeader("If-None-Match",condGetETag));
			httpGet.addHeader(new BasicHeader("If-Modified-Since",condGetLastModified));
			//httpGet.addHeader(new BasicHeader("Date", DateUtils.formatDate(new Date()));
		}
		return httpGet;
	}

	private HttpParams getHttpParams() {
		HttpParams httpParams = new BasicHttpParams();

		// connection
		HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeOut);
		HttpConnectionParams.setSoTimeout(httpParams, sockeTimeOut);

//		// proxy
//		if (!StringUtils.isEmpty(proxyHost)) {
//			HttpHost proxy = new HttpHost(proxyHost, proxyPort);
//			httpParams.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
//			// TODO: add proxy exclude support
//		}  

		// protocol
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(httpParams, "utf-8");

		// user-agent
		if (!StringUtils.isEmpty(userAgent)) {
			httpParams.setParameter(CoreProtocolPNames.USER_AGENT, userAgent);
		}

		// redirect
		httpParams.setParameter("http.protocol.handle-redirects",followRedirect);

		return httpParams;
	}

	private HashMap<String,String> getResponseHeader(HttpResponse response) {
		if (response != null) {
			Header[] headers = response.getAllHeaders();
			return converHeaders2Map(headers);
		} else {
			return new HashMap<String,String>();
		}
	}

	private HashMap<String,String> converHeaders2Map(Header[] headers) {
		HashMap<String,String> hashMap = new HashMap<String,String>();
		for (Header header : headers) {
			hashMap.put(header.getName(), header.getValue());
		}
		return hashMap;
	}

	private String getResponseHeader(String headerName) {
		if (responseHeader==null) return null;
		String value = responseHeader.get(headerName);
		if (StringUtils.isEmpty(value)) return null;
		return value;
	}
	
    public static boolean isRss(String contentType, String rawPage) {
        if (contentType==null) return false;
        if (
                        contentType.toLowerCase().startsWith("application/xml") 
                        || contentType.toLowerCase().startsWith("application/rss+xml") 
                        || contentType.toLowerCase().startsWith("application/atom+xml") 
                        || contentType.toLowerCase().startsWith("application/rdf+xml") 
                        || contentType.toLowerCase().startsWith("text/xml")
                        ) {
            if (rawPage==null) return true;
            return isFeed(rawPage);
        }
        return false;
    }
  
    public static boolean isHtmlOrText(String contentType) {
        if (contentType==null) return false;
        return (contentType.toLowerCase().startsWith("text/plain") || isHtml(contentType));
    }
    
    public static boolean isHtml(String contentType) {
        if (contentType==null) return false;
        if (contentType.toLowerCase().startsWith("text/html") || contentType.toLowerCase().startsWith("application/x-shockwave-flash") || contentType.toLowerCase().startsWith("application/xhtml+xml")) return true;
        return false;
    }
       
    public static boolean isFeed(String rawPage) {
        try {
            XmlReader xmlReader = new XmlReader(new ByteArrayInputStream(rawPage.getBytes()));
            SyndFeedInput input = new SyndFeedInput();
            @SuppressWarnings("unused")
			SyndFeed feed = input.build(xmlReader);
            //SyndFeed feed = input.build(new InputSource(new ByteArrayInputStream(rawPage.getBytes())));
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    
    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}

/*
 * Created on Aug 7, 2003
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package org.kohsuke.jnt;

import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * {@link TrustManager} that trust all.
 * 
 * java.net has a bogus SSL certificate, and JDK SSL library doesn't like it
 * unless we override the trust manager.
 * See http://www.javaalmanac.com/egs/javax.net.ssl/TrustAll.html 
 * 
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class SSLTrustAllManager implements X509TrustManager {
	private SSLTrustAllManager() {}
	
	/**
	 * Install the all-trusting trust manager.
	 */
	public static void install() {
		try {
			TrustManager[] trustAllCerts = new TrustManager[]{new SSLTrustAllManager()};

			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

			new URL("https://ohmygod.xml.jp");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public X509Certificate[] getAcceptedIssuers() {
		return null;
	}
	public void checkClientTrusted(X509Certificate[] certs, String authType) {
	}
	public void checkServerTrusted(X509Certificate[] certs, String authType) {
	}

}

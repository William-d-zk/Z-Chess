package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import protocol.KnockKnockProtocol;

/**
 * @author garden
 * @create 8/11/18
 */
public class SSLClient
{
    public static void main(String[] args)
    {
        try {
            System.setProperty("javax.net.debug", "all");
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = "mqtt-test";
            InputStream inputStream = ClassLoader.getSystemClassLoader()
                                                 .getResourceAsStream("cert/client-certificate.p12");
            keyStore.load(inputStream, password.toCharArray());

            // TrustManagerFactory ()
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            String password2 = "mqtt-test";
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
            InputStream inputStream1 = ClassLoader.getSystemClassLoader()
                                                  .getResourceAsStream("cert/server-certificate.p12");
            trustStore.load(inputStream1, password2.toCharArray());
            trustManagerFactory.init(trustStore);
            X509TrustManager x509TrustManager = null;
            for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
                if (trustManager instanceof X509TrustManager) {
                    x509TrustManager = (X509TrustManager) trustManager;
                    break;
                }
            }

            if (x509TrustManager == null) throw new NullPointerException();

            // KeyManagerFactory ()
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
            keyManagerFactory.init(keyStore, password.toCharArray());
            X509KeyManager x509KeyManager = null;
            for (KeyManager keyManager : keyManagerFactory.getKeyManagers()) {
                if (keyManager instanceof X509KeyManager) {
                    x509KeyManager = (X509KeyManager) keyManager;
                    break;
                }
            }
            if (x509KeyManager == null) throw new NullPointerException();

            // set up the SSL Context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[] { x509KeyManager
            }, new TrustManager[] { x509TrustManager
            }, null);

            SSLSocketFactory socketFactory = sslContext.getSocketFactory();
            SSLSocket kkSocket = (SSLSocket) socketFactory.createSocket("127.0.0.1", 1888);
            kkSocket.setEnabledProtocols(new String[] { "TLSv1.2"
            });

            PrintWriter out = new PrintWriter(kkSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(kkSocket.getInputStream()));

            BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
            String fromServer;
            String fromUser;

            String outputLine;
            KnockKnockProtocol kkp = new KnockKnockProtocol();
            outputLine = kkp.processInput(null);
            out.println(outputLine);

            while ((fromServer = in.readLine()) != null) {
                System.out.println("Server: " + fromServer);
                if (fromServer.equals("Bye.")) break;

                fromUser = stdIn.readLine();
                if (fromUser != null) {
                    System.out.println("Client: " + fromUser);
                    out.println(fromUser);
                }
            }
        }
        catch (IOException |
               CertificateException |
               NoSuchAlgorithmException |
               UnrecoverableKeyException |
               KeyStoreException |
               KeyManagementException |
               NoSuchProviderException e)
        {
            e.printStackTrace();
        }

    }
}

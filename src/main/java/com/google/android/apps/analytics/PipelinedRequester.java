package com.google.android.apps.analytics;

import android.util.Log;
import java.io.IOException;
import java.net.Socket;
import org.apache.http.Header;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.params.BasicHttpParams;

class PipelinedRequester
{
  DefaultHttpClientConnection connection = new DefaultHttpClientConnection();
  Callbacks callbacks;
  int lastStatusCode;
  boolean canPipeline = true;
  HttpHost host;
  SocketFactory socketFactory;

  public PipelinedRequester(HttpHost paramHttpHost)
  {
    this(paramHttpHost, new PlainSocketFactory());
  }

  public PipelinedRequester(HttpHost paramHttpHost, SocketFactory paramSocketFactory)
  {
    this.host = paramHttpHost;
    this.socketFactory = paramSocketFactory;
  }

  public void installCallbacks(Callbacks paramCallbacks)
  {
    this.callbacks = paramCallbacks;
  }

  public void addRequest(HttpRequest paramHttpRequest)
    throws HttpException, IOException
  {
    maybeOpenConnection();
    this.connection.sendRequestHeader(paramHttpRequest);
  }

  public void sendRequests()
    throws IOException, HttpException
  {
    this.connection.flush();
    HttpConnectionMetrics localHttpConnectionMetrics = this.connection.getMetrics();
    while (localHttpConnectionMetrics.getResponseCount() < localHttpConnectionMetrics.getRequestCount())
    {
      HttpResponse localHttpResponse = this.connection.receiveResponseHeader();
      if (!localHttpResponse.getStatusLine().getProtocolVersion().greaterEquals(HttpVersion.HTTP_1_1))
      {
        this.callbacks.pipelineModeChanged(false);
        this.canPipeline = false;
      }
      Header[] arrayOfHeader1 = localHttpResponse.getHeaders("Connection");
      if (arrayOfHeader1 != null)
        for (Header localHeader : arrayOfHeader1)
        {
          if (!"close".equalsIgnoreCase(localHeader.getValue()))
            continue;
          this.callbacks.pipelineModeChanged(false);
          this.canPipeline = false;
        }
      this.lastStatusCode = localHttpResponse.getStatusLine().getStatusCode();
      if (this.lastStatusCode != 200)
      {
        this.callbacks.serverError(this.lastStatusCode);
        closeConnection();
        return;
      }
      this.connection.receiveResponseEntity(localHttpResponse);
      localHttpResponse.getEntity().consumeContent();
      this.callbacks.requestSent();
      if (GoogleAnalyticsTracker.getInstance().getDebug())
        Log.v("GoogleAnalyticsTracker", "HTTP Response Code: " + localHttpResponse.getStatusLine().getStatusCode());
      if (!this.canPipeline)
      {
        closeConnection();
        return;
      }
    }
  }

  public void finishedCurrentRequests()
  {
    closeConnection();
  }

  private void maybeOpenConnection()
    throws IOException
  {
    if ((this.connection == null) || (!this.connection.isOpen()))
    {
      BasicHttpParams localBasicHttpParams = new BasicHttpParams();
      Socket localSocket = this.socketFactory.createSocket();
      localSocket = this.socketFactory.connectSocket(localSocket, this.host.getHostName(), this.host.getPort(), null, 0, localBasicHttpParams);
      this.connection.bind(localSocket, localBasicHttpParams);
    }
  }

  private void closeConnection()
  {
    if ((this.connection != null) && (this.connection.isOpen()))
      try
      {
        this.connection.close();
      }
      catch (IOException localIOException)
      {
      }
  }

  static abstract interface Callbacks
  {
    public abstract void pipelineModeChanged(boolean paramBoolean);

    public abstract void serverError(int paramInt);

    public abstract void requestSent();
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.PipelinedRequester
 * JD-Core Version:    0.6.0
 */
package com.google.android.apps.analytics;

import android.os.Build;
import android.os.Build.VERSION;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Locale;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ParseException;
import org.apache.http.message.BasicHttpRequest;

class NetworkDispatcher
  implements Dispatcher
{
  private static final HttpHost GOOGLE_ANALYTICS_HOST = new HttpHost("www.google-analytics.com", 80);
  private static final String USER_AGENT_TEMPLATE = "%s/%s (Linux; U; Android %s; %s-%s; %s Build/%s)";
  private final String userAgent;
  private static final int MAX_EVENTS_PER_PIPELINE = 30;
  private static final int MAX_SEQUENTIAL_REQUESTS = 5;
  private static final long MIN_RETRY_INTERVAL = 2L;
  private DispatcherThread dispatcherThread;
  private boolean dryRun = false;

  public NetworkDispatcher()
  {
    this("GoogleAnalytics", "1.2");
  }

  public NetworkDispatcher(String paramString1, String paramString2)
  {
    Locale localLocale = Locale.getDefault();
    this.userAgent = String.format("%s/%s (Linux; U; Android %s; %s-%s; %s Build/%s)", new Object[] { paramString1, paramString2, Build.VERSION.RELEASE, localLocale.getLanguage() != null ? localLocale.getLanguage().toLowerCase() : "en", localLocale.getCountry() != null ? localLocale.getCountry().toLowerCase() : "", Build.MODEL, Build.ID });
  }

  public void init(Dispatcher.Callbacks paramCallbacks, String paramString)
  {
    stop();
    this.dispatcherThread = new DispatcherThread(paramCallbacks, paramString, this.userAgent, this, null);
    this.dispatcherThread.start();
  }

  public void init(Dispatcher.Callbacks paramCallbacks, PipelinedRequester paramPipelinedRequester, String paramString)
  {
    stop();
    this.dispatcherThread = new DispatcherThread(paramCallbacks, paramPipelinedRequester, paramString, this.userAgent, this, null);
    this.dispatcherThread.start();
  }

  public void dispatchEvents(Event[] paramArrayOfEvent)
  {
    if (this.dispatcherThread != null)
      this.dispatcherThread.dispatchEvents(paramArrayOfEvent);
  }

  public void setDryRun(boolean paramBoolean)
  {
    this.dryRun = paramBoolean;
  }

  public boolean isDryRun()
  {
    return this.dryRun;
  }

  public void waitForThreadLooper()
  {
    this.dispatcherThread.getLooper();
  }

  public void stop()
  {
    if ((this.dispatcherThread != null) && (this.dispatcherThread.getLooper() != null))
    {
      this.dispatcherThread.getLooper().quit();
      this.dispatcherThread = null;
    }
  }

  String getUserAgent()
  {
    return this.userAgent;
  }

  private static class DispatcherThread extends HandlerThread
  {
    private Handler handlerExecuteOnDispatcherThread;
    private final PipelinedRequester pipelinedRequester;
    private final String referrer;
    private final String userAgent;
    private int lastStatusCode;
    private int maxEventsPerRequest = 30;
    private long retryInterval;
    private AsyncDispatchTask currentTask = null;
    private final Dispatcher.Callbacks callbacks;
    private final RequesterCallbacks requesterCallBacks;
    private final NetworkDispatcher parent;

    private DispatcherThread(Dispatcher.Callbacks paramCallbacks, String paramString1, String paramString2, NetworkDispatcher paramNetworkDispatcher)
    {
      this(paramCallbacks, new PipelinedRequester(NetworkDispatcher.GOOGLE_ANALYTICS_HOST), paramString1, paramString2, paramNetworkDispatcher);
    }

    private DispatcherThread(Dispatcher.Callbacks paramCallbacks, PipelinedRequester paramPipelinedRequester, String paramString1, String paramString2, NetworkDispatcher paramNetworkDispatcher)
    {
      super("DispatcherThread");
      this.callbacks = paramCallbacks;
      this.referrer = paramString1;
      this.userAgent = paramString2;
      this.pipelinedRequester = paramPipelinedRequester;
      this.requesterCallBacks = new RequesterCallbacks();
      this.pipelinedRequester.installCallbacks(this.requesterCallBacks);
      this.parent = paramNetworkDispatcher;
    }

    protected void onLooperPrepared()
    {
      this.handlerExecuteOnDispatcherThread = new Handler();
    }

    public void dispatchEvents(Event[] paramArrayOfEvent)
    {
      if (this.handlerExecuteOnDispatcherThread != null)
        this.handlerExecuteOnDispatcherThread.post(new AsyncDispatchTask(paramArrayOfEvent));
    }

    private class RequesterCallbacks
      implements PipelinedRequester.Callbacks
    {
      private RequesterCallbacks()
      {
      }

      public void pipelineModeChanged(boolean paramBoolean)
      {
        if (paramBoolean)
          NetworkDispatcher.DispatcherThread.access$1002(NetworkDispatcher.DispatcherThread.this, 30);
        else
          NetworkDispatcher.DispatcherThread.access$1002(NetworkDispatcher.DispatcherThread.this, 1);
      }

      public void requestSent()
      {
        if (NetworkDispatcher.DispatcherThread.this.currentTask == null)
          return;
        Event localEvent = NetworkDispatcher.DispatcherThread.this.currentTask.removeNextEvent();
        if (localEvent != null)
          NetworkDispatcher.DispatcherThread.this.callbacks.eventDispatched(localEvent.eventId);
      }

      public void serverError(int paramInt)
      {
        NetworkDispatcher.DispatcherThread.access$502(NetworkDispatcher.DispatcherThread.this, paramInt);
      }
    }

    private class AsyncDispatchTask
      implements Runnable
    {
      private final LinkedList<Event> events = new LinkedList();

      public AsyncDispatchTask(Event[] arg2)
      {
        Collections.addAll(this.events, arg2);
      }

      public void run()
      {
        NetworkDispatcher.DispatcherThread.access$402(NetworkDispatcher.DispatcherThread.this, this);
        for (int i = 0; (i < 5) && (this.events.size() > 0); i++)
          try
          {
            long l = 0L;
            if ((NetworkDispatcher.DispatcherThread.this.lastStatusCode == 500) || (NetworkDispatcher.DispatcherThread.this.lastStatusCode == 503))
            {
              l = ()(Math.random() * NetworkDispatcher.DispatcherThread.this.retryInterval);
              if (NetworkDispatcher.DispatcherThread.this.retryInterval < 256L)
                NetworkDispatcher.DispatcherThread.access$630(NetworkDispatcher.DispatcherThread.this, 2L);
            }
            else
            {
              NetworkDispatcher.DispatcherThread.access$602(NetworkDispatcher.DispatcherThread.this, 2L);
            }
            Thread.sleep(l * 1000L);
            dispatchSomePendingEvents(NetworkDispatcher.this.isDryRun());
          }
          catch (InterruptedException localInterruptedException)
          {
            Log.w("GoogleAnalyticsTracker", "Couldn't sleep.", localInterruptedException);
            break;
          }
          catch (IOException localIOException)
          {
            Log.w("GoogleAnalyticsTracker", "Problem with socket or streams.", localIOException);
            break;
          }
          catch (HttpException localHttpException)
          {
            Log.w("GoogleAnalyticsTracker", "Problem with http streams.", localHttpException);
            break;
          }
        NetworkDispatcher.DispatcherThread.this.pipelinedRequester.finishedCurrentRequests();
        NetworkDispatcher.DispatcherThread.this.callbacks.dispatchFinished();
        NetworkDispatcher.DispatcherThread.access$402(NetworkDispatcher.DispatcherThread.this, null);
      }

      private void dispatchSomePendingEvents(boolean paramBoolean)
        throws IOException, ParseException, HttpException
      {
        if ((GoogleAnalyticsTracker.getInstance().getDebug()) && (paramBoolean))
          Log.v("GoogleAnalyticsTracker", "dispatching events in dry run mode");
        for (int i = 0; (i < this.events.size()) && (i < NetworkDispatcher.DispatcherThread.this.maxEventsPerRequest); i++)
        {
          Event localEvent = (Event)this.events.get(i);
          String str;
          if ("__##GOOGLEPAGEVIEW##__".equals(localEvent.category))
            str = NetworkRequestUtil.constructPageviewRequestPath(localEvent, NetworkDispatcher.DispatcherThread.this.referrer);
          else if ("__##GOOGLETRANSACTION##__".equals(localEvent.category))
            str = NetworkRequestUtil.constructTransactionRequestPath(localEvent, NetworkDispatcher.DispatcherThread.this.referrer);
          else if ("__##GOOGLEITEM##__".equals(localEvent.category))
            str = NetworkRequestUtil.constructItemRequestPath(localEvent, NetworkDispatcher.DispatcherThread.this.referrer);
          else
            str = NetworkRequestUtil.constructEventRequestPath(localEvent, NetworkDispatcher.DispatcherThread.this.referrer);
          BasicHttpRequest localBasicHttpRequest = new BasicHttpRequest("GET", str);
          localBasicHttpRequest.addHeader("Host", NetworkDispatcher.GOOGLE_ANALYTICS_HOST.getHostName());
          localBasicHttpRequest.addHeader("User-Agent", NetworkDispatcher.DispatcherThread.this.userAgent);
          if (GoogleAnalyticsTracker.getInstance().getDebug())
            Log.i("GoogleAnalyticsTracker", localBasicHttpRequest.getRequestLine().toString());
          if (paramBoolean)
            NetworkDispatcher.DispatcherThread.this.requesterCallBacks.requestSent();
          else
            NetworkDispatcher.DispatcherThread.this.pipelinedRequester.addRequest(localBasicHttpRequest);
        }
        if (!paramBoolean)
          NetworkDispatcher.DispatcherThread.this.pipelinedRequester.sendRequests();
      }

      public Event removeNextEvent()
      {
        return (Event)this.events.poll();
      }
    }
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.NetworkDispatcher
 * JD-Core Version:    0.6.0
 */
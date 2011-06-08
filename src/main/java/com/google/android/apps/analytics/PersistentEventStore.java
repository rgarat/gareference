package com.google.android.apps.analytics;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

class PersistentEventStore
  implements EventStore
{
  private static final int MAX_EVENTS = 1000;
  private static final String STORE_ID = "store_id";
  private static final String EVENT_ID = "event_id";
  private static final String SCREEN_WIDTH = "screen_width";
  private static final String SCREEN_HEIGHT = "screen_height";
  private static final String VALUE = "value";
  private static final String LABEL = "label";
  private static final String ACTION = "action";
  private static final String CATEGORY = "category";
  private static final String VISITS = "visits";
  private static final String TIMESTAMP_CURRENT = "timestamp_current";
  private static final String TIMESTAMP_PREVIOUS = "timestamp_previous";
  private static final String TIMESTAMP_FIRST = "timestamp_first";
  private static final String RANDOM_VAL = "random_val";
  private static final String ACCOUNT_ID = "account_id";
  private static final String USER_ID = "user_id";
  private static final String REFERRER = "referrer";
  private static final String CUSTOMVAR_ID = "cv_id";
  private static final String CUSTOMVAR_INDEX = "cv_index";
  private static final String CUSTOMVAR_NAME = "cv_name";
  private static final String CUSTOMVAR_VALUE = "cv_value";
  private static final String CUSTOMVAR_SCOPE = "cv_scope";
  private static final String CUSTOM_VARIABLE_COLUMN_TYPE = "CHAR(64) NOT NULL";
  private static final String TRANSACTION_ID = "tran_id";
  private static final String ORDER_ID = "order_id";
  private static final String STORE_NAME = "tran_storename";
  private static final String TOTAL_COST = "tran_totalcost";
  private static final String TOTAL_TAX = "tran_totaltax";
  private static final String SHIPPING_COST = "tran_shippingcost";
  private static final String ITEM_ID = "item_id";
  private static final String ITEM_SKU = "item_sku";
  private static final String ITEM_NAME = "item_name";
  private static final String ITEM_CATEGORY = "item_category";
  private static final String ITEM_PRICE = "item_price";
  private static final String ITEM_COUNT = "item_count";
  private static final String DATABASE_NAME = "google_analytics.db";
  private static final int DATABASE_VERSION = 3;
  private DataBaseHelper databaseHelper;
  private int storeId;
  private long timestampFirst;
  private long timestampPrevious;
  private long timestampCurrent;
  private int visits;
  private int numStoredEvents;
  private boolean sessionUpdated;
  private boolean useStoredVisitorVars;
  private SQLiteStatement compiledCountStatement = null;
  private static final String CREATE_EVENTS_TABLE = "CREATE TABLE events (" + String.format(" '%s' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,", new Object[] { "event_id" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "user_id" }) + String.format(" '%s' CHAR(256) NOT NULL,", new Object[] { "account_id" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "random_val" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "timestamp_first" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "timestamp_previous" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "timestamp_current" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "visits" }) + String.format(" '%s' CHAR(256) NOT NULL,", new Object[] { "category" }) + String.format(" '%s' CHAR(256) NOT NULL,", new Object[] { "action" }) + String.format(" '%s' CHAR(256), ", new Object[] { "label" }) + String.format(" '%s' INTEGER,", new Object[] { "value" }) + String.format(" '%s' INTEGER,", new Object[] { "screen_width" }) + String.format(" '%s' INTEGER);", new Object[] { "screen_height" });
  private static final String CREATE_SESSION_TABLE = "CREATE TABLE session (" + String.format(" '%s' INTEGER PRIMARY KEY,", new Object[] { "timestamp_first" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "timestamp_previous" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "timestamp_current" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "visits" }) + String.format(" '%s' INTEGER NOT NULL);", new Object[] { "store_id" });
  private static final String CREATE_INSTALL_REFERRER_TABLE = "CREATE TABLE install_referrer (referrer TEXT PRIMARY KEY NOT NULL);";
  private static final String CREATE_CUSTOM_VARIABLES_TABLE = "CREATE TABLE custom_variables (" + String.format(" '%s' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,", new Object[] { "cv_id" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "event_id" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "cv_index" }) + String.format(" '%s' CHAR(64) NOT NULL,", new Object[] { "cv_name" }) + String.format(" '%s' CHAR(64) NOT NULL,", new Object[] { "cv_value" }) + String.format(" '%s' INTEGER NOT NULL);", new Object[] { "cv_scope" });
  private static final String CREATE_CUSTOM_VAR_CACHE_TABLE = "CREATE TABLE custom_var_cache (" + String.format(" '%s' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,", new Object[] { "cv_id" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "event_id" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "cv_index" }) + String.format(" '%s' CHAR(64) NOT NULL,", new Object[] { "cv_name" }) + String.format(" '%s' CHAR(64) NOT NULL,", new Object[] { "cv_value" }) + String.format(" '%s' INTEGER NOT NULL);", new Object[] { "cv_scope" });
  private static final String CREATE_TRANSACTION_EVENTS_TABLE = "CREATE TABLE transaction_events (" + String.format(" '%s' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,", new Object[] { "tran_id" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "event_id" }) + String.format(" '%s' TEXT NOT NULL,", new Object[] { "order_id" }) + String.format(" '%s' TEXT,", new Object[] { "tran_storename" }) + String.format(" '%s' TEXT NOT NULL,", new Object[] { "tran_totalcost" }) + String.format(" '%s' TEXT,", new Object[] { "tran_totaltax" }) + String.format(" '%s' TEXT);", new Object[] { "tran_shippingcost" });
  private static final String CREATE_ITEM_EVENTS_TABLE = "CREATE TABLE item_events (" + String.format(" '%s' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,", new Object[] { "item_id" }) + String.format(" '%s' INTEGER NOT NULL,", new Object[] { "event_id" }) + String.format(" '%s' TEXT NOT NULL,", new Object[] { "order_id" }) + String.format(" '%s' TEXT NOT NULL,", new Object[] { "item_sku" }) + String.format(" '%s' TEXT,", new Object[] { "item_name" }) + String.format(" '%s' TEXT,", new Object[] { "item_category" }) + String.format(" '%s' TEXT NOT NULL,", new Object[] { "item_price" }) + String.format(" '%s' TEXT NOT NULL);", new Object[] { "item_count" });

  PersistentEventStore(DataBaseHelper paramDataBaseHelper)
  {
    this.databaseHelper = paramDataBaseHelper;
    try
    {
      paramDataBaseHelper.getWritableDatabase().close();
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
  }

  public void deleteEvent(long paramLong)
  {
    String str = "event_id=" + paramLong;
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getWritableDatabase();
      if (localSQLiteDatabase.delete("events", str, null) != 0)
      {
        this.numStoredEvents -= 1;
        localSQLiteDatabase.delete("custom_variables", str, null);
        localSQLiteDatabase.delete("transaction_events", str, null);
        localSQLiteDatabase.delete("item_events", str, null);
      }
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
  }

  public Event[] peekEvents()
  {
    return peekEvents(1000);
  }

  public Event[] peekEvents(int paramInt)
  {
    ArrayList localArrayList = new ArrayList();
    Cursor localCursor = null;
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getReadableDatabase();
      localCursor = localSQLiteDatabase.query("events", null, null, null, null, null, "event_id", Integer.toString(paramInt));
      while (localCursor.moveToNext())
      {
        Event localObject1 = new Event(localCursor.getLong(0), localCursor.getInt(1), localCursor.getString(2), localCursor.getInt(3), localCursor.getInt(4), localCursor.getInt(5), localCursor.getInt(6), localCursor.getInt(7), localCursor.getString(8), localCursor.getString(9), localCursor.getString(10), localCursor.getInt(11), localCursor.getInt(12), localCursor.getInt(13));
        long l = localCursor.getLong(localCursor.getColumnIndex("event_id"));
        if ("__##GOOGLETRANSACTION##__".equals(((Event)localObject1).category))
        {
          Transaction localObject2 = getTransaction(l);
          if (localObject2 == null)
            Log.w("GoogleAnalyticsTracker", "missing expected transaction for event " + l);
          ((Event)localObject1).setTransaction((Transaction)localObject2);
        }
        else if ("__##GOOGLEITEM##__".equals(((Event)localObject1).category))
        {
          Item localObject2 = getItem(l);
          if (localObject2 == null)
            Log.w("GoogleAnalyticsTracker", "missing expected item for event " + l);
          ((Event)localObject1).setItem((Item)localObject2);
        }
        else
        {
          CustomVariableBuffer localObject2 = getCustomVariables(l);
          ((Event)localObject1).setCustomVariableBuffer((CustomVariableBuffer)localObject2);
        }
        localArrayList.add(localObject1);
      }
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
      Event[] localObject1 = new Event[0];
      
      return localObject1;
    }
    finally
    {
      if (localCursor != null)
        localCursor.close();
    }
    return (Event[])localArrayList.toArray(new Event[localArrayList.size()]);
  }

  Transaction getTransaction(long paramLong)
  {
    Cursor localCursor = null;
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getReadableDatabase();
      localCursor = localSQLiteDatabase.query("transaction_events", null, "event_id=" + paramLong, null, null, null, null);
      if (localCursor.moveToFirst())
      {
        Transaction localTransaction = new Transaction.Builder(localCursor.getString(localCursor.getColumnIndex("order_id")), localCursor.getDouble(localCursor.getColumnIndex("tran_totalcost"))).setStoreName(localCursor.getString(localCursor.getColumnIndex("tran_storename"))).setTotalTax(localCursor.getDouble(localCursor.getColumnIndex("tran_totaltax"))).setShippingCost(localCursor.getDouble(localCursor.getColumnIndex("tran_shippingcost"))).build();
        return localTransaction;
      }
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
    finally
    {
      if (localCursor != null)
        localCursor.close();
    }
    return null;
  }

  Item getItem(long paramLong)
  {
    Cursor localCursor = null;
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getReadableDatabase();
      localCursor = localSQLiteDatabase.query("item_events", null, "event_id=" + paramLong, null, null, null, null);
      if (localCursor.moveToFirst())
      {
        Item localItem = new Item.Builder(localCursor.getString(localCursor.getColumnIndex("order_id")), localCursor.getString(localCursor.getColumnIndex("item_sku")), localCursor.getDouble(localCursor.getColumnIndex("item_price")), localCursor.getLong(localCursor.getColumnIndex("item_count"))).setItemName(localCursor.getString(localCursor.getColumnIndex("item_name"))).setItemCategory(localCursor.getString(localCursor.getColumnIndex("item_category"))).build();
        return localItem;
      }
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
    finally
    {
      if (localCursor != null)
        localCursor.close();
    }
    return null;
  }

  CustomVariableBuffer getCustomVariables(long paramLong)
  {
    Cursor localCursor = null;
    CustomVariableBuffer localCustomVariableBuffer = new CustomVariableBuffer();
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getReadableDatabase();
      localCursor = localSQLiteDatabase.query("custom_variables", null, "event_id=" + paramLong, null, null, null, null);
      while (localCursor.moveToNext())
      {
        CustomVariable localCustomVariable = new CustomVariable(localCursor.getInt(localCursor.getColumnIndex("cv_index")), localCursor.getString(localCursor.getColumnIndex("cv_name")), localCursor.getString(localCursor.getColumnIndex("cv_value")), localCursor.getInt(localCursor.getColumnIndex("cv_scope")));
        localCustomVariableBuffer.setCustomVariable(localCustomVariable);
      }
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
    finally
    {
      if (localCursor != null)
        localCursor.close();
    }
    return localCustomVariableBuffer;
  }

  public String getVisitorCustomVar(int paramInt)
  {
    Cursor localCursor = null;
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getReadableDatabase();
      localCursor = localSQLiteDatabase.query("custom_var_cache", null, "cv_scope = 1 AND cv_index = " + paramInt, null, null, null, null);
      String str1 = null;
      if (localCursor.getCount() > 0)
      {
        localCursor.moveToFirst();
        str1 = localCursor.getString(localCursor.getColumnIndex("cv_value"));
      }
      else
      {
        str1 = null;
      }
      localSQLiteDatabase.close();
      String str2 = str1;
      return str2;
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
      String str1 = null;
      return str1;
    }
    finally
    {
      if (localCursor != null)
        localCursor.close();
    }
  }

  public void putEvent(Event paramEvent)
  {
    if (this.numStoredEvents >= 1000)
    {
      Log.w("GoogleAnalyticsTracker", "Store full. Not storing last event.");
      return;
    }
    if (!this.sessionUpdated)
      storeUpdatedSession();
    SQLiteDatabase localSQLiteDatabase = null;
    try
    {
      localSQLiteDatabase = this.databaseHelper.getWritableDatabase();
      localSQLiteDatabase.beginTransaction();
      ContentValues localContentValues = new ContentValues();
      localContentValues.put("user_id", Integer.valueOf(paramEvent.userId));
      localContentValues.put("account_id", paramEvent.accountId);
      localContentValues.put("random_val", Integer.valueOf((int)(Math.random() * 2147483647.0D)));
      localContentValues.put("timestamp_first", Long.valueOf(this.timestampFirst));
      localContentValues.put("timestamp_previous", Long.valueOf(this.timestampPrevious));
      localContentValues.put("timestamp_current", Long.valueOf(this.timestampCurrent));
      localContentValues.put("visits", Integer.valueOf(this.visits));
      localContentValues.put("category", paramEvent.category);
      localContentValues.put("action", paramEvent.action);
      localContentValues.put("label", paramEvent.label);
      localContentValues.put("value", Integer.valueOf(paramEvent.value));
      localContentValues.put("screen_width", Integer.valueOf(paramEvent.screenWidth));
      localContentValues.put("screen_height", Integer.valueOf(paramEvent.screenHeight));
      long l1 = localSQLiteDatabase.insert("events", "event_id", localContentValues);
      if (l1 != -1L)
      {
        this.numStoredEvents += 1;
        Cursor localCursor = localSQLiteDatabase.query("events", new String[] { "event_id" }, null, null, null, null, "event_id DESC", null);
        localCursor.moveToPosition(0);
        long l2 = localCursor.getLong(0);
        localCursor.close();
        if (paramEvent.category.equals("__##GOOGLETRANSACTION##__"))
          putTransaction(paramEvent, l2);
        else if (paramEvent.category.equals("__##GOOGLEITEM##__"))
          putItem(paramEvent, l2);
        else
          putCustomVariables(paramEvent, l2);
        localSQLiteDatabase.setTransactionSuccessful();
      }
      else
      {
        Log.e("GoogleAnalyticsTracker", "Error when attempting to add event to database.");
      }
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
    finally
    {
      if (localSQLiteDatabase != null)
        localSQLiteDatabase.endTransaction();
    }
  }

  void putTransaction(Event paramEvent, long paramLong)
  {
    Transaction localTransaction = paramEvent.getTransaction();
    if (localTransaction == null)
    {
      Log.w("GoogleAnalyticsTracker", "missing transaction details for event " + paramLong);
      return;
    }
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getWritableDatabase();
      ContentValues localContentValues = new ContentValues();
      localContentValues.put("event_id", Long.valueOf(paramLong));
      localContentValues.put("order_id", localTransaction.getOrderId());
      localContentValues.put("tran_storename", localTransaction.getStoreName());
      localContentValues.put("tran_totalcost", localTransaction.getTotalCost() + "");
      localContentValues.put("tran_totaltax", localTransaction.getTotalTax() + "");
      localContentValues.put("tran_shippingcost", localTransaction.getShippingCost() + "");
      localSQLiteDatabase.insert("transaction_events", "event_id", localContentValues);
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
  }

  void putItem(Event paramEvent, long paramLong)
  {
    Item localItem = paramEvent.getItem();
    if (localItem == null)
    {
      Log.w("GoogleAnalyticsTracker", "missing item details for event " + paramLong);
      return;
    }
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getWritableDatabase();
      ContentValues localContentValues = new ContentValues();
      localContentValues.put("event_id", Long.valueOf(paramLong));
      localContentValues.put("order_id", localItem.getOrderId());
      localContentValues.put("item_sku", localItem.getItemSKU());
      localContentValues.put("item_name", localItem.getItemName());
      localContentValues.put("item_category", localItem.getItemCategory());
      localContentValues.put("item_price", localItem.getItemPrice() + "");
      localContentValues.put("item_count", localItem.getItemCount() + "");
      localSQLiteDatabase.insert("item_events", "event_id", localContentValues);
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
  }

  void putCustomVariables(Event paramEvent, long paramLong)
  {
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getWritableDatabase();
      CustomVariableBuffer localCustomVariableBuffer1 = paramEvent.getCustomVariableBuffer();
      Object localObject;
      if (this.useStoredVisitorVars)
      {
        if (localCustomVariableBuffer1 == null)
        {
          localCustomVariableBuffer1 = new CustomVariableBuffer();
          paramEvent.setCustomVariableBuffer(localCustomVariableBuffer1);
        }
        CustomVariableBuffer localCustomVariableBuffer2 = getVisitorVarBuffer();
        for (int j = 1; j <= 5; j++)
        {
          localObject = localCustomVariableBuffer2.getCustomVariableAt(j);
          CustomVariable localCustomVariable2 = localCustomVariableBuffer1.getCustomVariableAt(j);
          if ((localObject == null) || (localCustomVariable2 != null))
            continue;
          localCustomVariableBuffer1.setCustomVariable((CustomVariable)localObject);
        }
        this.useStoredVisitorVars = false;
      }
      if (localCustomVariableBuffer1 != null)
        for (int i = 1; i <= 5; i++)
        {
          if (localCustomVariableBuffer1.isIndexAvailable(i))
            continue;
          CustomVariable localCustomVariable1 = localCustomVariableBuffer1.getCustomVariableAt(i);
          localObject = new ContentValues();
          ((ContentValues)localObject).put("event_id", Long.valueOf(paramLong));
          ((ContentValues)localObject).put("cv_index", Integer.valueOf(localCustomVariable1.getIndex()));
          ((ContentValues)localObject).put("cv_name", localCustomVariable1.getName());
          ((ContentValues)localObject).put("cv_scope", Integer.valueOf(localCustomVariable1.getScope()));
          ((ContentValues)localObject).put("cv_value", localCustomVariable1.getValue());
          localSQLiteDatabase.insert("custom_variables", "event_id", (ContentValues)localObject);
          localSQLiteDatabase.update("custom_var_cache", (ContentValues)localObject, "cv_index=" + localCustomVariable1.getIndex(), null);
        }
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
  }

  CustomVariableBuffer getVisitorVarBuffer()
  {
    CustomVariableBuffer localCustomVariableBuffer = new CustomVariableBuffer();
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getReadableDatabase();
      Cursor localCursor = localSQLiteDatabase.query("custom_var_cache", null, "cv_scope=1", null, null, null, null);
      while (localCursor.moveToNext())
      {
        CustomVariable localCustomVariable = new CustomVariable(localCursor.getInt(localCursor.getColumnIndex("cv_index")), localCursor.getString(localCursor.getColumnIndex("cv_name")), localCursor.getString(localCursor.getColumnIndex("cv_value")), localCursor.getInt(localCursor.getColumnIndex("cv_scope")));
        localCustomVariableBuffer.setCustomVariable(localCustomVariable);
      }
      localCursor.close();
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
    return localCustomVariableBuffer;
  }

  public int getNumStoredEvents()
  {
    try
    {
      if (this.compiledCountStatement == null)
        this.compiledCountStatement = this.databaseHelper.getReadableDatabase().compileStatement("SELECT COUNT(*) from events");
      return (int)this.compiledCountStatement.simpleQueryForLong();
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
    return 0;
  }

  public int getStoreId()
  {
    return this.storeId;
  }

  public void startNewVisit()
  {
    this.sessionUpdated = false;
    this.useStoredVisitorVars = true;
    this.numStoredEvents = getNumStoredEvents();
    Cursor localCursor = null;
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getWritableDatabase();
      localCursor = localSQLiteDatabase.query("session", null, null, null, null, null, null);
      if (!localCursor.moveToFirst())
      {
        long l = System.currentTimeMillis() / 1000L;
        this.timestampFirst = l;
        this.timestampPrevious = l;
        this.timestampCurrent = l;
        this.visits = 1;
        this.storeId = (new SecureRandom().nextInt() & 0x7FFFFFFF);
        ContentValues localContentValues = new ContentValues();
        localContentValues.put("timestamp_first", Long.valueOf(this.timestampFirst));
        localContentValues.put("timestamp_previous", Long.valueOf(this.timestampPrevious));
        localContentValues.put("timestamp_current", Long.valueOf(this.timestampCurrent));
        localContentValues.put("visits", Integer.valueOf(this.visits));
        localContentValues.put("store_id", Integer.valueOf(this.storeId));
        localSQLiteDatabase.insert("session", "timestamp_first", localContentValues);
      }
      else
      {
        this.timestampFirst = localCursor.getLong(0);
        this.timestampPrevious = localCursor.getLong(2);
        this.timestampCurrent = (System.currentTimeMillis() / 1000L);
        this.visits = (localCursor.getInt(3) + 1);
        this.storeId = localCursor.getInt(4);
      }
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
    finally
    {
      if (localCursor != null)
        localCursor.close();
    }
  }

  void storeUpdatedSession()
  {
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getWritableDatabase();
      ContentValues localContentValues = new ContentValues();
      localContentValues.put("timestamp_previous", Long.valueOf(this.timestampPrevious));
      localContentValues.put("timestamp_current", Long.valueOf(this.timestampCurrent));
      localContentValues.put("visits", Integer.valueOf(this.visits));
      localSQLiteDatabase.update("session", localContentValues, "timestamp_first=?", new String[] { Long.toString(this.timestampFirst) });
      this.sessionUpdated = true;
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
  }

  public void setReferrer(String paramString)
  {
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getWritableDatabase();
      ContentValues localContentValues = new ContentValues();
      localContentValues.put("referrer", paramString);
      localSQLiteDatabase.insert("install_referrer", null, localContentValues);
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
  }

  public String getReferrer()
  {
    Cursor localCursor = null;
    try
    {
      SQLiteDatabase localSQLiteDatabase = this.databaseHelper.getReadableDatabase();
      localCursor = localSQLiteDatabase.query("install_referrer", new String[] { "referrer" }, null, null, null, null, null);
      String str1 = null;
      if (localCursor.moveToFirst())
        str1 = localCursor.getString(0);
      String str2 = str1;
      return str2;
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
      String str1 = null;
      return str1;
    }
    finally
    {
      if (localCursor != null)
        localCursor.close();
    }
  }

  static class DataBaseHelper extends SQLiteOpenHelper
  {
    private final int databaseVersion;

    public DataBaseHelper(Context paramContext)
    {
      this(paramContext, "google_analytics.db", 3);
    }

    public DataBaseHelper(Context paramContext, String paramString)
    {
      this(paramContext, paramString, 3);
    }

    DataBaseHelper(Context paramContext, String paramString, int paramInt)
    {
      super(paramContext,paramString,(CursorFactory)null,paramInt);
      this.databaseVersion = paramInt;
    }

    public void onCreate(SQLiteDatabase paramSQLiteDatabase)
    {
      paramSQLiteDatabase.execSQL("DROP TABLE IF EXISTS events;");
      paramSQLiteDatabase.execSQL(PersistentEventStore.CREATE_EVENTS_TABLE);
      paramSQLiteDatabase.execSQL("DROP TABLE IF EXISTS session;");
      paramSQLiteDatabase.execSQL(PersistentEventStore.CREATE_SESSION_TABLE);
      paramSQLiteDatabase.execSQL("DROP TABLE IF EXISTS install_referrer;");
      paramSQLiteDatabase.execSQL("CREATE TABLE install_referrer (referrer TEXT PRIMARY KEY NOT NULL);");
      if (this.databaseVersion > 1)
        createCustomVariableTables(paramSQLiteDatabase);
      if (this.databaseVersion > 2)
        createECommerceTables(paramSQLiteDatabase);
    }

    void createCustomVariableTables(SQLiteDatabase paramSQLiteDatabase)
    {
      paramSQLiteDatabase.execSQL("DROP TABLE IF EXISTS custom_variables;");
      paramSQLiteDatabase.execSQL(PersistentEventStore.CREATE_CUSTOM_VARIABLES_TABLE);
      paramSQLiteDatabase.execSQL("DROP TABLE IF EXISTS custom_var_cache;");
      paramSQLiteDatabase.execSQL(PersistentEventStore.CREATE_CUSTOM_VAR_CACHE_TABLE);
      for (int i = 1; i <= 5; i++)
      {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put("event_id", Integer.valueOf(0));
        localContentValues.put("cv_index", Integer.valueOf(i));
        localContentValues.put("cv_name", "");
        localContentValues.put("cv_scope", Integer.valueOf(3));
        localContentValues.put("cv_value", "");
        paramSQLiteDatabase.insert("custom_var_cache", "event_id", localContentValues);
      }
    }

    private void createECommerceTables(SQLiteDatabase paramSQLiteDatabase)
    {
      paramSQLiteDatabase.execSQL("DROP TABLE IF EXISTS transaction_events;");
      paramSQLiteDatabase.execSQL(PersistentEventStore.CREATE_TRANSACTION_EVENTS_TABLE);
      paramSQLiteDatabase.execSQL("DROP TABLE IF EXISTS item_events;");
      paramSQLiteDatabase.execSQL(PersistentEventStore.CREATE_ITEM_EVENTS_TABLE);
    }

    public void onUpgrade(SQLiteDatabase paramSQLiteDatabase, int paramInt1, int paramInt2)
    {
      if ((paramInt1 < 2) && (paramInt2 > 1))
        createCustomVariableTables(paramSQLiteDatabase);
      if ((paramInt1 < 3) && (paramInt2 > 2))
        createECommerceTables(paramSQLiteDatabase);
    }

    public void onDowngrade(SQLiteDatabase paramSQLiteDatabase, int paramInt1, int paramInt2)
    {
      Log.w("GoogleAnalyticsTracker", "Downgrading database version from " + paramInt1 + " to " + paramInt2 + " not recommended.");
    }
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.PersistentEventStore
 * JD-Core Version:    0.6.0
 */
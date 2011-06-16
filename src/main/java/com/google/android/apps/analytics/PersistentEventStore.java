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

  PersistentEventStore(DataBaseHelper databaseHelper)
  {
    this.databaseHelper = databaseHelper;
    try
    {
      databaseHelper.getWritableDatabase().close();
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
  }

  public void deleteEvent(long eventId)
  {
    String str = "event_id=" + eventId;
    try
    {
      SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
      if (database.delete("events", str, null) != 0)
      {
        this.numStoredEvents -= 1;
        database.delete("custom_variables", str, null);
        database.delete("transaction_events", str, null);
        database.delete("item_events", str, null);
      }
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
  }

  public Event[] peekEvents()
  {
    return peekEvents(1000);
  }

  public Event[] peekEvents(int cantEvents)
  {
    ArrayList<Event> events = new ArrayList<Event>();
    Cursor cursor = null;
    try
    {
      SQLiteDatabase database = this.databaseHelper.getReadableDatabase();
      cursor = database.query("events", null, null, null, null, null, "event_id", Integer.toString(cantEvents));
      while (cursor.moveToNext())
      {
        Event event = new Event(cursor.getLong(0), cursor.getInt(1), cursor.getString(2), cursor.getInt(3), cursor.getInt(4), cursor.getInt(5), cursor.getInt(6), cursor.getInt(7), cursor.getString(8), cursor.getString(9), cursor.getString(10), cursor.getInt(11), cursor.getInt(12), cursor.getInt(13));
        long eventId = cursor.getLong(cursor.getColumnIndex("event_id"));
        if ("__##GOOGLETRANSACTION##__".equals(event.category))
        {
          Transaction transaction = getTransaction(eventId);
          if (transaction == null)
            Log.w("GoogleAnalyticsTracker", "missing expected transaction for event " + eventId);
          event.setTransaction(transaction);
        }
        else if ("__##GOOGLEITEM##__".equals(event.category))
        {
          Item item = getItem(eventId);
          if (item == null)
            Log.w("GoogleAnalyticsTracker", "missing expected item for event " + eventId);
          event.setItem(item);
        }
        else
        {
          CustomVariableBuffer customVariableBuffer = getCustomVariables(eventId);
          event.setCustomVariableBuffer(customVariableBuffer);
        }
        events.add(event);
      }
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
      Event[] emptyEvents = new Event[0];
      
      return emptyEvents;
    }
    finally
    {
      if (cursor != null)
        cursor.close();
    }
    return events.toArray(new Event[events.size()]);
  }

  Transaction getTransaction(long eventId)
  {
    Cursor cursor = null;
    try
    {
      SQLiteDatabase database = this.databaseHelper.getReadableDatabase();
      cursor = database.query("transaction_events", null, "event_id=" + eventId, null, null, null, null);
      if (cursor.moveToFirst())
      {
        String orderId = cursor.getString(cursor.getColumnIndex("order_id"));
		double totalCost = cursor.getDouble(cursor.getColumnIndex("tran_totalcost"));
		String storeName = cursor.getString(cursor.getColumnIndex("tran_storename"));
		double totalTax = cursor.getDouble(cursor.getColumnIndex("tran_totaltax"));
		double shippingCost = cursor.getDouble(cursor.getColumnIndex("tran_shippingcost"));
		Transaction transaction = new Transaction.Builder(orderId, totalCost).setStoreName(storeName).setTotalTax(totalTax).setShippingCost(shippingCost).build();
        return transaction;
      }
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
    finally
    {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  Item getItem(long eventId)
  {
    Cursor cursor = null;
    try
    {
      SQLiteDatabase database = this.databaseHelper.getReadableDatabase();
      cursor = database.query("item_events", null, "event_id=" + eventId, null, null, null, null);
      if (cursor.moveToFirst())
      {
        String orderId = cursor.getString(cursor.getColumnIndex("order_id"));
		String itemSKU = cursor.getString(cursor.getColumnIndex("item_sku"));
		double itemPrice = cursor.getDouble(cursor.getColumnIndex("item_price"));
		long itemCount = cursor.getLong(cursor.getColumnIndex("item_count"));
		String itemName = cursor.getString(cursor.getColumnIndex("item_name"));
		String itemCategory = cursor.getString(cursor.getColumnIndex("item_category"));
		Item item = new Item.Builder(orderId, itemSKU, itemPrice, itemCount).setItemName(itemName).setItemCategory(itemCategory).build();
        return item;
      }
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
    finally
    {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  CustomVariableBuffer getCustomVariables(long eventId)
  {
    Cursor cursor = null;
    CustomVariableBuffer customVariableBuffer = new CustomVariableBuffer();
    try
    {
      SQLiteDatabase database = this.databaseHelper.getReadableDatabase();
      cursor = database.query("custom_variables", null, "event_id=" + eventId, null, null, null, null);
      while (cursor.moveToNext())
      {
        int index = cursor.getInt(cursor.getColumnIndex("cv_index"));
		String name = cursor.getString(cursor.getColumnIndex("cv_name"));
		String value = cursor.getString(cursor.getColumnIndex("cv_value"));
		int scope = cursor.getInt(cursor.getColumnIndex("cv_scope"));
		CustomVariable customVariable = new CustomVariable(index, name, value, scope);
        customVariableBuffer.setCustomVariable(customVariable);
      }
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
    finally
    {
      if (cursor != null)
        cursor.close();
    }
    return customVariableBuffer;
  }

  public String getVisitorCustomVar(int index)
  {
    Cursor cursor = null;
    try
    {
      SQLiteDatabase database = this.databaseHelper.getReadableDatabase();
      cursor = database.query("custom_var_cache", null, "cv_scope = 1 AND cv_index = " + index, null, null, null, null);
      String value = null;
      if (cursor.getCount() > 0)
      {
        cursor.moveToFirst();
        value = cursor.getString(cursor.getColumnIndex("cv_value"));
      }
      else
      {
        value = null;
      }
      database.close();
      return value;
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
      return null;
    }
    finally
    {
      if (cursor != null)
        cursor.close();
    }
  }

  public void putEvent(Event event)
  {
    if (this.numStoredEvents >= 1000)
    {
      Log.w("GoogleAnalyticsTracker", "Store full. Not storing last event.");
      return;
    }
    if (!this.sessionUpdated)
      storeUpdatedSession();
    SQLiteDatabase database = null;
    try
    {
      database = this.databaseHelper.getWritableDatabase();
      database.beginTransaction();
      ContentValues contentValues = new ContentValues();
      contentValues.put("user_id", Integer.valueOf(event.userId));
      contentValues.put("account_id", event.accountId);
      contentValues.put("random_val", Integer.valueOf((int)(Math.random() * 2147483647.0D)));
      contentValues.put("timestamp_first", Long.valueOf(this.timestampFirst));
      contentValues.put("timestamp_previous", Long.valueOf(this.timestampPrevious));
      contentValues.put("timestamp_current", Long.valueOf(this.timestampCurrent));
      contentValues.put("visits", Integer.valueOf(this.visits));
      contentValues.put("category", event.category);
      contentValues.put("action", event.action);
      contentValues.put("label", event.label);
      contentValues.put("value", Integer.valueOf(event.value));
      contentValues.put("screen_width", Integer.valueOf(event.screenWidth));
      contentValues.put("screen_height", Integer.valueOf(event.screenHeight));
      long rowId = database.insert("events", "event_id", contentValues);
      if (rowId != -1L)
      {
        this.numStoredEvents += 1;
        Cursor cursor = database.query("events", new String[] { "event_id" }, null, null, null, null, "event_id DESC", null);
        cursor.moveToPosition(0);
        long eventId = cursor.getLong(0);
        cursor.close();
        if (event.category.equals("__##GOOGLETRANSACTION##__"))
          putTransaction(event, eventId);
        else if (event.category.equals("__##GOOGLEITEM##__"))
          putItem(event, eventId);
        else
          putCustomVariables(event, eventId);
        database.setTransactionSuccessful();
      }
      else
      {
        Log.e("GoogleAnalyticsTracker", "Error when attempting to add event to database.");
      }
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
    finally
    {
      if (database != null)
        database.endTransaction();
    }
  }

  void putTransaction(Event event, long eventId)
  {
    Transaction transaction = event.getTransaction();
    if (transaction == null)
    {
      Log.w("GoogleAnalyticsTracker", "missing transaction details for event " + eventId);
      return;
    }
    try
    {
      SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
      ContentValues contentValues = new ContentValues();
      contentValues.put("event_id", Long.valueOf(eventId));
      contentValues.put("order_id", transaction.getOrderId());
      contentValues.put("tran_storename", transaction.getStoreName());
      contentValues.put("tran_totalcost", transaction.getTotalCost() + "");
      contentValues.put("tran_totaltax", transaction.getTotalTax() + "");
      contentValues.put("tran_shippingcost", transaction.getShippingCost() + "");
      database.insert("transaction_events", "event_id", contentValues);
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
  }

  void putItem(Event event, long eventId)
  {
    Item item = event.getItem();
    if (item == null)
    {
      Log.w("GoogleAnalyticsTracker", "missing item details for event " + eventId);
      return;
    }
    try
    {
      SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
      ContentValues contentValues = new ContentValues();
      contentValues.put("event_id", Long.valueOf(eventId));
      contentValues.put("order_id", item.getOrderId());
      contentValues.put("item_sku", item.getItemSKU());
      contentValues.put("item_name", item.getItemName());
      contentValues.put("item_category", item.getItemCategory());
      contentValues.put("item_price", item.getItemPrice() + "");
      contentValues.put("item_count", item.getItemCount() + "");
      database.insert("item_events", "event_id", contentValues);
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
  }

  void putCustomVariables(Event event, long eventId)
  {
    try
    {
      SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
      CustomVariableBuffer eventCustomVariableBuffer = event.getCustomVariableBuffer();
      
      if (this.useStoredVisitorVars)
      {
        if (eventCustomVariableBuffer == null)
        {
          eventCustomVariableBuffer = new CustomVariableBuffer();
          event.setCustomVariableBuffer(eventCustomVariableBuffer);
        }
        CustomVariableBuffer visitorVariableBuffer = getVisitorVarBuffer();
        for (int j = 1; j <= 5; j++)
        {
          CustomVariable visitorCustomVariable = visitorVariableBuffer.getCustomVariableAt(j);
          CustomVariable eventCustomVariable = eventCustomVariableBuffer.getCustomVariableAt(j);
          if ((visitorCustomVariable == null) || (eventCustomVariable != null))
            continue;
          eventCustomVariableBuffer.setCustomVariable(visitorCustomVariable);
        }
        this.useStoredVisitorVars = false;
      }
      if (eventCustomVariableBuffer != null)
        for (int i = 1; i <= 5; i++)
        {
          if (eventCustomVariableBuffer.isIndexAvailable(i))
            continue;
          CustomVariable customVariable = eventCustomVariableBuffer.getCustomVariableAt(i);
          ContentValues contentValues = new ContentValues();
          contentValues.put("event_id", Long.valueOf(eventId));
          contentValues.put("cv_index", Integer.valueOf(customVariable.getIndex()));
          contentValues.put("cv_name", customVariable.getName());
          contentValues.put("cv_scope", Integer.valueOf(customVariable.getScope()));
          contentValues.put("cv_value", customVariable.getValue());
          database.insert("custom_variables", "event_id", (ContentValues)contentValues);
          database.update("custom_var_cache", (ContentValues)contentValues, "cv_index=" + customVariable.getIndex(), null);
        }
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
  }

  CustomVariableBuffer getVisitorVarBuffer()
  {
    CustomVariableBuffer customVariableBuffer = new CustomVariableBuffer();
    try
    {
      SQLiteDatabase database = this.databaseHelper.getReadableDatabase();
      Cursor cursor = database.query("custom_var_cache", null, "cv_scope=1", null, null, null, null);
      while (cursor.moveToNext())
      {
        int index = cursor.getInt(cursor.getColumnIndex("cv_index"));
		String name = cursor.getString(cursor.getColumnIndex("cv_name"));
		String value = cursor.getString(cursor.getColumnIndex("cv_value"));
		int scope = cursor.getInt(cursor.getColumnIndex("cv_scope"));
		CustomVariable customVariable = new CustomVariable(index, name, value, scope);
        customVariableBuffer.setCustomVariable(customVariable);
      }
      cursor.close();
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
    return customVariableBuffer;
  }

  public int getNumStoredEvents()
  {
    try
    {
      if (this.compiledCountStatement == null)
        this.compiledCountStatement = this.databaseHelper.getReadableDatabase().compileStatement("SELECT COUNT(*) from events");
      return (int)this.compiledCountStatement.simpleQueryForLong();
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
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
    Cursor cursor = null;
    try
    {
      SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
      cursor = database.query("session", null, null, null, null, null, null);
      if (!cursor.moveToFirst())
      {
        long now = System.currentTimeMillis() / 1000L;
        this.timestampFirst = now;
        this.timestampPrevious = now;
        this.timestampCurrent = now;
        this.visits = 1;
        this.storeId = (new SecureRandom().nextInt() & 0x7FFFFFFF);
        ContentValues contentValues = new ContentValues();
        contentValues.put("timestamp_first", Long.valueOf(this.timestampFirst));
        contentValues.put("timestamp_previous", Long.valueOf(this.timestampPrevious));
        contentValues.put("timestamp_current", Long.valueOf(this.timestampCurrent));
        contentValues.put("visits", Integer.valueOf(this.visits));
        contentValues.put("store_id", Integer.valueOf(this.storeId));
        database.insert("session", "timestamp_first", contentValues);
      }
      else
      {
        this.timestampFirst = cursor.getLong(0);
        this.timestampPrevious = cursor.getLong(2);
        this.timestampCurrent = (System.currentTimeMillis() / 1000L);
        this.visits = (cursor.getInt(3) + 1);
        this.storeId = cursor.getInt(4);
      }
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
    finally
    {
      if (cursor != null)
        cursor.close();
    }
  }

  void storeUpdatedSession()
  {
    try
    {
      SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
      ContentValues contentValues = new ContentValues();
      contentValues.put("timestamp_previous", Long.valueOf(this.timestampPrevious));
      contentValues.put("timestamp_current", Long.valueOf(this.timestampCurrent));
      contentValues.put("visits", Integer.valueOf(this.visits));
      database.update("session", contentValues, "timestamp_first=?", new String[] { Long.toString(this.timestampFirst) });
      this.sessionUpdated = true;
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
    }
  }

  public void setReferrer(String referrer)
  {
    try
    {
      SQLiteDatabase database = this.databaseHelper.getWritableDatabase();
      ContentValues contentValues = new ContentValues();
      contentValues.put("referrer", referrer);
      database.insert("install_referrer", null, contentValues);
    }
    catch (SQLiteException localSQLiteException)
    {
      Log.e("GoogleAnalyticsTracker", localSQLiteException.toString());
    }
  }

  public String getReferrer()
  {
    Cursor cursor = null;
    try
    {
      SQLiteDatabase database = this.databaseHelper.getReadableDatabase();
      cursor = database.query("install_referrer", new String[] { "referrer" }, null, null, null, null, null);
      if (cursor.moveToFirst())
        return cursor.getString(0);
      else 
    	return null;
    }
    catch (SQLiteException exception)
    {
      Log.e("GoogleAnalyticsTracker", exception.toString());
      return null;
    }
    finally
    {
      if (cursor != null)
        cursor.close();
    }
  }

  static class DataBaseHelper extends SQLiteOpenHelper
  {
    private final int databaseVersion;

    public DataBaseHelper(Context context)
    {
      this(context, "google_analytics.db", 3);
    }

    public DataBaseHelper(Context context, String name)
    {
      this(context, name, 3);
    }

    DataBaseHelper(Context paramContext, String name, int databaseVersion)
    {
      super(paramContext,name,(CursorFactory)null,databaseVersion);
      this.databaseVersion = databaseVersion;
    }

    public void onCreate(SQLiteDatabase database)
    {
      database.execSQL("DROP TABLE IF EXISTS events;");
      database.execSQL(PersistentEventStore.CREATE_EVENTS_TABLE);
      database.execSQL("DROP TABLE IF EXISTS session;");
      database.execSQL(PersistentEventStore.CREATE_SESSION_TABLE);
      database.execSQL("DROP TABLE IF EXISTS install_referrer;");
      database.execSQL("CREATE TABLE install_referrer (referrer TEXT PRIMARY KEY NOT NULL);");
      if (this.databaseVersion > 1)
        createCustomVariableTables(database);
      if (this.databaseVersion > 2)
        createECommerceTables(database);
    }

    void createCustomVariableTables(SQLiteDatabase database)
    {
      database.execSQL("DROP TABLE IF EXISTS custom_variables;");
      database.execSQL(PersistentEventStore.CREATE_CUSTOM_VARIABLES_TABLE);
      database.execSQL("DROP TABLE IF EXISTS custom_var_cache;");
      database.execSQL(PersistentEventStore.CREATE_CUSTOM_VAR_CACHE_TABLE);
      for (int i = 1; i <= 5; i++)
      {
        ContentValues contentValues = new ContentValues();
        contentValues.put("event_id", Integer.valueOf(0));
        contentValues.put("cv_index", Integer.valueOf(i));
        contentValues.put("cv_name", "");
        contentValues.put("cv_scope", Integer.valueOf(3));
        contentValues.put("cv_value", "");
        database.insert("custom_var_cache", "event_id", contentValues);
      }
    }

    private void createECommerceTables(SQLiteDatabase database)
    {
      database.execSQL("DROP TABLE IF EXISTS transaction_events;");
      database.execSQL(PersistentEventStore.CREATE_TRANSACTION_EVENTS_TABLE);
      database.execSQL("DROP TABLE IF EXISTS item_events;");
      database.execSQL(PersistentEventStore.CREATE_ITEM_EVENTS_TABLE);
    }

    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion)
    {
      if ((oldVersion < 2) && (newVersion > 1))
        createCustomVariableTables(database);
      if ((oldVersion < 3) && (newVersion > 2))
        createECommerceTables(database);
    }

    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion)
    {
      Log.w("GoogleAnalyticsTracker", "Downgrading database version from " + oldVersion + " to " + newVersion + " not recommended.");
    }
  }
}

/* Location:           /tmp/GoogleAnalyticsAndroid_1.2/libGoogleAnalytics.jar
 * Qualified Name:     com.google.android.apps.analytics.PersistentEventStore
 * JD-Core Version:    0.6.0
 */
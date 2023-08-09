package org.tensorflow.lite.examples.transfer.api;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHandler extends SQLiteOpenHelper {

    private static final String DB_NAME = "FL_log_database";
    private static final int DB_VERSION = 2;

    private static final String TABLE_NAME1 = "ROUND";
    private static final String TABLE_NAME2 = "EPOCH";

    private static final String TABLE_NAME3 = "BATCH";

    // for run and epoch
    private static final String ID_COL1 = "RUN_ID";
    private static final String ID_COL2 = "EPOCH_ID";
    private static final String ID_COL3 = "BATCH_ID";

    // for run and epoch
    private static final String START_TIME = "START_TIME";
    private static final String END_TIME = "END_TIME";

    // for every run
    private static final String BATTERY_STATE_START = "BATTERY_STATE_START";
    private static final String BATTERY_STATE_END = "BATTERY_STATE_END";
    private static final String ACCURACY = "ACCURACY";

    // for epoch
    private static final String CPU_UTILIZATION = "CPU_UTILIZATION";
    private static final String MEMORY_CONSUMPTION = "MEMORY_CONSUMPTION";
    private static final String CPU_MAX = "CPU_MAX";
    private static final String MEMORY_MAX = "MEMORY_MAX";
    private static final String BATTERY_DIFF = "BATTERY_DIFF";
    private static final String TIME_OF_FRWD_PASS = "TIME_OF_FRWD_PASS";
    private static final String TIME_OF_BKWD_PASS = "TIME_OF_BKWD_PASS";
    private static final String OPTIMIZER_STEP = "OPTIMIZER_STEP";
    private static final String LOSS = "LOSS";

    // creating a constructor for our database handler.
    public DBHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query1 = "CREATE TABLE " + TABLE_NAME1 + " ("
                + ID_COL1 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + START_TIME + " TEXT,"
                + END_TIME + " TEXT,"
                + BATTERY_STATE_START + " DOUBLE,"
                + BATTERY_STATE_END + " DOUBLE,"
                + ACCURACY + " DOUBLE)";

        String query2 = "CREATE TABLE " + TABLE_NAME2 + " ("
                + ID_COL2 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + START_TIME + " TEXT,"
                + END_TIME + " TEXT)";

        String query3 = "CREATE TABLE " + TABLE_NAME3 + " ("
                + ID_COL3 + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + START_TIME + " TEXT,"
                + END_TIME + " TEXT,"
                + CPU_UTILIZATION + " DOUBLE,"
                + MEMORY_CONSUMPTION + " DOUBLE,"
                + CPU_MAX + " DOUBLE,"
                + MEMORY_MAX + " DOUBLE,"
                + BATTERY_DIFF + " DOUBLE,"
                + TIME_OF_FRWD_PASS + " TEXT,"
                + TIME_OF_BKWD_PASS + " TEXT,"
                + OPTIMIZER_STEP + " TEXT,"
                + LOSS + " DOUBLE)";

        db.execSQL(query1);
        db.execSQL(query2);
        db.execSQL(query3);
    }

    public String readDB(){
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM RUN_ID;", null);
        c.moveToFirst();
        String s = c.getString(51);
        c.close();
        return s;
    }

    public void addNewRound(String startTime, String endTime, float batStart, float batEnd, float acc) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(START_TIME, startTime);
        values.put(END_TIME, endTime);
        values.put(BATTERY_STATE_START, batStart);
        values.put(BATTERY_STATE_END, batEnd);
        values.put(ACCURACY, acc);

        db.insert(TABLE_NAME1, null, values);

        //db.close();
    }

    public void addNewEpoch(String startTime, String endTime) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(START_TIME, startTime);
        values.put(END_TIME, endTime);

        db.insert(TABLE_NAME2, null, values);

    }

    public void addNewBatch(String startTime, String endTime, float cpu, float mem, float cpu_max,
                            float mem_max, float bat, String frwd, String bkwd, String opt, float loss) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(START_TIME, startTime);
        values.put(END_TIME, endTime);
        values.put(CPU_UTILIZATION, cpu);
        values.put(MEMORY_CONSUMPTION, mem);
        values.put(CPU_MAX, cpu_max);
        values.put(MEMORY_MAX, mem_max);
        values.put(BATTERY_DIFF, bat);
        values.put(TIME_OF_FRWD_PASS, frwd);
        values.put(TIME_OF_BKWD_PASS, bkwd);
        values.put(OPTIMIZER_STEP, opt);
        values.put(LOSS, loss);

        db.insert(TABLE_NAME3, null, values);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME3);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME2);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME1);
        onCreate(db);
    }
}

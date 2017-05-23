package com.sense.geo.geosense.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.sense.geo.geosense.model.dynamic.ConstantsCollection;
import com.sense.geo.geosense.model.dynamic.DB_BASIC;
import com.sense.geo.geosense.model.dynamic.DateUtils;
import com.sense.geo.geosense.model.dynamic.ReflectionUtils;
import com.sense.geo.geosense.model.tables.location_track;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by anoop.r on 18/05/17.
 */

public class DBHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "testapp.db";
    public static final int DB_VERSION = 1;
    private static final String LOG_TAG = "DBHelper";

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        CreateTable(new location_track(),sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }


    //dynamic table creation by using Model(class objects)
    public void CreateTable(DB_BASIC object, SQLiteDatabase db) {
        if (object != null) {

            try {
                if (db == null)
                    db = getWritableDatabase();

                Class<? extends DB_BASIC> c = object.getClass();
                String tableName = c.getName();

                tableName = ReflectionUtils.GetClassName(c);

                //Fields of the object
                Field[] fields = c.getDeclaredFields();

                ArrayList<Field> rawFieldList = new ArrayList<>(Arrays.asList(fields));
                List<Field> fieldList = new LinkedList<>();

//                int changeIndex;
//                if ((changeIndex = fieldList.indexOf("$change"))!=-1) {
//                    fieldList.remove(changeIndex);
//                }
                for (Field f : rawFieldList) {
                    if (!f.getName().startsWith("KEY_") && !f.getName().contains("$change")) {
                        fieldList.add(f);
                    }
                }
                StringBuilder sbCreateTable = new StringBuilder();

                //Beginning of the CREATE raw query
                sbCreateTable.append(ConstantsCollection.SQLITE_CREATE_TABLE_IF_NOT_EXISTS);
                sbCreateTable.append(tableName);
                sbCreateTable.append(ConstantsCollection.SQLITE_OPENNING_BRACKET);

                //Iterates on the given object fields using reflection
                //and creates appropriate column definition
                int fieldCount = fieldList.size();
                for (int i = 0; i < fieldCount; i++) {
                    Field currentField = fieldList.get(i);
                    String fieldName = currentField.getName();
                    currentField.setAccessible(true);
                    //Skip all psfs fields, and residual fields in debug mode
                    /*if (currentField.getName().startsWith("KEY_") || currentField.getName().contains("$change")) {
                        continue;
                    }*/
                    if (fieldName.equalsIgnoreCase(ConstantsCollection.ID)) {//Creates an auto increment index named ID
                        if (tableName.startsWith("android_")) {
                            sbCreateTable.append(fieldName);
                            sbCreateTable.append(ConstantsCollection.SQLITE_INTEGER_PRIMARY_KEY_AUTOINCREMENT);
                        } else {
                            sbCreateTable.append(fieldName);
                            sbCreateTable.append(ConstantsCollection.SQLITE_INTEGER_PRIMARY_KEY);
                        }
                    } else {//Creates column declaration
                        String rowname = GetSqliteType(currentField.getType());

                        if (rowname != null) {
                            sbCreateTable.append(fieldName);
                            sbCreateTable.append(ConstantsCollection.SQLITE_SPACE);
                            sbCreateTable.append(rowname);
                        }
                    }

                    if (i != fieldCount - 1) {//Always adds , in the end of each column declaration except the last one
                        sbCreateTable.append(ConstantsCollection.SQLITE_COMMA);
                        sbCreateTable.append(ConstantsCollection.SQLITE_SPACE);
                    }
                }

                //Closing raw CREATE Query with }; characters
                sbCreateTable.append(ConstantsCollection.SQLITE_CLOSING_BRACKET);
                sbCreateTable.append(ConstantsCollection.SQLITE_CLOSING_SEMICOLUMN);

                String createTableSql = sbCreateTable.toString();
//            Log.e(LOG_TAG, createTableSql);
                //Executes raw SQlite statement
                db.execSQL(createTableSql);
            } catch (SecurityException e) {
                Log.e(LOG_TAG, e.toString());
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            } finally {
                //Closing the DB connection
//                CloseDB(db);
            }
        }
    }


    /**
     * Adds given object to the database, by its class name. Perform INSERT Sqlite operation
     *
     * @param object to be inserted
     * @return id of the inserted object
     */
    public long AddNewObject(DB_BASIC object) {
        long result = ConstantsCollection.INDEX_NOT_DEFINED;
        if (object != null) {
            SQLiteDatabase db = null;
            try {
                db = getWritableDatabase();

                ContentValues values = new ContentValues();
                Class<? extends DB_BASIC> c = object.getClass();
                Field[] fields = c.getDeclaredFields();

                //Iterates on object's members
                for (Field field : fields) {
                    field.setAccessible(true);

                    //Skip all psfs fields
                    if (field.getName().startsWith("KEY_") || field.getName().contains("serialVersionUID")) {
                        continue;
                    }
                    Object val = GetValue(field, object);

                    if (val != null) {
                        String rawValue = null;
                        if (field.getType().equals(Date.class)) {
                            try {
                                rawValue = DateUtils.DateToValue((Date) val);
                            } catch (ParseException e) {
                                Log.e(LOG_TAG, e.toString());
                            }
                        } else {
                            rawValue = val.toString();
                        }

                        String name = field.getName();

                        //if value of id is 0 then skip i.e. dummy row from web service
                        if (name.equals("id") && (rawValue == null || rawValue.equals("0"))) {
                            continue;
                        }
                        values.put(name, rawValue);
                    }
                }

                String tableName = ReflectionUtils.GetClassName(object.getClass());

                if (values.size() > 0) {
                    result = db.insertWithOnConflict(tableName, null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            } finally {
                CloseDB(db);
            }
        }

        return result;
    }

    /**
     * Gets the value of the fields in specified object using reflection
     *
     * @return the value of the field
     */
    private Object GetValue(Field field, DB_BASIC object) {
        Object result = null;
        try {
            result = field.get(object);
        } catch (IllegalAccessException e1) {
            Log.e(LOG_TAG, e1.toString());
        } catch (IllegalArgumentException e1) {
            Log.e(LOG_TAG, e1.toString());
        }
        return result;
    }

    /**
     * Gets all data from specified table by class instance
     *
     * @return null if no objects are located, List<DB_BASIC> there are records
     */
    public List<DB_BASIC> GetTableData(Class<? extends DB_BASIC> clazz) {
        List<DB_BASIC> list;
        String tableName = ReflectionUtils.GetClassName(clazz);

        SQLiteDatabase db = null;
        try {
            db = this.getWritableDatabase();
            String[] columns = null;

            Cursor cursor = db.query(tableName, columns, null, null, null, null, null);

            list = ConvertCursorToObjects(cursor, clazz);
        } finally {
            CloseDB(db);
        }
        return list;
    }

    /**
     * Converts cursor to List of objects
     *
     * @param cursor database cursor
     * @param clazz  the desired clazz
     * @return converted cursor object to List collection
     **/
    @SuppressWarnings("unchecked")
    private <T> List<T> ConvertCursorToObjects(Cursor cursor, Class<? extends DB_BASIC> clazz) {
        List<T> list = new ArrayList<T>();

        //moves the cursor to the first row
        if (cursor.moveToFirst()) {
            String[] ColumnNames = cursor.getColumnNames();
            do {
                Object obj = ReflectionUtils.GetInstance(clazz);

                //iterates on column names
                for (int i = 0; i < ColumnNames.length; i++) {
                    try {

                        Field field = obj.getClass().getField(ColumnNames[i]);
                        Object objectValue = null;
                        String str = cursor.getString(i);

                        if (str != null) {
                            //Converting stored Sqlite data to java objects
                            if (field.getType().equals(Date.class)) {
                                Date date = DateUtils.ValueToDate(str);
                                objectValue = date;
                                field.set(obj, objectValue);
                            } else if (field.getType().equals(Number.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                            } else if (field.getType().equals(Long.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                                long value = Long.parseLong(objectValue.toString());
                                field.set(obj, value);
                            } else if (field.getType().equals(Integer.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                                int value = Integer.parseInt(str);
                                field.set(obj, value);
                            } else if (field.getType().equals(Double.class)) {
                                objectValue = NumberFormat.getInstance().parse(str);
                                double value = Double.parseDouble(objectValue.toString());
                                field.set(obj, value);
                            } else {
                                objectValue = str;
                                field.set(obj, objectValue);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(LOG_TAG, e.toString());
                    } catch (IllegalAccessException e) {
                        Log.e(LOG_TAG, e.toString());
                    } catch (ParseException e) {
                        Log.e(LOG_TAG, e.toString());
                    } catch (SecurityException e) {
                        Log.e(LOG_TAG, e.toString());
                    } catch (NoSuchFieldException e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                }

                if (obj instanceof DB_BASIC) {
                    list.add((T) obj);
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * Finds appropriate Sqlite raw string class to given java class
     *
     * @return Sqlite row format
     */
    private String GetSqliteType(Class<?> c) {
        String type = "TEXT";

        if (c.equals(String.class)) {
            type = ConstantsCollection.SQLITE_TEXT;
        } else if (c.equals(Integer.class)
                || c.equals(Long.class)
                || c.equals(Number.class)
                || c.equals(Date.class)) {
            type = ConstantsCollection.SQLITE_INTEGER;
        } else if (c.equals(Double.class)) {
            type = ConstantsCollection.SQLITE_DOUBLE;
        }
        return type;
    }

    /**
     * Closes database connection
     *
     * @param db database reference
     */
    private void CloseDB(SQLiteDatabase db) {
        try {
            if (db != null) {
                db.close();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        }

    }

    /**
     * ---------------------------------------------------------------------------------------------
     * database manager : >>
     */
    public ArrayList<Cursor> getData(String Query) {
        //get writable database
        SQLiteDatabase sqlDB = this.getWritableDatabase();
        String[] columns = new String[]{"mesage"};
        //an array list of cursor to save two cursors one has results from the query
        //other cursor stores error message if any errors are triggered
        ArrayList<Cursor> alc = new ArrayList<Cursor>(2);
        MatrixCursor Cursor2 = new MatrixCursor(columns);
        alc.add(null);
        alc.add(null);


        try {
            String maxQuery = Query;
            //execute the query results will be save in Cursor c
            Cursor c = sqlDB.rawQuery(maxQuery, null);

            //add value to cursor2
            Cursor2.addRow(new Object[]{"Success"});

            alc.set(1, Cursor2);
            if (null != c && c.getCount() > 0) {


                alc.set(0, c);
                c.moveToFirst();

                return alc;
            }
            return alc;
        } catch (SQLException sqlEx) {
            Log.d("printing exception", sqlEx.getMessage());
            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[]{"" + sqlEx.getMessage()});
            alc.set(1, Cursor2);
            return alc;
        } catch (Exception ex) {

            Log.d("printing exception", ex.getMessage());

            //if any exceptions are triggered save the error message to cursor an return the arraylist
            Cursor2.addRow(new Object[]{"" + ex.getMessage()});
            alc.set(1, Cursor2);
            return alc;
        }


    }


    ////////////user functions : >>>>>

    public void clearLocTrack() {
        SQLiteDatabase db = getWritableDatabase();
        String query = "DELETE FROM " + location_track.KEY_TABLE_NAME;

        db.execSQL(query);
    }
}

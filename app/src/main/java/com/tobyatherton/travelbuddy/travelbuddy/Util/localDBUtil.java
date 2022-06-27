package com.tobyatherton.travelbuddy.travelbuddy.Util;

/**
 * https://www.tutorialspoint.com/android/android_sqlite_database.htm
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

public class localDBUtil extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "localStorage.db";
    public static final String TABLE_NAME = "geoStats";
    public static final String GEOSTATS_COLUMN_ID = "id";
    public static final String GEOSTATS_COLUMN_LASTADDRESS = "lastaddress";
    public static final String GEOSTATS_COLUMN_TOPSPEED = "topspeed";
    public static final String GEOSTATS_COLUMN_HIGHESTALTITUDE = "highestaltitude";
    public static final String GEOSTATS_COLUMN_LOWESTALTITUDE = "lowestaltitude";
    public static final String GEOSTATS_COLUMN_DISTANCETRAVELLED = "distancetravelled";
    public static final String GEOSTATS_COLUMN_DISTANCETRAVELLEDONTRIP = "distancetravelledontrip";
    public static final String GEOSTATS_COLUMN_TOTALSTEPS = "totalsteps";
    public static final String GEOSTATS_COLUMN_HIGHESTSTEPSONJOURNEY = "higheststepsonjourney";
    public static final String GEOSTATS_COLUMN_TOTALCOUNTRIES = "totalcountries";
    public static final String GEOSTATS_COLUMN_CURRENTCOUNTRY = "currentcountry";
    private HashMap hp;
    private SQLiteDatabase mDB = null;

    public localDBUtil(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }



    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table "+ TABLE_NAME +
                        "(id integer primary key AUTOINCREMENT, lastaddress text,topspeed text,highestaltitude text, lowestaltitude text,distancetravelled text, distancetravelledontrip text, totalsteps text,higheststepsonjourney text)"
        );

        db.execSQL("CREATE TABLE user_saves (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, data TEXT);");

        db.execSQL("CREATE TABLE user_friends (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, phonenumber TEXT);");

        db.execSQL("CREATE TABLE user_friendsRequests (_id INTEGER PRIMARY KEY AUTOINCREMENT, id TEXT, name TEXT, phonenumber TEXT);");

        setDB(db);
        //insertUser("4 test road", "36","25","5","500");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);

        db.execSQL("DROP TABLE IF EXISTS user_saves"); //add variable for table name

        db.execSQL("DROP TABLE IF EXISTS user_friends");

        db.execSQL("DROP TABLE IF EXISTS user_friendsRequests");

        onCreate(db);
    }

    public boolean insertUser(String name, String phone, String email, String street, String place) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(GEOSTATS_COLUMN_LASTADDRESS, name);
        contentValues.put(GEOSTATS_COLUMN_TOPSPEED, phone);
        contentValues.put(GEOSTATS_COLUMN_HIGHESTALTITUDE, email);
        contentValues.put(GEOSTATS_COLUMN_LOWESTALTITUDE, street);
        contentValues.put(GEOSTATS_COLUMN_DISTANCETRAVELLED, place);
        db.insert(TABLE_NAME,null , contentValues);
        return true;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from "+TABLE_NAME+" where id=" + id + "", null );
        return res;
    }

    public Cursor getSpecificCol(int id , String colName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  getReadableDatabase().rawQuery( "select " + colName + " from " + TABLE_NAME + " where id=" + id + "", null );
        return res;
    }

    public boolean updateSpecificCol(int id , String colName, String colVal) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("\""+colName+"\"", colVal); //wrong here
        db.update(TABLE_NAME, contentValues, "id = ? ", new String[] { Integer.toString(id) } );
        return true;
    }

    public String cursorToString(Cursor cursor, String colName) {
        String processedCursor;
        if (cursor.moveToFirst()) {
            processedCursor = cursor.getString(cursor.getColumnIndex(colName));
            return processedCursor;
        }
        return null;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        return numRows;
    }

    public boolean updateContact (Integer id, String name, String phone, String email, String street,String place) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(GEOSTATS_COLUMN_LASTADDRESS, name);
        contentValues.put(GEOSTATS_COLUMN_TOPSPEED, phone);
        contentValues.put(GEOSTATS_COLUMN_HIGHESTALTITUDE, email);
        contentValues.put(GEOSTATS_COLUMN_LOWESTALTITUDE, street);
        contentValues.put("place", place);
        db.update(TABLE_NAME, contentValues, "id = ? ", new String[] { Integer.toString(id) } );
        return true;
    }

    public Integer deleteContact (Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME,
                "id = ? ",
                new String[] { Integer.toString(id) });
    }

    public void setDB(SQLiteDatabase db){
        mDB = db;
    }

    public SQLiteDatabase getDB(){
        return mDB;
    }

    public ArrayList<String> getAllCotacts() {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from " + TABLE_NAME + "", null );
        res.moveToFirst();

        while(res.isAfterLast() == false){
            array_list.add(res.getString(res.getColumnIndex(GEOSTATS_COLUMN_LASTADDRESS)));
            res.moveToNext();
        }
        return array_list;
    }

    //=================================== save methods ======================================================

    public void addSave(String name,String data)
    {

        ContentValues values=new ContentValues(2);

        values.put("name", name);

        values.put("data", data);

        getWritableDatabase().insert("user_saves", "name", values);

    }

    public void addFriend(String name,String phonenumber)
    {

        ContentValues values=new ContentValues(2);

        values.put("name", name);

        values.put("phonenumber", phonenumber);

        getWritableDatabase().insert("user_friends", "name", values);

    }

    public void addFriendRequest(String id, String name,String phonenumber)
    {

        ContentValues values=new ContentValues(2);

        values.put("id", id);

        values.put("name", name);

        values.put("phonenumber", phonenumber);

        getWritableDatabase().insert("user_friendsRequests", "name", values);

    }

    public Cursor getSaves()

    {

        Cursor cursor = getReadableDatabase().rawQuery("select * from user_saves", null);

        return cursor;

    }

    public Cursor getFriends()

    {

        Cursor cursor = getReadableDatabase().rawQuery("select * from user_friends", null);

        return cursor;

    }

    public Cursor getFriendsRequests()

    {

        Cursor cursor = getReadableDatabase().rawQuery("select * from user_friendsRequests", null);

        return cursor;

    }

    public Cursor getSaveSpecific()

    {

        Cursor cursor = getReadableDatabase().query("user_saves",

                new String[]{"_id", "name", "data"},

                null, null, null, null, null);

        return cursor;

    }

    public void deleteAllFriends()

    {

        getWritableDatabase().delete("user_friends", null, null);

    }

    public void deleteAllUsers()

    {

        getWritableDatabase().delete("user_saves", null, null);

    }

    public void deleteAllFriendsRequests()

    {

        getWritableDatabase().delete("user_friendsRequests", null, null);

    }

    public void deleteSave(String data)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete("user_saves","data=?",new String[]  {data});
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            db.close();
        }

    }

    public void deleteFriend(String data)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete("user_friends","data=?",new String[]  {data});
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            db.close();
        }

    }

    public void deleteFriendRequest(String data)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete("user_friendsRequests","data=?",new String[]  {data});
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            db.close();
        }

    }

    //possibly not right
    public ArrayList<String> getAllSaves() {

        Cursor AllSaves = getSaves();
        ArrayList<String> testarr = new ArrayList<String>();
        AllSaves.moveToFirst();
        String name = "";
        String data = "";
        String current = "";
        int count = 0;
        while (!AllSaves.isAfterLast()) {

            name = AllSaves.getString(1);

            data = AllSaves.getString(2);

            current = "Save Number: "+ count + "\nSave Name: " + name + "\nData: " +data+"\n";

            testarr.add(current);

            AllSaves.moveToNext();
            count++;
        }
        return testarr;
    }

    public ArrayList<String> getAllFriends() {

        Cursor AllFriends = getFriends();
        ArrayList<String> testarr = new ArrayList<String>();
        AllFriends.moveToFirst();
        String name = "";
        String phonenumber = "";
        String current = "";
        int count = 0;
        while (!AllFriends.isAfterLast()) {

            name = AllFriends.getString(1);

            phonenumber = AllFriends.getString(2);

            current = "Friend Number: "+ count + "\nFriend Name: " + name + "\nPhone Number: " +phonenumber+"\n";

            testarr.add(current);

            AllFriends.moveToNext();
            count++;
        }
        return testarr;
    }

    public ArrayList<String> getAllFriendsRequests() {

        Cursor AllFriendsRequests = getFriendsRequests();
        ArrayList<String> testarr = new ArrayList<String>();
        AllFriendsRequests.moveToFirst();
        String id = "";
        String name = "";
        String phonenumber = "";
        String current = "";
        int count = 0;
        while (!AllFriendsRequests.isAfterLast()) {

            id = AllFriendsRequests.getString(1);

            name = AllFriendsRequests.getString(2);

            phonenumber = AllFriendsRequests.getString(3);

            current = "Friend Number: "+ count + "\nFriend ID: " + id + "\nFriend Name: " + name + "\nPhone Number: " +phonenumber+"\n";

            testarr.add(current);

            AllFriendsRequests.moveToNext();
            count++;
        }
        return testarr;
    }

    public Cursor getSpecificRequest(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  getReadableDatabase().rawQuery( "select * from user_friendsRequests where id=" + id + "", null );
        return res;
    }

    public ArrayList<String> getFriendsRequestsIDs() {

        Cursor AllFriendsRequests = getFriendsRequests();
        ArrayList<String> testarr = new ArrayList<String>();
        AllFriendsRequests.moveToFirst();
        String id = "";
        String current = "";
        int count = 0;
        while (!AllFriendsRequests.isAfterLast()) {

            id = AllFriendsRequests.getString(1);

            current = id;

            testarr.add(current);

            AllFriendsRequests.moveToNext();
            count++;
        }
        return testarr;
    }
}
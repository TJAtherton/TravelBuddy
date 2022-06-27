package com.tobyatherton.travelbuddy.travelbuddy;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.tobyatherton.travelbuddy.travelbuddy.Util.EncryptionUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;



public class JourneyUtil extends Activity {

    private static JourneyUtil singleton = new JourneyUtil();

    int count = 1;
    Calendar calendar;
    SimpleDateFormat sdf;
    String formattedDate;

    public void setStart(String start) { //move these
        this.start = start;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public String getEnd() {
        return end;
    }

    String start = ""; //make encapsulated
    String end = "";

    //private static final String TAG = JourneyUtil.class.getName();
    //private static final String FILENAME = "myFile.txt";
    //private File myFile = null;
    //private InputStream is;

    File path = new File(Environment.getExternalStorageDirectory() +
            File.separator + "Travel_Buddy");
	File file = new File(path, "Saved_Journeys.txt");
    boolean success = false;

    File publicKeyData = new File("public.der");
    File encryptFile = new File("encrypt.data");
    File privateKeyFile = new File("private.der");
    File secureFile = new File("secure.data");
    EncryptionUtil secure; //test
    ProgressDialog progress;

    //test
    ArrayList<Date> dateTime;

    private static final String TAG = "JourneyUtil";

    /* A private Constructor prevents any other
    * class from instantiating.
    */
    public JourneyUtil() { //singleton

    }

    /* Static 'instance' method */
    public static JourneyUtil getInstance( ) {
        return singleton;
    }

    /** Called when the class is first created. */
    protected void Initialise(List data, List time) {

        sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss a");
        calendar = Calendar.getInstance();
        formattedDate = sdf.format(new Time(System.currentTimeMillis()).getTime());

        try {
            secure = new EncryptionUtil(); // test
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }

        //writeFile(data, time); //was below readfile

        if (!path.exists()) {
            success = path.mkdirs();
        } else {
            success = true;
        }
        //if (file != null) {
            try {
                if (success) {
                    writeFile(data, time);
                } else {
                    Toast.makeText(this,"File Creation Error",Toast.LENGTH_LONG).show();
                }
                //return readFile(file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        //}
        //String textToSaveString = "Hello Android";

        //writeToFile(textToSaveString);

        //String textFromFileString =  readFromFile();

        /*if ( textToSaveString.equals(textFromFileString) )
            Toast.makeText(getApplicationContext(), "both string are equal", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getApplicationContext(), "there is a problem", Toast.LENGTH_SHORT).show();*/
            //save to new line in file
        //return null;
    }

/*    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent.getStringExtra("readSingleJourney").contains(",")){

            String[] split = intent.getStringExtra("readSingleJourney").split(",");
            setStart(split[0]);
            setEnd(split[1]);

*//*            try {
                readSingleJourney(start, end);
            } catch (IOException e) {
                e.printStackTrace();
            }*//*
        }
    }*/

    //make a read for 1 journey and all journies, so distances can be calculated
    public ArrayList<LatLng> readFile() throws IOException { //readFile(File file)throws IOException {


        String str = "";
        ArrayList<LatLng> readPoints = new ArrayList<LatLng>();
        dateTime = new ArrayList<Date>();
        Date dateoflocation = null;

        //progress = new ProgressDialog(this);

        //progress.setMessage("Loading Historical Journeys ");
        //progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        //progress.setIndeterminate(true);


        if (file != null) {

            //decrypts the file for reading
            //decryptFile(file);


            String newstr = "";
            Scanner sc = new Scanner(file);
            int mProgress = 0;

            //  progress.setProgress(0);
            //progress.show();
            Date locationDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH); //
            String datetimeoflocation = "";

            while (sc.hasNextLine()) {
                str = sc.nextLine();
                if (str != null) {
                    if (str.startsWith("lat/lng:")) {
                        str = str.replace("lat/lng: (", "");
                        str = str.replace(")", "");
                        //int pos = str.indexOf(',', str.indexOf(',')); // str.indexOf(',') + 1
                        //if (pos > -1) {
                        //    String datetimeoflocation = str.substring(0, pos);
                        //}

                        try {
                            datetimeoflocation = str.substring(str.lastIndexOf(",") + 1);
                            datetimeoflocation = datetimeoflocation.trim();
                            //datetimeoflocation = sdf.format(datetimeoflocation); //maybe not here
                            locationDate = format.parse(datetimeoflocation);

                            dateTime.add(locationDate);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        //String datetimeoflocation = str.substring(str.lastIndexOf(",") + 1); // need to figure out why date is causing problems
                        //java.text.DateFormat localeDateFormat = android.text.format.DateFormat.getDateFormat(this); //get date format based on where user is
                        //datetimeoflocation = datetimeoflocation.trim();
                        //datetimeoflocation = localeDateFormat.format(datetimeoflocation); //maybe not here
                        /*try {
                            dateoflocation = localeDateFormat.parse(datetimeoflocation) ;
                            dateTime.add(dateoflocation);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }*/

                        //int endIndex = str.lastIndexOf(",") + 1;
                        //if (endIndex != -1) {
                        //str = str.substring(0, endIndex);
                        String[] latlngs = str.split(",");
                        //String[] latlngs = str.split("\\s*,\\s*");
                        double lat = Double.parseDouble(latlngs[0]);
                        double lng = Double.parseDouble(latlngs[1]);
                        LatLng loc = new LatLng(lat, lng);
                        readPoints.add(loc);
                        //}
                        mProgress++;
                        //progress.setProgress(mProgress);
                    } /*else if (str.contains("-- End Journey")) {
                        calendar = Calendar.getInstance();
                        format.format(calendar); //end datetime

                    } else if (str.contains("-- Start Journey")) {
                        calendar = Calendar.getInstance();
                        format.format(calendar); //start datetime
                    }*/
                }
            }
            sc.close();




            //get initial base file to read from
            //is = getResources().openRawResource(R.raw.base);
            //InputStream is = new FileInputStream(file);
            //BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            //String line = reader.readLine();


            //while (line != null) {

            //line = reader.readLine();

            //}


            //reader.close();

            //encryptFile(file);

            //progress.dismiss();

            return readPoints;

        } else {

            return null;
        }
    }


    //read single journey
    public ArrayList<LatLng> readSingleJourney(String start, String end) throws IOException { //readFile(File file)throws IOException {


        String str = "";
        ArrayList<LatLng> readPoints = new ArrayList<LatLng>();
        dateTime = new ArrayList<Date>();
        Date dateoflocation = null;
        Boolean bRead = false;

        if (file != null) {

            String newstr = "";
            Scanner sc = new Scanner(file);
            int mProgress = 0;

            Date locationDate = new Date();
            SimpleDateFormat format = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH); //
            String datetimeoflocation = "";

            while (sc.hasNextLine()) {
                str = sc.nextLine();
                if (str != null) {
                    if(str.contains(start)) {
                        bRead = true;
                        //Toast.makeText(getBaseContext(), "bRead = true;", Toast.LENGTH_LONG).show();
                    } else if (str.contains(end)) {
                        bRead = false;
                        //Toast.makeText(getBaseContext(), "bRead = false;", Toast.LENGTH_LONG).show();
                    }
                    if(bRead) {
                        if (str.startsWith("lat/lng:")) {
                            str = str.replace("lat/lng: (", "");
                            str = str.replace(")", "");

                            try {
                                datetimeoflocation = str.substring(str.lastIndexOf(",") + 1);
                                datetimeoflocation = datetimeoflocation.trim();
                                locationDate = format.parse(datetimeoflocation);

                                dateTime.add(locationDate);
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            String[] latlngs = str.split(",");
                            double lat = Double.parseDouble(latlngs[0]);
                            double lng = Double.parseDouble(latlngs[1]);
                            LatLng loc = new LatLng(lat, lng);
                            readPoints.add(loc);
                            //Toast.makeText(getBaseContext(), "HIT!", Toast.LENGTH_LONG).show();
                            //mProgress++;

                        }
                    }
                }
            }
            sc.close();
            return readPoints;

        } else {

            return null;
        }
    }

    public void writeFile(List list, List time) {
        //WRITE RESULTS(whatever they are) INTO TEXT FILE

        try {
            //decrypts the file for reading
            if (file != null) {
                decryptFile(file);
            }
            //for all in one file creation
            //File file = new File("/sdcard/Saved_Journeys.txt");
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file, true);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            //test to check if file is empty
            BufferedReader br = new BufferedReader(new FileReader(file));



            //if (file.length() == 0) {
            if (br.readLine() == null) {
                //if internet connected add the address into this
                myOutWriter.write("//DO NOT DELETE! This File is to store journey data for offline use.\n");
                myOutWriter.write(formattedDate);
                myOutWriter.write("-- Journey: " + count + " --\n");
                for (int i = 0; i < list.size(); i++) {
                    myOutWriter.write(list.get(i).toString()+ "," + time.get(i) +"\n"); //test
                    Log.e(TAG, "write str: " + list.get(i).toString()+ "," + time.get(i) +"\n");
                }
                myOutWriter.write("\n-- End Journey " + count + " --");
                count++;
            } else {
                myOutWriter.write(formattedDate);
                myOutWriter.append("-- Journey: " + count + " --\n");
                for (int i = 0; i < list.size(); i++) {
                    myOutWriter.append(list.get(i).toString() + "," + time.get(i) +"\n"); //test
                    Log.e(TAG, "write str: " + list.get(i).toString()+ "," + time.get(i) +"\n");
                }
                count++;
                myOutWriter.append("-- End Journey " + count + " --");
                count++;
            }

            //for individual file creation
/*            File file = new File("/sdcard/Saved_Journeys_" + count + ".txt");
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            //test to check if file is empty
            BufferedReader br = new BufferedReader(new FileReader("/sdcard/Saved_Journeys_" + count + ".txt"));



            //if (file.length() == 0) {
            if (br.readLine() == null) {
                myOutWriter.write("//DO NOT DELETE! This File is to store journey data for offline use.\n");
                myOutWriter.write("-- Journey: " + count + " --\n");
                for (int i = 0; i < list.size(); i++) {
                    myOutWriter.write(list.get(i).toString()); //test
                }
                myOutWriter.write("\n-- End Journey " + count + " --");
                count++;
            } else {
                myOutWriter.append("-- Journey: " + count + " --\n");
                for (int i = 0; i < list.size(); i++) {
                    myOutWriter.append(list.get(i).toString()); //test
                }
                count++;
                myOutWriter.append("\n-- End Journey " + count + " --");
                count++;
            }*/

            myOutWriter.close();
            fOut.close();

            encryptFile(file); //test

            //myFile = file;
            //Toast.makeText(this, "Done writing SD '"+file.getName()+"'", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            //Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            Log.i(TAG, "WRITE ERROR!" + e.toString());
        }
    }

    public float getDistanceTraveled(List list) {
        float[] dist = new float[1];
        float totalDist = 0;

        for(int i = 0; list.size()-2 >= i; i++) {
            //get lon and lat vaules from list here?
            //double lat1 = list.get(i).getLatitude();
            //double lon1 = list.get(i).getLongitude();
            //double lat2 = list.get(i+1).getLatitude();
            //double lon2 = list.get(i+1).getLongitude();

            //Location.distanceBetween(lat1, lon1, lat2, lon2, dist);
            //totalDist += dist[0];
            //Log.d("", "totaldist " + String.valueOf(totalDist));

        }
        return totalDist;
    }


    //http://www.macs.hw.ac.uk/~ml355/lore/pkencryption.htm
    //http://www.codejava.net/coding/file-encryption-and-decryption-simple-example
    public void encryptFile(File pfile)
    {

        try {
            // create AES key
            secure.makeKey();

            // save AES key using public key
            secure.saveKey(encryptFile, publicKeyData);

            // save original file securely
            secure.encrypt(pfile, secureFile);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public ArrayList<Date> getDateTime() {
        return dateTime;
    }

    public void decryptFile(File pfile)
    {

        try {
            // load AES key
            secure.loadKey(encryptFile, privateKeyFile);

            // decrypt file
            secure.decrypt(secureFile, pfile);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*private boolean createFile() {
        try {

            myFile = new File("Documents/myFile.txt");

            if (myFile.createNewFile()){
                Toast.makeText(getApplicationContext(), "File created", Toast.LENGTH_SHORT).show();
                return true;
            }else{
                Toast.makeText(getApplicationContext(), "File exists", Toast.LENGTH_SHORT).show();
                return false;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }




    private void writeToFile(List data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(openFileOutput(FILENAME, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e(TAG, "File write failed: " + e.toString());
        }

    }

    private String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = openFileInput(FILENAME);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }*/
}